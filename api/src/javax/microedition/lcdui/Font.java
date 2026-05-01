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
package javax.microedition.lcdui;

import java.util.Vector;

import nds.pstros.video.NDSFont;


public class Font {
	
	public static final int STYLE_PLAIN = 0;
	public static final int STYLE_BOLD = 1;
	public static final int STYLE_ITALIC = 2;
	public static final int STYLE_UNDERLINED = 4;
	public static final int SIZE_SMALL = 8;
	public static final int SIZE_MEDIUM = 0;
	public static final int SIZE_LARGE = 16;
	public static final int FACE_SYSTEM = 0;
	public static final int FACE_MONOSPACE = 32;
	public static final int FACE_PROPORTIONAL = 64;
	public static final int FONT_STATIC_TEXT = 0;
	public static final int FONT_INPUT_TEXT = 1;

	private static Vector fontCache = new Vector();
	
	private int face;
	private int style;
	private int size;
	private NDSFont emuFont;
	
	
	private static Font defaultFont = new Font(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
	private static char[] charBuff =  new char[1];
	
	private Font(int face, int style, int size) {
		this.face = face;
		this.style = style;
		this.size = size;
		String fontName;
		switch(face) {
			case FACE_MONOSPACE:
				fontName =  "Monospaced"; break;
			case FACE_PROPORTIONAL:
				fontName = "Serif"; break;
			default: 
				fontName = "Dialog"; break;

		}
		
		int fontSize = 12;
		switch (size) {
			case SIZE_SMALL: fontSize = 10; break;
			case SIZE_LARGE: fontSize = 14; break;
		}
		
		emuFont = new NDSFont(fontName, style, fontSize);
	}
	
	
	public static Font getDefaultFont() {
		return defaultFont;		
	}
	//Implementation supplied by Shade
	public static Font getFont(int fontSpecifier) {
		int face, style, size;		
		switch (fontSpecifier) {
			case FONT_STATIC_TEXT:
				face = FACE_PROPORTIONAL;
				style = STYLE_PLAIN;
				size = SIZE_SMALL;
				break;
			case FONT_INPUT_TEXT:
				face = FACE_MONOSPACE;
				style = STYLE_PLAIN;
				size = SIZE_SMALL;
				break;
			default:
				throw new IllegalArgumentException("Invalid font specifier: "+fontSpecifier);
		}
		
		return getFont(face, style, size);
	}
	
	
	public static Font getFont(int face, int style, int size) {
		Font font;
		//Bugfix by Shade
		int cacheSize = fontCache.size();
		for (int i = 0; i < cacheSize; i++) {
			font = (Font) fontCache.elementAt(i);
			if (font.getFace() == face && font.getStyle() == style && font.getSize() == size) {
				return font;
			}
		}
		font = new Font(face, style, size);
		fontCache.addElement(font);
		return font;
	}
	public NDSFont emuGetFont() {
		return emuFont;
	}
	
	public int getStyle() {
		return style;
	}
	
	public int getSize() {
		return size;
	}
	
	public int getFace(){
		return face;
	}
	
	public boolean isPlain() {
		return (style == STYLE_PLAIN); 
	}
	
	public boolean isBold() {
		return (style & STYLE_BOLD) != 0; 
	}
	public boolean isItalic() {
		return (style & STYLE_ITALIC) != 0; 
	}
	
	public boolean isUnderlined() {
		return (style & STYLE_UNDERLINED) != 0; 
	}
	
	public int charWidth(char ch) {
		charBuff[0] = ch;
		return emuFont.getStringWidth(charBuff, 0, 1);
		//System.out.println("charWidth=" + rect.getWidth());
	}
	public int charsWidth(char[] ch, int offset, int length) {
		return stringWidth(String.valueOf(ch, offset, length));
	}
	
	public int stringWidth(String str) {
		return emuFont.getStringWidth(str.toCharArray(), 0, str.length());
	}
	
	public int substringWidth(String str, int offset, int len) {
		return stringWidth(str.substring(offset, offset+len));
	}
	public int getHeight(){
		return emuFont.getHeight() + 2 ;
	}
	public int getBaselinePosition() {
		return emuFont.getBaseLine();
	}

}
