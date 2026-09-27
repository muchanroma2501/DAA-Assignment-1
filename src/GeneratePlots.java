import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeneratePlots {
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 1000;
    private static final int LEFT = 145;
    private static final int TOP = 125;
    private static final int RIGHT = 350;
    private static final int BOTTOM = 125;

    private static final Color GRID_COLOR = new Color(218, 224, 232);
    private static final Color TEXT_COLOR = new Color(35, 42, 52);
    private static final Color AXIS_COLOR = new Color(55, 63, 74);
    private static final DecimalFormat NUMBER_FORMAT =
            new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.US));

    private record DataPoint(int inputSize, double value) {
    }

    private static final class Series {
        private final String name;
        private final Color color;
        private final List<DataPoint> timePoints = new ArrayList<>();
        private final List<DataPoint> depthPoints = new ArrayList<>();

        private Series(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }

    public static void main(String[] args) throws IOException {
        Path csvPath = Path.of("results", "results.csv");
        if (!Files.isRegularFile(csvPath)) {
            throw new IOException("CSV file not found: " + csvPath.toAbsolutePath());
        }

        Map<String, Series> series = createSeries();
        readCsv(csvPath, series);
        int pointCount = series.values().stream().mapToInt(s -> s.timePoints.size()).sum();
        if (pointCount == 0) {
            throw new IllegalStateException("No Random rows for the four requested algorithms were found in " + csvPath);
        }
        series.values().forEach(s -> {
            s.timePoints.sort(Comparator.comparingInt(DataPoint::inputSize));
            s.depthPoints.sort(Comparator.comparingInt(DataPoint::inputSize));
        });

        Path plotsDirectory = Path.of("plots");
        Files.createDirectories(plotsDirectory);
        drawChart(plotsDirectory.resolve("time_vs_n.png"), series, true);
        drawChart(plotsDirectory.resolve("recursion_depth_vs_n.png"), series, false);
        System.out.println("Created plots/time_vs_n.png and plots/recursion_depth_vs_n.png");
    }

    private static Map<String, Series> createSeries() {
        Map<String, Series> series = new LinkedHashMap<>();
        series.put("MergeSort", new Series("MergeSort", new Color(31, 91, 150)));
        series.put("QuickSort", new Series("QuickSort", new Color(194, 91, 16)));
        series.put("DeterministicSelect", new Series("DeterministicSelect", new Color(35, 125, 67)));
        series.put("ClosestPair", new Series("ClosestPair", new Color(174, 48, 48)));
        return series;
    }

    private static void readCsv(Path csvPath, Map<String, Series> series) throws IOException {
        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("CSV file is empty: " + csvPath);
        }

        List<String> header = parseCsvLine(lines.get(0));
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            columns.put(header.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        int algorithmColumn = requiredColumn(columns, "algorithm");
        int sizeColumn = requiredColumn(columns, "inputsize");
        int inputTypeColumn = requiredColumn(columns, "inputtype");
        int timeColumn = requiredColumn(columns, "executiontimems");
        int depthColumn = requiredColumn(columns, "maxrecursiondepth");

        for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
            String line = lines.get(lineNumber - 1);
            if (line.isBlank()) {
                continue;
            }
            List<String> fields = parseCsvLine(line);
            int requiredLength = Math.max(Math.max(algorithmColumn, sizeColumn),
                    Math.max(Math.max(inputTypeColumn, timeColumn), depthColumn));
            if (fields.size() <= requiredLength) {
                throw new IOException("Malformed CSV row at line " + lineNumber + ": not enough columns");
            }
            if (!fields.get(inputTypeColumn).trim().equalsIgnoreCase("Random")) {
                continue;
            }
            Series target = series.get(fields.get(algorithmColumn).trim());
            if (target == null) {
                continue;
            }

            try {
                int inputSize = Integer.parseInt(fields.get(sizeColumn).trim());
                double executionTime = Double.parseDouble(fields.get(timeColumn).trim());
                int recursionDepth = Integer.parseInt(fields.get(depthColumn).trim());
                if (inputSize <= 0 || executionTime < 0 || recursionDepth < 0
                        || !Double.isFinite(executionTime)) {
                    throw new IllegalArgumentException("values are outside the supported range");
                }
                target.timePoints.add(new DataPoint(inputSize, executionTime));
                target.depthPoints.add(new DataPoint(inputSize, recursionDepth));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid numeric data at CSV line " + lineNumber + ": " + line, exception);
            }
        }
    }

    private static int requiredColumn(Map<String, Integer> columns, String name) throws IOException {
        Integer index = columns.get(name);
        if (index == null) {
            throw new IOException("CSV header is missing required column: " + name);
        }
        return index;
    }

    private static List<String> parseCsvLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (current == ',' && !inQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        if (inQuotes) {
            throw new IOException("Malformed CSV row: unclosed quoted field");
        }
        fields.add(field.toString());
        return fields;
    }

    private static void drawChart(Path output, Map<String, Series> series, boolean timeChart) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        int plotRight = WIDTH - RIGHT;
        int plotBottom = HEIGHT - BOTTOM;
        int plotWidth = plotRight - LEFT;
        int plotHeight = plotBottom - TOP;
        double minX = series.values().stream().flatMap(s -> pointsFor(s, timeChart).stream())
                .mapToInt(DataPoint::inputSize).min().orElseThrow();
        double maxX = series.values().stream().flatMap(s -> pointsFor(s, timeChart).stream())
                .mapToInt(DataPoint::inputSize).max().orElseThrow();
        double logMinX = Math.log10(minX);
        double logMaxX = Math.log10(maxX);
        if (logMinX == logMaxX) {
            logMinX -= 0.5;
            logMaxX += 0.5;
        }

        double minY;
        double maxY;
        if (timeChart) {
            double smallestPositive = series.values().stream().flatMap(s -> pointsFor(s, true).stream())
                    .mapToDouble(DataPoint::value).filter(value -> value > 0).min().orElse(1.0);
            double largest = series.values().stream().flatMap(s -> pointsFor(s, true).stream())
                    .mapToDouble(DataPoint::value).max().orElse(1.0);
            minY = Math.pow(10, Math.floor(Math.log10(smallestPositive)));
            maxY = Math.pow(10, Math.ceil(Math.log10(largest)));
            if (minY == maxY) {
                minY /= 10;
                maxY *= 10;
            }
        } else {
            maxY = series.values().stream().flatMap(s -> pointsFor(s, false).stream())
                    .mapToDouble(DataPoint::value).max().orElse(1.0);
            maxY = Math.max(8, Math.ceil(Math.ceil(maxY * 1.1) / 8) * 8);
            minY = 0;
        }

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        if (timeChart) {
            drawLogXTicks(graphics, LEFT, TOP, plotWidth, plotHeight, logMinX, logMaxX);
            drawLogYTicks(graphics, LEFT, TOP, plotWidth, plotHeight, Math.log10(minY), Math.log10(maxY));
        } else {
            drawLogXTicks(graphics, LEFT, TOP, plotWidth, plotHeight, logMinX, logMaxX);
            drawLinearYTicks(graphics, LEFT, TOP, plotWidth, plotHeight, maxY);
        }

        graphics.setColor(AXIS_COLOR);
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawLine(LEFT, TOP, LEFT, plotBottom);
        graphics.drawLine(LEFT, plotBottom, plotRight, plotBottom);

        graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Series current : series.values()) {
            graphics.setColor(current.color);
            int previousX = 0;
            int previousY = 0;
            boolean hasPrevious = false;
            for (DataPoint point : pointsFor(current, timeChart)) {
                int x = mapLogX(point.inputSize(), logMinX, logMaxX, LEFT, plotWidth);
                int y = timeChart
                        ? mapLogY(point.value(), Math.log10(minY), Math.log10(maxY), TOP, plotHeight)
                        : mapLinearY(point.value(), maxY, TOP, plotHeight);
                if (hasPrevious) {
                    graphics.drawLine(previousX, previousY, x, y);
                }
                graphics.fill(new Ellipse2D.Double(x - 6, y - 6, 12, 12));
                previousX = x;
                previousY = y;
                hasPrevious = true;
            }
        }

        graphics.setColor(TEXT_COLOR);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        String title = timeChart
                ? "Execution Time vs Input Size N (Random Input)"
                : "Max Recursion Depth vs Input Size N (Random Input)";
        centeredText(graphics, title, LEFT, plotRight, 55);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 21));
        centeredText(graphics, "Input Size N (log scale)", LEFT, plotRight, HEIGHT - 42);
        graphics.rotate(-Math.PI / 2);
        centeredText(graphics, timeChart ? "Execution Time (ms, log scale)" : "Max Recursion Depth",
                -plotBottom, -TOP, 43);
        graphics.rotate(Math.PI / 2);

        drawLegend(graphics, series, plotRight + 50, TOP + 20);
        graphics.dispose();
        ImageIO.write(image, "png", output.toFile());
    }

    private static void drawLogYTicks(Graphics2D graphics, int left, int top, int width, int height,
                                      double logMinY, double logMaxY) {
        int firstExponent = (int) Math.ceil(logMinY);
        int lastExponent = (int) Math.floor(logMaxY);
        for (int exponent = firstExponent; exponent <= lastExponent; exponent++) {
            double value = Math.pow(10, exponent);
            int y = mapLogY(value, logMinY, logMaxY, top, height);
            graphics.setColor(GRID_COLOR);
            graphics.setStroke(new BasicStroke(1f));
            graphics.drawLine(left, y, left + width, y);
            graphics.setColor(TEXT_COLOR);
            graphics.drawString(NUMBER_FORMAT.format(value), left - 18 - graphics.getFontMetrics().stringWidth(
                    NUMBER_FORMAT.format(value)), y + 6);
        }
    }

    private static void drawLogXTicks(Graphics2D graphics, int left, int top, int width, int height,
                                      double logMinX, double logMaxX) {
        int firstExponent = (int) Math.ceil(logMinX);
        int lastExponent = (int) Math.floor(logMaxX);
        for (int exponent = firstExponent; exponent <= lastExponent; exponent++) {
            double value = Math.pow(10, exponent);
            int x = mapLogX(value, logMinX, logMaxX, left, width);
            graphics.setColor(GRID_COLOR);
            graphics.setStroke(new BasicStroke(1f));
            graphics.drawLine(x, top, x, top + height);
            graphics.setColor(TEXT_COLOR);
            String label = NUMBER_FORMAT.format(value);
            graphics.drawString(label, x - graphics.getFontMetrics().stringWidth(label) / 2, top + height + 30);
        }
    }

    private static void drawLinearYTicks(Graphics2D graphics, int left, int top, int width, int height,
                                         double maxY) {
        int tickCount = 8;
        for (int i = 0; i <= tickCount; i++) {
            double value = Math.round(maxY * i / tickCount);
            int y = mapLinearY(value, maxY, top, height);
            graphics.setColor(GRID_COLOR);
            graphics.setStroke(new BasicStroke(1f));
            graphics.drawLine(left, y, left + width, y);
            graphics.setColor(TEXT_COLOR);
            String label = NUMBER_FORMAT.format(value);
            graphics.drawString(label, left - 18 - graphics.getFontMetrics().stringWidth(label), y + 6);
        }
    }

    private static int mapLogX(double value, double logMin, double logMax, int left, int width) {
        return left + (int) Math.round((Math.log10(value) - logMin) / (logMax - logMin) * width);
    }

    private static int mapLogY(double value, double logMin, double logMax, int top, int height) {
        double safeValue = value > 0 ? value : Math.pow(10, logMin);
        return top + height - (int) Math.round((Math.log10(safeValue) - logMin) / (logMax - logMin) * height);
    }

    private static int mapLinearY(double value, double max, int top, int height) {
        return top + height - (int) Math.round(value / max * height);
    }

    private static List<DataPoint> pointsFor(Series series, boolean timeChart) {
        return timeChart ? series.timePoints : series.depthPoints;
    }

    private static void drawLegend(Graphics2D graphics, Map<String, Series> series, int x, int y) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.setColor(TEXT_COLOR);
        graphics.drawString("Algorithms", x, y);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 19));
        int lineY = y + 42;
        for (Series current : series.values()) {
            graphics.setColor(current.color);
            graphics.setStroke(new BasicStroke(4f));
            graphics.drawLine(x, lineY - 6, x + 32, lineY - 6);
            graphics.fill(new Ellipse2D.Double(x + 11, lineY - 12, 12, 12));
            graphics.setColor(TEXT_COLOR);
            graphics.drawString(current.name, x + 45, lineY);
            lineY += 38;
        }
    }

    private static void centeredText(Graphics2D graphics, String text, int left, int right, int y) {
        int x = left + (right - left - graphics.getFontMetrics().stringWidth(text)) / 2;
        graphics.drawString(text, x, y);
    }
}
