package ga.functional;

import ga.Chromosome;
import ga.GA;

public interface Mutator {
    Chromosome mutate(Chromosome c, GA ga);
}
