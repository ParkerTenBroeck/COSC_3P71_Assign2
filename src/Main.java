import ga.*;
import util.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

class Main {
    public static void main(String[] args) throws IOException {
        var cli = new CliArgs(args);
        System.out.println(cli.problemSet);

        ensureRunDir();

        var runs = runConfigurations(cli).peek(item -> {
            try {
                Files.write(Paths.get("runs/run"+item.run+".json"), item.toString().getBytes());
                Files.write(Paths.get("runs/run"+item.run+".tex"), LatexGraph.graph(item).getBytes());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        outputLatexStats(runs);
    }

    /**
     * does a statistical analysis over the configuration runs and outputs latex tables for them
     * @param runs  A list of all the runs performed
     */
    private static void outputLatexStats(List<GARuns> runs) throws IOException {
        var runGens = new LatexTable(6);
        var runFitness = new LatexTable(6);
        var runParameters = new LatexTable(6);
        runParameters.entry("");
        runParameters.entry("crossover rate");
        runParameters.entry("mutation rate");
        runParameters.entry("crossover method");
        runParameters.nextRow();

        runGens.entry("");
        runGens.entry("min");
        runGens.entry("max");
        runGens.entry("mean");
        runGens.entry("median");
        runGens.entry("$\\sigma$");
        runGens.nextRow();

        runFitness.entry("");
        runFitness.entry("min");
        runFitness.entry("max");
        runFitness.entry("mean");
        runFitness.entry("median");
        runFitness.entry("$\\sigma$");
        runFitness.nextRow();
        for(var run:runs){
            runGens.entry("config"+run.run);
            runGens.entry(String.format("$%.0f$", run.gen.min));
            runGens.entry(String.format("$%.0f$", run.gen.max));
            runGens.entry(String.format("$%.2f$", run.gen.mean));
            runGens.entry(String.format("$%.1f$", run.gen.median));
            runGens.entry(String.format("$%.3f$", run.gen.std));
            runGens.nextRow();

            runFitness.entry("config"+run.run);
            runFitness.entry(String.format("$%.0f$", run.normalized.min));
            runFitness.entry(String.format("$%.0f$", run.normalized.max));
            runFitness.entry(String.format("$%.2f$", run.normalized.mean));
            runFitness.entry(String.format("$%.1f$", run.normalized.median));
            runFitness.entry(String.format("$%.3f$", run.normalized.std));
            runFitness.nextRow();

            runParameters.entry("config"+run.run);
            runParameters.entry(String.format("$%.0f\\%%$", run.params.crossoverRate*100));
            runParameters.entry(String.format("$%.0f\\%%$", run.params.mutationRate*100));
            runParameters.entry(run.params.crossover.toString());
            runParameters.nextRow();
        }
        runGens.end();
        runFitness.end();
        runParameters.end();

        var zvalue = new LatexTable(runs.size()+1);
        var pvalue = new LatexTable(runs.size()+1);
        var sig = new LatexTable(runs.size()+1);
        sig.entry("");
        zvalue.entry("");
        pvalue.entry("");
        for(int i = 0; i < runs.size(); i++){
            sig.entry("config" + (i+1));
            zvalue.entry("config" + (i+1));
            pvalue.entry("config" + (i+1));
        }
        sig.nextRow();
        zvalue.nextRow();
        pvalue.nextRow();
        for(var run1:runs){
            sig.entry("config"+run1.run);
            zvalue.entry("config"+run1.run);
            pvalue.entry("config"+run1.run);
            for(var run2:runs){
                var meow = new GARuns.ZTest(run1.gen, run2.gen);
                var color = meow.significant?
                        run1.gen.mean>run2.gen.mean?"\\cellcolor{red!25}":"\\cellcolor{green!25}":
                        "\\cellcolor{yellow!25}";
                sig.entry(color+(meow.significant?run1.gen.mean>run2.gen.mean?"$>$":"$<$":"NA"));
                zvalue.entry(String.format("%s$%.2f$", color, meow.z));
                pvalue.entry(String.format("%s$%.2f$", color, meow.p));

            }
            sig.nextRow();
            zvalue.nextRow();
            pvalue.nextRow();
        }
        sig.end();
        zvalue.end();
        pvalue.end();

        Files.write(Path.of("runs/run_params.tex"), runParameters.toString().getBytes());
        Files.write(Path.of("runs/run_gens.tex"), runGens.toString().getBytes());
        Files.write(Path.of("runs/run_fitness.tex"), runFitness.toString().getBytes());
        Files.write(Path.of("runs/compare.tex"), sig.toString().getBytes());
        Files.write(Path.of("runs/zvalues.tex"), zvalue.toString().getBytes());
        Files.write(Path.of("runs/pvalues.tex"), pvalue.toString().getBytes());
    }

    @SuppressWarnings("all")
    static void ensureRunDir() throws IOException {
        try{
            for (File file : new File("runs").listFiles()) {
                file.delete();
            }
        }catch (Exception ignore){}
        Files.deleteIfExists(Paths.get("runs"));
        Files.createDirectory(Paths.get("runs"));
    }

    /**
     * @return A stream of results for all the permutations of parameters given the arguments
     */
    static Stream<GARuns> runConfigurations(CliArgs cli){

        var paramGen = Stream.of(new GAParameters(cli.problemSet))
                .flatMap(flat(cli.crossoverKinds::stream, (t, v) -> t.crossover = v))
            .flatMap(flat(cli.elitismRates::stream, (t, v) -> t.elitismRate = v))
            .flatMap(flat(cli.crossoverRates::stream, (t, v) -> t.crossoverRate = v))
            .flatMap(flat(cli.mutationRates::stream, (t, v) -> t.mutationRate = v))
            .flatMap(flat(cli.populationSizes::stream, (t, v) -> t.populationSize = v))
            .flatMap(flat(cli.initializerKinds::stream, (t, v) -> t.initialize = v))
            .flatMap(flat(cli.selectionKinds::stream, (t, v) -> t.select = v))
            .flatMap(flat(cli.fitnessKinds::stream, (t, v) -> t.fitness = v))
            .flatMap(flat(cli.mutationKinds::stream, (t, v) -> t.mutate = v));

        var runNum = new AtomicInteger(1);
        return paramGen
                .filter(v -> v.mutationRate != 0.01 || v.crossover == GAParameters.CrossoverKind.BestAttempt)
                .map(params -> runConfiguration(params.clone(), runNum.getAndIncrement(), cli.generations, cli.seeds));
    }

    static <T, V> Function<T, Stream<T>> flat(Supplier<Stream<V>> in, BiConsumer<T, V> setter){
        return t -> in.get().map(v -> {
            setter.accept(t, v);
            return t;
        });
    }

    /**
     * @return  The stats for the GA runs from the provided parameters and seeds
     */
    static GARuns runConfiguration(GAParameters params, int runNum, int generations, long[] seeds){
        var stats = new GARuns(params);
        System.out.println("Started run " + runNum);

        var configs = Arrays.stream(seeds).mapToObj(seed -> new Util.Tuple<>(new GA(
                seed, params.problemSet,
                params.elitismRate, params.crossoverRate, params.mutationRate, params.populationSize,
                params.initialize.initializer,
                params.select.selector,
                params.fitness.fitness,
                params.mutate.fitness,
                params.crossover.crossover,
                null
        ), stats.run(seed))). toList();
        configs.stream().map(t -> new Thread(() -> {
            var c = t.t1;
            var run = t.t2;
            var result = c.run(generations);
            run.best = result.result;
            run.finished = params.fitness.fitness.complete(run.best.fitness);
            run.generationStats = result.stats;
            System.out.println("\tFinished seed " + c.seed);
        })).peek(Thread::start).toList().forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        var max = configs.stream().mapToInt(c -> c.t2.generationStats.size()).max().orElse(0);
        var indecies = new int[configs.size()];

        for(int c = 0; c < max; c ++){
            var rawMin = 0.0;
            var rawMax = 0.0;
            var rawAvg = 0.0;
            var norMin = 0.0;
            var norMax = 0.0;
            var norAvg = 0.0;

            var count = configs.size();
            for(int i = 0; i < count; i ++){
                var result = configs.get(i).t2.generationStats.get(indecies[i]);
                indecies[i] = Math.min(indecies[i]+1, configs.get(i).t2.generationStats.size()-1);

                rawMin += result.minRawFit;
                rawMax += result.maxRawFit;
                rawAvg += result.averageRawFit;
                norMin += result.minFit;
                norMax += result.maxFit;
                norAvg += result.averageFit;
            }
            stats.averagedStats.add(new GenerationStat(norMin/count,norMax/count,norAvg/count,rawMin/count,rawMax/count,rawAvg/count));

        }

        stats.run = runNum;
        stats.calculateFinalResults();

        return stats;
    }
}