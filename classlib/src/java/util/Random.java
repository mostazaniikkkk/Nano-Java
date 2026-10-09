package java.util;

/* The java.util.Random 48-bit linear congruential generator, bit-exact. */
public class Random {
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;

    /* Keeps two Randoms created in the same millisecond apart. */
    private static long seedUniquifier = 8682522807148012L;

    private long seed;

    public Random() {
        this(nextSeedUniquifier() ^ System.currentTimeMillis());
    }

    private static synchronized long nextSeedUniquifier() {
        seedUniquifier *= 181783497276652981L;
        return seedUniquifier;
    }

    public Random(long seed) {
        setSeed(seed);
    }

    public synchronized void setSeed(long seed) {
        this.seed = (seed ^ MULTIPLIER) & MASK;
    }

    protected synchronized int next(int bits) {
        seed = (seed * MULTIPLIER + ADDEND) & MASK;
        return (int) (seed >>> (48 - bits));
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }
        if ((n & -n) == n) {
            return (int) ((n * (long) next(31)) >> 31);
        }
        int bits;
        int val;
        do {
            bits = next(31);
            val = bits % n;
        } while (bits - val + (n - 1) < 0);
        return val;
    }

    public long nextLong() {
        return ((long) next(32) << 32) + next(32);
    }

    public float nextFloat() {
        return next(24) / ((float) (1 << 24));
    }

    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) * (1.0 / (1L << 53));
    }
}
