package nds; 
 
public class Memory { 
    public static int REG_EXMEMCNT = (0x04000204);
    public static int REG_EXMEMSTAT = (0x04000204);

    public static int ARM7_MAIN_RAM_PRIORITY = 1<<15;
    public static int ARM7_OWNS_CARD = 1<<11;
    public static int ARM7_OWNS_ROM =  1<<7;

    public static int ALLRAM =        (0x00000000);

    public static int MAINRAM8 =      (0x02000000);
    public static int MAINRAM16 =     (0x02000000);
    public static int MAINRAM32 =     (0x02000000);

    // fixme: shared RAM

    // GBA_BUS is volatile, while GBAROM is not
    public static int GBA_BUS =       ((0x08000000));
    public static int GBAROM =        (0x08000000);
    public static int SRAM =          (0x0A000000);


    public static int PALETTE =       (0x05000000);
    public static int PALETTE_SUB =   (0x05000400);

    public static int BG_PALETTE =       (0x05000000);
    public static int BG_PALETTE_SUB =   (0x05000400);

    public static int SPRITE_PALETTE = (0x05000200);
    public static int SPRITE_PALETTE_SUB = (0x05000600);

    public static int BG_GFX =                  (0x6000000);
    public static int BG_GFX_SUB =              (0x6200000);
    public static int SPRITE_GFX =                      (0x6400000);
    public static int SPRITE_GFX_SUB =          (0x6600000);

    public static int VRAM_0 =        (0x6000000);
    public static int VRAM =          (0x6800000);
    public static int VRAM_A =        (0x6800000);
    public static int VRAM_B =        (0x6820000);
    public static int VRAM_C =        (0x6840000);
    public static int VRAM_D =        (0x6860000);
    public static int VRAM_E =        (0x6880000);
    public static int VRAM_F =        (0x6890000);
    public static int VRAM_G =        (0x6894000);
    public static int VRAM_H =        (0x6898000);
    public static int VRAM_I =        (0x68A0000);

    public static int OAM =           (0x07000000);
    public static int OAM_SUB =       (0x07000400);

    // Changes only the gba rom bus ownership
    public static void sysSetCartOwner(boolean arm9) {
      W16(REG_EXMEMCNT, (R16(REG_EXMEMCNT) & ~ARM7_OWNS_ROM) | (arm9 ? 0 :  ARM7_OWNS_ROM));
    }
    // Changes only the nds card bus ownership
    public static void sysSetCardOwner(boolean arm9) {
      W16(REG_EXMEMCNT, (R16(REG_EXMEMCNT) & ~ARM7_OWNS_CARD) | (arm9 ? 0 : ARM7_OWNS_CARD));
    }

    // Changes all bus ownerships
    public static void sysSetBusOwners(boolean arm9rom, boolean arm9card) {
      int pattern = R16(REG_EXMEMCNT) & ~(ARM7_OWNS_CARD|ARM7_OWNS_ROM);
      pattern = pattern | (arm9card ?  0: ARM7_OWNS_CARD ) |
                          (arm9rom ?  0: ARM7_OWNS_ROM );
      W16(REG_EXMEMCNT, pattern);
    }

    // These are hacks to read & write values directly from memory.
    // You should only use them for messing with hardware registers
    public static native int   R32(int addr);
    public static native void  W32(int addr, int data);
    public static native short R16(int addr);
    public static native void  W16(int addr, int data);
    public static native byte  R8 (int addr);
    public static native void  W8 (int addr, int data);

    public static native void  WArrShort(int addr, short[] data);
}
