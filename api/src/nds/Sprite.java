package nds;

public class Sprite {
    public byte entry; // 0 - 128

    public char x,y;

    public char attr0, attr1, attr2;

    public native void update();

// Attribute 0 consists of 8 bits of Y plus the following flags:
    public static final char ATTR0_NORMAL = (0<<8);
    public static final char ATTR0_ROTSCALE = (1<<8);
    public static final char ATTR0_DISABLED = (2<<8);
    public static final char ATTR0_ROTSCALE_DOUBLE = (3<<8);
//
    public static final char ATTR0_TYPE_NORMAL = (0<<10);
    public static final char ATTR0_TYPE_BLENDED = (1<<10);
    public static final char ATTR0_TYPE_WINDOWED = (2<<10);
    public static final char ATTR0_BMP = (3<<10);
//
    public static final char ATTR0_MOSAIC = (1<<12);
//
    public static final char ATTR0_COLOR_16 = (0<<13); //16 color in tile mode...16 bit in bitmap mode;
    public static final char ATTR0_COLOR_256 = (1<<13);
//
    public static final char ATTR0_SQUARE = (0<<14);
    public static final char ATTR0_WIDE = (1<<14);
    public static final char ATTR0_TALL = (2<<14);
//
    public static final char OBJ_Y(char m) { return (char)((m)&(char)0x00ff); }
//
// // Atribute 1 consists of 9 bits of X plus the following flags:
    public static final char ATTR1_ROTDATA(char n) { return (char)((n)<<9); }  // note: overlaps with flip flags;
    public static final char ATTR1_FLIP_X = (1<<12);
    public static final char ATTR1_FLIP_Y = (1<<13);
    public static final char ATTR1_SIZE_8 = (0<<14);
    public static final char ATTR1_SIZE_16 = (1<<14);
    public static final char ATTR1_SIZE_32 = (2<<14);
    public static final char ATTR1_SIZE_64 = (3<<14);
//
    public static final char OBJ_X(char m) {return (char)((m)&(char)0x01ff); }
//
// // Atribute 2 consists of the following:
    public static final char ATTR2_PRIORITY(char n) {return (char)((n)<<10); }
    public static final char ATTR2_PALETTE(char n) {return (char)((n)<<12); }
    public static final char ATTR2_ALPHA(char n) {return (char)((n)<<12); }
//


}
