package util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    /**
     * Something that holds two somethings
     */
    public static final class Tuple<T1, T2>{
        public T1 t1;
        public T2 t2;

        public Tuple(T1 t1, T2 t2){
            this.t1 = t1;
            this.t2 = t2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple<?, ?> tuple = (Tuple<?, ?>) o;
            return Objects.equals(t1, tuple.t1) && Objects.equals(t2, tuple.t2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2);
        }

        @Override
        public String toString() {
            return "(" + t1 +", " + t2 + ')';
        }
    }

    /**
     * Something that holds FOUR somethings
     */
    public static final class QuaTuple<T1, T2, T3, T4>{
        public T1 t1;
        public T2 t2;
        public T3 t3;
        public T4 t4;

        public QuaTuple() {}

        public QuaTuple(T1 t1, T2 t2, T3 t3, T4 t4) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuaTuple<?, ?, ?, ?> quaTuple = (QuaTuple<?, ?, ?, ?>) o;
            return Objects.equals(t1, quaTuple.t1) && Objects.equals(t2, quaTuple.t2) && Objects.equals(t3, quaTuple.t3) && Objects.equals(t4, quaTuple.t4);
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2, t3, t4);
        }

        @Override
        public String toString() {
            return "(" +
                    "t1: " + t1 +
                    ", t2: " + t2 +
                    ", t3: " + t3 +
                    ", t4: " + t4 +
                    ')';
        }
    }

    public static String indent(String... lines){
        return indent(Arrays.stream(lines));
    }

    public static String indent(Stream<String> lines){
        return "\t"+lines.flatMap(String::lines).collect(Collectors.joining("\n\t"));
    }

    public static String lines(String... lines){
        return String.join("\n", lines);
    }

    public static String field(String fieldName, String value){
        return "\"" + fieldName + "\": " + value;
    }

    public static String field(String fieldName, int value){
        return "\"" + fieldName + "\": " + value;
    }

    public static String field(String fieldName, long value){
        return "\"" + fieldName + "\": " + value;
    }

    public static String field(String fieldName, double value){
        return "\"" + fieldName + "\": " + value;
    }

    public static String field(String fieldName, boolean value){
        return "\"" + fieldName + "\": " + value;
    }

    public static String fieldStr(String fieldName, String string) {
        return "\"" + fieldName + "\": \"" + string + "\"";
    }

    public static String fieldsln(String... fields){
        return String.join(",\n", fields);
    }

    public static String fieldsln(Stream<String> fields){
        return fields.collect(Collectors.joining(",\n"));
    }

    public static String fields(String... fields){
        return String.join(", ", fields);
    }


    public static String objln(String... fields){
        return lines(
                "{",
                indent(fieldsln(fields)),
                "}"
        );
    }

    public static String arrln(Stream<String> fields){
        return lines(
                "[",
                indent(fieldsln(fields)),
                "]"
        );
    }

    public static String obj(String... fields){
        return  "{"+fields(fields)+"}";
    }
}
