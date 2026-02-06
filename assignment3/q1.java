import java.util.concurrent.*;
import java.util.Random;

public class q1 {
    private static final int NUM_THREADS = 4;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java q1 <k> <m>");
            return;
        }

        int k = Integer.parseInt(args[0]); // Percentage of resize operations
        int m = Integer.parseInt(args[1]); // Number of operations per thread

        System.out.println("Testing q1a (blocking)...");
        runTest(new q1a(), k, m);
        System.out.println("Testing q1b (Lock-Free)...");
        runTest(new q1b(), k, m);
    }

    private static void runTest(Object arrayImpl, int k, int m) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        long startTime = System.nanoTime();

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.execute(new Worker(arrayImpl, k, m));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        System.out.println("Execution Time: " + (endTime - startTime) / 1_000_000 + " ms");
    }

    static class Worker implements Runnable {
        private final Object arrayImpl;
        private final int k;
        private final int m;
        private final Random rand = new Random();

        Worker(Object arrayImpl, int k, int m) {
            this.arrayImpl = arrayImpl;
            this.k = k;
            this.m = m;
        }

        @Override
        public void run() {
            for (int j = 0; j < m; j++) {
                int currentSize = getCurrentSize();
                int index = rand.nextInt(currentSize); // Random index in a reasonable range
                boolean shouldResize = rand.nextInt(100) < k;

                if (shouldResize) {
                    index = currentSize; // access past end to trigger resize
                }

                if (rand.nextBoolean()) {
                    set(index, j); // 50% chance of writing
                } else {
                    get(index); // 50% chance of reading
                }
            }
        }

        private void get(int index) {
            try {
                if (arrayImpl instanceof q1a) {
                    ((q1a) arrayImpl).get(index);
                } else if (arrayImpl instanceof q1b) {
                    ((q1b) arrayImpl).get(index);
                }
            } catch (Exception e) {
                // Ignore out-of-bounds errors
            }
        }

        private void set(int index, Object value) {
            try {
                if (arrayImpl instanceof q1a) {
                    ((q1a) arrayImpl).set(index, value);
                } else if (arrayImpl instanceof q1b) {
                    ((q1b) arrayImpl).set(index, value);
                }
            } catch (Exception e) {
                // Ignore out-of-bounds errors
            }
        }

        private int getCurrentSize() {
            if (arrayImpl instanceof q1a) {
                return ((q1a) arrayImpl).getSize();
            } else if (arrayImpl instanceof q1b) {
                return ((q1b) arrayImpl).getSize();
            }
            return 0;
        }
    }
}
