import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image.
 * The mean filter is used to smooth images by averaging the pixel values
 * in a neighborhood defined by a kernel size.
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * ImageMeanFilter.applyMeanFilter("input.jpg", "output.jpg", 3);
 * }
 * </pre>
 * 
 * <p>Supported image formats: JPG, PNG</p>
 * 
 * <p>Author: temmanuel@comptuacao.ufcg.edu.br</p>
 */
public class ImageMeanFilter {
    
    private static class FilterWorker extends Thread {
        private final BufferedImage imagem;
        private final BufferedImage resultado;
        private final int kernelSize;
        private final int startY;
        private final int endY;

        // Local (per-thread) metrics, read by main only after join().
        private long mudou = 0;
        private long naoMudou = 0;

        FilterWorker(BufferedImage imagem, BufferedImage resultado, int kernelSize, int startY, int endY) {
            this.imagem = imagem;
            this.resultado = resultado;
            this.kernelSize = kernelSize;
            this.startY = startY;
            this.endY = endY;
        }

        @Override
        public void run() {
            int width = imagem.getWidth();
            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < width; x++) {
                    // Calculate neighborhood average
                    int[] avgColor = calculateNeighborhoodAverage(imagem, x, y, kernelSize);

                    int newRgb = (avgColor[0] << 16) | (avgColor[1] << 8) | avgColor[2];

                    int oldRgb = imagem.getRGB(x, y) & 0xFF;
                    if (oldRgb == newRgb) {
                        naoMudou++;            
                    } else {
                        mudou++;
                    }

                    // Set filtered pixel
                    resultado.setRGB(x, y, newRgb);
                }
            }
        }

        long getMudou() {
            return mudou;
        }

        long getNaoMudou() {
            return naoMudou;
        } 
    }
    
    /**
     * Applies mean filter to an image
     * 
     * @param inputPath  Path to input image
     * @param outputPath Path to output image 
     * @param kernelSize Size of mean kernel
     * @throws IOException If there is an error reading/writing
     * @throws InterruptedException 
     */
    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize, int num_threads) throws IOException, InterruptedException {
        // Load image
        BufferedImage originalImage = ImageIO.read(new File(inputPath));
        
        // Create result image
        BufferedImage filteredImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        
        // Image processing
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
       
        if (num_threads < 2) {
            num_threads = 2;
        }

        FilterWorker[] workers = new FilterWorker[num_threads];
        int baseRows = height / num_threads;
        int remainder = height % num_threads;

        int startY = 0;
        for (int i = 0; i < num_threads; i++) {
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
            totalChanged += worker.getMudou();
            totalUnchanged += worker.getNaoMudou();
        }

        
        // Save filtered image
        ImageIO.write(filteredImage, "jpg", new File(outputPath));
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
     * Usage: java ImageMeanFilter <input_file>
     * 
     * Arguments:
     *   input_file - Path to the input image file to be processed
     *                Supported formats: JPG, PNG
     * 
     * Example:
     *   java ImageMeanFilter input.jpg
     * 
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel
     * @throws InterruptedException 
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ImageMeanFilter <input_file> <num_threads>");
            System.exit(1);
        }

        String inputFile = args[0]; 
        int num_threads = Integer.parseInt(args[1]);
        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7, num_threads);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Processing was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
