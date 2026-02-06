import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class q2 {
    public static class Triple {
        boolean b;
        int f, m;

        Triple(boolean b, int f, int m) {
            this.b = b;
            this.f = f;
            this.m = m;
        }
    }

    public static class BracketTask implements Callable<Triple> {
        private final char[] arr;
        private final int start, end;

        public BracketTask(char[] arr, int start, int end) {
            this.arr = arr;
            this.start = start;
            this.end = end;
        }

        @Override
        public Triple call() {
            return computeSequentially(arr, start, end);
        }
    }

    private static Triple computeSequentially(char[] arr, int start, int end) {
        int f = 0, m = 0;
        for (int i = start; i < end; i++) {
            char c = arr[i];
            if (c == '[') f++;
            else if (c == ']') f--;

            m = Math.min(m, f);
        }
        boolean b = (f == 0 && m >= 0);
        return new Triple(b, f, m);
    }

    private static Triple mergeTriples(Triple left, Triple right) {
        boolean isValid = (left.b && right.b) || (left.f + right.f == 0 && left.m >= 0 && left.f + right.m >= 0);
        int f = left.f + right.f;
        int m = Math.min(left.m, left.f + right.m);
        return new Triple(isValid, f, m);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java q2 n t [s]");
            return;
        }

        int n = Integer.parseInt(args[0]);
        int t = Integer.parseInt(args[1]);
        int seed = (args.length > 2) ? Integer.parseInt(args[2]) : (int) System.currentTimeMillis();

        char[] arr = Bracket.construct(n, seed);

        ExecutorService pool = Executors.newFixedThreadPool(t);
        List<Future<Triple>> futures = new ArrayList<>();
        int chunkSize = (n + t - 1) / t;  // Ensures even distribution

        long startTime = System.nanoTime();

        for (int i = 0; i < t; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, n);
            if (start < end) {
                futures.add(pool.submit(new BracketTask(arr, start, end)));
//                System.out.println("Start: " + String.valueOf(start) + "End: " + String.valueOf(end));
            }
        }

        List<Triple> triples = new ArrayList<>();
        try {
            for (Future<Triple> future : futures) {
                triples.add(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // Step 2: Tree-like merge
        while (triples.size() > 1) {
            List<Triple> mergedTriples = new ArrayList<>();
            for (int i = 0; i < triples.size(); i += 2) {
                if (i + 1 < triples.size()) {
                    mergedTriples.add(mergeTriples(triples.get(i), triples.get(i + 1)));
                } else {
                    mergedTriples.add(triples.get(i)); // Carry over the last element if odd number
                }
            }
            triples = mergedTriples; // Move to next merging round
        }

        long endTime = System.nanoTime();
        pool.shutdown();

        double elapsedTimeMs = (endTime - startTime) / 1_000_000.0;
        System.out.printf("Time: %.3f ms%n", elapsedTimeMs);
        System.out.println(triples.get(0).b + " " + Bracket.verify());
    }
}

