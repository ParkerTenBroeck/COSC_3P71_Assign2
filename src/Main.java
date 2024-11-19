import ga.*;
import util.AveragedQueue;
import util.GAPopulationGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        var cli = new CliArgs(
//                "--gui",
                "--problem-set", "./data/t1",
                "--generations", "250",
                "--seeds-linear", "30",
                "--elitism-rates", "1",
                "--crossover-rates", "0.9,1.0",
                "--mutation-rates", "0.01,0.1",
                "--population-sizes", "100,500",
                "--initializer-kinds", "RandomInitializer",
                "--selection-kinds", "TournamentSelection",
                "--fitness-kinds", "ConflictsFitness",
                "--mutation-kinds", "SingleGeneMutation",
                "--crossover-kinds", "BestAttemptCrossover,UniformCrossover"
        );
        System.out.println(cli.problemSet);

        ensureRunDir();

        runs(cli).forEach(item -> {
            try {
                Files.write(Paths.get("runs/run"+item.run+".json"), item.toString().getBytes());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("\nFinished everything");
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

    static Stream<GARuns> runs(CliArgs cli){
        GAPopulationGraph window;
        if(cli.gui) {
            window = new GAPopulationGraph();
        }else {
            window = null;
        }

        var paramGen = Stream.of(new GAParameters(cli.problemSet))
            .flatMap(flat(cli.elitismRates::stream, (t, v) -> t.elitismRate = v))
            .flatMap(flat(cli.crossoverRates::stream, (t, v) -> t.crossoverRate = v))
            .flatMap(flat(cli.mutationRates::stream, (t, v) -> t.mutationRate = v))
            .flatMap(flat(cli.populationSizes::stream, (t, v) -> t.populationSize = v))
            .flatMap(flat(cli.initializerKinds::stream, (t, v) -> t.initialize = v))
            .flatMap(flat(cli.selectionKinds::stream, (t, v) -> t.select = v))
            .flatMap(flat(cli.fitnessKinds::stream, (t, v) -> t.fitness = v))
            .flatMap(flat(cli.mutationKinds::stream, (t, v) -> t.mutate = v))
            .flatMap(flat(cli.crossoverKinds::stream, (t, v) -> t.crossover = v));

        var forkJoinPool = new ForkJoinPool(cli.seeds.length);
        var runNum = new AtomicInteger(1);
        return paramGen.map(params -> runner(forkJoinPool, window, params.clone(), runNum.getAndIncrement(), cli.generations, cli.seeds));
    }

    static <T, V> Function<T, Stream<T>> flat(Supplier<Stream<V>> in, BiConsumer<T, V> setter){
        return t -> in.get().map(v -> {
            setter.accept(t, v);
            return t;
        });
    }

    static GARuns runner(ForkJoinPool pool, GAPopulationGraph window, GAParameters params, int runNum, int generations, long[] seeds){
        var stats = new GARuns(params);
        var graph = window==null?null:window.graph(Arrays.toString(seeds)+"\n"+params);
        var averager = new AveragedQueue(seeds.length, average -> {
            if(graph != null) graph.addDataPoint(average);
            stats.averagedStats.add(average);
        });
        System.out.println("Started run " + runNum);

        var bestResults = pool.submit(() -> Arrays.stream(seeds).parallel().mapToObj(seed -> {
            var run = stats.run(seed);
            var consumer = averager.provider();
            var result = new GA(
                    seed, params.problemSet,
                    params.elitismRate, params.crossoverRate, params.mutationRate, params.populationSize,
                    params.initialize.initializer,
                    params.select.selector,
                    params.fitness.fitness,
                    params.mutate.fitness,
                    params.crossover.crossover,
                    consumer
            ).run(generations);
            run.best = result.result;
            run.finished = params.fitness.fitness.complete(run.best.fitness);
            run.generationStats = result.stats;
            System.out.println("\tFinished seed " + seed);

            consumer.accept(null);
            if(graph != null) {
                graph.addVerticalLabel(
                result.stats.size() - 1,
                "G:" + (result.stats.size() - 1) +
                    " S:" + seed +
                    " F:" + Math.round(result.result.fitness)
                );
            }
            return result;
        }).toList()).join();

        stats.run = runNum;

        stats.averageBestFitness = bestResults.stream()
                .mapToDouble(value -> value.result.fitness)
                .average()
                .orElse(0.0);
        stats.completed = (int)bestResults.stream()
                .mapToDouble(value -> value.result.fitness)
                .filter(params.fitness.fitness::complete)
                .count();
        stats.averageCompletedGen = bestResults.stream()
                .filter(v -> params.fitness.fitness.complete(v.result.fitness))
                .mapToDouble(value -> value.stats.size()-1)
                .average()
                .orElse(0.0);
        stats.averageGen = bestResults.stream()
                .mapToDouble(value -> value.stats.size()-1)
                .average()
                .orElse(0.0);

        if(graph != null) {
            graph.showResults(
                    "Average Fitness: " + stats.averageBestFitness +
                    "\nCompleted: " + stats.completed +
                    "\nAverage Completed Generation: " + stats.averageCompletedGen
            );
        }

        return stats;
    }
}