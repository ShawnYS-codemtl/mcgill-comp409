import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class q2 {

    static class Monitor {
        private int taCount = 0; // Number of TAs waiting with questions
        private int gradCount = 0; // Number of grad students arrived
        private int wokenGradCount = 0;
        private static final int NUM_TAS = 3;
        private final Lock lock = new ReentrantLock();
        private final Condition needMoreTAs = lock.newCondition();  // Condition for TAs
        private final Condition profWakeup = lock.newCondition(); // Condition for Professor
        private final Condition waitForProf = lock.newCondition(); // Condition for Grad Students
        private final Condition waitForGrads = lock.newCondition(); // Condition for Professor

        // TA-related conditions
        public void taWaitForGroup() throws InterruptedException {
            lock.lock();
            try {
                taCount++;
                System.out.println("a TA comes up with a question");
                if (taCount < NUM_TAS) {
                    needMoreTAs.await();
                }

                if (taCount == NUM_TAS){

                    // Wake up professor
                    profWakeup.signal();

                    // Wake exactly two TAs
                    needMoreTAs.signal();
                    needMoreTAs.signal();
                }
            } finally {
                lock.unlock();
            }
        }

        public void professorRoutine() throws InterruptedException {
            lock.lock();
            try {
                while (taCount < NUM_TAS && gradCount < 5) {  // Wait until either condition is met
                    System.out.println("P goes to sleep");
                    profWakeup.await(); // Wait for either event
                }

                System.out.println("P wakes up. (taCount: " + taCount + ", gradCount: " + gradCount + ")");

                if (gradCount == 5) {
                    System.out.println("P wakes their grad students");
                    waitForProf.signalAll();

                    while (wokenGradCount < 5){
                        waitForGrads.await();
                    }

                    System.exit(0);

                } else if (taCount == NUM_TAS) {
                    // Professor addresses set of TA questions
                    System.out.println("a group of TAs starts to be seen by P,");
                    Thread.sleep(500);
                    System.out.println("a group of TAs questions have been answered,");

                    // Reset taCount only AFTER handling the TAs
                    taCount = 0;
                }
            } finally {
                lock.unlock();
            }
        }

        // Grad student related conditions
        public void gradArrives() throws InterruptedException {
            lock.lock();
            try {
                gradCount++;
                System.out.println("a grad student arrives");
                if (gradCount == 5){
                    // notify professor
                    profWakeup.signal();
                    System.out.println("a grad student interrupts a TA session");
                    // already awake and ready for research
                    wokenGradCount++;
                } else {
                    waitForProf.await();
                    System.out.println("a grad student wakes up for research");
                    wokenGradCount++;
                    if (wokenGradCount == 5) {
                        System.out.println("all grad students have been woken");
                        waitForGrads.signal();
                    }

                }
            } finally {
                lock.unlock();
            }
        }
    }

    static class TA extends Thread {
        private final Monitor monitor;
        private final int id;
        private final Random rand;

        public TA(Monitor monitor, int id) {
            this.monitor = monitor;
            this.id = id;
            this.rand = new Random();
        }

        public void run() {
            try {
                while (true) {
                    if (rand.nextInt(100) < 10) { // 10% chance to ask a question
                        monitor.taWaitForGroup();
                    }
                    Thread.sleep(1000); // Sleep for 1 second before possibly asking another question
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class GradStudent extends Thread {
        private final Monitor monitor;
        private final int id;

        public GradStudent(Monitor monitor, int id) {
            this.monitor = monitor;
            this.id = id;
        }

        public void run() {
            try {
                Thread.sleep(new Random().nextInt(51) + 10 * 1000); // Random arrival time between 10 and 60 seconds
                monitor.gradArrives();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Professor extends Thread {
        private final Monitor monitor;

        public Professor(Monitor monitor) {
            this.monitor = monitor;
        }

        public void run() {
            try {
                while (true) {
                    monitor.professorRoutine();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int k = Integer.parseInt(args[0]); // Number of TAs
        int q = Integer.parseInt(args[1]); // Probability of TA asking a question (q%)

        Monitor monitor = new Monitor();

        // Create and start TA threads
        TA[] tas = new TA[k];
        for (int i = 0; i < k; i++) {
            tas[i] = new TA(monitor, i + 1);
            tas[i].start();
        }

        // Create and start GradStudent threads
        GradStudent[] gradStudents = new GradStudent[5];
        for (int i = 0; i < 5; i++) {
            gradStudents[i] = new GradStudent(monitor, i + 1);
            gradStudents[i].start();
        }

        // Create and start Professor thread
        Professor professor = new Professor(monitor);
        professor.start();
    }
}

