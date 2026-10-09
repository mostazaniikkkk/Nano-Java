package javax.microedition.lcdui;

public class Command {
    public static final int SCREEN = 1;
    public static final int BACK = 2;
    public static final int CANCEL = 3;
    public static final int OK = 4;
    public static final int HELP = 5;
    public static final int STOP = 6;
    public static final int EXIT = 7;
    public static final int ITEM = 8;

    private final String shortLabel;
    private final String longLabel;
    private final int type;
    private final int priority;

    public Command(String label, int commandType, int priority) {
        this(label, null, commandType, priority);
    }

    public Command(String shortLabel, String longLabel, int commandType, int priority) {
        if (shortLabel == null) {
            throw new NullPointerException();
        }
        if (commandType < SCREEN || commandType > ITEM) {
            throw new IllegalArgumentException();
        }
        this.shortLabel = shortLabel;
        this.longLabel = longLabel;
        this.type = commandType;
        this.priority = priority;
    }

    public String getLabel() {
        return shortLabel;
    }

    public String getLongLabel() {
        return longLabel;
    }

    public int getCommandType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    /* Commands that dismiss a screen go to the right soft key. */
    boolean isNegative() {
        return type == BACK || type == CANCEL || type == EXIT || type == STOP;
    }
}
