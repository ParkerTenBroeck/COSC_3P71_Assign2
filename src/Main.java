import data.Course;
import data.ProblemSet;
import data.Room;
import data.Timeslot;
import ga.Chromosome;
import ga.GA;
import ga.Gene;
import ga.GenerationStat;
import ga.functional.*;
import util.AveragedQueue;
import util.GAPopulationGraph;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {
        var probSet = new ProblemSet("./data/t1");
        System.out.println(probSet);

        GAPopulationGraph window;
        if(!Arrays.asList(args).contains("--gui")) {
            window = new GAPopulationGraph();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }else {
            window = null;
        }



        var seeds = new long[30];
        for(int i = 0; i < seeds.length; i ++){
//             seeds[i] = (long) (Long.MAX_VALUE*Math.random());
            seeds[i] = i+1;
        }

        var map = Stream.of(new GAParameters(
                probSet,
                1,
                0.9,
                0.08,
                500,
                InitializerKind.RandomInitializer,
                SelectionKind.TournamentSelection,
                FitnessKind.ConflictsFitness,
                MutationKind.SingleGeneMutation,
                CrossoverKind.UniformCrossover
            ))
            .flatMap(v -> Stream.of(1.0, 0.9).map(c -> v.crossoverRate = c).map(unused -> v))
            .flatMap(v -> Stream.of(0.01, 0.1).map(c -> v.mutationRate = c).map(unused -> v))
            .flatMap(v -> Stream.of(100, 500).map(c -> v.populationSize = c).map(unused -> v))
//            .flatMap(v -> Stream.of(FitnessKind.ConflictsFitness, FitnessKind.Conflicts2Fitness).map(c -> v.fitness = c).map(unused -> v))
            .flatMap(v -> Stream.of(CrossoverKind.BestAttemptCrossover, CrossoverKind.UniformCrossover).map(c -> v.crossover = c).map(unused -> v))
        ;

        var forkJoinPool = new ForkJoinPool(seeds.length);
        var it = map.map(meow -> runner(forkJoinPool, window, meow.clone(), seeds)).iterator();

        try{
            Arrays.stream(Objects.requireNonNull(new File("runs").listFiles())).forEach(File::delete);
        }catch (Exception ignore){}
        Files.deleteIfExists(Paths.get("runs"));
        Files.createDirectory(Paths.get("runs"));
        for (int i = 1; it.hasNext(); i++) {
            var item = it.next();
            try {
                Files.write(Paths.get("runs/run"+i+".json"), item.toString().getBytes());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }



    static GARuns runner(ForkJoinPool pool, GAPopulationGraph window, GAParameters params, long[] seeds){
        var stats = new GARuns(params);

        var graph = window != null?window.graph(Arrays.toString(seeds)+"\n"+params):null;
        var averager = new AveragedQueue(seeds.length, average -> {
            if(graph != null) graph.addDataPoint(average);
            stats.averagedStats.add(average);
        });
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
            ).run(250);
            run.best = result.result;
            run.finished = params.fitness.fitness.complete(run.best.fitness);
            run.generationStats = result.stats;

            consumer.accept(null);
            if(graph != null)
                graph.addVerticalLabel(result.stats.size()-1, "G:" + (result.stats.size()-1) + " S:" + seed + " F:" + Math.round(result.result.fitness));
            return result;
        }).toList()).join();

        stats.averageBestFitness = bestResults.stream().mapToDouble(value -> value.result.fitness).average().orElse(0.0);
        stats.completed = (int)bestResults.stream().mapToDouble(value -> value.result.fitness).filter(params.fitness.fitness::complete).count();
        stats.averageCompletedGen = bestResults.stream()
                .filter(v -> params.fitness.fitness.complete(v.result.fitness))
                .mapToDouble(value -> value.stats.size()-1)
                .average()
                .orElse(0.0);
        stats.averageGen = bestResults.stream()
                .mapToDouble(value -> value.stats.size()-1)
                .average()
                .orElse(0.0);

        if(graph != null)
            graph.showResults("Average Fitness: " + stats.averageBestFitness + "\nCompleted: " + stats.completed + "\nAverage Completed Generation: " + stats.averageCompletedGen);

        return stats;
    }

    private static class GAParameters {
        public ProblemSet problemSet;
        public int elitismRate;
        public double crossoverRate;
        public double mutationRate;
        public int populationSize;
        public InitializerKind initialize;
        public SelectionKind select;
        public FitnessKind fitness;
        public MutationKind mutate;
        public CrossoverKind crossover;

        public GAParameters(
                ProblemSet problemSet,
                int elitismRate,
                double crossoverRate,
                double mutationRate,
                int populationSize,
                InitializerKind initialize,
                SelectionKind select,
                FitnessKind fitness,
                MutationKind mutate,
                CrossoverKind crossover
        ) {
            this.problemSet = problemSet;
            this.elitismRate = elitismRate;
            this.crossoverRate = crossoverRate;
            this.mutationRate = mutationRate;
            this.populationSize = populationSize;
            this.initialize = initialize;
            this.select = select;
            this.fitness = fitness;
            this.mutate = mutate;
            this.crossover = crossover;
        }

        @Override
        public GAParameters clone() {
            return new GAParameters(problemSet,elitismRate,crossoverRate,mutationRate,populationSize,initialize,select,fitness,mutate,crossover);
        }

        @Override
        public String toString() {
            return "  elitismRate: " + elitismRate +
                    "  crossoverRate: " + crossoverRate +
                    "  mutationRate: " + mutationRate +
                    "  populationSize: " + populationSize +
                    "\n" + initialize + " " + select + " " + fitness + " " + mutate + " " + crossover;
        }

        public String json() {
            return Util.objln(
                Util.field("elitism_rate", elitismRate),
                Util.field("crossover_rate", crossoverRate),
                Util.field("mutation_rate", mutationRate),
                Util.field("population_size", populationSize),
                Util.fieldStr("initialize", initialize.toString()),
                Util.fieldStr("select", select.toString()),
                Util.fieldStr("fitness", fitness.toString()),
                Util.fieldStr("mutate", mutate.toString()),
                Util.fieldStr("crossover", crossover.toString())
            );

        }
    }

    private static class GARuns{
        public GAParameters params;
        public final ArrayList<GARun> runs = new ArrayList<>();
        public final ArrayList<GenerationStat> averagedStats = new ArrayList<>();

        public double averageBestFitness;
        public int completed;
        public double averageCompletedGen;
        public double averageGen;

        public GARuns(GAParameters params) {
            this.params = params;
        }

        public GARun run(long seed){
            var run = new GARun(seed);
            this.runs.add(run);
            return run;
        }


        private class GARun{
            public long seed;
            public ArrayList<GenerationStat> generationStats;
            public Chromosome best;
            public boolean finished;

            public GARun(long seed) {
                this.seed = seed;
            }

            public String json() {
                return Util.objln(
                    Util.field("seed", seed),
                    Util.field("finished", finished),
                    Util.field("gen_stats", Util.arrln(generationStats.stream().map(GARuns::json))),
                    Util.field("best", Util.objln(
                            Util.field("normalized", best.fitness),
                            Util.field("raw", best.rawFitness),
                            Util.field("chromosome", Util.arrln(best.genes().map(GARuns.this::json)))
                    ))
                );
            }
        }

        private String json(Gene stat){
            return Util.obj(
              Util.field("room", json(params.problemSet.rooms.get(stat.roomIdx))),
                Util.field("course", json(params.problemSet.courses.get(stat.roomIdx))),
                Util.field("timeslot", json(params.problemSet.timeslots.get(stat.roomIdx)))
            );
        }

        private static String json(GenerationStat stat){
            return Util.obj(
                Util.field("normalized", Util.obj(
                    Util.field("max", stat.maxFit),
                    Util.field("min", stat.minFit),
                    Util.field("average", stat.averageFit)
                )),
                Util.field("raw", Util.obj(
                    Util.field("max", stat.maxRawFit),
                    Util.field("min", stat.minRawFit),
                    Util.field("average", stat.averageRawFit)
                ))
            );
        }

        private static String json(Room room){
            return Util.obj(
                    Util.fieldStr("name", room.name),
                    Util.field("capacity", room.capacity)
            );
        }
        private static String json(Timeslot timeslot){
            return Util.obj(
                    Util.fieldStr("day", timeslot.day.toString()),
                    Util.field("hour", timeslot.hour)
            );
        }
        private static String json(Course course){
            return Util.obj(
              Util.fieldStr("name", course.name),
                Util.field("students", course.students),
                Util.fieldStr("professor", course.professor),
                Util.field("duration", course.duration)
            );
        }

        @Override
        public String toString(){
            return Util.objln(
            Util.field("average_best_fitness", averageBestFitness),
                Util.field("completed", completed),
                Util.field("average_completed_gen", averageCompletedGen),
                Util.field("average_gen", averageGen),
                Util.field("params: ", params.json()),
                Util.field("averaged_gen_stats", Util.arrln(averagedStats.stream().map(GARuns::json))),
                Util.field("runs", Util.arrln(runs.stream().map(GARun::json))),
                Util.field("problem_set", Util.objln(
                    Util.field("courses", Util.arrln(params.problemSet.courses.stream().map(GARuns::json))),
                    Util.field("rooms", Util.arrln(params.problemSet.rooms.stream().map(GARuns::json))),
                    Util.field("timeslots", Util.arrln(params.problemSet.timeslots.stream().map(GARuns::json)))
                ))
            );
        }
    }

    private enum InitializerKind {
        RandomInitializer(Chromosome::random);
        public final Initializer initializer;
        InitializerKind(Initializer initializer) {
            this.initializer = initializer;
        }
    }

    private enum SelectionKind {
        TournamentSelection((chromosomes, ga) -> {
            var max = ga.rng.randomInt(chromosomes.length);
            for(int i = 1; i < 4; i++){
                var inx = ga.rng.randomInt(chromosomes.length);
                if(ga.fitness.rank(ga.problemSet).compare(chromosomes[inx], chromosomes[max]) > 0)
                    max = inx;
            }
            return chromosomes[max];
        }),
        RandomSelection((chromosomes, ga) -> chromosomes[ga.rng.randomInt(chromosomes.length)]);
        public final Selector selector;
        SelectionKind(Selector selector) {
            this.selector = selector;
        }
    }

    private enum FitnessKind{
        ConflictsFitness(Chromosome::conflicts);
        public final Fitness fitness;
        FitnessKind(Fitness fitness) {
            this.fitness = fitness;
        }
    }

    private enum MutationKind{
        SingleGeneMutation(Chromosome::singleGeneMutation);
        public final Mutator fitness;
        MutationKind(Mutator fitness) {
            this.fitness = fitness;
        }
    }

    private enum CrossoverKind{
        NoCrossover((c1, c2, ga) -> new Util.Tuple<>(c1,c2)),
        OnePointCrossover(Chromosome::onePointCrossover),
        UniformCrossover(Chromosome::uniformCrossover),
        BestAttemptCrossover(Chromosome::bestAttemptCrossover);
        public final Crossover crossover;
        CrossoverKind(Crossover crossover) {
            this.crossover = crossover;
        }
    }
}