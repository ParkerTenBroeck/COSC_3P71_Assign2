package util;

import data.ProblemSet;
import ga.GAParameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Parses and validates the CLI inputs for this program
 */
public class CliArgs {
    public long[] seeds;
    public boolean gui;
    public Integer generations;
    public ProblemSet problemSet;
    public List<Integer> elitismRates;
    public List<Double> crossoverRates;
    public List<Double> mutationRates;
    public List<Integer> populationSizes;
    public List<GAParameters.InitializerKind> initializerKinds;
    public List<GAParameters.SelectionKind> selectionKinds;
    public List<GAParameters.FitnessKind> fitnessKinds;
    public List<GAParameters.MutationKind> mutationKinds;
    public List<GAParameters.CrossoverKind> crossoverKinds;

    public CliArgs(String... args) throws IOException {
        for(int ai = 0; ai < args.length; ai ++){
            switch(args[ai]){
                case "--gui" -> gui = true;
                case "--seeds-linear" -> {
                    if(seeds != null) throw new RuntimeException("Seeds already specified elsewhere");
                    int len = Integer.parseInt(args[++ai]);
                    seeds = new long[len];
                    for(int i = 0; i < len; i ++)seeds[i] = i;
                }
                case "--seeds-rand" -> {
                    if(seeds != null) throw new RuntimeException("Seeds already specified elsewhere");
                    int len = Integer.parseInt(args[++ai]);
                    seeds = new long[len];
                    for(int i = 0; i < len; i ++)seeds[i] = (long) (Math.random()*Long.MAX_VALUE);
                }
                case "--seeds" -> {
                    if(seeds != null) throw new RuntimeException("Seeds already specified elsewhere");
                    seeds = Arrays.stream(args[++ai].split(",")).map(String::trim).mapToLong(Long::parseLong).toArray();
                }
                case "--problem-set" -> {
                    if(problemSet != null) throw new RuntimeException("Problem set already specified elsewhere");
                    problemSet = new ProblemSet(args[++ai]);
                }
                case "--elitism-rates" -> {
                    if(elitismRates != null) throw new RuntimeException("Elitism rates already specified elsewhere");
                    elitismRates = Arrays.stream(args[++ai].split(",")).map(String::trim).map(Integer::parseInt).toList();
                }
                case "--crossover-rates" -> {
                    if(crossoverRates != null) throw new RuntimeException("Crossover rates already specified elsewhere");
                    crossoverRates = Arrays.stream(args[++ai].split(",")).map(String::trim).map(Double::parseDouble).toList();
                }
                case "--mutation-rates" -> {
                    if(mutationRates != null) throw new RuntimeException("Mutation rates already specified elsewhere");
                    mutationRates = Arrays.stream(args[++ai].split(",")).map(String::trim).map(Double::parseDouble).toList();
                }
                case "--population-sizes" -> {
                    if(populationSizes != null) throw new RuntimeException("Population sizes already specified elsewhere");
                    populationSizes = Arrays.stream(args[++ai].split(",")).map(String::trim).map(Integer::parseInt).toList();
                }
                case "--initializer-kinds" -> {
                    if(initializerKinds != null) throw new RuntimeException("Initializer kinds already specified elsewhere");
                    initializerKinds = Arrays.stream(args[++ai].split(",")).map(String::trim).map(GAParameters.InitializerKind::valueOf).toList();
                }
                case "--selection-kinds" -> {
                    if(selectionKinds != null) throw new RuntimeException("Selection kinds already specified elsewhere");
                    selectionKinds = Arrays.stream(args[++ai].split(",")).map(String::trim).map(GAParameters.SelectionKind::valueOf).toList();
                }
                case "--fitness-kinds" -> {
                    if(fitnessKinds != null) throw new RuntimeException("Fitness kinds already specified elsewhere");
                    fitnessKinds = Arrays.stream(args[++ai].split(",")).map(String::trim).map(GAParameters.FitnessKind::valueOf).toList();
                }
                case "--mutation-kinds" -> {
                    if(mutationKinds != null) throw new RuntimeException("Mutation kinds already specified elsewhere");
                    mutationKinds = Arrays.stream(args[++ai].split(",")).map(String::trim).map(GAParameters.MutationKind::valueOf).toList();
                }
                case "--crossover-kinds" -> {
                    if(crossoverKinds != null) throw new RuntimeException("Crossover kinds already specified elsewhere");
                    crossoverKinds = Arrays.stream(args[++ai].split(",")).map(String::trim).map(GAParameters.CrossoverKind::valueOf).toList();
                }
                case "--generations" -> {
                    if(generations != null) throw new RuntimeException("Crossover kinds already specified elsewhere");
                    generations = Integer.parseInt(args[++ai].trim());
                }
            }
        }
        if(seeds == null) throw new RuntimeException("Seeds never specified");
        if(generations == null) throw new RuntimeException("Generations never specified");
        if(problemSet == null) throw new RuntimeException("Problem set never specified");
        if(elitismRates == null) throw new RuntimeException("Elitism rates never specified");
        if(crossoverRates == null) throw new RuntimeException("Crossover rates never specified");
        if(mutationRates == null) throw new RuntimeException("Mutation rates never specified");
        if(populationSizes == null) throw new RuntimeException("Population sizes never specified");
        if(initializerKinds == null) throw new RuntimeException("Initializer kinds never specified");
        if(selectionKinds == null) throw new RuntimeException("Selection kinds never specified");
        if(fitnessKinds == null) throw new RuntimeException("Fitness kinds never specified");
        if(mutationKinds == null) throw new RuntimeException("Mutation kinds never specified");
        if(crossoverKinds == null) throw new RuntimeException("Crossover kinds never specified");
    }
}
