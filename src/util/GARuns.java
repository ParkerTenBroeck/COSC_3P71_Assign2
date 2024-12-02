package util;

import data.Course;
import data.Room;
import data.Timeslot;
import ga.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.DoubleStream;

/**
 * A collection of data from many GA runs using different seeds but the same parameters.
 */
public class GARuns {
    public GAParameters params;
    public final ArrayList<GARun> runs = new ArrayList<>();
    public final ArrayList<GenerationStat> averagedStats = new ArrayList<>();

    public int completed;
    public Statistics normalized;
    public Statistics raw;
    public Statistics completedGen;
    public Statistics gen;
    public int run;

    /**
     * Basic statistics calculated for some provided list of doubles
     */
    public static class Statistics{
        public double mean;
        public double median;
        public double std;
        public double[] values;
        public double max;
        public double min;

        public Statistics(DoubleStream stream){
            this.values = stream.sorted().toArray();
            this.mean = Arrays.stream(this.values).average().orElse(0.0);

            this.max = Arrays.stream(this.values).max().orElse(0.0);
            this.min = Arrays.stream(this.values).min().orElse(0.0);

            if (this.values.length == 0){
                median = 0;
            }else if ((this.values.length&1) == 1){
                median = this.values[this.values.length/2];
            }else{
                median = (this.values[this.values.length/2]+this.values[this.values.length/2+1])/2;
            }

            if(this.values.length != 0){
                for(double num:this.values){
                    var diff = num-mean;
                    std += diff*diff;
                }
                std = Math.sqrt(std/this.values.length);
            }
        }
    }

    /**
     * Given two sets of statistics perform a Z-Test on them and calculate z, p, and if the result is significant or not
     * (p< 0.05)
     */
    public static class ZTest {
        public final double p;
        public final double z;
        public final boolean significant;

        public ZTest(Statistics s1, Statistics s2){
            this.z = (s1.mean-s2.mean)/Math.sqrt(s1.std*s1.std/s1.values.length+s2.std*s2.std/s2.values.length);

            this.p = pValueFromZ(this.z);
            this.significant = this.p < 0.05;
        }

        /* Source: http://introcs.cs.princeton.edu/java/21function/ErrorFunction.java.html */
        public static double erf(double z) {
            double t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
            double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
            double ans = 1.0 - poly * Math.exp(-z*z);
            if (z >= 0) return  ans;
            else        return -ans;
        }

        public static double pValueFromZ(double x) {
            return 1-erf(Math.abs(x)/Math.sqrt(2));
        }
    }


    public GARuns(GAParameters params) {
        this.params = params;
    }

    public synchronized GARun run(long seed) {
        var run = new GARun(seed);
        this.runs.add(run);
        return run;
    }

    public void calculateFinalResults() {
        runs.sort((c1, c2) -> params.fitness.fitness.rank(params.problemSet).compare(c1.best, c2.best));

        normalized = new Statistics(runs.stream().mapToDouble(v -> v.best.fitness));
        raw = new Statistics(runs.stream().mapToDouble(v -> v.best.rawFitness));

        completedGen = new Statistics(runs.stream()
                .filter(v -> params.fitness.fitness.complete(v.best.fitness))
                .mapToDouble(v -> v.generationStats.size()-1)
        );
        gen = new Statistics(runs.stream()
                .mapToDouble(v -> v.generationStats.size()-1)
        );
        completed = (int)runs.stream().filter(v -> params.fitness.fitness.complete(v.best.fitness)).count();
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

    public String json(Statistics stat){
        return Util.objln(
                Util.field("min", stat.min),
                Util.field("max", stat.max),
                Util.field("average", stat.mean),
                Util.field("median", stat.median),
                Util.field("std", stat.std)
        );
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
                Util.field("completed", completed),
                Util.field("normalized", json(normalized)),
                Util.field("raw", json(raw)),
                Util.field("completedGen", json(completedGen)),
                Util.field("gen", json(gen)),
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
