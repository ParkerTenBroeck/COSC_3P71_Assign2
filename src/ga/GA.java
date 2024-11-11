package ga;

import data.ProblemSet;
import ga.functional.*;
import util.Util;

import java.text.NumberFormat;
import java.util.*;
import java.util.function.Consumer;

public class GA {

    private final int elitismRate;
    private final double crossoverRate;
    private final double mutationRate;
    private final int populationSize;

    private Chromosome[] prevPopulation;
    private Chromosome[] population;


    public final ProblemSet problemSet;
    public final GaRNG rng;
    private final Initializer initialize;
    private final Selector select;
    public final Fitness fitness;
    private final Mutator mutate;
    private final Crossover crossover;

    private final Consumer<GenerationStat> statConsumer;

    private final ArrayList<GenerationStat> stats = new ArrayList<>();

    public GA(
            long seed,
            ProblemSet problemSet,
            int elitismRate,
            double crossoverRate,
            double mutationRate,
            int populationSize,
            Initializer initialize,
            Selector select,
            Fitness fitness,
            Mutator mutate,
            Crossover crossover,
            Consumer<GenerationStat> statConsumer
    ) {
        this.rng = new GaRNG(seed);
        prevPopulation = new Chromosome[populationSize];
        population = new Chromosome[populationSize];
        this.elitismRate = elitismRate;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
        this.populationSize = populationSize;
        this.problemSet = problemSet;
        this.initialize = initialize;
        this.select = select;
        this.fitness = fitness;
        this.mutate = mutate;
        this.crossover = crossover;
        this.statConsumer = statConsumer;
    }

    public GAResult run(int maxGens){
        initialize();
        var popBest = evaluate();
        if(fitness.complete(popBest.fitness)) return new GAResult(stats, popBest);
        for(int i = 1; i <= maxGens; i ++){
            elitism();
            selection();
            crossover();
            mutation();
            popBest = evaluate();
            if(fitness.complete(popBest.fitness)) return new GAResult(stats, popBest);
        }
        return new GAResult(stats, popBest);
    }

    private void initialize(){
        for(int i = 0; i < populationSize; i ++)
            population[i] = initialize.initialize(this);
    }

    private void elitism(){
        population[0] = prevPopulation[0];
        for(int i = 1; i < populationSize; i ++){
            for(int k = 0; k < elitismRate; k ++){
                if(fitness.rank(problemSet).compare(prevPopulation[i], population[k])>0){
                    for(int j = elitismRate-1; j > k+1; j --){
                        population[j] = population[j-1];
                    }
                    population[k] = prevPopulation[i];
                }
            }
        }
    }

    private void selection(){
        var idxFrom = Math.min(elitismRate, populationSize);
        for(int i = idxFrom; i < populationSize; i ++){
            population[i] = select.select(prevPopulation, this);
        }
    }

    private void crossover(){
        for(int i = 0; i < populationSize; i ++){
            if(rng.percent(crossoverRate)){
                var i1 = rng.randomInt(populationSize);
                var i2 = rng.randomInt(populationSize);
                var result = crossover.crossover(population[i1], population[i2], this);
                population[i1] = result.t1;
                population[i2] = result.t2;
            }
        }
    }

    private void mutation(){
        for(int i = 0; i < populationSize; i ++){
            if(rng.percent(mutationRate))
                population[i] = mutate.mutate(population[i], this);
        }
    }

    private Chromosome evaluate(){
        var stat = new GenerationStat(population);
        if(statConsumer != null)
            statConsumer.accept(stat);
        stats.add(stat);
        var tmp = prevPopulation;
        prevPopulation = population;
        population = tmp;
        return Arrays.stream(prevPopulation).max(fitness.rank(problemSet)).orElse(null);
    }

    public static final class GaRNG{
        Random rand;
        public GaRNG(long seed){
            rand = new Random(seed);
        }

        public int randomInt(int max){
            return rand.nextInt(max);
        }

        public boolean percent(double percent){
            return rand.nextDouble() < percent;
        }
    }

    public static final class GenerationStat{
        public final double minFit;
        public final double maxFit;
        public final double averageFit;

        public final double minRawFit;
        public final double maxRawFit;
        public final double averageRawFit;

        public GenerationStat(double minFit, double maxFit, double averageFit, double minRawFit, double maxRawFit, double averageRawFit) {
            this.minFit = minFit;
            this.maxFit = maxFit;
            this.averageFit = averageFit;
            this.minRawFit = minRawFit;
            this.maxRawFit = maxRawFit;
            this.averageRawFit = averageRawFit;
        }

        private GenerationStat(Chromosome[] population) {
            this.maxFit = Arrays.stream(population).mapToDouble(c -> c.fitness).max().orElse(0.0);
            this.minFit = Arrays.stream(population).mapToDouble(c -> c.fitness).min().orElse(0.0);
            this.averageFit = Arrays.stream(population).mapToDouble(c -> c.fitness).average().orElse(0.0);


            this.maxRawFit = Arrays.stream(population).mapToDouble(c -> c.rawFitness).max().orElse(0.0);
            this.minRawFit = Arrays.stream(population).mapToDouble(c -> c.rawFitness).min().orElse(0.0);
            this.averageRawFit = Arrays.stream(population).mapToDouble(c -> c.rawFitness).average().orElse(0.0);
        }

        private static String percent(double value){
            NumberFormat defaultFormat = NumberFormat.getPercentInstance();
            defaultFormat.setMaximumFractionDigits(2);
            defaultFormat.setMinimumFractionDigits(2);
            return defaultFormat.format(value);
        }

        @Override
        public String toString() {
            return "(max: " + percent(maxFit) + ", min: " + percent(minFit) + ", average: " + percent(averageFit) + ')' + "(max: " + maxRawFit + ", min: " + minRawFit + ", average: " + averageRawFit + ')';
        }
    }

    public final class GAResult{
        public final ArrayList<GenerationStat> stats;
        public final Chromosome result;

        public GAResult(ArrayList<GenerationStat> stats, Chromosome result) {
            this.stats = stats;
            this.result = result;
        }

        @Override
        public String toString() {
            return Util.lines(
                "GAResult{",
                Util.indent(
                    "stats: [",
                    Util.indent(stats.stream().map(Objects::toString)),
                    "]",
                    "result: " + result.toString(problemSet)
                ),
                "}"
            );
        }
    }
}
