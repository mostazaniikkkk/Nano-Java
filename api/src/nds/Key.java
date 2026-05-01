package nds;

public class Key {
    public static final int A      = 1<<0;  //!< Keypad A button.
    public static final int B      = 1<<1;  //!< Keypad B button.
    public static final int SELECT = 1<<2;  //!< Keypad SELECT button.
    public static final int START  = 1<<3;  //!< Keypad START button.
    public static final int RIGHT  = 1<<4;  //!< Keypad RIGHT button.
    public static final int LEFT   = 1<<5;  //!< Keypad LEFT button.
    public static final int UP     = 1<<6;  //!< Keypad UP button.
    public static final int DOWN   = 1<<7;  //!< Keypad DOWN button.
    public static final int R      = 1<<8;  //!< Right shoulder button.
    public static final int L      = 1<<9;  //!< Left shoulder button.
    public static final int X      = 1<<10; //!< Keypad X button.
    public static final int Y      = 1<<11; //!< Keypad Y button.
    public static final int TOUCH  = 1<<12; //!< Touchscreen pendown.
    public static final int LID    = 1<<13;  //!< Lid state.

    /**
     * Updates internal variables. Call this once per frame.
     */
    public static native void scan();

    /**
     * Returns bitmap of keys that are currently held down.
     */
    public static native int held();

    /**
     * Returns bitmap of keys that were just pressed down this frame.
     */
    public static native int down();

    /**
     * Returns bitmap of keys that were just pressed down this frame, or are being repeated.
     * @see setRepeat
     */
    public static native int downRepeat();

    /**
     * Sets how often the keys repeat
     */
    public static native void setRepeat(int delay, int repeat);

    /**
     * Returns bitmap of keys that were released this frame.
     */
    public static native int up();

}
