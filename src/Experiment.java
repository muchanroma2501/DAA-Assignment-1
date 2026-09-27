import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public final class Experiment {
    private static final int[] INPUT_SIZES = {100, 1_000, 10_000, 50_000, 100_000, 500_000};
    private static final InputType[] INPUT_TYPES = InputType.values();
    private static final String CSV_HEADER =
            "Algorithm,InputSize,InputType,ExecutionTimeMs,MaxRecursionDepth,Operations";

    private Experiment() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of("results", "results.csv") : Path.of(args[0]);
        run(output);
    }

    public static void run(Path output) throws IOException {
        Path absoluteOutput = output.toAbsolutePath();
        Path parent = absoluteOutput.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        System.out.println("Divide-and-Conquer Algorithm Experiments");
        System.out.println("Benchmarking six input sizes, four input types, and four algorithms.");
        System.out.println("Results CSV: " + absoluteOutput);
        System.out.println();

        try (BufferedWriter writer = Files.newBufferedWriter(absoluteOutput, StandardCharsets.UTF_8)) {
            writer.write(CSV_HEADER);
            writer.newLine();
            for (int size : INPUT_SIZES) {
                for (InputType inputType : INPUT_TYPES) {
                    int[] input = createInput(size, inputType);
                    System.out.printf(Locale.ROOT, "Input: %-16s N=%,d (%s)%n",
                            inputType.csvName, size, sizeCategory(size));
                    benchmarkMergeSort(writer, input, size, inputType);
                    benchmarkQuickSort(writer, input, size, inputType);
                    benchmarkSelection(writer, input, size, inputType);
                    benchmarkClosestPair(writer, input, size, inputType);
                    System.out.println();
                }
            }
        }
        System.out.println("CSV results successfully written to " + absoluteOutput);
    }

    private static void benchmarkMergeSort(BufferedWriter writer, int[] input, int size,
                                           InputType inputType) throws IOException {
        System.out.println("  Running MergeSort...");
        MergeSorter.Metrics metrics = MergeSorter.sort(input.clone());
        writeRow(writer, "MergeSort", size, inputType, metrics.executionTimeNanos(),
                metrics.maxRecursionDepth(),
                "Comparisons=" + metrics.comparisons() + "; Swaps=0; DistanceCalcs=0");
    }

    private static void benchmarkQuickSort(BufferedWriter writer, int[] input, int size,
                                           InputType inputType) throws IOException {
        System.out.println("  Running QuickSort...");
        QuickSorter.Metrics metrics = QuickSorter.sort(input.clone());
        writeRow(writer, "QuickSort", size, inputType, metrics.executionTimeNanos(),
                metrics.maxRecursionDepth(),
                "Comparisons=" + metrics.comparisons() + "; Swaps=" + metrics.swaps()
                        + "; DistanceCalcs=0");
    }

    private static void benchmarkSelection(BufferedWriter writer, int[] input, int size,
                                           InputType inputType) throws IOException {
        System.out.println("  Running DeterministicSelect...");
        DeterministicSelector.SelectionResult metrics =
                DeterministicSelector.selectWithMetrics(input.clone(), size / 2);
        writeRow(writer, "DeterministicSelect", size, inputType, metrics.executionTimeNanos(),
                metrics.maxRecursionDepth(),
                "Comparisons=" + metrics.comparisons() + "; Swaps=" + metrics.swaps()
                        + "; DistanceCalcs=0");
    }

    private static void benchmarkClosestPair(BufferedWriter writer, int[] input, int size,
                                             InputType inputType) throws IOException {
        System.out.println("  Running ClosestPair...");
        Point[] points = new Point[size];
        for (int i = 0; i < input.length; i++) {
            points[i] = new Point(i, input[i]);
        }
        ClosestPairSolver.Result metrics = ClosestPairSolver.solve(points);
        writeRow(writer, "ClosestPair", size, inputType, metrics.executionTimeNanos(),
                metrics.maxRecursionDepth(),
                "Comparisons=" + metrics.comparisons() + "; Swaps=0; DistanceCalcs="
                        + metrics.distanceCalculations());
    }

    private static void writeRow(BufferedWriter writer, String algorithm, int size,
                                 InputType inputType, long timeNanos, int recursionDepth,
                                 String operations) throws IOException {
        double timeMillis = timeNanos / 1_000_000.0;
        System.out.printf(Locale.ROOT,
                "    %-22s Type=%-16s N=%,7d Time=%10.3f ms Depth=%2d Operations: %s%n",
                algorithm, inputType.csvName, size, timeMillis, recursionDepth, operations);

        writer.write(algorithm);
        writer.write(',');
        writer.write(Integer.toString(size));
        writer.write(',');
        writer.write(inputType.csvName);
        writer.write(',');
        writer.write(String.format(Locale.ROOT, "%.6f", timeMillis));
        writer.write(',');
        writer.write(Integer.toString(recursionDepth));
        writer.write(',');
        writer.write(csvEscape(operations));
        writer.newLine();
    }

    private static String csvEscape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String sizeCategory(int size) {
        if (size <= 1_000) {
            return "Small";
        }
        if (size <= 50_000) {
            return "Medium";
        }
        return "Large";
    }

    private static int[] createInput(int size, InputType inputType) {
        int[] values = new int[size];
        Random random = new Random(0x5EEDL + size + inputType.ordinal());
        for (int i = 0; i < size; i++) {
            values[i] = switch (inputType) {
                case RANDOM -> random.nextInt();
                case SORTED -> i;
                case REVERSE_SORTED -> size - i;
                case DUPLICATE_HEAVY -> random.nextInt(10);
            };
        }
        return values;
    }

    private enum InputType {
        RANDOM("Random"),
        SORTED("Sorted"),
        REVERSE_SORTED("Reverse-sorted"),
        DUPLICATE_HEAVY("Duplicate-heavy");

        private final String csvName;

        InputType(String csvName) {
            this.csvName = csvName;
        }
    }
}
