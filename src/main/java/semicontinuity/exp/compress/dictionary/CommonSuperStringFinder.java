package semicontinuity.exp.compress.dictionary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Common Super-string algorithm.
 *
 * Common super-string is a string, that contains all given strings.
 * The goal is to find the shortest possible common super-string.
 *
 * This algorithm computes overlaps between all given substrings,
 * and greedily merges substrings, starting with the pair with the biggest overlap.
 * Extremely memory-hungry.
 */
public class CommonSuperStringFinder {

    private final List<Bytes> substrings;

    @SuppressWarnings("WeakerAccess")
    public CommonSuperStringFinder(List<Bytes> substrings) {
        this.substrings = substrings;
    }

    static class Overlap implements Comparable<Overlap> {
        final int value;
        final Bytes first;
        final Bytes second;

        Overlap(int value, Bytes first, Bytes second) {
            this.value = value;
            this.first = first;
            this.second = second;
        }

        @Override
        public int compareTo(@Nonnull Overlap o) {
            return o.value - this.value;
        }

        @Override
        public String toString() {
            return "Overlap{value=" + value + ", first=" + first + ", second=" + second + '}';
        }
    }
    
    static class Bytes implements Comparable<Bytes> {
        final byte[] value;
        final Map<Bytes, Overlap> in;
        final PriorityQueue<CommonSuperStringFinder.Overlap> queue;

        Bytes(byte[] value, int capacity) {
            this.value = value;
            this.in = new HashMap<>(capacity);
            this.queue = new PriorityQueue<>(capacity);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Bytes bytes = (Bytes) o;
            return Arrays.equals(value, bytes.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return DatatypeConverter.printHexBinary(value);
        }

        public int indexOf(Bytes other) {
            byte[] otherValue = other.value;
            if (this.value.length < otherValue.length) {
                return -1;
            }
            int maxStart = this.value.length - otherValue.length;
            outer:
            for (int start = 0; start <= maxStart; start++) {
                for (int i = 0; i < otherValue.length; i++) {
                    if (this.value[start + i] != otherValue[i]) {
                        continue outer;
                    }
                }
                return start;
            }
            return -1;
        }

        @Override
        public int compareTo(@Nonnull Bytes o) {
            return compare(value, o.value);
        }

        public static int compare(byte[] left, byte[] right) {
            for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
                int a = left[i] & 0xff;
                int b = right[j] & 0xff;
                if (a != b) {
                    return a - b;
                }
            }
            return left.length - right.length;
        }

        int overlapLength(Bytes second) {
            int maxPossibleOverlap = Math.min(this.value.length, second.value.length);
            outer:
            for (int overlap = maxPossibleOverlap; overlap > 0; --overlap) {
                int i = this.value.length - overlap;
                int j = 0;
                for (int k = 0; k < overlap; k++) {
                    if (this.value[i++] != second.value[j++]) {
                        continue outer;
                    }
                }
                return overlap;
            }
            return 0;
        }

        static Bytes collapse(Bytes first, Bytes second, int value, int capacity) {
            byte[] collapsedValue = new byte[first.value.length + second.value.length - value];
            System.arraycopy(first.value, 0, collapsedValue, 0, first.value.length);
            System.arraycopy(second.value, value, collapsedValue, first.value.length, second.value.length - value);
            return new Bytes(collapsedValue, capacity);
        }
    }

    private void collapse(Overlap overlap) {
        System.out.println("Collapsing " + overlap);
        Bytes first = overlap.first;
        Bytes second = overlap.second;
        int value = overlap.value;


        remove(first);
        remove(second);
        System.out.println("Removed");

        int capacity = substrings.size();
        Bytes collapsed = Bytes.collapse(first, second, value, capacity);
        substrings.add(collapsed);
        computeOverlaps(collapsed);
        System.out.println("Added");
    }



    private void computeOverlaps() {
        for (int i = 0; i < substrings.size(); i++) {
            if (i % 1000 == 0) {
                System.out.println("i = " + i);
            }
            Bytes substring = substrings.get(i);
            computeOverlaps(substring);
        }
    }
    
    private void computeOverlaps(Bytes bytes) {
        for (Bytes other : substrings) {
            if (other == bytes) {
                continue;
            }

            // create outgoing edges
            int overlapLength1 = bytes.overlapLength(other);
            if (overlapLength1 > 0) {
                Overlap overlap = new Overlap(overlapLength1, bytes, other);
                other.in.put(bytes, overlap);
                bytes.queue.add(overlap);
            }

            // create incoming edges
            int overlapLength2 = other.overlapLength(bytes);
            if (overlapLength2 > 0) {
                Overlap overlap = new Overlap(overlapLength2, other, bytes);
                bytes.in.put(other, overlap);
                other.queue.add(overlap);
            }
        }
    }
    
    private void remove(Bytes bytes) {
        bytes.in.forEach((from, overlap) -> {
            from.queue.remove(overlap);
        });
        bytes.queue.forEach(overlap -> {
            overlap.second.in.remove(bytes);
        });
        bytes.queue.clear();

        substrings.remove(bytes);
    }



    private void solve() {
        int i = 0;
        for (;;) {
            System.out.println("i = " + ++i);

            // priority queue of priority queues
            Overlap best = null;
            for (Bytes substring : substrings) {
                Overlap candidate = substring.queue.peek();
                if (candidate == null) {
                    continue;
                }
                if (best == null) {
                    best = candidate;
                } else {
                    if (candidate.value > best.value) {
                        best = candidate;
                    }
                }
            }

            if (best == null) {
                break;
            }
            collapse(best);
        }
    }

    public static void main(String[] args) throws IOException {
        List<Bytes> substrings = load(new File(args[0]));
        info(substrings, false);
        substrings = removeContainedInOthers(substrings);
        info(substrings, false);

        CommonSuperStringFinder solver = new CommonSuperStringFinder(
                substrings
                /*new PriorityQueue<>(substrings.size() * substrings.size() / 1000)*/
        );
        solver.computeOverlaps();
        solver.solve();

        int l = 0;
        for (Bytes bytes : substrings) {
            l += bytes.value.length;
        }
        byte[] result = new byte[l];

        int i = 0;
        for (Bytes bytes : substrings) {
            System.arraycopy(bytes.value, 0, result, i, bytes.value.length);
            i += bytes.value.length;
        }
        FileOutputStream stream = new FileOutputStream(args[1]);
        stream.write(result);
        stream.close();
    }


    private static List<Bytes> load(File file2) throws IOException {
        ArrayNode input = (ArrayNode) new ObjectMapper().readTree(file2);

        List<Bytes> substrings = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ArrayNode array = (ArrayNode) input.get(i);
            byte[] bytes = new byte[array.size()];
            for (int j = 0; j < bytes.length; j++) {
                bytes[j] = (byte) array.get(j).intValue();
            }
            substrings.add(new Bytes(bytes, input.size() / 100));
        }
        return substrings;
    }
    
    private static List<Bytes> removeContainedInOthers(List<Bytes> substrings) {
        boolean[] contained = new boolean[substrings.size()];

        for (int i = 0; i < substrings.size(); i++) {
            Bytes smaller = substrings.get(i);
            for (int j = 0; j < substrings.size(); j++) {
                if (i == j) {
                    continue;
                }
                Bytes larger = substrings.get(j);
                if (larger.indexOf(smaller) >= 0) {
                    contained[i] = true;
                    break;
                }
            }
        }

        ArrayList<Bytes> result = new ArrayList<>();
        for (int c = 0; c < substrings.size(); c++) {
            if (!contained[c]) {
                result.add(substrings.get(c));
            }
        }
        return result;
    }

    private static void info(List<Bytes> substrings, boolean verbose) {
        int l = 0;
        for (Bytes bytes : substrings) {
            l += bytes.value.length;
            if (verbose) {
                System.out.println(bytes);
            }
        }
        System.out.println("l = " + l);
    }

}
