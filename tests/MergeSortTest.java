import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MergeSortTest {
    @Test
    void sortsEmptyAndSingleElementArrays() {
        assertSortedByReference(new int[0]);
        assertSortedByReference(new int[]{7});
    }

    @Test
    void sortsRandomInput() {
        assertSortedByReference(new Random(1).ints(20_000, -10_000, 10_001).toArray());
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
        assertSortedByReference(new Random(11).ints(10_000, 0, 8).toArray());
    }

    private static void assertSortedByReference(int[] input) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        MergeSorter.sort(input);
        assertArrayEquals(expected, input);
    }
}
