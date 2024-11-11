package data;

import java.util.Objects;

public class Course {
    public String name;
    public String professor;
    public int students;
    public int duration;

    public Course(String name, String professor, int students, int duration) {
        this.name = name;
        this.professor = professor;
        this.students = students;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Course{" +
                "name: '" + name + '\'' +
                ", professor: '" + professor + '\'' +
                ", students: " + students +
                ", duration: " + duration +
                '}';
    }

    @Override
    public Course clone() {
        return new Course(this.name, this.professor, this.students, this.duration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return students == course.students && duration == course.duration && Objects.equals(name, course.name) && Objects.equals(professor, course.professor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, professor, students, duration);
    }
}
