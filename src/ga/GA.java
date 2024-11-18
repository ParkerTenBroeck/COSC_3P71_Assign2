package ga;

import data.ProblemSet;
import ga.functional.*;

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
        for(int i = 1; i < elitismRate; i ++) population[i] = null;
        outer:
        for(int popIdx = 1; popIdx < populationSize; popIdx ++){
            for(int elitIdx = 0; elitIdx < elitismRate; elitIdx ++){
                if(population[elitIdx]==null||fitness.rank(problemSet).compare(prevPopulation[popIdx], population[elitIdx])>0){
                    for(int insertIdx = elitismRate-1; insertIdx > elitIdx+1; insertIdx --){
                        population[insertIdx] = population[insertIdx-1];
                    }
                    population[elitIdx] = prevPopulation[popIdx];
                    continue outer;
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

    private void mutation(){
        var idxFrom = Math.min(elitismRate, populationSize);
        for(int i = idxFrom; i < populationSize; i ++){
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
}
