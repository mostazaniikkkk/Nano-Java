package nds;

public class System {
    
    //!     LCD status register.
    public static int REG_DISPSTAT =    (0x04000004);

    //!     The display currently in a vertical blank.
    public static int DISP_IN_VBLANK =     1<<0;

    //!     The display currently in a horizontal blank.
    public static int DISP_IN_HBLANK =     1<<1;

    //!     Current scanline and %DISP_Y match.
    public static int DISP_YTRIGGERED =    1<<2;

    //!     Interrupt on vertical blank.
    public static int DISP_VBLANK_IRQ =    1<<3;

    //!     Interrupt on horizontal blank.
    public static int DISP_HBLANK_IRQ =    1<<4;

    //!     Interrupt when current scanline and %DISP_Y match.
    public static int DISP_YTRIGGER_IRQ =  1<<5;

    public static void SetYtrigger(int Yvalue) {
        Memory.W16(REG_DISPSTAT, (Memory.R16(REG_DISPSTAT) & 0x007F ) | (Yvalue << 8) | (( Yvalue & 0x100 ) >> 2)) ;
    }

    //!     Current display scanline.
    public static int REG_VCOUNT =              (0x4000006);


    //!     Halt control register.
    /*!     Writing 0x40 to HALT_CR activates GBA mode.
            %HALT_CR can only be accessed via the BIOS.
    */
    public static int HALT_CR =       (0x04000300);
    //!     Power control register.
    /*!     This register controls what hardware should
            be turned on or off.
    */
    public static int REG_POWERCNT =    0x4000304;

    //!     Turns on specified hardware.
    /*!     This function should only be called after %powerSET.

            \param on What to power on.
    */
    public static void powerON(int on) { Memory.W16(REG_POWERCNT, Memory.R16(REG_POWERCNT) | on);}

    //!     Turns on only the specified hardware.
    /*!     Use this function to power on basic hardware types you
            wish to use throughout your program.

            \param on What to power on.
    */
    public static void powerSET(int on) { Memory.W16(REG_POWERCNT, on);}

    //!     Turns off the specified hardware.
    /*!     \param off What to power off.
    */
    public static void powerOFF(int off) { Memory.W16(REG_POWERCNT, Memory.R16(REG_POWERCNT) & ~off);}
    public static int POWER_LCD =                       1<<0;
    public static int POWER_2D_A =                      1<<1;
    public static int POWER_MATRIX =            1<<2;
    public static int POWER_3D_CORE =           1<<3;
    public static int POWER_2D_B =                      1<<9;
    public static int POWER_SWAP_LCDS =         1<<15;

    //!     Enables power to all hardware required for 2D video.
    public static int POWER_ALL_2D =     (POWER_LCD |POWER_2D_A |POWER_2D_B);

    //!     Enables power to all hardware required for 3D video.
    public static int POWER_ALL =                (POWER_ALL_2D | POWER_3D_CORE | POWER_MATRIX);

    //!     Switches the screens.
    public static  void lcdSwap() { Memory.W16(REG_POWERCNT, Memory.R16(REG_POWERCNT) ^ POWER_SWAP_LCDS); }

    //!     Forces the main core to display on the top.
    public static  void lcdMainOnTop() { Memory.W16(REG_POWERCNT, Memory.R16(REG_POWERCNT) | POWER_SWAP_LCDS); }

    //!     Forces the main core to display on the bottom.
    public static  void lcdMainOnBottom() { Memory.W16(REG_POWERCNT, Memory.R16(REG_POWERCNT) & ~POWER_SWAP_LCDS); }
    public static int POWER_SOUND =       1<<0;
    public static int POWER_UNKNOWN =     1<<1;
    //!     Key input register.
    /*!     On the ARM9, the hinge "button," the touch status, and the
            X and Y buttons cannot be accessed directly.
    */
    public static int REG_KEYINPUT =    (0x04000130);

    //!     Key input control register.
    public static int REG_KEYCNT =              (0x04000132);

}
