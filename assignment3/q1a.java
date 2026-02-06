import java.util.concurrent.locks.ReentrantLock;

public class q1a {
    private Object[] array;
    private Object[] locks; // Array of locks per index
    private volatile int size; // Ensures visibility across threads
    private final ReentrantLock resizeLock = new ReentrantLock(); // Only for resizing

    public q1a() {
        this.array = new Object[20]; // Initial size 20
        this.locks = new Object[20]; // Locks for each element
        this.size = 20;
        initializeLocks();
    }

    private void initializeLocks() {
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object(); // Each index gets a separate lock
        }
    }

    public Object get(int i) {
        if (i > size) {
            throw new IndexOutOfBoundsException("Access beyond allowed limit.");
        }
        if (i == size) { // Resize only when accessing exactly one past the end
            resize();
        }
        synchronized (locks[i]) { // Fine-grained lock for this index
            return array[i];
        }
    }

    public void set(int i, Object o) {
        if (i > size) {
            throw new IndexOutOfBoundsException("Access beyond allowed limit.");
        }
        if (i == size) { // Resize only when accessing exactly one past the end
            resize();
        }
        synchronized (locks[i]) { // Fine-grained lock for this index
            array[i] = o;
        }
    }

    private void resize() {
        resizeLock.lock(); // Ensure only one thread resizes at a time
        try {
            int newCapacity = size + 10; // Increase by 10
            Object[] newArray = new Object[newCapacity];
            Object[] newLocks = new Object[newCapacity]; // New lock array

            System.arraycopy(array, 0, newArray, 0, size);
            System.arraycopy(locks, 0, newLocks, 0, size);

            for (int i = size; i < newCapacity; i++) {
                newLocks[i] = new Object(); // Initialize new locks
            }

            array = newArray;
            locks = newLocks; // Swap in the new lock array
            size = newCapacity;
        } finally {
            resizeLock.unlock();
        }
    }

    public int getSize() {
        return size;
    }
}


