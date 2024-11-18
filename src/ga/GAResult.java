package ga;

import java.util.ArrayList;

public final class GAResult {
    public final ArrayList<GenerationStat> stats;
    public final Chromosome result;

    public GAResult(ArrayList<GenerationStat> stats, Chromosome result) {
        this.stats = stats;
        this.result = result;
    }
}
