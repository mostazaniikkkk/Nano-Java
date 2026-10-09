package java.util;

/*
 * Runs tasks on one background thread, started with the first schedule().
 * The pending tasks are kept in a binary min-heap on nextExecutionTime.
 */
public class Timer {
    private TimerTask[] queue = new TimerTask[16];
    private int size;
    private boolean cancelled;
    private Thread thread;
    private final Object lock = new Object();

    public Timer() {
    }

    public void schedule(TimerTask task, long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        sched(task, System.currentTimeMillis() + delay, 0);
    }

    public void schedule(TimerTask task, Date time) {
        sched(task, time.getTime(), 0);
    }

    public void schedule(TimerTask task, long delay, long period) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(task, System.currentTimeMillis() + delay, -period);
    }

    public void schedule(TimerTask task, Date firstTime, long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(task, firstTime.getTime(), -period);
    }

    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(task, System.currentTimeMillis() + delay, period);
    }

    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(task, firstTime.getTime(), period);
    }

    private void sched(TimerTask task, long time, long period) {
        if (time < 0) {
            throw new IllegalArgumentException("Illegal execution time.");
        }
        synchronized (lock) {
            if (cancelled) {
                throw new IllegalStateException("Timer already cancelled.");
            }
            synchronized (task.lock) {
                if (task.state != TimerTask.VIRGIN) {
                    throw new IllegalStateException("Task already scheduled or cancelled");
                }
                task.nextExecutionTime = time;
                task.period = period;
                task.state = TimerTask.SCHEDULED;
            }
            add(task);
            if (thread == null) {
                thread = new Thread(new Runnable() {
                    public void run() {
                        mainLoop();
                    }
                }, "Timer");
                thread.start();
            } else if (queue[1] == task) {
                lock.notify();
            }
        }
    }

    public void cancel() {
        synchronized (lock) {
            cancelled = true;
            for (int i = 1; i <= size; i++) {
                queue[i] = null;
            }
            size = 0;
            lock.notify();
        }
    }

    private void mainLoop() {
        while (true) {
            TimerTask task;
            synchronized (lock) {
                try {
                    while (size == 0 && !cancelled) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    // Spurious wakeups and interrupts just recheck the queue.
                    continue;
                }
                if (cancelled) {
                    return;
                }
                task = queue[1];
                boolean fire;
                long now;
                long exec;
                synchronized (task.lock) {
                    if (task.state == TimerTask.CANCELLED) {
                        removeMin();
                        continue;
                    }
                    now = System.currentTimeMillis();
                    exec = task.nextExecutionTime;
                    fire = exec <= now;
                    if (fire) {
                        if (task.period == 0) {
                            removeMin();
                            task.state = TimerTask.EXECUTED;
                        } else {
                            task.nextExecutionTime = task.period < 0
                                    ? now - task.period : exec + task.period;
                            fixDown(1);
                        }
                    }
                }
                if (!fire) {
                    try {
                        lock.wait(exec - now);
                    } catch (InterruptedException e) {
                        // Recheck the queue.
                    }
                    continue;
                }
            }
            boolean ok = false;
            try {
                task.run();
                ok = true;
            } finally {
                if (!ok) {
                    // As in J2SE, an exception thrown by a task kills the timer.
                    cancel();
                }
            }
        }
    }

    // Heap operations; the heap occupies queue[1..size].

    private void add(TimerTask task) {
        if (size + 1 == queue.length) {
            TimerTask[] q = new TimerTask[queue.length * 2];
            System.arraycopy(queue, 0, q, 0, queue.length);
            queue = q;
        }
        queue[++size] = task;
        fixUp(size);
    }

    private void removeMin() {
        queue[1] = queue[size];
        queue[size--] = null;
        fixDown(1);
    }

    private void fixUp(int k) {
        while (k > 1) {
            int j = k >> 1;
            if (queue[j].nextExecutionTime <= queue[k].nextExecutionTime) {
                break;
            }
            TimerTask t = queue[j];
            queue[j] = queue[k];
            queue[k] = t;
            k = j;
        }
    }

    private void fixDown(int k) {
        int j;
        while ((j = k << 1) <= size && j > 0) {
            if (j < size && queue[j].nextExecutionTime > queue[j + 1].nextExecutionTime) {
                j++;
            }
            if (queue[k].nextExecutionTime <= queue[j].nextExecutionTime) {
                break;
            }
            TimerTask t = queue[j];
            queue[j] = queue[k];
            queue[k] = t;
            k = j;
        }
    }
}
