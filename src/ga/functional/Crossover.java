package ga.functional;

import ga.Chromosome;
import ga.GA;
import util.Util;

public interface Crossover {
    Util.Tuple<Chromosome, Chromosome> crossover(Chromosome c1, Chromosome c2, GA ga);
}
