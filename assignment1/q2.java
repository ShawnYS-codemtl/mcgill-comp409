
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class CELL {
    int position;  // position on the board (0 to 99 for a 10x10 grid)
    Integer snakeOrLadderDestination;  // stores destination if there's a snake or ladder

    // Constructor for empty cell
    public CELL(int position) {
        this.position = position;
        this.snakeOrLadderDestination = null;  // No snake/ladder initially
    }

    // Constructor for snake or ladder cell
    public CELL(int position, int destination) {
        this.position = position;
        this.snakeOrLadderDestination = destination;
    }

    // Check if cell has a snake or ladder
    public boolean hasSnakeOrLadder() {
        return snakeOrLadderDestination != null;
    }

    public int getDestination() {
        return snakeOrLadderDestination;
    }

    public void setDestination(int destination) {
        this.snakeOrLadderDestination = destination;
    }

    public void resetDestination(){
        this.snakeOrLadderDestination = null;
    }

    public int getPosition() {
        return position;
    }
}

class q2 {
    private static final int SIZE = 10;
    private static final int TOTAL_CELLS = SIZE * SIZE;
    private static final Random rand = new Random();
    private static CELL[][] board = new CELL[SIZE][SIZE];  // 2D array of CELL objects
    private static final List<String> log = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean running = true;
    private static final Object boardLock = new Object();
    private static final ReadWriteLock boardReadWriteLock = new ReentrantReadWriteLock();

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            System.out.println("Usage: java SnakesAndLadders <adderSleepMs> <removerSleepMs> <runSeconds>");
            return;
        }
        int k = Integer.parseInt(args[0]);
        int j = Integer.parseInt(args[1]);
        int s = Integer.parseInt(args[2]);

        initializeBoard();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.execute(new PlayerThread());
        executor.execute(new AdderThread(k));
        executor.execute(new RemoverThread(j));

        Thread.sleep(s * 1000);
        running = false;
        executor.shutdownNow();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        printLog();
    }

    private static void initializeBoard() {

        int position = 0;

        for (int row = 0; row < SIZE; row++) {  // Top row (0) to bottom row (9)
            for (int col = 0; col < SIZE; col++) {
                board[row][col] = new CELL(position++);
            }
        }
        addSnake();
        addLadder();
    }

    private static void addSnake() {
        synchronized (boardLock) {
            int start, end;
            // Ensure both start and end positions are not 0 or 99
            for (int i = 0; i < 10; i++) {  // Add 10 snakes
                do {
                    start = rand.nextInt(TOTAL_CELLS - 2) + 1;  // start between 1 and 98 (not 0 or 99)
                    end = rand.nextInt(TOTAL_CELLS - 2) + 1;    // end between 1 and 98 (not 0 or 99)
                } while (start == end ||
                        board[start / SIZE][start % SIZE].hasSnakeOrLadder() ||
                        board[end / SIZE][end % SIZE].hasSnakeOrLadder() ||
                        (start / SIZE == end / SIZE) ||  // Ensure different rows
                        start < end);  // Ensure snakes (start > end)

                // Add snake (start > end)
                board[start/SIZE][start%SIZE].setDestination(end);
                board[end / SIZE][end % SIZE].setDestination(end);   //mark the snake head with its own position to mark occupied
                log(System.currentTimeMillis(), "Adder ", "Snake", start, end);
            }
        }
    }

    private static void addLadder() {
        synchronized (boardLock) {
            int start, end;
            // Ensure both start and end positions are not 0 or 99
            for (int i = 0; i < 9; i++) {  // Add 9 ladders
                do {
                    start = rand.nextInt(TOTAL_CELLS - 2) + 1;  // start between 1 and 98 (not 0 or 99)
                    end = rand.nextInt(TOTAL_CELLS - 2) + 1;    // end between 1 and 98 (not 0 or 99)
                } while (start == end ||
                        board[start / SIZE][start % SIZE].hasSnakeOrLadder() ||
                        board[end / SIZE][end % SIZE].hasSnakeOrLadder() ||
                        (start / SIZE == end / SIZE) ||  // Ensure different rows
                        start > end);  // Ensure ladders (start < end)

                // Add ladder (start < end)
                board[start / SIZE][start % SIZE].setDestination(end);
                board[end / SIZE][end % SIZE].setDestination(end);
                log(System.currentTimeMillis(), "Adder ", "Ladder", start, end);
            }
        }
    }

    private static void addRandomSnakeOrLadder() {
        synchronized (boardLock) {
            int start, end;
            // if start > end: Snake (moves down)
            // if start < end: Ladder (moves up)

            // Ensure both start and end positions are not 0 or 99
            do {
                start = rand.nextInt(TOTAL_CELLS - 2) + 1; // between 1 and 98
                end = rand.nextInt(TOTAL_CELLS - 2) + 1; // between 1 and 98
            } while (start == end || board[start / SIZE][start % SIZE].hasSnakeOrLadder() ||
                    board[end/SIZE][end%SIZE].hasSnakeOrLadder() ||
                    (start / SIZE == end / SIZE));
            // start and end are not the same, neither cell is already occupied, two cells are not on the same row

            // Add snake or ladder to the board
            board[start / SIZE][start % SIZE].setDestination(end);
            board[end / SIZE][end % SIZE].setDestination(end);

            String snakeOrLadder;
            if (start > end){
                snakeOrLadder = "Snake";
            } else{
                snakeOrLadder = "Ladder";
            }
            log(System.currentTimeMillis(), "Adder ", snakeOrLadder, start, end);
        }
    }

    static class PlayerThread implements Runnable {
        public void run() {
            int position = 0;
            while (running) {
                int roll = rand.nextInt(6) + 1;
                position = move(position, roll);
                if (position >= TOTAL_CELLS - 1) {
                    log(System.currentTimeMillis(), "Player wins", "");
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    position = 0;
                }
                try { Thread.sleep(rand.nextInt(31) + 20); } catch (InterruptedException ignored) {}
            }
        }
    }

    static class AdderThread implements Runnable {
        private final int sleepMs;
        public AdderThread(int sleepMs) { this.sleepMs = sleepMs; }
        public void run() {
            while (running) {
                boardReadWriteLock.writeLock().lock();
                try{
                    addRandomSnakeOrLadder();
                } finally {
                    boardReadWriteLock.writeLock().unlock();
                }
                try { Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // Exit the loop if interrupted
                }
            }
        }
    }

    static class RemoverThread implements Runnable {
        private final int sleepMs;
        public RemoverThread(int sleepMs) { this.sleepMs = sleepMs; }

        public void run() {
            while (running) {
                boardReadWriteLock.writeLock().lock();
                try  {
                    // Find all the positions that still contain a snake or ladder (non-null cells)
                    List<int[]> availableCells = new ArrayList<>();
                    for (int row = 0; row < SIZE; row++) {
                        for (int col = 0; col < SIZE; col++) {
                            // If there is a CELL present (snake or ladder) and it's the start
                            if (board[row][col].hasSnakeOrLadder() && (board[row][col].getDestination() != board[row][col].getPosition())) {
                                availableCells.add(new int[]{row, col});
                            }
                        }
                    }

                    if (!availableCells.isEmpty()) {
                        // Randomly pick a cell from available cells
                        int[] randomCell = availableCells.get(rand.nextInt(availableCells.size()));
                        int row = randomCell[0];
                        int col = randomCell[1];

                        // Retrieve the CELL and reset its destination, also reset the endpoint's destination
                        CELL cell = board[row][col];
                        int endPos = board[row][col].getDestination();
                        int endRow = endPos / SIZE;
                        int endCol = endPos % SIZE;
                        board[endRow][endCol].resetDestination();
                        board[row][col].resetDestination();


                        String snakeOrLadder;
                        if (endPos > cell.getPosition()){
                            snakeOrLadder = "Ladder";
                        } else{
                            snakeOrLadder = "Snake";
                        }

                        // Log the removal of the snake or ladder, using the position of the cell
                        log(System.currentTimeMillis(), "Remover ", snakeOrLadder, cell.getPosition(), endPos);
                    }
                } finally {
                    boardReadWriteLock.writeLock().unlock();
                }
                try {
                    Thread.sleep(sleepMs); // Sleep for the specified interval
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }


    private static int move(int position, int roll) {
        boardReadWriteLock.readLock().lock();
        try {
            int newPos = position + roll;
            if (newPos < TOTAL_CELLS) {
                int row = newPos / SIZE;
                int col = newPos % SIZE;
                CELL cell = board[row][col];
                if (cell.hasSnakeOrLadder() && cell.getDestination() != newPos)  {
                    int finalPos = cell.getDestination();
                    log(System.currentTimeMillis(), "Player", "", newPos, finalPos);
                    return finalPos;
                }
                log(System.currentTimeMillis(), "Player", "", newPos);
            }
            return Math.min(newPos, TOTAL_CELLS - 1);
        } finally {
            boardReadWriteLock.readLock().unlock();
        }
    }

    private static void log(long timestamp, String action, String snakeOrLadder, int... cells) {
        StringBuilder entry = new StringBuilder(String.format("%09d %s", timestamp, action + snakeOrLadder));
        for (int cell : cells) {
            entry.append(" ").append(cell);
        }
        synchronized (log) { log.add(entry.toString()); }
    }

    private static void printLog() {
        Collections.sort(log);
        log.forEach(System.out::println);
    }
}

