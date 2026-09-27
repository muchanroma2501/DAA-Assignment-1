import java.util.Objects;

public final class MergeSorter {
    // Tiny ranges avoid recursive and merge overhead by using insertion sort instead.
    private static final int INSERTION_SORT_CUTOFF = 15;

    private MergeSorter() {
    }

    public static Metrics sort(int[] values) {
        Objects.requireNonNull(values, "values");
        long start = System.nanoTime();
        int[] auxiliary = new int[values.length];
        Counters counters = new Counters();
        if (values.length > 1) {
            sort(values, auxiliary, 0, values.length - 1, 1, counters);
        }
        return new Metrics(System.nanoTime() - start, counters.maxDepth, counters.comparisons);
    }

    private static void sort(int[] values, int[] auxiliary, int low, int high, int depth,
                             Counters counters) {
        counters.maxDepth = Math.max(counters.maxDepth, depth);
        if (high - low + 1 <= INSERTION_SORT_CUTOFF) {
            insertionSort(values, low, high, counters);
            return;
        }

        int middle = low + (high - low) / 2;
        sort(values, auxiliary, low, middle, depth + 1, counters);
        sort(values, auxiliary, middle + 1, high, depth + 1, counters);
        if (compare(values[middle], values[middle + 1], counters) <= 0) {
            return;
        }
        merge(values, auxiliary, low, middle, high, counters);
    }

    private static void insertionSort(int[] values, int low, int high, Counters counters) {
        for (int i = low + 1; i <= high; i++) {
            int value = values[i];
            int j = i - 1;
            while (j >= low && compare(values[j], value, counters) > 0) {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    private static void merge(int[] values, int[] auxiliary, int low, int middle, int high,
                              Counters counters) {
        System.arraycopy(values, low, auxiliary, low, high - low + 1);
        int left = low;
        int right = middle + 1;
        for (int destination = low; destination <= high; destination++) {
            if (left > middle) {
                values[destination] = auxiliary[right++];
            } else if (right > high) {
                values[destination] = auxiliary[left++];
            } else if (compare(auxiliary[left], auxiliary[right], counters) <= 0) {
                values[destination] = auxiliary[left++];
            } else {
                values[destination] = auxiliary[right++];
            }
        }
    }

    private static int compare(int first, int second, Counters counters) {
        counters.comparisons++;
        return Integer.compare(first, second);
    }

    private static final class Counters {
        private long comparisons;
        private int maxDepth;
    }

    public record Metrics(long executionTimeNanos, int maxRecursionDepth, long comparisons) {
    }
}
