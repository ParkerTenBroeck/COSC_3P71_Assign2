package data;

import java.util.Objects;

public class Timeslot {
    public Day day;
    public int hour;

    public Timeslot(Day day, int hour) {
        this.day = day;
        this.hour = hour;
    }

    @Override
    public String toString() {
        return "Timeslot{" +
                "day: " + day +
                ", hour: " + hour +
                '}';
    }

    @Override
    public Timeslot clone(){
        return new Timeslot(this.day, this.hour);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timeslot timeslot = (Timeslot) o;
        return hour == timeslot.hour && day == timeslot.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, hour);
    }

    public enum Day{
        Monday,
        Tuesday,
        Wednesday,
        Thursday,
        Friday,
        Saturday,
        Sunday
    }
}
