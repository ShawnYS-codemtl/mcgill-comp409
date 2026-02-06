import java.util.concurrent.atomic.*;

public class q1b {
    private final AtomicReference<Object>[] array;
    private final int size;
    private static final AtomicReference<q1b> instance = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    public q1b() {
        this.array = new AtomicReference[20];
        for (int i = 0; i < 20; i++) {
            this.array[i] = new AtomicReference<>(null);
        }
        this.size = 20;
        instance.set(this);
    }

    public Object get(int i) {
        while (true) {
            q1b current = instance.get();
            if (i >= current.size) {
                throw new IndexOutOfBoundsException("Access beyond allowed limit.");
            }

            // Fetch again in case resize happened
            current = instance.get();
            return current.array[i].get();
        }
    }

    public void set(int i, Object o) {
        while (true) {
            q1b current = instance.get();

            if (i >= current.size) {
                ensureCapacity(i);
                continue; // Retry with updated instance
            }

            // Fetch again in case resize happened
            current = instance.get();
            current.array[i].set(o);
            return;
        }
    }

    public void ensureCapacity(int i) {
        while (true) {
            q1b current = instance.get();
            if (i < current.size) {
                return; // Already large enough
            }

            q1b newInstance = new q1b(current.size + 10, current);

            // Only one thread will succeed in replacing the instance
            if (instance.compareAndSet(current, newInstance)) {
                return;
            }
            // If CAS fails, another thread resized, so retry with new instance
        }
    }

    private q1b(int newSize, q1b oldInstance) {
        this.array = new AtomicReference[newSize];
        for (int i = 0; i < oldInstance.size; i++) {
            this.array[i] = new AtomicReference<>(oldInstance.array[i].get());
        }
        for (int i = oldInstance.size; i < newSize; i++) {
            this.array[i] = new AtomicReference<>(null);
        }
        this.size = newSize;
    }

    public int getSize() {
        return instance.get().size;
    }
}
