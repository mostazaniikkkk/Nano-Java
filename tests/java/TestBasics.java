// Core bytecode semantics: arithmetic, control flow, objects, arrays,
// exceptions and class initialization.
public class TestBasics {
    interface Shape {
        int area();
    }

    static abstract class Base implements Shape {
        protected String name;

        Base(String name) {
            this.name = name;
        }

        public String describe() {
            return name + ":" + area();
        }
    }

    static class Rect extends Base {
        int w, h;

        Rect(int w, int h) {
            super("rect");
            this.w = w;
            this.h = h;
        }

        public int area() {
            return w * h;
        }
    }

    static class Square extends Rect {
        Square(int s) {
            super(s, s);
            name = "square";
        }

        public String describe() {
            return "[" + super.describe() + "]";
        }
    }

    static int initCount;
    static final int[] TABLE;

    static {
        TABLE = new int[5];
        for (int i = 0; i < TABLE.length; i++) {
            TABLE[i] = i * i;
        }
        initCount++;
    }

    static class Lazy {
        static String value;

        static {
            System.out.println("Lazy.<clinit>");
            value = "lazy";
        }
    }

    static int fib(int n) {
        return n < 2 ? n : fib(n - 1) + fib(n - 2);
    }

    static String classify(int x) {
        switch (x) {
        case 1:
            return "one";
        case 2:
            return "two";
        case 3:
            return "three";
        case 100:
            return "hundred";
        case -5:
            return "minus five";
        default:
            return "other";
        }
    }

    static String dense(int x) {
        switch (x) {
        case 0: return "a";
        case 1: return "b";
        case 2: return "c";
        case 3: return "d";
        default: return "?";
        }
    }

    static int finallyTest(int x) {
        int r = 0;
        try {
            if (x == 0) {
                throw new IllegalStateException("zero");
            }
            r = 10 / x;
        } catch (IllegalStateException e) {
            r = -1;
        } finally {
            r += 1000;
        }
        return r;
    }

    public static void main(String[] args) {
        // ints and overflow
        int a = Integer.MAX_VALUE;
        System.out.println(a + 1);
        System.out.println(Integer.MIN_VALUE / -1);
        System.out.println(Integer.MIN_VALUE % -1);
        System.out.println(-7 / 2 + " " + -7 % 2 + " " + (-7 >> 1) + " " + (-7 >>> 28));
        System.out.println((1 << 33) + " " + (byte) 200 + " " + (short) 70000 + " " + (char) 65);

        // longs
        long l = Long.MAX_VALUE;
        System.out.println(l + 1);
        System.out.println(123456789L * 987654321L);
        System.out.println((-1L >>> 1) + " " + (1L << 63) + " " + (-17L / 5) + " " + (-17L % 5));
        System.out.println(Long.MIN_VALUE / -1);

        // floating point
        double d = 1.0 / 3;
        float f = 2.5f;
        System.out.println(d);
        System.out.println(f * 3);
        System.out.println((int) 3.99 + " " + (int) -3.99 + " " + (int) Double.NaN);
        System.out.println((int) 1e20 + " " + (long) -1e30 + " " + (long) Float.POSITIVE_INFINITY);
        System.out.println(5.5 % 2 + " " + (0.0 / 0.0 == 0.0 / 0.0) + " " + (1.0 / 0));
        System.out.println(Math.sqrt(2) + " " + Math.floor(-1.5) + " " + Math.ceil(-1.5));
        System.out.println(100.0f + " " + 1e10 + " " + 0.001 + " " + 1.0E-4 + " " + 1234.5f);

        // control flow
        System.out.println(fib(20));
        System.out.println(classify(2) + " " + classify(100) + " " + classify(-5) + " " + classify(7));
        System.out.println(dense(0) + dense(3) + dense(4) + dense(-1));
        System.out.println(finallyTest(5) + " " + finallyTest(0));

        // objects and dispatch
        Shape[] shapes = {new Rect(2, 3), new Square(4)};
        for (int i = 0; i < shapes.length; i++) {
            Base b = (Base) shapes[i];
            System.out.println(b.describe() + " " + (shapes[i] instanceof Square));
        }

        // arrays
        int[][] grid = new int[3][4];
        grid[2][3] = 7;
        System.out.println(grid.length + " " + grid[0].length + " " + grid[2][3]);
        char[] cs = {'h', 'i'};
        char[] copy = (char[]) cs.clone();
        copy[0] = 'H';
        System.out.println(new String(cs) + new String(copy));
        Object[] objs = new String[2];
        try {
            objs[0] = new Integer(1);
        } catch (ArrayStoreException e) {
            System.out.println("ArrayStoreException");
        }
        long[] longs = new long[3];
        longs[1] = -5L;
        System.out.println(longs[0] + longs[1] + longs[2]);
        boolean[] flags = new boolean[2];
        flags[1] = true;
        System.out.println(flags[0] + " " + flags[1]);

        // exceptions
        try {
            int[] x = new int[2];
            x[2] = 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("AIOOBE " + e.getMessage());
        }
        try {
            Object o = null;
            o.toString();
        } catch (NullPointerException e) {
            System.out.println("NPE");
        }
        try {
            Object o = "s";
            Integer i = (Integer) o;
            System.out.println(i);
        } catch (ClassCastException e) {
            System.out.println("CCE");
        }
        try {
            System.out.println(1 / (args.length));
        } catch (ArithmeticException e) {
            System.out.println("AE " + e.getMessage());
        }
        try {
            new int[-1].toString();
        } catch (NegativeArraySizeException e) {
            System.out.println("NASE");
        }
        try {
            throw new RuntimeException("custom");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // static init
        System.out.println(initCount + " " + TABLE[4]);
        System.out.println("before lazy");
        System.out.println(Lazy.value);
        System.out.println(Lazy.value);

        // strings and identity
        String s1 = "hello";
        String s2 = "hel" + "lo";
        String s3 = new StringBuffer("hel").append("lo").toString();
        System.out.println((s1 == s2) + " " + (s1 == s3) + " " + s1.equals(s3) + " " + (s1 == s3.intern()));
        System.out.println(s1.hashCode() + " " + "".hashCode());

        // Class
        System.out.println(new Square(1).getClass().getName());
        System.out.println(new int[0].getClass().getName() + " " + new String[0].getClass().getName());
        try {
            Class c = Class.forName("TestBasics$Rect");
            System.out.println(c.getName() + " " + c.isInstance(new Square(2)));
        } catch (ClassNotFoundException e) {
            System.out.println("CNFE");
        }
        try {
            Class.forName("does.not.Exist");
        } catch (ClassNotFoundException e) {
            System.out.println("CNFE " + e.getMessage());
        }
    }
}
