package data;

import util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

public class ProblemSet {
    public final ArrayList<Course> courses = new ArrayList<>();
    public final ArrayList<Room> rooms = new ArrayList<>();
    public final ArrayList<Timeslot> timeslots = new ArrayList<>();

    public ProblemSet(String problemSetDirectory) throws IOException {
        streamFile(problemSetDirectory + File.separator + "courses.txt")
                .map(ProblemSet::parseCourse)
                .forEach(courses::add);
        streamFile(problemSetDirectory + File.separator + "rooms.txt")
                .map(ProblemSet::parseRoom)
                .forEach(rooms::add);
        streamFile(problemSetDirectory + File.separator + "timeslots.txt")
                .map(ProblemSet::parseTimeslot)
                .forEach(timeslots::add);
    }

    private static Stream<String[]> streamFile(String path) throws IOException {
        return Files.readString(Path.of(path)).lines().skip(1).map(s -> s.split(","));
    }

    private static Course parseCourse(String[] args){
        if(args.length != 4) throw new RuntimeException("Invalid course data length");
        return new Course(
                args[0].trim(),
                args[1].trim(),
                Integer.parseInt(args[2].trim()),
                Integer.parseInt(args[3].trim())
        );
    }

    private static Room parseRoom(String[] args){
        if(args.length != 2) throw new RuntimeException("Invalid room data length");
        return new Room(
                args[0].trim(),
                Integer.parseInt(args[1].trim())
        );
    }

    private static Timeslot parseTimeslot(String[] args){
        if(args.length != 2) throw new RuntimeException("Invalid timeslot data length");
        return new Timeslot(
                Timeslot.Day.valueOf(args[0].trim()),
                Integer.parseInt(args[1].trim())
        );
    }

    @Override
    public String toString() {
        return Util.lines(
            "ProblemSet{",
            Util.indent(
              "courses: [", Util.indent(courses.stream().map(Object::toString)), "],",
                "rooms: [", Util.indent(rooms.stream().map(Object::toString)), "],",
                "timeslots: [", Util.indent(timeslots.stream().map(Object::toString)), "]"
            ),
            "}"
        );
    }
}
