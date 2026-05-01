/* **************************************************************************
 This file is part of the Pstros J2ME execution environment 
 
 Copyright (C) 2005-2007, Marek Olejnik
 
 Disclaimer of Warranty
 
 These software programs are available to the user without any license fee or
 royalty on an "as is" basis.  The copyright-holder disclaims any and all 
 warranties, whether express, implied, or statuary, including any implied 
 warranties or merchantability or of fitness for a particular  purpose. In no 
 event shall the copyright-holder be liable for any incidental, punitive, 
 or consequential damages of any kind whatsoever arising from the use 
 of these programs.
 
 This disclaimer of warranty extends to the user of these programs and user's
 customers, employees, agents, transferees, successors, and assigns.

 Maintainers:
 Marek Olejnik - ole00@post.cz

***************************************************************************/
package com.nokia.mid.ui;

import javax.microedition.lcdui.Image;


public interface DirectGraphics {
	
	public static final int FLIP_HORIZONTAL = 0x2000;
	public static final int FLIP_VERTICAL = 0x4000;
	public static final int ROTATE_90 = 90;	//5A
	public static final int ROTATE_180 = 180; //B4
	public static final int ROTATE_270 = 270; //10E
	
	public static final int TYPE_BYTE_1_GRAY = 1;
	public static final int TYPE_BYTE_1_GRAY_VERTICAL = -1;
	public static final int TYPE_BYTE_2_GRAY = 2;
	public static final int TYPE_BYTE_4_GRAY = 4;
	public static final int TYPE_BYTE_8_GRAY = 8;
	public static final int TYPE_BYTE_332_RGB = 332;
	public static final int TYPE_USHORT_4444_ARGB = 4444;
	public static final int TYPE_USHORT_444_RGB = 444;
	public static final int TYPE_USHORT_555_RGB = 555;
	public static final int TYPE_USHORT_1555_ARGB = 1555;
	public static final int TYPE_USHORT_565_RGB = 565;
	public static final int TYPE_INT_888_RGB = 888;
	public static final int TYPE_INT_8888_ARGB = 8888;
	
	
	public void drawImage(Image img, int x, int y, int anchor, int manipulation);
	
	public void drawPixels(	byte[] pixels,
				byte[] transparencyMask,
				int offset,
				int scanlength,
				int x,
				int y,
				int width,
				int height,
				int manipulation,
				int format);
				
	public void drawPixels(	int[] pixels,
				boolean transparency,
				int offset,
				int scanlength,
				int x,
				int y,
				int width,
				int height,
				int manipulation,
				int format);
				
	public void drawPixels(	short[] pixels,
				boolean transparency,
				int offset,
				int scanlength,
				int x,
				int y,
				int width,
				int height,
				int manipulation,
				int format);
				
	public void drawPolygon(int[] xPoints,
				int xOffset,
				int[] yPoints,
				int yOffset,
				int nPoints,
				int argbColor);
				
	public void drawTriangle(int x1,
				int y1,
				int x2,
				int y2,
				int x3,
				int y3,
				int argbColor);
				
	public void fillPolygon(int[] xPoints,
				int xOffset,
				int[] yPoints,
				int yOffset,
				int nPoints,
				int argbColor);

	public void fillTriangle(int x1,
				int y1,
				int x2,
				int y2,
				int x3,
				int y3,
				int argbColor);
				
	public int getAlphaComponent();
	
	public int getNativePixelFormat();
	
	
	public void getPixels(	byte[] pixels,
				byte[] transparencyMask,
				int offset,
				int scanlength,
				int x,
				int y,
				int width,
				int height,
				int format);
								
	public void getPixels(int[] pixels,
				int offset,
				int scanlength,
				int x,
				int y,
				int width,
				int height,
				int format);
				
	public void getPixels(short[] pixels,
				int offset,
				int scanlength,
				int x,
				int y,
				int width,
				int height,
				int format);

	public void setARGBColor(int argbColor);				
												
}
