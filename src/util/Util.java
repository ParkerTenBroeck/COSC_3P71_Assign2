package util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {
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

    public static String indent(String... lines){
        return indent(Arrays.stream(lines));
    }

    public static String indent(Stream<String> lines){
        return "\t"+lines.flatMap(String::lines).collect(Collectors.joining("\n\t"));
    }

    public static String lines(String... lines){
        return String.join("\n", lines);
    }
}
