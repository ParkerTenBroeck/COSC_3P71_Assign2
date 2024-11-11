package ga.functional;

import data.ProblemSet;
import ga.Chromosome;

import java.util.Comparator;

public interface Fitness {
    double calcRaw(Chromosome c, ProblemSet ga);
    default double normalize(double raw){
        return 100/(1+raw);
    }
    default boolean complete(double normalized){
        return Math.abs(100-normalized)<Double.MIN_VALUE;
    }

    default Comparator<Chromosome> rank(ProblemSet ps){
        return Chromosome::compareTo;
    }
}
