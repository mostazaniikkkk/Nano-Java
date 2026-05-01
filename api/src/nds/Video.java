package nds; 
 
public class Video { 
    

    public static native void lcdSwap(); 
    public static native void initVideo();   
    public static native void blit(
	short[] dst, int dstW, int dstH,
	short[] src, int srcW, int srcH,
	int dstX, int dstY,
	int clipX, int clipY, int clipW, int clipH,
	byte transp, byte[] alpha
    );
    public static native void blitGray(
    	short[] dst, int dstW, int dstH,
	byte[] src, int srcW, int srcH,
	int dstX, int dstY, short color, 
	int clipX, int clipY, int clipW, int clipH 
    );
    public static native void blitRGB(
	short[] dst, int dstW, int dstH,
	int[] src, int srcW, int srcH, 
	int dstX,int dstY, int offset, int scanlength,
	int clipX, int clipY, int clipW, int clipH, 
	byte[] alpha, boolean  processAlpha
    );
    public static native void blitRGB4444(
	short[] dst, int dstW, int dstH,
	short[] src, int srcW, int srcH,
	int dstX,int dstY, int offset, int scan,
	byte[] alpha, boolean  processAlpha,
	int manipX, int manipY, int rotate
    );
    public static native void blitRGB8888(
	short[] dst, int dstW, int dstH,
	int[] src, int srcW, int srcH,
	int dstX,int dstY, int offset, int scan,
	byte[] alpha, boolean  processAlpha,
	int manipX, int manipY, int rotate
    );
    public static native void getRGB(
	short[] dst, int dstW, int dstH,
	int[] src, int srcW, int srcH, 
	int dstX,int dstY, int offset, int scanlength,
	byte[] alpha
    );
    public static native void fillRect(
    	short[] dst, int dstW, int dstH, //surface data 
	int dstX, int dstY, int srcW, int srcH, short color, //command data
	int clipX, int clipY, int clipW, int clipH,
	int alpha
    );

    public static native void drawLine(
       	short[] dst, int dstW, int dstH,			//surface data
	int x1, int y1, int x2, int y2, short color, //command data			
	int clipX, int clipY, int clipW, int clipH     
    ) ;

    public static native void fillTriangle(
	short[] dst, int dstW, int dstH,
	int x1, int y1, int x2, int y2, int x3, int y3, short color, 
	int clipX, int clipY, int clipW, int clipH 
    );

    public static native void drawArc(
       	short[] dst, int dstW, int dstH,			//surface data 			
	int x, int y, int width, int height, int startAngle, int arcAngle, short color, //command data			
	int clipX, int clipY, int clipW, int clipH     
    );

    public static native void fillArc(
       	short[] dst, int dstW, int dstH,			//surface data 			
	int x, int y, int width, int height, int startAngle, int arcAngle, short color, //command data			
	int clipX, int clipY, int clipW, int clipH     
    );

    public static int getImageSize( byte[] imageBuffer) {
	if ((imageBuffer[0] & 0xFF) != 0x89 || imageBuffer[1] != 0x50 || imageBuffer[2] != 0x4E || imageBuffer[3] != 0x47){
		return 0;
	}
    	
    	int w = readInt(imageBuffer, 16);
    	int h = readInt(imageBuffer, 20);
    	return (w<<16) | h;
    }
    private static int readInt(byte[] tmpBuf, int pos) {
		int size  = (tmpBuf[pos++] & 0xFF) << 24;
		size |= 	(tmpBuf[pos++] & 0xFF) << 16;
		size |= 	(tmpBuf[pos++] & 0xFF) << 8;
		size |= 	(tmpBuf[pos] & 0xFF);
		return size;
    }
    public static native int decodePngImage(byte[] imageBuffer, int size, short[] pixelBuf, byte[] alphaBuf) ;

// macro creates a 15 bit color from 3x5 bit components
    public static final int RGB15(int r, int g, int b) { return ((r)|((g)<<5)|((b)<<10)); }
    public static final int RGB5(int r, int g, int b) { return ((r)|((g)<<5)|((b)<<10)); }
    public static final int RGB8(int r, int g, int b) { return (((r)>>3)|(((g)>>3)<<5)|(((b)>>3)<<10)); }

    public static final int SCREEN_HEIGHT = 192;
    public static final int SCREEN_WIDTH = 256;
//	Vram Control
    public static final int VRAM_CR = (0x04000240);
    public static final int VRAM_A_CR = (0x04000240);
    public static final int VRAM_B_CR = (0x04000241);
    public static final int VRAM_C_CR = (0x04000242);
    public static final int VRAM_D_CR = (0x04000243);
    public static final int VRAM_E_CR = (0x04000244);
    public static final int VRAM_F_CR = (0x04000245);
    public static final int VRAM_G_CR = (0x04000246);
    public static final int WRAM_CR = (0x04000247);
    public static final int VRAM_H_CR = (0x04000248);
    public static final int VRAM_I_CR = (0x04000249);

    public static final int VRAM_ENABLE = (1<<7);

    public static final int VRAM_OFFSET(int n) {return ((n)<<3); }


    public static final int VRAM_A_LCD	=	0;
    public static final int VRAM_A_MAIN_BG  = 1;
    public static final int VRAM_A_MAIN_BG_0x06000000	= 1 | VRAM_OFFSET(0);
    public static final int VRAM_A_MAIN_BG_0x06020000	= 1 | VRAM_OFFSET(1);
    public static final int VRAM_A_MAIN_BG_0x06040000	= 1 | VRAM_OFFSET(2);
    public static final int VRAM_A_MAIN_BG_0x06060000	= 1 | VRAM_OFFSET(3);
    public static final int VRAM_A_MAIN_SPRITE = 2;
    public static final int VRAM_A_MAIN_SPRITE_0x06400000	= 2;
    public static final int VRAM_A_MAIN_SPRITE_0x06420000	= 2 | VRAM_OFFSET(1);
    public static final int VRAM_A_TEXTURE = 3;
    public static final int VRAM_A_TEXTURE_SLOT0	= 3 | VRAM_OFFSET(0);
    public static final int VRAM_A_TEXTURE_SLOT1	= 3 | VRAM_OFFSET(1);
    public static final int VRAM_A_TEXTURE_SLOT2	= 3 | VRAM_OFFSET(2);
    public static final int VRAM_A_TEXTURE_SLOT3	= 3 | VRAM_OFFSET(3);

    public static final int VRAM_B_LCD = 0;
    public static final int VRAM_B_MAIN_BG	= 1 | VRAM_OFFSET(1);
    public static final int VRAM_B_MAIN_BG_0x06000000	= 1 | VRAM_OFFSET(0);
    public static final int VRAM_B_MAIN_BG_0x06020000	= 1 | VRAM_OFFSET(1);
    public static final int VRAM_B_MAIN_BG_0x06040000	= 1 | VRAM_OFFSET(2);
    public static final int VRAM_B_MAIN_BG_0x06060000	= 1 | VRAM_OFFSET(3);
    public static final int VRAM_B_MAIN_SPRITE	= 2 | VRAM_OFFSET(1);
    public static final int VRAM_B_MAIN_SPRITE_0x06400000	= 2;
    public static final int VRAM_B_MAIN_SPRITE_0x06420000	= 2 | VRAM_OFFSET(1);
    public static final int VRAM_B_TEXTURE	= 3 | VRAM_OFFSET(1);
    public static final int VRAM_B_TEXTURE_SLOT0	= 3 | VRAM_OFFSET(0);
    public static final int VRAM_B_TEXTURE_SLOT1	= 3 | VRAM_OFFSET(1);
    public static final int VRAM_B_TEXTURE_SLOT2	= 3 | VRAM_OFFSET(2);
    public static final int VRAM_B_TEXTURE_SLOT3	= 3 | VRAM_OFFSET(3);

    public static final int VRAM_C_LCD = 0;
    public static final int VRAM_C_MAIN_BG  = 1 | VRAM_OFFSET(2);
    public static final int VRAM_C_MAIN_BG_0x06000000	= 1 | VRAM_OFFSET(0);
    public static final int VRAM_C_MAIN_BG_0x06020000	= 1 | VRAM_OFFSET(1);
    public static final int VRAM_C_MAIN_BG_0x06040000	= 1 | VRAM_OFFSET(2);
    public static final int VRAM_C_MAIN_BG_0x06060000	= 1 | VRAM_OFFSET(3);
    public static final int VRAM_C_ARM7	= 2;
    public static final int VRAM_C_ARM7_0x06000000 = 2;
    public static final int VRAM_C_ARM7_0x06020000 = 2 | VRAM_OFFSET(1);
    public static final int VRAM_C_SUB_BG	= 4;
    public static final int VRAM_C_SUB_BG_0x06200000	= 4 | VRAM_OFFSET(0);
    public static final int VRAM_C_SUB_BG_0x06220000	= 4 | VRAM_OFFSET(1);
    public static final int VRAM_C_SUB_BG_0x06240000	= 4 | VRAM_OFFSET(2);
    public static final int VRAM_C_SUB_BG_0x06260000	= 4 | VRAM_OFFSET(3);
    public static final int VRAM_C_TEXTURE	= 3 | VRAM_OFFSET(2);
    public static final int VRAM_C_TEXTURE_SLOT0	= 3 | VRAM_OFFSET(0);
    public static final int VRAM_C_TEXTURE_SLOT1	= 3 | VRAM_OFFSET(1);
    public static final int VRAM_C_TEXTURE_SLOT2	= 3 | VRAM_OFFSET(2);
    public static final int VRAM_C_TEXTURE_SLOT3	= 3 | VRAM_OFFSET(3);

    public static final int VRAM_D_LCD = 0;
    public static final int VRAM_D_MAIN_BG  = 1 | VRAM_OFFSET(3);
    public static final int VRAM_D_MAIN_BG_0x06000000  = 1 | VRAM_OFFSET(0);
    public static final int VRAM_D_MAIN_BG_0x06020000  = 1 | VRAM_OFFSET(1);
    public static final int VRAM_D_MAIN_BG_0x06040000  = 1 | VRAM_OFFSET(2);
    public static final int VRAM_D_MAIN_BG_0x06060000  = 1 | VRAM_OFFSET(3);
    public static final int VRAM_D_ARM7 = 2 | VRAM_OFFSET(1);
    public static final int VRAM_D_ARM7_0x06000000 = 2;
    public static final int VRAM_D_ARM7_0x06020000 = 2 | VRAM_OFFSET(1);
    public static final int VRAM_D_SUB_SPRITE  = 4;
    public static final int VRAM_D_TEXTURE = 3 | VRAM_OFFSET(3);
    public static final int VRAM_D_TEXTURE_SLOT0 = 3 | VRAM_OFFSET(0);
    public static final int VRAM_D_TEXTURE_SLOT1 = 3 | VRAM_OFFSET(1);
    public static final int VRAM_D_TEXTURE_SLOT2 = 3 | VRAM_OFFSET(2);
    public static final int VRAM_D_TEXTURE_SLOT3 = 3 | VRAM_OFFSET(3);

    public static final int VRAM_E_LCD             = 0;
    public static final int VRAM_E_MAIN_BG         = 1;
    public static final int VRAM_E_MAIN_SPRITE     = 2;
    public static final int VRAM_E_TEX_PALETTE     = 3;
    public static final int VRAM_E_BG_EXT_PALETTE  = 4;
    public static final int VRAM_E_OBJ_EXT_PALETTE = 5;

    public static final int VRAM_F_LCD             = 0;
    public static final int VRAM_F_MAIN_BG         = 1;
    public static final int VRAM_F_MAIN_SPRITE     = 2;
    public static final int VRAM_F_MAIN_SPRITE_0x06000000     = 2;
    public static final int VRAM_F_MAIN_SPRITE_0x06004000     = 2 | VRAM_OFFSET(1);
    public static final int VRAM_F_MAIN_SPRITE_0x06010000     = 2 | VRAM_OFFSET(2);
    public static final int VRAM_F_MAIN_SPRITE_0x06014000     = 2 | VRAM_OFFSET(3);
    public static final int VRAM_F_TEX_PALETTE     = 3;
    public static final int VRAM_F_BG_EXT_PALETTE  = 4;
    public static final int VRAM_F_OBJ_EXT_PALETTE = 5;

    public static final int VRAM_G_LCD             = 0;
    public static final int VRAM_G_MAIN_BG         = 1;
    public static final int VRAM_G_MAIN_SPRITE     = 2;
    public static final int VRAM_G_MAIN_SPRITE_0x06000000     = 2;
    public static final int VRAM_G_MAIN_SPRITE_0x06004000     = 2 | VRAM_OFFSET(1);
    public static final int VRAM_G_MAIN_SPRITE_0x06010000     = 2 | VRAM_OFFSET(2);
    public static final int VRAM_G_MAIN_SPRITE_0x06014000     = 2 | VRAM_OFFSET(3);
    public static final int VRAM_G_TEX_PALETTE     = 3;
    public static final int VRAM_G_BG_EXT_PALETTE  = 4;
    public static final int VRAM_G_OBJ_EXT_PALETTE = 5;

    public static final int VRAM_H_LCD                = 0;
    public static final int VRAM_H_SUB_BG             = 1;
    public static final int VRAM_H_SUB_BG_EXT_PALETTE = 2;

    public static final int VRAM_I_LCD                    = 0;
    public static final int VRAM_I_SUB_BG                 = 1;
    public static final int VRAM_I_SUB_SPRITE             = 2;
    public static final int VRAM_I_SUB_SPRITE_EXT_PALETTE = 3;


    public static final native int vramSetMainBanks(int a, int b, int c, int d);
    public static final native void vramRestoreMainBanks(int vramTemp);

    public static final native void vramSetBankA(int a);
    public static final native void vramSetBankB(int b);
    public static final native void vramSetBankC(int c);
    public static final native void vramSetBankD(int d);
    public static final native void vramSetBankE(int e);
    public static final native void vramSetBankF(int f);
    public static final native void vramSetBankG(int g);
    public static final native void vramSetBankH(int h);
    public static final native void vramSetBankI(int i);


// Display control registers
    public static final int DISPLAY_CR = (0x04000000);
    public static final int SUB_DISPLAY_CR = (0x04001000);

    public static final int MODE_0_2D = 0x10000;
    public static final int MODE_1_2D = 0x10001;
    public static final int MODE_2_2D = 0x10002;
    public static final int MODE_3_2D = 0x10003;
    public static final int MODE_4_2D = 0x10004;
    public static final int MODE_5_2D = 0x10005;

// main display only
    public static final int MODE_6_2D = 0x10006;
    public static final int MODE_FIFO = (3<<16);

    public static final int ENABLE_3D = (1<<3);

    public static final int DISPLAY_BG0_ACTIVE = (1 << 8);
    public static final int DISPLAY_BG1_ACTIVE = (1 << 9);
    public static final int DISPLAY_BG2_ACTIVE = (1 << 10);
    public static final int DISPLAY_BG3_ACTIVE = (1 << 11);
    public static final int DISPLAY_SPR_ACTIVE = (1 << 12);
    public static final int DISPLAY_WIN0_ON = (1 << 13);
    public static final int DISPLAY_WIN1_ON = (1 << 14);
    public static final int DISPLAY_SPR_WIN_ON = (1 << 15);


// Main display only
    public static final int MODE_0_3D = (MODE_0_2D | DISPLAY_BG0_ACTIVE | ENABLE_3D);
    public static final int MODE_1_3D = (MODE_1_2D | DISPLAY_BG0_ACTIVE | ENABLE_3D);
    public static final int MODE_2_3D = (MODE_2_2D | DISPLAY_BG0_ACTIVE | ENABLE_3D);
    public static final int MODE_3_3D = (MODE_3_2D | DISPLAY_BG0_ACTIVE | ENABLE_3D);
    public static final int MODE_4_3D = (MODE_4_2D | DISPLAY_BG0_ACTIVE | ENABLE_3D);
    public static final int MODE_5_3D = (MODE_5_2D | DISPLAY_BG0_ACTIVE | ENABLE_3D);
    public static final int MODE_6_3D = (MODE_6_2D | DISPLAY_BG0_ACTIVE | ENABLE_3D);

    public static final int MODE_FB0 = (0x00020000);
    public static final int MODE_FB1 = (0x00060000);
    public static final int MODE_FB2 = (0x000A0000);
    public static final int MODE_FB3 = (0x000E0000);

    public static final int DISPLAY_SPR_HBLANK = (1 << 23);

    public static final int DISPLAY_SPR_1D_LAYOUT = (1 << 4);

    public static final int DISPLAY_SPR_1D = (1 << 4);
    public static final int DISPLAY_SPR_2D = (0 << 4);
    public static final int DISPLAY_SPR_1D_BMP = (4 << 4);
    public static final int DISPLAY_SPR_2D_BMP_128 = (0 << 4);
    public static final int DISPLAY_SPR_2D_BMP_256 = (2 << 4);


    public static final int DISPLAY_SPR_1D_SIZE_32 = (0 << 20);
    public static final int DISPLAY_SPR_1D_SIZE_64 = (1 << 20);
    public static final int DISPLAY_SPR_1D_SIZE_128 = (2 << 20);
    public static final int DISPLAY_SPR_1D_SIZE_256 = (3 << 20);
    public static final int DISPLAY_SPR_1D_BMP_SIZE_128 = (0 << 22);
    public static final int DISPLAY_SPR_1D_BMP_SIZE_256 = (1 << 22);


    public static final int DISPLAY_SPR_EXT_PALETTE = (1 << 31);
    public static final int DISPLAY_BG_EXT_PALETTE = (1 << 30);

    public static final int DISPLAY_SCREEN_OFF = (1 << 7);

// The next two defines only apply to MAIN 2d engine
// In tile modes, this is multiplied by 64KB and added to BG_TILE_BASE
// In all bitmap modes, it is not used.
    public static final int DISPLAY_CHAR_BASE(int n) { return (((n)&7)<<24); }

// In tile modes, this is multiplied by 64KB and added to BG_MAP_BASE
// In bitmap modes, this is multiplied by 64KB and added to BG_BMP_BASE
// In large bitmap modes, this is not used
    public static final int DISPLAY_SCREEN_BASE(int n) { return (((n)&7)<<27); }

    public static void setMode(int mode) {
        Memory.W32(DISPLAY_CR, mode);
    }

    public static void setModeSub(int mode) {
        Memory.W32(SUB_DISPLAY_CR, mode);
    }

    public static final int BRIGHTNESS = (0x0400006C);
    public static final int SUB_BRIGHTNESS = (0x0400106C);

    public static final int BGCTRL = (0x04000008);
    public static final int BG0_CR = (0x04000008);
    public static final int BG1_CR = (0x0400000A);
    public static final int BG2_CR = (0x0400000C);
    public static final int BG3_CR = (0x0400000E);

    public static final int BGCTRL_SUB = (0x04001008);
    public static final int SUB_BG0_CR = (0x04001008);
    public static final int SUB_BG1_CR = (0x0400100A);
    public static final int SUB_BG2_CR = (0x0400100C);
    public static final int SUB_BG3_CR = (0x0400100E);

    public static final int BG_256_COLOR = ((1<<7));
    public static final int BG_16_COLOR = (0);

    public static final int BG_MOSAIC_ON = ((1<<6));
    public static final int BG_MOSAIC_OFF = (0);

    public static final int BG_PRIORITY(char n) {return (n);}
    public static final int BG_PRIORITY_0 = (0);
    public static final int BG_PRIORITY_1 = (1);
    public static final int BG_PRIORITY_2 = (2);
    public static final int BG_PRIORITY_3 = (3);

    public static final int BG_TILE_BASE(int base) { return ((base) << 2); }
    public static final int BG_MAP_BASE(int base) { return ((base) << 8); }
    public static final int BG_BMP_BASE(int base) { return ((base) << 8); }

    public static final int BG_MAP_RAM(int base) { return (((base)*0x800) + 0x06000000); }
    public static final int BG_MAP_RAM_SUB(int base) { return (((base)*0x800) + 0x06200000); }

    public static final int BG_TILE_RAM(int base) { return (((base)*0x4000) + 0x06000000); }
    public static final int BG_TILE_RAM_SUB(int base) { return (((base)*0x4000) + 0x06200000); }

    public static final int BG_BMP_RAM(int base) { return (((base)*0x4000) + 0x06000000); }
    public static final int BG_BMP_RAM_SUB(int base) { return (((base)*0x4000) + 0x06200000); }

    public static final int BG_WRAP_OFF = (0);
    public static final int BG_WRAP_ON = (1 << 13);

    public static final int BG_32x32 = (0 << 14);
    public static final int BG_64x32 = (1 << 14);
    public static final int BG_32x64 = (2 << 14);
    public static final int BG_64x64 = (3 << 14);

    public static final int BG_RS_16x16 = (0 << 14);
    public static final int BG_RS_32x32 = (1 << 14);
    public static final int BG_RS_64x64 = (2 << 14);
    public static final int BG_RS_128x128 = (3 << 14);

    public static final int BG_BMP8_128x128 = (BG_RS_16x16 | BG_256_COLOR);
    public static final int BG_BMP8_256x256 = (BG_RS_32x32 | BG_256_COLOR);
    public static final int BG_BMP8_512x256 = (BG_RS_64x64 | BG_256_COLOR);
    public static final int BG_BMP8_512x512 = (BG_RS_128x128 | BG_256_COLOR);
    public static final int BG_BMP8_1024x512 = (1<<14);
    public static final int BG_BMP8_512x1024 = 0;

    public static final int BG_BMP16_128x128 = (BG_RS_16x16 | BG_256_COLOR | (1<<2));
    public static final int BG_BMP16_256x256 = (BG_RS_32x32 | BG_256_COLOR | (1<<2));
    public static final int BG_BMP16_512x256 = (BG_RS_64x64 | BG_256_COLOR | (1<<2));
    public static final int BG_BMP16_512x512 = (BG_RS_128x128 | BG_256_COLOR | (1<<2));

    public static final int BG_PALETTE_SLOT0 = 0;
    public static final int BG_PALETTE_SLOT1 = 0;
    public static final int BG_PALETTE_SLOT2 = (1<<13);
    public static final int BG_PALETTE_SLOT3 = (1<<13);

    public static final int BG0_X0 = (0x04000010);
    public static final int BG0_Y0 = (0x04000012);
    public static final int BG1_X0 = (0x04000014);
    public static final int BG1_Y0 = (0x04000016);
    public static final int BG2_X0 = (0x04000018);
    public static final int BG2_Y0 = (0x0400001A);
    public static final int BG3_X0 = (0x0400001C);
    public static final int BG3_Y0 = (0x0400001E);

    public static final int BG2_XDX = (0x04000020);
    public static final int BG2_XDY = (0x04000022);
    public static final int BG2_YDX = (0x04000024);
    public static final int BG2_YDY = (0x04000026);
    public static final int BG2_CX = (0x04000028);
    public static final int BG2_CY = (0x0400002C);

    public static final int BG3_XDX = (0x04000030);
    public static final int BG3_XDY = (0x04000032);
    public static final int BG3_YDX = (0x04000034);
    public static final int BG3_YDY = (0x04000036);
    public static final int BG3_CX = (0x04000038);
    public static final int BG3_CY = (0x0400003C);

    public static final int SUB_BG0_X0 = (0x04001010);
    public static final int SUB_BG0_Y0 = (0x04001012);
    public static final int SUB_BG1_X0 = (0x04001014);
    public static final int SUB_BG1_Y0 = (0x04001016);
    public static final int SUB_BG2_X0 = (0x04001018);
    public static final int SUB_BG2_Y0 = (0x0400101A);
    public static final int SUB_BG3_X0 = (0x0400101C);
    public static final int SUB_BG3_Y0 = (0x0400101E);

    public static final int SUB_BG2_XDX = (0x04001020);
    public static final int SUB_BG2_XDY = (0x04001022);
    public static final int SUB_BG2_YDX = (0x04001024);
    public static final int SUB_BG2_YDY = (0x04001026);
    public static final int SUB_BG2_CX = (0x04001028);
    public static final int SUB_BG2_CY = (0x0400102C);

    public static final int SUB_BG3_XDX = (0x04001030);
    public static final int SUB_BG3_XDY = (0x04001032);
    public static final int SUB_BG3_YDX = (0x04001034);
    public static final int SUB_BG3_YDY = (0x04001036);
    public static final int SUB_BG3_CX = (0x04001038);
    public static final int SUB_BG3_CY = (0x0400103C);

// Window 0
    public static final int WIN0_X0 = (0x04000041);
    public static final int WIN0_X1 = (0x04000040);
    public static final int WIN0_Y0 = (0x04000045);
    public static final int WIN0_Y1 = (0x04000044);

// Window 1
    public static final int WIN1_X0 = (0x04000042);
    public static final int WIN1_X1 = (0x04000043);
    public static final int WIN1_Y0 = (0x04000047);
    public static final int WIN1_Y1 = (0x04000046);

    public static final int WIN_IN = (0x04000048);
    public static final int WIN_OUT = (0x0400004A);

// Window 0
    public static final int SUB_WIN0_X0 = (0x04001041);
    public static final int SUB_WIN0_X1 = (0x04001040);
    public static final int SUB_WIN0_Y0 = (0x04001045);
    public static final int SUB_WIN0_Y1 = (0x04001044);

// Window 1
    public static final int SUB_WIN1_X0 = (0x04001042);
    public static final int SUB_WIN1_X1 = (0x04001043);
    public static final int SUB_WIN1_Y0 = (0x04001047);
    public static final int SUB_WIN1_Y1 = (0x04001046);

    public static final int SUB_WIN_IN = (0x04001048);
    public static final int SUB_WIN_OUT = (0x0400104A);

    public static final int MOSAIC_CR = (0x0400004C);
    public static final int SUB_MOSAIC_CR = (0x0400104C);

    public static final int BLEND_CR = (0x04000050);
    public static final int BLEND_AB = (0x04000052);
    public static final int BLEND_Y = (0x04000054);

    public static final int SUB_BLEND_CR = (0x04001050);
    public static final int SUB_BLEND_AB = (0x04001052);
    public static final int SUB_BLEND_Y = (0x04001054);

    public static final int BLEND_NONE = (0<<6);
    public static final int BLEND_ALPHA = (1<<6);
    public static final int BLEND_FADE_WHITE = (2<<6);
    public static final int BLEND_FADE_BLACK = (3<<6);

    public static final int BLEND_SRC_BG0 = (1<<0);
    public static final int BLEND_SRC_BG1 = (1<<1);
    public static final int BLEND_SRC_BG2 = (1<<2);
    public static final int BLEND_SRC_BG3 = (1<<3);
    public static final int BLEND_SRC_SPRITE = (1<<4);
    public static final int BLEND_SRC_BACKDROP = (1<<5);

    public static final int BLEND_DST_BG0 = (1<<8);
    public static final int BLEND_DST_BG1 = (1<<9);
    public static final int BLEND_DST_BG2 = (1<<10);
    public static final int BLEND_DST_BG3 = (1<<11);
    public static final int BLEND_DST_SPRITE = (1<<12);
    public static final int BLEND_DST_BACKDROP = (1<<13);

// Background control defines

// BGxCNT defines ///
    public static final int BG_MOSAIC_ENABLE = 0x40;
    public static final int BG_COLOR_256 = 0x80;
    public static final int BG_COLOR_16 = 0x0;

    public static final int CHAR_BASE_BLOCK(int n) { return (((n)*0x4000)+0x6000000); }
    public static final int CHAR_BASE_BLOCK_SUB(int n) { return (((n)*0x4000)+0x6200000); }
    public static final int SCREEN_BASE_BLOCK(int n) { return (((n)*0x800)+0x6000000); }
    public static final int SCREEN_BASE_BLOCK_SUB(int n) { return (((n)*0x800)+0x6200000); }

    public static final int CHAR_SHIFT = 2;
    public static final int SCREEN_SHIFT = 8;
    public static final int TEXTBG_SIZE_256x256 = 0x0;
    public static final int TEXTBG_SIZE_256x512 = 0x8000;
    public static final int TEXTBG_SIZE_512x256 = 0x4000;
    public static final int TEXTBG_SIZE_512x512 = 0xC000;

    public static final int ROTBG_SIZE_128x128 = 0x0;
    public static final int ROTBG_SIZE_256x256 = 0x4000;
    public static final int ROTBG_SIZE_512x512 = 0x8000;
    public static final int ROTBG_SIZE_1024x1024 = 0xC000;

    public static final int WRAPAROUND = 0x1;

}

