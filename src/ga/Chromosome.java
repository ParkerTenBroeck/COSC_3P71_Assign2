package ga;

import data.Course;
import data.ProblemSet;
import data.Room;
import data.Timeslot;
import util.Util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A chromosome represents a potential solution to our problem.
 * <br/>
 * Once a chromosome is created it is immutable and cannot be changed.
 */
public class Chromosome {

    private final Gene[] genes;
    public final double fitness;
    public final double rawFitness;
    private final static boolean VERIFY_TIMESLOT = false;

    private Chromosome(Gene[] genes, GA ga) {
        this.genes = genes;
        this.rawFitness = ga.fitness.calcRaw(this, ga.problemSet);
        this.fitness = ga.fitness.normalize(this.rawFitness);
    }

    /**
     * Constructs a completely randomized chromosome
     */
    public static Chromosome random(GA ga){
        var genes = new Gene[ga.problemSet.courses.size()];
        for(int i = 0; i < genes.length; i ++){
            genes[i] = new Gene(
                    ga.rng.randomInt(ga.problemSet.rooms.size()),
                    ga.rng.randomInt(ga.problemSet.courses.size()),
                    ga.rng.randomInt(ga.problemSet.timeslots.size())
            );
        }
        return new Chromosome(genes, ga);
    }

    /**
     * @return A stream of all the genes in this chromosome
     */
    public Stream<Gene> genes(){
        return Arrays.stream(this.genes);
    }

    /**
     * A weighted sum of the conflicts present in this chromosome
     */
    public int conflicts(ProblemSet problemSet){
        int conflicts = 0;

        HashSet<Util.Tuple<Timeslot, Room>> roomUsage = new HashSet<>();
        HashSet<Util.Tuple<Timeslot, String>> profSched = new HashSet<>();

        for(var item : this.genes){
            var room = problemSet.rooms.get(item.roomIdx);
            var timeslot = problemSet.timeslots.get(item.timeslotIdx);
            var course = problemSet.courses.get(item.courseIdx);

            if(course.students > room.capacity) conflicts += VERIFY_TIMESLOT?3:4;

            for(int i = timeslot.hour; i < timeslot.hour + course.duration; i ++){
                var currTimeslot = new Timeslot(timeslot.day, i);
                var conflict = !profSched.add(new Util.Tuple<>(currTimeslot, course.professor));
                if(conflict) conflicts += 1;

                conflict = !roomUsage.add(new Util.Tuple<>(currTimeslot, room));
                if(conflict) conflicts += 2;

                if(VERIFY_TIMESLOT){
                    conflict = !problemSet.timeslots.contains(currTimeslot);
                    if(conflict) conflicts += 3;
                }
            }
        }

        return conflicts;
    }

    /**
     * Crate a new chromosome which has a single gene in it randomly mutated
     */
    public Chromosome singleGeneMutation(GA ga){
        var copy = genes.clone();
        copy[ga.rng.randomInt(copy.length)] = new Gene(
                ga.rng.randomInt(ga.problemSet.rooms.size()),
                ga.rng.randomInt(ga.problemSet.courses.size()),
                ga.rng.randomInt(ga.problemSet.timeslots.size())
        );
        return new Chromosome(copy, ga);
    }

    /**
     * Perform one point crossover on this and another chromosome creating two children as a result
     */
    public Util.Tuple<Chromosome, Chromosome> onePointCrossover(Chromosome other, GA ga){
        var g1 = this.genes.clone();
        var g2 = other.genes.clone();
        var index = ga.rng.randomInt(Math.min(g1.length, g2.length));
        for(int i = index; i < Math.min(g1.length, g2.length); i++){
            var tmp = g1[i];
            g1[i] = g2[i];
            g2[i] = tmp;
        }
        return new Util.Tuple<>(new Chromosome(g1, ga), new Chromosome(g2, ga));
    }


    /**
     * Perform uniform crossover on this and another chromosome creating two children as a result
     */
    public Util.Tuple<Chromosome, Chromosome> uniformCrossover(Chromosome other, GA ga){
        var g1 = this.genes.clone();
        var g2 = other.genes.clone();
        for(int i = 0; i < Math.min(g1.length, g2.length); i++){
            if(ga.rng.percent(0.5)){
                var tmp = g1[i];
                g1[i] = g2[i];
                g2[i] = tmp;
            }
        }
        return new Util.Tuple<>(new Chromosome(g1, ga), new Chromosome(g2, ga));
    }

    /**
     * Perform best attempt crossover on this and another chromosome creating two children as a result
     */
    public Util.Tuple<Chromosome, Chromosome> bestAttemptCrossover(Chromosome other, GA ga){
        var c1 = this.genes.clone();
        var c2 = other.genes.clone();

        HashSet<Util.Tuple<Timeslot, Room>> roomUsage1 = new HashSet<>();
        HashSet<Util.Tuple<Timeslot, String>> profSched1 = new HashSet<>();
        HashSet<Util.Tuple<Timeslot, Room>> roomUsage2 = new HashSet<>();
        HashSet<Util.Tuple<Timeslot, String>> profSched2 = new HashSet<>();

        var g1 = new Util.QuaTuple<Gene, Room, Timeslot, Course>();
        var g2 = new Util.QuaTuple<Gene, Room, Timeslot, Course>();
        BiConsumer<Gene, Util.QuaTuple<Gene, Room, Timeslot, Course>> set = (g, r) -> {
            r.t1 = g;
            r.t2 = ga.problemSet.rooms.get(g.roomIdx);
            r.t3 = ga.problemSet.timeslots.get(g.timeslotIdx);
            r.t4 = ga.problemSet.courses.get(g.courseIdx);
        };
        BiFunction< Util.QuaTuple<Gene, Room, Timeslot, Course>,  Util.QuaTuple<Gene, Room, Timeslot, Course>, Integer> costFunc = (G1, G2) -> {
            var cost = 0;
            if(G1.t4.students > G1.t2.capacity) {
                cost += 1;
            }
            if(G2.t4.students > G2.t2.capacity) {
                cost += 1;
            }
            for(int j = G1.t3.hour; j < G1.t3.hour + G1.t4.duration; j ++){
                var currTimeslot = new Timeslot(G1.t3.day, j);
                cost += profSched1.contains(new Util.Tuple<>(currTimeslot, G1.t4.professor))?1:0;
                cost += roomUsage1.contains(new Util.Tuple<>(currTimeslot, G1.t2))?1:0;
                if(VERIFY_TIMESLOT)
                    cost += ga.problemSet.timeslots.contains(currTimeslot)?0:1;
            }
            for(int j = G2.t3.hour; j < G2.t3.hour + G2.t4.duration; j ++){
                var currTimeslot = new Timeslot(G2.t3.day, j);
                cost += profSched2.contains(new Util.Tuple<>(currTimeslot, G2.t4.professor))?1:0;
                cost += roomUsage2.contains(new Util.Tuple<>(currTimeslot, G2.t2))?1:0;
                if(VERIFY_TIMESLOT)
                    cost += ga.problemSet.timeslots.contains(currTimeslot)?0:1;
            }

            return cost;
        };
        var length = Math.min(c1.length, c2.length);
        for(int i = 0; i != length; i ++){
            set.accept(c1[i], g1);
            set.accept(c2[length-1-i], g2);

            int nonSwapCost = costFunc.apply(g1, g2);
            int swapCost = costFunc.apply(g2, g1);

            if(swapCost < nonSwapCost || (swapCost == nonSwapCost && ga.rng.percent(0.5))) {
                var tmp = g1;
                g1 = g2;
                g2 = tmp;
            }

            c1[i] = g1.t1;
            c2[length-1-i] = g2.t1;

            for(int j = g1.t3.hour; j < g1.t3.hour + g1.t4.duration; j ++){
                var currTimeslot = new Timeslot(g1.t3.day, j);
                profSched1.add(new Util.Tuple<>(currTimeslot, g1.t4.professor));
                roomUsage1.add(new Util.Tuple<>(currTimeslot, g1.t2));
            }

            for(int j = g2.t3.hour; j < g2.t3.hour + g2.t4.duration; j ++){
                var currTimeslot = new Timeslot(g2.t3.day, j);
                profSched2.add(new Util.Tuple<>(currTimeslot, g2.t4.professor));
                roomUsage2.add(new Util.Tuple<>(currTimeslot, g2.t2));
            }
        }
        return new Util.Tuple<>(new Chromosome(c1, ga), new Chromosome(c2, ga));
    }
}
