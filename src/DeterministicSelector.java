import java.util.Objects;

public final class DeterministicSelector {
    // Groups of five give the median-of-medians pivot its linear worst-case guarantee.
    private static final int GROUP_SIZE = 5;

    private DeterministicSelector() {
    }

    public static int select(int[] values, int k) {
        return selectWithMetrics(values, k).value();
    }

    public static SelectionResult selectWithMetrics(int[] values, int k) {
        Objects.requireNonNull(values, "values");
        if (k < 0 || k >= values.length) {
            throw new IllegalArgumentException("k must be a valid zero-based index");
        }
        long start = System.nanoTime();
        Counters counters = new Counters();
        int value = select(values, 0, values.length - 1, k, 1, counters);
        return new SelectionResult(value, System.nanoTime() - start, counters.maxDepth,
                counters.comparisons, counters.swaps);
    }

    private static int select(int[] values, int low, int high, int target, int depth,
                              Counters counters) {
        counters.maxDepth = Math.max(counters.maxDepth, depth);
        if (high - low + 1 <= GROUP_SIZE) {
            insertionSort(values, low, high, counters);
            return values[target];
        }

        int pivot = medianOfMedians(values, low, high, depth + 1, counters);
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

        if (target < less) {
            return select(values, low, less - 1, target, depth + 1, counters);
        }
        if (target > greater) {
            return select(values, greater + 1, high, target, depth + 1, counters);
        }
        return pivot;
    }

    private static int medianOfMedians(int[] values, int low, int high, int depth,
                                       Counters counters) {
        int length = high - low + 1;
        if (length <= GROUP_SIZE) {
            insertionSort(values, low, high, counters);
            return values[low + length / 2];
        }

        int medians = 0;
        for (int groupStart = low; groupStart <= high; groupStart += GROUP_SIZE) {
            int groupEnd = Math.min(groupStart + GROUP_SIZE - 1, high);
            insertionSort(values, groupStart, groupEnd, counters);
            int median = groupStart + (groupEnd - groupStart) / 2;
            swap(values, low + medians, median, counters);
            medians++;
        }
        return select(values, low, low + medians - 1, low + medians / 2, depth, counters);
    }

    private static void insertionSort(int[] values, int low, int high, Counters counters) {
        for (int i = low + 1; i <= high; i++) {
            int value = values[i];
            int j = i - 1;
            while (j >= low) {
                counters.comparisons++;
                if (values[j] <= value) {
                    break;
                }
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
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

    public record SelectionResult(int value, long executionTimeNanos, int maxRecursionDepth,
                                  long comparisons, long swaps) {
    }
}
