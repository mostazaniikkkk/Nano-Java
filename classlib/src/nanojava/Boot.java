package nanojava;

/**
 * First Java code the VM runs. Arguments:
 *   -main Class args...   run a CLDC program's static main(String[])
 *   -midlet [Class]       run a MIDlet (class from the jar manifest if empty)
 */
public final class Boot {
    private Boot() {
    }

    public static void main(String[] args) throws Throwable {
        if (args.length >= 2 && args[0].equals("-main")) {
            String[] rest = new String[args.length - 2];
            System.arraycopy(args, 2, rest, 0, rest.length);
            Class c;
            try {
                c = Class.forName(args[1]);
            } catch (ClassNotFoundException e) {
                System.out.println("nanojava: class not found: " + args[1]);
                exit(1);
                return;
            }
            invokeMain(c, rest);
        } else if (args.length >= 1 && args[0].equals("-midlet")) {
            MIDletRunner.run(args.length > 1 ? args[1] : "");
        } else {
            System.out.println("nanojava: bad boot arguments");
            exit(2);
        }
    }

    /* Calls c.main(args) after initializing c. */
    private static native void invokeMain(Class c, String[] args);

    static native void exit(int code);
}
