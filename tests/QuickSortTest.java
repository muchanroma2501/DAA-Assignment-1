import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class QuickSortTest {
    @Test
    void sortsEmptyAndSingleElementArrays() {
        assertSortedByReference(new int[0]);
        assertSortedByReference(new int[]{7});
    }

    @Test
    void sortsRandomInputAndBoundsRecursionDepth() {
        int[] input = new Random(2).ints(20_000, -10_000, 10_001).toArray();
        int[] expected = input.clone();
        Arrays.sort(expected);

        QuickSorter.Metrics metrics = QuickSorter.sort(input);

        assertArrayEquals(expected, input);
        assertTrue(metrics.maxRecursionDepth() < 32);
    }

    @Test
    void sortsAlreadySortedInput() {
        int[] input = new int[2_000];
        for (int i = 0; i < input.length; i++) {
            input[i] = i;
        }
        assertSortedByReference(input);
    }

    @Test
    void sortsReverseSortedInput() {
        int[] input = new int[2_000];
        for (int i = 0; i < input.length; i++) {
            input[i] = input.length - i;
        }
        assertSortedByReference(input);
    }

    @Test
    void sortsDuplicateHeavyInput() {
        assertSortedByReference(new Random(12).ints(10_000, 0, 8).toArray());
    }

    private static void assertSortedByReference(int[] input) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        QuickSorter.sort(input);
        assertArrayEquals(expected, input);
    }
}
