package ga.functional;

import ga.Chromosome;
import ga.GA;

public interface Initializer {
    Chromosome initialize(GA ga);
}
