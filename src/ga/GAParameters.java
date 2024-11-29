package ga;

import data.ProblemSet;
import ga.functional.*;
import util.Util;

@SuppressWarnings("unused")
public class GAParameters {
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

    public GAParameters(ProblemSet problemSet) {
        this.problemSet = problemSet;
    }

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
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public GAParameters clone() {
        return new GAParameters(problemSet, elitismRate, crossoverRate, mutationRate, populationSize, initialize, select, fitness, mutate, crossover);
    }

    @Override
    public String toString() {
        return "  elitismRate: " + elitismRate +
                "  crossoverRate: " + crossoverRate +
                "  mutationRate: " + mutationRate +
                "  populationSize: " + populationSize +
                "\n" + initialize + " " + select + " " + fitness + " " + mutate + " " + crossover;
    }


    public enum InitializerKind {
        Random(Chromosome::random);
        public final Initializer initializer;
        InitializerKind(Initializer initializer) {
            this.initializer = initializer;
        }
    }

    public enum SelectionKind {
        Tournament((chromosomes, ga) -> {
            var max = ga.rng.randomInt(chromosomes.length);
            for(int i = 1; i < 4; i++){
                var inx = ga.rng.randomInt(chromosomes.length);
                if(ga.cmp.compare(chromosomes[inx], chromosomes[max]) > 0)
                    max = inx;
            }
            return chromosomes[max];
        }),
        Random((chromosomes, ga) -> chromosomes[ga.rng.randomInt(chromosomes.length)]);
        public final Selector selector;
        SelectionKind(Selector selector) {
            this.selector = selector;
        }
    }

    public enum FitnessKind{
        WeightedConflicts(Chromosome::conflicts);
        public final Fitness fitness;
        FitnessKind(Fitness fitness) {
            this.fitness = fitness;
        }
    }

    public enum MutationKind{
        SingleGene(Chromosome::singleGeneMutation);
        public final Mutator fitness;
        MutationKind(Mutator fitness) {
            this.fitness = fitness;
        }
    }

    public enum CrossoverKind{
        None((c1, c2, ga) -> new Util.Tuple<>(c1,c2)),
        OnePoint(Chromosome::onePointCrossover),
        Uniform(Chromosome::uniformCrossover),
        BestAttempt(Chromosome::bestAttemptCrossover);
        public final Crossover crossover;
        CrossoverKind(Crossover crossover) {
            this.crossover = crossover;
        }
    }
}
