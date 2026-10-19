import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image.
 * The mean filter is used to smooth images by averaging the pixel values
 * in a neighborhood defined by a kernel size.
 *
 * <p>Concurrent version: the image is split into disjoint horizontal bands,
 * one per thread. Each thread only reads the shared source image and writes
 * to its own band of the output image, so no mutual exclusion is needed in
 * the main processing loop (data parallelism / embarrassingly parallel).
 * The only synchronization is the main thread joining the workers before
 * saving the result (signaling / barrier).</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * ImageMeanFilter.applyMeanFilter("input.jpg", "output.jpg", 3, 4);
 * }
 * </pre>
 *
 * <p>Supported image formats: JPG, PNG</p>
 *
 * <p>Author: temmanuel@comptuacao.ufcg.edu.br</p>
 */
public class ImageMeanFilter {

    /**
     * Worker thread that applies the mean filter to a horizontal band of the
     * image (rows [startY, endY)). It keeps its own local counters for how
     * many pixels were changed / kept equal, avoiding shared-state contention.
     */
    private static class FilterWorker extends Thread {
        private final BufferedImage source;
        private final BufferedImage result;
        private final int kernelSize;
        private final int startY;
        private final int endY;

        // Local (per-thread) metrics, read by main only after join().
        private long changed = 0;
        private long unchanged = 0;

        FilterWorker(BufferedImage source, BufferedImage result, int kernelSize, int startY, int endY) {
            this.source = source;
            this.result = result;
            this.kernelSize = kernelSize;
            this.startY = startY;
            this.endY = endY;
        }

        @Override
        public void run() {
            int width = source.getWidth();
            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < width; x++) {
                    // Calculate neighborhood average
                    int[] avgColor = calculateNeighborhoodAverage(source, x, y, kernelSize);

                    int newRgb = (avgColor[0] << 16) | (avgColor[1] << 8) | avgColor[2];

                    // Compare only the RGB channels (ignore the alpha bits) to
                    // decide whether the filter actually changed this pixel.
                    int oldRgb = source.getRGB(x, y) & 0xFFFFFF;
                    if (oldRgb == newRgb) {
                        unchanged++;
                    } else {
                        changed++;
                    }

                    // Set filtered pixel
                    result.setRGB(x, y, newRgb);
                }
            }
        }

        long getChanged() {
            return changed;
        }

        long getUnchanged() {
            return unchanged;
        }
    }

    /**
     * Applies mean filter to an image using multiple threads.
     *
     * @param inputPath  Path to input image
     * @param outputPath Path to output image
     * @param kernelSize Size of mean kernel
     * @param numThreads Number of worker threads (minimum 2)
     * @throws IOException          If there is an error reading/writing
     * @throws InterruptedException If a worker thread is interrupted
     */
    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize, int numThreads)
            throws IOException, InterruptedException {
        // Load image
        BufferedImage originalImage = ImageIO.read(new File(inputPath));

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Create result image
        BufferedImage filteredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Never spawn more threads than there are rows to process.
        int effectiveThreads = Math.min(numThreads, height);
        if (effectiveThreads < 1) {
            effectiveThreads = 1;
        }

        // Split the rows into disjoint bands, distributing the remainder so
        // the bands differ by at most one row (balanced static partitioning).
        FilterWorker[] workers = new FilterWorker[effectiveThreads];
        int baseRows = height / effectiveThreads;
        int remainder = height % effectiveThreads;

        int startY = 0;
        for (int i = 0; i < effectiveThreads; i++) {
            int rows = baseRows + (i < remainder ? 1 : 0);
            int endY = startY + rows;
            workers[i] = new FilterWorker(originalImage, filteredImage, kernelSize, startY, endY);
            startY = endY;
        }

        // Start all workers.
        for (FilterWorker worker : workers) {
            worker.start();
        }

        // Wait for every worker to finish (signaling / barrier) and aggregate
        // the per-thread metrics.
        long totalChanged = 0;
        long totalUnchanged = 0;
        for (FilterWorker worker : workers) {
            worker.join();
            totalChanged += worker.getChanged();
            totalUnchanged += worker.getUnchanged();
        }

        // Save filtered image
        ImageIO.write(filteredImage, "jpg", new File(outputPath));

        // Report metrics (Etapa 2)
        System.out.println("Pixels alterados: " + totalChanged);
        System.out.println("Pixels inalterados: " + totalUnchanged);
    }

    /**
     * Calculates average colors in a pixel's neighborhood
     *
     * @param image      Source image
     * @param centerX    X coordinate of center pixel
     * @param centerY    Y coordinate of center pixel
     * @param kernelSize Kernel size
     * @return Array with R, G, B averages
     */
    private static int[] calculateNeighborhoodAverage(BufferedImage image, int centerX, int centerY, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pad = kernelSize / 2;

        // Arrays for color sums
        long redSum = 0, greenSum = 0, blueSum = 0;
        int pixelCount = 0;

        // Process neighborhood
        for (int dy = -pad; dy <= pad; dy++) {
            for (int dx = -pad; dx <= pad; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                // Check image bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    // Get pixel color
                    int rgb = image.getRGB(x, y);

                    // Extract color components
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    // Sum colors
                    redSum += red;
                    greenSum += green;
                    blueSum += blue;
                    pixelCount++;
                }
            }
        }

        // Calculate average
        return new int[] {
            (int)(redSum / pixelCount),
            (int)(greenSum / pixelCount),
            (int)(blueSum / pixelCount)
        };
    }

    /**
     * Main method for demonstration
     *
     * Usage: java ImageMeanFilter <input_file> <num_threads>
     *
     * Arguments:
     *   input_file  - Path to the input image file to be processed
     *                 Supported formats: JPG, PNG
     *   num_threads - Number of threads to use (minimum 2)
     *
     * Example:
     *   java ImageMeanFilter input.jpg 4
     *
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ImageMeanFilter <input_file> <num_threads>");
            System.exit(1);
        }

        String inputFile = args[0];

        int numThreads = 0;
        try {
            numThreads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: num_threads must be an integer.");
            System.exit(1);
        }

        if (numThreads < 2) {
            System.err.println("Error: num_threads must be at least 2.");
            System.exit(1);
        }

        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7, numThreads);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Processing was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}

/*
Discussão
---------
- De 2 a 8 threads o tempo cai de forma consistente: o trabalho é
  dominado por CPU e bem balanceado entre as faixas, então mais threads =
  mais núcleos ocupados em paralelo.
- O ganho é SUBLINEAR (8 threads dão ~3,6x, não 8x). Possíveis causas:
  * parte serial não paralelizável (carregar/decodificar e salvar/encodar
    o JPEG, alocação da imagem) - Lei de Amdahl;
  * competição por memória / largura de banda de cache: getRGB/setRGB em
    BufferedImage acessam muita memória e várias threads disputam o mesmo
    subsistema de memória;
  * overhead de criação e coordenação das threads.
- De 8 para 16 threads NÃO há ganho (chega a piorar um pouco): a máquina
  tem 12 núcleos, então criar mais threads do que núcleos só acrescenta
  overhead de escalonamento e troca de contexto, sem mais paralelismo real.
  Note como o tempo "user" cresce (8,4s -> 18,8s) enquanto o "real"
  estabiliza: há cada vez mais tempo de CPU gasto em coordenação, não em
  trabalho útil.

Conclusão
---------
O ponto de melhor custo-benefício fica em torno do número de núcleos
físicos disponíveis (~4 a 8 threads nesta máquina). Acima disso o overhead
de threads e a contenção de memória anulam o benefício do paralelismo.
*/