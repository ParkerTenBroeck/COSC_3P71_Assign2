package ga;

import data.ProblemSet;
import data.Room;
import data.Timeslot;
import util.Util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Chromosome implements Comparable<Chromosome> {

    private final Gene[] genes;
    public final double fitness;
    public final double rawFitness;

    private Chromosome(Gene[] genes, GA ga) {
        this.genes = genes;
        this.rawFitness = ga.fitness.calcRaw(this, ga.problemSet);
        this.fitness = ga.fitness.normalize(this.rawFitness);
    }

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
    /*
     * number of conflicts = 0
     * room usage = dict
     * professor schedule = dict
     *
     * for each (course, room, timeslot) in chromosome:
     *     if course number of students > room capacity:
     *         number of conflicts += 2
     *
     *     for each possible hour of class: # (0 to course hours -1)
     *         current slot = timeslot index + hour of class
     *         if (room index, current slot) not in room usage:
     *             room usage [(room index, current slot)] = 0
     *         add 1 to room usage [(room index, current slot)]
     *         if room usage [(room index, current slot)] list is > 1
     *             number conflicts += 3
     *
     *         if (course professor, current slot) not in professor schedule
     *             professor schedule [(course professor, current slot)] = 0
     *         professor schedule [(course professor, current slot)] += 1
     *         if professor schedule [(course professor, current slot)] > 1
     *             number of conflicts += 1
     *
     * return 1 / (1 + number of conflicts)
     */

    public int conflicts(ProblemSet problemSet){
        int conflicts = 0;

        HashSet<Util.Tuple<Timeslot, Room>> roomUsage = new HashSet<>();
        HashSet<Util.Tuple<Timeslot, String>> profSched = new HashSet<>();

        for(var item : this.genes){
            var room = problemSet.rooms.get(item.roomIdx);
            var timeslot = problemSet.timeslots.get(item.timeslotIdx);
            var course = problemSet.courses.get(item.courseIdx);

            if(course.students > room.capacity) conflicts += 2;

            for(int i = timeslot.hour; i < timeslot.hour + course.duration; i ++){
                var currTimeslot = new Timeslot(timeslot.day, i);
                var conflict = !profSched.add(new Util.Tuple<>(currTimeslot, course.professor));
                if(conflict) conflicts += 1;

                conflict = !roomUsage.add(new Util.Tuple<>(currTimeslot, room));
                if(conflict) conflicts += 3;
            }
        }

        return conflicts;
    }

    public int conflicts2(ProblemSet problemSet){

        HashMap<Util.Tuple<Timeslot, Room>, Integer> roomUsage = new HashMap<>();
        HashMap<Util.Tuple<Timeslot, String>, Integer> profSched = new HashMap<>();
        int overbooked = 0;

        for(var item : this.genes){
            var room = problemSet.rooms.get(item.roomIdx);
            var timeslot = problemSet.timeslots.get(item.timeslotIdx);
            var course = problemSet.courses.get(item.courseIdx);

            if(course.students > room.capacity) overbooked += 1;

            for(int i = timeslot.hour; i < timeslot.hour + course.duration; i ++){
                var currTimeslot = new Timeslot(timeslot.day, i);
                profSched.compute(new Util.Tuple<>(currTimeslot, course.professor), (k, v) -> v==null?0:v+1);
                roomUsage.compute(new Util.Tuple<>(currTimeslot, room), (k, v) -> v==null?0:v+1);
            }
        }

        return overbooked*3
                +roomUsage.values().stream().mapToInt(Integer::intValue).sum()*2
                +profSched.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Chromosome singleGeneMutation(GA ga){
        var copy = genes.clone();
        copy[ga.rng.randomInt(copy.length)] = new Gene(
                ga.rng.randomInt(ga.problemSet.rooms.size()),
                ga.rng.randomInt(ga.problemSet.courses.size()),
                ga.rng.randomInt(ga.problemSet.timeslots.size())
        );
        return new Chromosome(copy, ga);
    }

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

    public Util.Tuple<Chromosome, Chromosome> bestAttemptCrossover(Chromosome other, GA ga){
        var g1 = this.genes.clone();
        var g2 = other.genes.clone();

        int capacityConflicts1 = 0;
        HashMap<Util.Tuple<Timeslot, Room>, Integer> roomUsage1 = new HashMap<>();
        HashMap<Util.Tuple<Timeslot, String>, Integer> profSched1 = new HashMap<>();
        int capacityConflicts2 = 0;
        HashMap<Util.Tuple<Timeslot, Room>, Integer> roomUsage2 = new HashMap<>();
        HashMap<Util.Tuple<Timeslot, String>, Integer> profSched2 = new HashMap<>();



        var length = Math.min(g1.length, g2.length);
        for(int i = 0; i != length; i ++){

            var room1 = ga.problemSet.rooms.get(g1[i].roomIdx);
            var timeslot1 = ga.problemSet.timeslots.get(g1[i].timeslotIdx);
            var course1 = ga.problemSet.courses.get(g1[i].courseIdx);

            var room2 = ga.problemSet.rooms.get(g2[length-1-i].roomIdx);
            var timeslot2 = ga.problemSet.timeslots.get(g2[length-1-i].timeslotIdx);
            var course2 = ga.problemSet.courses.get(g2[length-1-i].courseIdx);


            var g1Cons1 = capacityConflicts1;
            g1Cons1 += roomUsage1.values().stream().filter(v -> v > 1).mapToInt(Integer::intValue).sum();
            g1Cons1 += profSched1.values().stream().filter(v -> v > 1).mapToInt(Integer::intValue).sum();
            var g1Cons2 = g1Cons1;

            var g2Cons1 = capacityConflicts2*3;
            g2Cons1 += roomUsage2.values().stream().filter(v -> v > 1).mapToInt(Integer::intValue).sum();
            g2Cons1 += profSched2.values().stream().filter(v -> v > 1).mapToInt(Integer::intValue).sum();
            var g2Cons2 = g2Cons1;


            if(course1.students > room1.capacity) g1Cons2 += 1;
            for(int j = timeslot1.hour; j < timeslot1.hour + course1.duration; j ++){
                var currTimeslot = new Timeslot(timeslot1.day, j);
                g1Cons2 += profSched2.containsKey(new Util.Tuple<>(currTimeslot, course1.professor))?1:0;
                g1Cons2 += roomUsage2.containsKey(new Util.Tuple<>(currTimeslot, room1))?1:0;
            }
            if(course2.students > room2.capacity) g2Cons1 += 1;
            for(int j = timeslot2.hour; j < timeslot2.hour + course2.duration; j ++){
                var currTimeslot = new Timeslot(timeslot2.day, j);
                g2Cons1 += profSched1.containsKey(new Util.Tuple<>(currTimeslot, course2.professor))?1:0;
                g2Cons1 += roomUsage1.containsKey(new Util.Tuple<>(currTimeslot, room2))?1:0;
            }
            if(course1.students > room1.capacity) g1Cons1 += 1;
            for(int j = timeslot1.hour; j < timeslot1.hour + course1.duration; j ++){
                var currTimeslot = new Timeslot(timeslot1.day, j);
                g1Cons1 += profSched1.containsKey(new Util.Tuple<>(currTimeslot, course1.professor))?1:0;
                g1Cons1 += roomUsage1.containsKey(new Util.Tuple<>(currTimeslot, room1))?1:0;
            }
            if(course2.students > room2.capacity) g2Cons2 += 1;
            for(int j = timeslot2.hour; j < timeslot2.hour + course2.duration; j ++){
                var currTimeslot = new Timeslot(timeslot2.day, j);
                g2Cons2 += profSched2.containsKey(new Util.Tuple<>(currTimeslot, course2.professor))?1:0;
                g2Cons2 += roomUsage2.containsKey(new Util.Tuple<>(currTimeslot, room2))?1:0;
            }

            boolean swap;
            if(g2Cons1 + g1Cons2 < g1Cons1 + g2Cons2)
                swap = true;
            else if(g2Cons1 + g1Cons2 == g1Cons1 + g2Cons2)
                swap = ga.rng.percent(0.5);
            else
                swap = false;

            if(swap){
                var tmp = g1[i];
                g1[i] = g2[length-1-i];
                g2[length-1-i] = tmp;

                if(course1.students > room1.capacity) capacityConflicts2 += 1;
                for(int j = timeslot1.hour; j < timeslot1.hour + course1.duration; j ++){
                    var currTimeslot = new Timeslot(timeslot1.day, j);
                    profSched2.compute(new Util.Tuple<>(currTimeslot, course1.professor), (k, v) -> v==null?0:v+1);
                    roomUsage2.compute(new Util.Tuple<>(currTimeslot, room1), (k, v) -> v==null?0:v+1);
                }

                if(course2.students > room2.capacity) capacityConflicts1 += 1;
                for(int j = timeslot2.hour; j < timeslot2.hour + course2.duration; j ++){
                    var currTimeslot = new Timeslot(timeslot2.day, j);
                    profSched1.compute(new Util.Tuple<>(currTimeslot, course2.professor), (k, v) -> v==null?0:v+1);
                    roomUsage1.compute(new Util.Tuple<>(currTimeslot, room2), (k, v) -> v==null?0:v+1);
                }
            }else{
                if(course1.students > room1.capacity) capacityConflicts1 += 1;
                for(int j = timeslot1.hour; j < timeslot1.hour + course1.duration; j ++){
                    var currTimeslot = new Timeslot(timeslot1.day, j);
                    profSched1.compute(new Util.Tuple<>(currTimeslot, course1.professor), (k, v) -> v==null?0:v+1);
                    roomUsage1.compute(new Util.Tuple<>(currTimeslot, room1), (k, v) -> v==null?0:v+1);
                }

                if(course2.students > room2.capacity) capacityConflicts2 += 1;
                for(int j = timeslot2.hour; j < timeslot2.hour + course2.duration; j ++){
                    var currTimeslot = new Timeslot(timeslot2.day, j);
                    profSched2.compute(new Util.Tuple<>(currTimeslot, course2.professor), (k, v) -> v==null?0:v+1);
                    roomUsage2.compute(new Util.Tuple<>(currTimeslot, room2), (k, v) -> v==null?0:v+1);
                }
            }
        }
        return new Util.Tuple<>(new Chromosome(g1, ga), new Chromosome(g2, ga));
    }

    @Override
    public int compareTo(Chromosome o) {
        return Double.compare(this.fitness, o.fitness);
    }

    public String toString(ProblemSet problemSet) {
        return Util.lines(
            "Chromosome{",
            Util.indent(
            "fitness: " + fitness,
                "genes: [",
                Util.indent(Arrays.stream(genes).map(g -> g.toString(problemSet))),
                "]"
            ),
            "}"
        );
    }
}
