package ga;

import data.ProblemSet;

public class Gene {
    public final int roomIdx;
    public final int courseIdx;
    public final int timeslotIdx;

    public Gene(int roomIdx, int courseIdx, int timeslotIdx) {
        this.roomIdx = roomIdx;
        this.courseIdx = courseIdx;
        this.timeslotIdx = timeslotIdx;
    }

    public String toString(ProblemSet problemSet) {
        return "Gene(" +
                problemSet.timeslots.get(timeslotIdx) + ", " +
                problemSet.rooms.get(roomIdx) + ", " +
                problemSet.courses.get(courseIdx) + ")";
    }
}
