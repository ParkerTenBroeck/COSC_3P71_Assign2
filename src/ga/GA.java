package ga;

import data.ProblemSet;
import ga.functional.*;

import java.util.*;
import java.util.function.Consumer;

public class GA {

    public final long seed;
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
    public final Comparator<Chromosome> cmp;

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
        this.seed = seed;
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
        this.cmp = fitness.rank(problemSet);
        this.mutate = mutate;
        this.crossover = crossover;
        this.statConsumer = statConsumer;
    }

    /**
     * Actually runs the GA
     * @return the calculated statistics as well as the best solution found
     */
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

    /**
     * Initialize the population with the provided algorithm
     */
    private void initialize(){
        for(int i = 0; i < populationSize; i ++)
            population[i] = initialize.initialize(this);
    }

    /**
     * Perform elitism and select the best k members from the previous population and place them in the new population
     */
    private void elitism(){
        for(int i = 0; i < elitismRate; i ++) population[i] = null;
        outer:
        for(int popIdx = 0; popIdx < populationSize; popIdx ++){
            for(int elitIdx = 0; elitIdx < elitismRate; elitIdx ++){
                // if empty insert there
                if(population[elitIdx]==null){
                    population[elitIdx] = prevPopulation[popIdx];
                    continue outer;
                }
                // if the new value is larger than the current elite value insert it moving everything else down
                if(cmp.compare(prevPopulation[popIdx], population[elitIdx])>0){
                    for(int insertIdx = elitismRate-1; insertIdx > elitIdx+1; insertIdx --){
                        population[insertIdx] = population[insertIdx-1];
                    }
                    population[elitIdx] = prevPopulation[popIdx];
                    continue outer;
                }
            }
        }
    }

    /**
     * Perform selection of the provided type to fill out the remainder of the population
     */
    private void selection(){
        var idxFrom = Math.min(elitismRate, populationSize);
        for(int i = idxFrom; i < populationSize; i ++){
            population[i] = select.select(prevPopulation, this);
        }
    }

    /**
     * Performs crossover on all chromosomes but the elites at the specified rate using the specified algorithm.
     */
    private void crossover(){
        var idxFrom = Math.min(elitismRate, populationSize);
        for(int i = idxFrom; i < populationSize; i ++){
            if(rng.percent(crossoverRate)){
                var i2 = rng.randomInt(populationSize-idxFrom)+idxFrom;
                var result = crossover.crossover(population[i], population[i2], this);
                population[i] = result.t1;
                population[i2] = result.t2;
            }
        }
    }

    /**
     * Mutate individuals in the population except the elites using the specified mutation algorithm
     * at the specified mutation rate.
     */
    private void mutation(){
        var idxFrom = Math.min(elitismRate, populationSize);
        for(int i = idxFrom; i < populationSize; i ++){
            if(rng.percent(mutationRate))
                population[i] = mutate.mutate(population[i], this);
        }
    }

    /**
     * Evaluate and report the statistics for this population. return the individual with the highest fitness.
     */
    private Chromosome evaluate(){
        var stat = new GenerationStat(population);
        if(statConsumer != null)
            statConsumer.accept(stat);
        stats.add(stat);
        var tmp = prevPopulation;
        prevPopulation = population;
        population = tmp;
        return Arrays.stream(prevPopulation).max(cmp).orElse(null);
    }

    /**
     * A random number generator with wrapper functions for the GA
     */
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
}
