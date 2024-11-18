package ga;

import java.text.NumberFormat;
import java.util.Arrays;

public final class GenerationStat {
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

    GenerationStat(Chromosome[] population) {
        this.maxFit = Arrays.stream(population).mapToDouble(c -> c.fitness).max().orElse(0.0);
        this.minFit = Arrays.stream(population).mapToDouble(c -> c.fitness).min().orElse(0.0);
        this.averageFit = Arrays.stream(population).mapToDouble(c -> c.fitness).average().orElse(0.0);


        this.maxRawFit = Arrays.stream(population).mapToDouble(c -> c.rawFitness).max().orElse(0.0);
        this.minRawFit = Arrays.stream(population).mapToDouble(c -> c.rawFitness).min().orElse(0.0);
        this.averageRawFit = Arrays.stream(population).mapToDouble(c -> c.rawFitness).average().orElse(0.0);
    }

    private static String percent(double value) {
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
