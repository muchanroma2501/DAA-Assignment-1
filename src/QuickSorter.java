import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class QuickSorter {
    private QuickSorter() {
    }

    public static Metrics sort(int[] values) {
        Objects.requireNonNull(values, "values");
        long start = System.nanoTime();
        Counters counters = new Counters();
        if (values.length > 1) {
            sort(values, 0, values.length - 1, 1, counters);
        }
        return new Metrics(System.nanoTime() - start, counters.maxDepth, counters.comparisons,
                counters.swaps);
    }

    private static void sort(int[] values, int low, int high, int depth, Counters counters) {
        while (low < high) {
            counters.maxDepth = Math.max(counters.maxDepth, depth);
            int pivotIndex = ThreadLocalRandom.current().nextInt(low, high + 1);
            int pivot = values[pivotIndex];
            int less = low;
            int current = low;
            int greater = high;

            while (current <= greater) {
                counters.comparisons++;
                if (values[current] < pivot) {
                    swap(values, less++, current++, counters);
                } else {
                    counters.comparisons++;
                    if (values[current] > pivot) {
                        swap(values, current, greater--, counters);
                    } else {
                        current++;
                    }
                }
            }

            int leftSize = less - low;
            int rightSize = high - greater;
            if (leftSize < rightSize) {
                if (low < less - 1) {
                    sort(values, low, less - 1, depth + 1, counters);
                }
                low = greater + 1;
            } else {
                if (greater + 1 < high) {
                    sort(values, greater + 1, high, depth + 1, counters);
                }
                high = less - 1;
            }
        }
    }

    private static void swap(int[] values, int first, int second, Counters counters) {
        if (first != second) {
            int temporary = values[first];
            values[first] = values[second];
            values[second] = temporary;
            counters.swaps++;
        }
    }

    private static final class Counters {
        private long comparisons;
        private long swaps;
        private int maxDepth;
    }

    public record Metrics(long executionTimeNanos, int maxRecursionDepth, long comparisons,
                          long swaps) {
    }
}
