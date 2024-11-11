package ga.functional;

import ga.Chromosome;
import ga.GA;

public interface Selector {
    Chromosome select(Chromosome[] chromosomes, GA ga);
}
