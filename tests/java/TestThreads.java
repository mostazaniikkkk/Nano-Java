// Green threads: start/join, synchronized, wait/notify, sleep, interrupt.
public class TestThreads {
    static int counter;
    static final Object lock = new Object();

    static class Adder extends Thread {
        public void run() {
            for (int i = 0; i < 20000; i++) {
                synchronized (lock) {
                    counter++;
                }
            }
        }
    }

    static class Queue {
        private int[] items = new int[4];
        private int count, head;

        synchronized void put(int v) throws InterruptedException {
            while (count == items.length) {
                wait();
            }
            items[(head + count) % items.length] = v;
            count++;
            notifyAll();
        }

        synchronized int take() throws InterruptedException {
            while (count == 0) {
                wait();
            }
            int v = items[head];
            head = (head + 1) % items.length;
            count--;
            notifyAll();
            return v;
        }
    }

    public static void main(String[] args) throws Exception {
        Thread[] ts = new Thread[4];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Adder();
            ts[i].start();
        }
        for (int i = 0; i < ts.length; i++) {
            ts[i].join();
        }
        System.out.println("counter " + counter);

        final Queue q = new Queue();
        Thread producer = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int i = 1; i <= 100; i++) {
                        q.put(i);
                    }
                    q.put(-1);
                } catch (InterruptedException e) {
                    System.out.println("producer interrupted");
                }
            }
        });
        producer.start();
        int sum = 0;
        for (;;) {
            int v = q.take();
            if (v < 0) {
                break;
            }
            sum += v;
        }
        System.out.println("sum " + sum);

        long t0 = System.currentTimeMillis();
        Thread.sleep(50);
        long dt = System.currentTimeMillis() - t0;
        System.out.println("slept " + (dt >= 45 && dt < 1000));

        Thread sleeper = new Thread() {
            public void run() {
                try {
                    Thread.sleep(10000);
                    System.out.println("woke normally");
                } catch (InterruptedException e) {
                    System.out.println("sleeper interrupted");
                }
            }
        };
        sleeper.start();
        Thread.sleep(20);
        sleeper.interrupt();
        sleeper.join();
        System.out.println("alive " + sleeper.isAlive());

        synchronized (lock) {
            t0 = System.currentTimeMillis();
            lock.wait(30);
            dt = System.currentTimeMillis() - t0;
        }
        System.out.println("timed wait " + (dt >= 25));

        try {
            lock.notify();
        } catch (IllegalMonitorStateException e) {
            System.out.println("IMSE");
        }

        Thread t = new Thread("named");
        System.out.println(t.getName() + " " + Thread.currentThread().getName());

        System.out.println("done");
    }
}
