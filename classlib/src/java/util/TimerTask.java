package java.util;

public abstract class TimerTask implements Runnable {
    static final int VIRGIN = 0;
    static final int SCHEDULED = 1;
    static final int EXECUTED = 2;
    static final int CANCELLED = 3;

    final Object lock = new Object();
    int state = VIRGIN;
    long nextExecutionTime;
    /* 0: one-shot; > 0: fixed-rate; < 0: fixed-delay of -period. */
    long period;

    protected TimerTask() {
    }

    public abstract void run();

    public boolean cancel() {
        synchronized (lock) {
            boolean result = state == SCHEDULED;
            state = CANCELLED;
            return result;
        }
    }

    public long scheduledExecutionTime() {
        synchronized (lock) {
            return period < 0 ? nextExecutionTime + period : nextExecutionTime - period;
        }
    }
}
