import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class q1 {
    static int s, n, t, k;
    static String[][] grid;
    static Cell[][] board;
    static Random rand;
    static HashSet<String> dictionary = new HashSet<>();

    static class Cell {
        String letter;
        Set<String> words = new HashSet<>();
        Lock lock = new ReentrantLock();
        int x;
        int y;

        public Cell(String letter, int x, int y) {
            this.letter = letter;
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Usage: java q1 <seed> <grid_size> <num_threads> <k>");
            return;
        }

        s = Integer.parseInt(args[0]);
        n = Integer.parseInt(args[1]);
        t = Integer.parseInt(args[2]);
        k = Integer.parseInt(args[3]);

        if (n <= 4) {
            System.out.println("Grid size must be greater than 4.");
            return;
        }

        rand = new Random(s);
        loadDictionary("dict.txt");
        generateGrid("freq.txt");
        printGrid();

        // Create threads
        Thread[] threads = new Thread[t];
        for (int i = 0; i < t; i++) {
            threads[i] = new Thread(new WordSearchWorker());
            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        printResults();
    }

    static void loadDictionary(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            dictionary.add(line.trim().toUpperCase());
        }
        br.close();
    }

    static void generateGrid(String filename) throws IOException {
        // Read letter frequencies
        BufferedReader br = new BufferedReader(new FileReader(filename));
        Map<String, Integer> freqMap = new LinkedHashMap<>();
        String line;
        while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
            String[] parts = line.split("\\s+");
            String letter = parts[0];
            int count = Integer.parseInt(parts[1]);
            freqMap.put(letter, count);
        }
        br.close();

        // Create a weighted selection array
        List<Map.Entry<String, Integer>> weightedList = new ArrayList<>(freqMap.entrySet());

        // Create grid
        grid = new String[n][n];
        board = new Cell[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                String letter = getRandomWeightedLetter(weightedList);
                grid[i][j] = letter;
                board[i][j] = new Cell(letter, i, j);
            }
        }
    }

    private static String getRandomWeightedLetter(List<Map.Entry<String, Integer>> weightedList) {
        int randVal = new Random().nextInt(100000);
        int cumulativeSum = 0;

        for (Map.Entry<String, Integer> entry : weightedList) {
            cumulativeSum += entry.getValue();
            if (randVal < cumulativeSum) {
                return entry.getKey();
            }
        }
        return "A"; // shouldn't come to this since randVal and cumulativeSum <= 100000
    }

    static void printGrid() {
        // print each row as a string
        for (int i = 0; i < n; i++) {
            System.out.println(Arrays.toString(grid[i]));
        }
    }

    static void printResults() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.println(i + " " + j + " " + String.join(" ", board[i][j].words));
            }
        }
    }

    static class WordSearchWorker implements Runnable {
        public void run() {
            for (int attempt = 0; attempt < k; attempt++) {
                int x = rand.nextInt(n);
                int y = rand.nextInt(n);
                findWords(x, y);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        void findWords(int x, int y) {
            List<int[]> moves = generateMoves(x, y);
            if (moves.isEmpty()) return;

            List<Cell> pathCells = new ArrayList<>();
            for (int[] move : moves) {
                pathCells.add(board[move[0]][move[1]]);
            }

            // Sort the path cells using a Comparator for x and y coordinates
            pathCells.sort(new Comparator<Cell>() {
                @Override
                public int compare(Cell cell1, Cell cell2) {
                    // Compare by x-coordinate first
                    int cmpX = Integer.compare(cell1.x, cell2.x);
                    if (cmpX != 0) return cmpX;  // If x's are not equal, return comparison result

                    // If x's are equal, compare by y-coordinate
                    return Integer.compare(cell1.y, cell2.y);
                }
            });

            // Lock all cells in the path
            pathCells.forEach(cell -> cell.lock.lock());
            try {
                StringBuilder word = new StringBuilder();
                for (Cell cell : pathCells) {
                    word.append(cell.letter);
                    if (word.length() >= 3 && dictionary.contains(word.toString())) {
                        for (Cell c : pathCells) {
                            c.words.add(word.toString());
                        }
                    }
                }
            } finally {
                pathCells.forEach(cell -> cell.lock.unlock());
            }
        }

        List<int[]> generateMoves(int x, int y) {
            List<int[]> moves = new ArrayList<>();
            boolean[][] visited = new boolean[n][n];
            visited[x][y] = true;
            moves.add(new int[]{x, y});

            for (int i = 0; i < 7; i++) {
                List<int[]> options = new ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = x + dx, ny = y + dy;
                        // Skip the current cell
                        if (dx == 0 && dy == 0) continue;
                        // checks if the move stays in bounds and hasn't been visited yet
                        if (nx >= 0 && ny >= 0 && nx < n && ny < n && !visited[nx][ny]) {
                            options.add(new int[]{nx, ny});
                        }
                    }
                }
                if (options.isEmpty()) break;

                int[] nextMove = options.get(rand.nextInt(options.size()));
                moves.add(nextMove);
                visited[nextMove[0]][nextMove[1]] = true;
                x = nextMove[0];
                y = nextMove[1];
            }

            return moves;
        }
    }
}
