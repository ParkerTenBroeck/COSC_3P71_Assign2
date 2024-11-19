package data;

import java.util.Objects;

public class Room {
    public String name;
    public int capacity;

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Room clone(){
        return new Room(this.name, this.capacity);
    }

    @Override
    public String toString() {
        return "Room{" +
                "name: '" + name + '\'' +
                ", capacity: " + capacity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return capacity == room.capacity && Objects.equals(name, room.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, capacity);
    }
}
