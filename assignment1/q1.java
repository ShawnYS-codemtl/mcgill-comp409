import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import java.util.concurrent.locks.ReentrantLock;

public class q1 {

    // Parameters
    public static int width;
    public static int height;
    public static int t;
    public static int n;

    // Shared image and lock for synchronization
    public static BufferedImage outputimage;
    public static volatile BufferedImage overlapCheck;
    private static final ReentrantLock lock = new ReentrantLock();


    // Snowman parameters
    public static final int MIN_SIZE = 8;
    public static final int RADIUS_REDUCTION = 2; // Reduction in radius for each circle

    // Thread class for drawing snowmen
    static class SnowmanThread extends Thread {
        private int snowmenToDraw;

        public SnowmanThread(int snowmenToDraw) {
            this.snowmenToDraw = snowmenToDraw;
        }

        @Override
        public void run() {
            Random rand = new Random();
            for (int i = 0; i < snowmenToDraw; i++) {
                // Randomly choose size, position, and orientation
                int size = rand.nextInt(16) + MIN_SIZE; // Size between 8 and 24
                int x = rand.nextInt(width);
                int y = rand.nextInt(height);
                int orientation = rand.nextInt(4); // 0: up, 1: down, 2: left, 3: right

                // Check if the snowman fits and doesn't overlap
                if (canPlaceSnowman(x, y, size, orientation)) {
                    markOccupied(x, y, size, orientation);
                    drawSnowman(x, y, size, orientation);
                }

            }
        }

        // Check if the snowman can be placed without overlapping
        private boolean canPlaceSnowman(int x, int y, int size, int orientation) {
            lock.lock();
            try {
                int[] radii = {size, size - RADIUS_REDUCTION, size - 2 * RADIUS_REDUCTION};
                int[][] offsets = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // up, down, left, right

                int cx = x;
                int cy = y;

                for (int i = 0; i < 3; i++) {
                    int radius = radii[i];

                    // Check bounds
                    if (cx - radius < 0 || cx + radius >= width || cy - radius < 0 || cy + radius >= height) {
                        return false;
                    }

                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            if (dx * dx + dy * dy <= radius * radius) {
                                if (overlapCheck.getRGB(cx + dx, cy + dy) != 0) {
                                    return false;
                                }
                            }
                        }
                    }

                    // Move to the next circle position
                    if (i < 2) { // Don't move after the last circle
                        int nextRadius = radii[i + 1];
                        cx += offsets[orientation][0] * (radius + nextRadius);
                        cy += offsets[orientation][1] * (radius + nextRadius);
                    }
                } return true;
            } finally {
                lock.unlock();
            }

        }

        private void markOccupied(int x, int y, int size, int orientation) {
            lock.lock();
            try {
                int[] radii = {size, size - RADIUS_REDUCTION, size - 2 * RADIUS_REDUCTION};
                int[][] offsets = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

                int cx = x;
                int cy = y;

                for (int i = 0; i < 3; i++) {
                    int radius = radii[i];

                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            if (dx * dx + dy * dy <= radius * radius) {
                                overlapCheck.setRGB(cx + dx, cy + dy, 0xFFFFFF);
                            }
                        }
                    }

                    if (i < 2) {
                        int nextRadius = radii[i + 1];
                        cx += offsets[orientation][0] * (radius + nextRadius);
                        cy += offsets[orientation][1] * (radius + nextRadius);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        // Draw the snowman
        private void drawSnowman(int x, int y, int size, int orientation) {
            int[] radii = {size, size - RADIUS_REDUCTION, size - 2 * RADIUS_REDUCTION};
            int[][] offsets = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // up, down, left, right

            int cx = x;
            int cy = y;

            for (int i = 0; i < 3; i++) {
                int radius = radii[i];

                // Draw the circle
                drawCircle(cx, cy, radius);

                // Move to the next circle position
                if (i < 2) { // Don't move after the last circle
                    int nextRadius = radii[i + 1];
                    cx += offsets[orientation][0] * (radius + nextRadius);
                    cy += offsets[orientation][1] * (radius + nextRadius);
                }
            }
        }

        // Draw a circle using the midpoint circle algorithm
        private void drawCircle(int cx, int cy, int radius) {
            int x = radius;
            int y = 0;
            int err = 0;

            while (x >= y) {
                setPixel(cx + x, cy + y);
                setPixel(cx + y, cy + x);
                setPixel(cx - y, cy + x);
                setPixel(cx - x, cy + y);
                setPixel(cx - x, cy - y);
                setPixel(cx - y, cy - x);
                setPixel(cx + y, cy - x);
                setPixel(cx + x, cy - y);

                if (err <= 0) {
                    y += 1;
                    err += 2 * y + 1;
                }
                if (err > 0) {
                    x -= 1;
                    err -= 2 * x + 1;
                }
            }
        }

        // Set a pixel in the image
        private void setPixel(int x, int y) {
            outputimage.setRGB(x, y, 0xFF000000);
        }
    }

    public static void main(String[] args) {

        try {
            // Parse command-line arguments
            width = Integer.parseInt(args[0]);
            height = Integer.parseInt(args[1]);
            t = Integer.parseInt(args[2]);
            n = Integer.parseInt(args[3]);

            // Create an empty image
            outputimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            overlapCheck = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // Start timing
            long startTime = System.currentTimeMillis();

            // Create and start threads
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < t; i++) {
                int snowmenPerThread = (i < n % t) ? (n / t + 1) : (n / t);
                Thread thread = new SnowmanThread(snowmenPerThread);
                threads.add(thread);
                thread.start();
            }

            // Wait for all threads to finish
            for (Thread thread : threads) {
                thread.join();
            }

            // End timing
            long endTime = System.currentTimeMillis();
            System.out.println("Time taken: " + (endTime - startTime) + " ms");

            // Write out the image
            File outputfile = new File("outputimage.png");
            ImageIO.write(outputimage, "png", outputfile);

        } catch (Exception e) {
            System.out.println("ERROR " + e);
            e.printStackTrace();
        }
    }
}
