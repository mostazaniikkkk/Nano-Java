package java.lang;

public class Thread implements Runnable {
    public static final int MIN_PRIORITY = 1;
    public static final int NORM_PRIORITY = 5;
    public static final int MAX_PRIORITY = 10;

    private static int counter;

    private Runnable target;
    private String name;
    private int priority = NORM_PRIORITY;
    private boolean started;

    public Thread() {
        this(null, null);
    }

    public Thread(String name) {
        this(null, name);
    }

    public Thread(Runnable target) {
        this(target, null);
    }

    public Thread(Runnable target, String name) {
        this.target = target;
        this.name = name != null ? name : "Thread-" + nextNumber();
        Thread parent = currentThread();
        if (parent != null) {
            priority = parent.priority;
        }
    }

    private static synchronized int nextNumber() {
        return counter++;
    }

    public static native Thread currentThread();

    public static native void yield();

    public static native void sleep(long millis) throws InterruptedException;

    public synchronized void start() {
        if (started) {
            throw new IllegalThreadStateException();
        }
        started = true;
        start0();
    }

    private native void start0();

    public void run() {
        if (target != null) {
            target.run();
        }
    }

    public final native boolean isAlive();

    public final native void join() throws InterruptedException;

    public void interrupt() {
        interrupt0();
    }

    private native void interrupt0();

    public static native int activeCount();

    public final void setPriority(int newPriority) {
        if (newPriority < MIN_PRIORITY || newPriority > MAX_PRIORITY) {
            throw new IllegalArgumentException();
        }
        priority = newPriority;
        setPriority0(newPriority);
    }

    private native void setPriority0(int p);

    public final int getPriority() {
        return priority;
    }

    public final String getName() {
        return name;
    }

    public String toString() {
        return "Thread[" + name + "," + priority + "]";
    }
}
