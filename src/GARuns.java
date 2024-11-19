import data.Course;
import data.Room;
import data.Timeslot;
import ga.Chromosome;
import ga.GAParameters;
import ga.Gene;
import ga.GenerationStat;
import util.Util;

import java.util.ArrayList;

class GARuns {
    public GAParameters params;
    public final ArrayList<GARun> runs = new ArrayList<>();
    public final ArrayList<GenerationStat> averagedStats = new ArrayList<>();

    public double averageBestFitness;
    public int completed;
    public double averageCompletedGen;
    public double averageGen;
    public int run;

    public GARuns(GAParameters params) {
        this.params = params;
    }

    public GARun run(long seed) {
        var run = new GARun(seed);
        this.runs.add(run);
        return run;
    }


    public static class GARun {
        public long seed;
        public ArrayList<GenerationStat> generationStats;
        public Chromosome best;
        public boolean finished;

        public GARun(long seed) {
            this.seed = seed;
        }
    }

    public String json(GARun run) {
        return Util.objln(
                Util.field("seed", run.seed),
                Util.field("finished", run.finished),
                Util.field("gen_stats", Util.arrln(run.generationStats.stream().map(GARuns::json))),
                Util.field("best", Util.objln(
                        Util.field("normalized", run.best.fitness),
                        Util.field("raw", run.best.rawFitness),
                        Util.field("chromosome", Util.arrln(run.best.genes().map(GARuns.this::json)))
                ))
        );
    }

    private String json(Gene stat) {
        return Util.obj(
                Util.field("room", json(params.problemSet.rooms.get(stat.roomIdx))),
                Util.field("course", json(params.problemSet.courses.get(stat.roomIdx))),
                Util.field("timeslot", json(params.problemSet.timeslots.get(stat.roomIdx)))
        );
    }

    private static String json(GenerationStat stat) {
        return Util.obj(
                Util.field("normalized", Util.obj(
                        Util.field("max", stat.maxFit),
                        Util.field("min", stat.minFit),
                        Util.field("average", stat.averageFit)
                )),
                Util.field("raw", Util.obj(
                        Util.field("max", stat.maxRawFit),
                        Util.field("min", stat.minRawFit),
                        Util.field("average", stat.averageRawFit)
                ))
        );
    }

    private static String json(Room room) {
        return Util.obj(
                Util.fieldStr("name", room.name),
                Util.field("capacity", room.capacity)
        );
    }

    private static String json(Timeslot timeslot) {
        return Util.obj(
                Util.fieldStr("day", timeslot.day.toString()),
                Util.field("hour", timeslot.hour)
        );
    }

    private static String json(Course course) {
        return Util.obj(
                Util.fieldStr("name", course.name),
                Util.field("students", course.students),
                Util.fieldStr("professor", course.professor),
                Util.field("duration", course.duration)
        );
    }

    private static String json(GAParameters params) {
        return Util.objln(
                Util.field("elitism_rate", params.elitismRate),
                Util.field("crossover_rate", params.crossoverRate),
                Util.field("mutation_rate", params.mutationRate),
                Util.field("population_size", params.populationSize),
                Util.fieldStr("initialize", params.initialize.toString()),
                Util.fieldStr("select", params.select.toString()),
                Util.fieldStr("fitness", params.fitness.toString()),
                Util.fieldStr("mutate", params.mutate.toString()),
                Util.fieldStr("crossover", params.crossover.toString())
        );
    }

    public String json(){
        return Util.objln(
                Util.field("average_best_fitness", averageBestFitness),
                Util.field("completed", completed),
                Util.field("average_completed_gen", averageCompletedGen),
                Util.field("average_gen", averageGen),
                Util.field("params: ", json(params)),
                Util.field("averaged_gen_stats", Util.arrln(averagedStats.stream().map(GARuns::json))),
                Util.field("runs", Util.arrln(runs.stream().map(this::json))),
                Util.field("problem_set", Util.objln(
                        Util.field("courses", Util.arrln(params.problemSet.courses.stream().map(GARuns::json))),
                        Util.field("rooms", Util.arrln(params.problemSet.rooms.stream().map(GARuns::json))),
                        Util.field("timeslots", Util.arrln(params.problemSet.timeslots.stream().map(GARuns::json)))
                ))
        );
    }

    @Override
    public String toString() {
        return json();
    }
}
