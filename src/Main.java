import data.ProblemSet;
import ga.Chromosome;
import ga.GA;
import ga.functional.*;
import util.AveragedQueue;
import util.GAPopulationGraph;
import util.Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {
        var probSet = new ProblemSet("./data/t1");
        System.out.println(probSet);

        var window = new GAPopulationGraph();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        var seeds = new long[30];
        for(int i = 0; i < seeds.length; i ++){
            seeds[i] = i+1;
        }

        var map = Stream.of(new Stuff(
                probSet,
                1,
                0.9,
                0.08,
                500,
                InitializerKind.RandomInitializer,
                SelectionKind.TournamentSelection,
                FitnessKind.Conflicts2Fitness,
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
        map.forEach(meow -> {
            var params = meow.clone();
            var graph = window.graph(Arrays.toString(seeds)+"\n"+params);
            var weird = new AveragedQueue(seeds.length, graph::addDataPoint);

            var bestResults = forkJoinPool.submit(() -> Arrays.stream(seeds).parallel().mapToObj(seed -> {
                var consumer = weird.provider();
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
                consumer.accept(null);
                graph.addVerticalLabel(result.stats.size()-1, "G:" + (result.stats.size()-1) + " S:" + seed + " F:" + Math.round(result.result.fitness));
                System.out.println(params + "\n" + result.stats.size());
                return result;
            }).toList()).join();

            var average = bestResults.stream().mapToDouble(value -> value.result.fitness).average().orElse(0.0);
            var completed = bestResults.stream().mapToDouble(value -> value.result.fitness).filter(params.fitness.fitness::complete).count();
            var averageCompletedGen = bestResults.stream()
                    .filter(v -> params.fitness.fitness.complete(v.result.fitness))
                    .mapToDouble(value -> value.stats.size()-1)
                    .average()
                    .orElse(0.0);
            graph.showResults("Average Fitness: " + average + "\nCompleted: " + completed + "\nAverage Completed Generation: " + averageCompletedGen);
        });
    }

    private static class Stuff{
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

        public Stuff(
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
        public Stuff clone() {
            return new Stuff(problemSet,elitismRate,crossoverRate,mutationRate,populationSize,initialize,select,fitness,mutate,crossover);
        }

        @Override
        public String toString() {
            return "  elitismRate: " + elitismRate +
                    "  crossoverRate: " + crossoverRate +
                    "  mutationRate: " + mutationRate +
                    "  populationSize: " + populationSize +
                    "\n" + initialize + " " + select + " " + fitness + " " + mutate + " " + crossover;
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
        ConflictsFitness(Chromosome::conflicts),
        Conflicts2Fitness(Chromosome::conflicts2),
        Conflicts3Fitness(Chromosome::conflicts3),
        Conflicts33Fitness(Chromosome::conflictsCustom33),
        ConflictsWeirdFitness(new Fitness() {
            @Override
            public double calcRaw(Chromosome c, ProblemSet ga) {
                return c.conflicts3(ga);
            }

            @Override
            public Comparator<Chromosome> rank(ProblemSet ps) {
                return (c1, c2) -> c1.paretoDominates(c2, ps);
            }
        }),
        ConflictsWeirdFitness2(new Fitness() {
            @Override
            public double calcRaw(Chromosome c, ProblemSet ga) {
                return c.conflicts3(ga);
            }

            @Override
            public Comparator<Chromosome> rank(ProblemSet ps) {
                return (c1, c2) -> c1.paretoDominates2(c2, ps);
            }
        }),
        ConflictsBoundedFitness(Chromosome::conflictsBounded),
        ConflictsCustomFitness(Chromosome::conflictsCustom);
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