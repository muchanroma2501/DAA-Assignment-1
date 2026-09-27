import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DeterministicSelectTest {
    @Test
    void matchesSortedReferenceForOneHundredRandomizedCases() {
        Random random = new Random(3);
        for (int iteration = 0; iteration < 100; iteration++) {
            int size = 1 + random.nextInt(500);
            int[] input = random.ints(size, -100, 101).toArray();
            int k = random.nextInt(size);
            int[] sorted = input.clone();
            Arrays.sort(sorted);

            int actual = DeterministicSelector.select(input, k);

            assertEquals(sorted[k], actual, "iteration " + iteration + ", k=" + k);
        }
    }

    @Test
    void rejectsOutOfRangeOrderStatistic() {
        assertThrows(IllegalArgumentException.class,
                () -> DeterministicSelector.select(new int[]{1, 2}, -1));
        assertThrows(IllegalArgumentException.class,
                () -> DeterministicSelector.select(new int[]{1, 2}, 2));
    }
}
