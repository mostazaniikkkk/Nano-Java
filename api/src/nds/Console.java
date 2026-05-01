package nds;

public class Console {
    /**
     * Clears the screen
     */
    public static native void cls();

    /**
     * Clears the screen and resets the cursor
     */
    public static native void cll();

    /**
     * Sets the cursor to the specified position
     */
    public static native void setpos(int x, int y);


    public static native void up();
    public static native void down();
    public static native void left();
    public static native void right();
    public static native void erase();
    
}

