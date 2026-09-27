import java.util.Arrays;
import java.util.Objects;

public final class ClosestPairSolver {
    private ClosestPairSolver() {
    }

    public static Result solve(Point[] points) {
        Objects.requireNonNull(points, "points");
        long start = System.nanoTime();
        Counters counters = new Counters();
        PointRef[] byX = new PointRef[points.length];
        for (int i = 0; i < points.length; i++) {
            byX[i] = new PointRef(Objects.requireNonNull(points[i], "points[" + i + "]"), i);
        }
        Arrays.sort(byX, (first, second) -> {
            counters.comparisons++;
            int byCoordinate = Double.compare(first.point.x(), second.point.x());
            if (byCoordinate == 0) {
                byCoordinate = Double.compare(first.point.y(), second.point.y());
            }
            return byCoordinate == 0 ? Integer.compare(first.originalIndex, second.originalIndex)
                    : byCoordinate;
        });
        for (int i = 0; i < byX.length; i++) {
            byX[i].rank = i;
        }
        PointRef[] byY = byX.clone();
        Arrays.sort(byY, (first, second) -> {
            counters.comparisons++;
            int byCoordinate = Double.compare(first.point.y(), second.point.y());
            if (byCoordinate == 0) {
                byCoordinate = Double.compare(first.point.x(), second.point.x());
            }
            return byCoordinate == 0 ? Integer.compare(first.rank, second.rank) : byCoordinate;
        });

        Candidate candidate;
        if (points.length < 2) {
            candidate = new Candidate(null, null, Double.POSITIVE_INFINITY);
        } else {
            candidate = solve(byX, byY, 0, points.length, 1, counters);
        }
        return new Result(pairOf(candidate), candidate.distance, System.nanoTime() - start,
                counters.maxDepth, counters.distanceCalculations, counters.comparisons);
    }

    public static Result bruteForce(Point[] points) {
        Objects.requireNonNull(points, "points");
        long start = System.nanoTime();
        Counters counters = new Counters();
        Candidate best = new Candidate(null, null, Double.POSITIVE_INFINITY);
        for (int i = 0; i < points.length; i++) {
            Point first = Objects.requireNonNull(points[i], "points[" + i + "]");
            for (int j = i + 1; j < points.length; j++) {
                Point second = Objects.requireNonNull(points[j], "points[" + j + "]");
                double distance = distance(first, second, counters);
                counters.comparisons++;
                if (distance < best.distance) {
                    best = new Candidate(new PointRef(first, i), new PointRef(second, j), distance);
                }
            }
        }
        return new Result(pairOf(best), best.distance, System.nanoTime() - start,
                0, counters.distanceCalculations, counters.comparisons);
    }

    private static Candidate solve(PointRef[] byX, PointRef[] byY, int low, int high, int depth,
                                   Counters counters) {
        counters.maxDepth = Math.max(counters.maxDepth, depth);
        int length = high - low;
        if (length <= 3) {
            Candidate best = new Candidate(null, null, Double.POSITIVE_INFINITY);
            for (int i = low; i < high; i++) {
                for (int j = i + 1; j < high; j++) {
                    double distance = distance(byX[i].point, byX[j].point, counters);
                    counters.comparisons++;
                    if (distance < best.distance) {
                        best = new Candidate(byX[i], byX[j], distance);
                    }
                }
            }
            return best;
        }

        int middle = low + length / 2;
        PointRef[] leftByY = new PointRef[middle - low];
        PointRef[] rightByY = new PointRef[high - middle];
        int leftPosition = 0;
        int rightPosition = 0;
        for (PointRef point : byY) {
            counters.comparisons++;
            if (point.rank < middle) {
                leftByY[leftPosition++] = point;
            } else {
                rightByY[rightPosition++] = point;
            }
        }

        Candidate left = solve(byX, leftByY, low, middle, depth + 1, counters);
        Candidate right = solve(byX, rightByY, middle, high, depth + 1, counters);
        Candidate best = nearer(left, right, counters);
        double dividerX = byX[middle].point.x();
        PointRef[] strip = new PointRef[length];
        int stripSize = 0;
        for (PointRef current : byY) {
            counters.comparisons++;
            if (Math.abs(current.point.x() - dividerX) < best.distance) {
                strip[stripSize++] = current;
            }
        }

        for (int i = 0; i < stripSize; i++) {
            PointRef first = strip[i];
            for (int j = i - 1; j >= 0; j--) {
                PointRef second = strip[j];
                counters.comparisons++;
                if (first.point.y() - second.point.y() >= best.distance) {
                    break;
                }
                double distance = distance(first.point, second.point, counters);
                counters.comparisons++;
                if (distance < best.distance) {
                    best = new Candidate(first, second, distance);
                }
            }
        }
        return best;
    }

    private static Candidate nearer(Candidate first, Candidate second, Counters counters) {
        counters.comparisons++;
        return first.distance <= second.distance ? first : second;
    }

    private static double distance(Point first, Point second, Counters counters) {
        counters.distanceCalculations++;
        return Math.hypot(first.x() - second.x(), first.y() - second.y());
    }

    private static PointPair pairOf(Candidate candidate) {
        if (candidate.first == null) {
            return null;
        }
        return new PointPair(candidate.first.point, candidate.second.point);
    }

    private static final class PointRef {
        private final Point point;
        private final int originalIndex;
        private int rank;

        private PointRef(Point point, int originalIndex) {
            this.point = point;
            this.originalIndex = originalIndex;
        }
    }

    private record Candidate(PointRef first, PointRef second, double distance) {
    }

    private static final class Counters {
        private int maxDepth;
        private long distanceCalculations;
        private long comparisons;
    }

    public record PointPair(Point first, Point second) {
    }

    public record Result(PointPair pair, double distance, long executionTimeNanos,
                         int maxRecursionDepth, long distanceCalculations, long comparisons) {
    }
}
