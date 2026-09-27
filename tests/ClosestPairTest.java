import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ClosestPairTest {
    @Test
    void agreesWithBruteForceOnSmallRandomDatasets() {
        Random random = new Random(4);
        int[] sizes = {2, 3, 4, 10, 100, 500, 2_000};
        for (int size : sizes) {
            Point[] points = new Point[size];
            for (int i = 0; i < size; i++) {
                points[i] = new Point(random.nextInt(1_000), random.nextInt(1_000));
            }

            ClosestPairSolver.Result expected = ClosestPairSolver.bruteForce(points);
            ClosestPairSolver.Result actual = ClosestPairSolver.solve(points);

            assertEquals(expected.distance(), actual.distance(), 1.0e-6, "size=" + size);
        }
    }

    @Test
    void agreesWithBruteForceOnNonduplicateFloatingPointDatasets() {
        Random random = new Random(5);
        for (int test = 0; test < 30; test++) {
            Point[] points = new Point[2 + random.nextInt(300)];
            for (int i = 0; i < points.length; i++) {
                points[i] = new Point(random.nextDouble() * 1_000,
                        random.nextDouble() * 1_000);
            }
            double expected = ClosestPairSolver.bruteForce(points).distance();
            double actual = ClosestPairSolver.solve(points).distance();
            assertEquals(expected, actual, 1.0e-6, "test=" + test);
        }
    }

    @Test
    void handlesDuplicatePointsAndInputsWithFewerThanTwoPoints() {
        Point[] duplicates = {new Point(2, 5), new Point(2, 5), new Point(9, 1)};
        assertEquals(0.0, ClosestPairSolver.solve(duplicates).distance());

        assertEquals(Double.POSITIVE_INFINITY, ClosestPairSolver.solve(new Point[0]).distance());
        assertNull(ClosestPairSolver.solve(new Point[]{new Point(1, 1)}).pair());
    }
}
