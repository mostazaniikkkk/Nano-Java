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

import nds.pstros.video.NDSFont;
import nds.pstros.video.NDSGraphics;


public class TextField extends Item {
	
	public static final int ANY = 0;
	public static final int EMAILADDR = 1;
	public static final int NUMERIC = 2;
	public static final int PHONENUMBER = 3;
	public static final int URL = 4;
	
	public static final int DECIMAL = 5;
	public static final int PASSWORD = 0x10000;
	public static final int UNEDITABLE = 0x20000;
	public static final int SENSITIVE = 0x40000;
	public static final int NON_PREDICTIVE =  0x80000;
	public static final int INITIAL_CAPS_WORD = 0x100000;
	public static final int INITIAL_CAPS_SENTENCE = 0x200000;
	public static final int CONSTRAINT_MASK =  0xFFFF;
	
	
	protected String text;
	protected int constraints;
	protected int maxSize;
	
	public TextField(String label, String text, int maxSize, int constraints) {
		super();
		setLabel(label);
		this.text = text;
		this.maxSize = maxSize;
		this.constraints = constraints;
		
		if (maxSize < 1) {
			throw new IllegalArgumentException(" maxSize < 1: maxSize=" + maxSize);
		}
		emuInteractive = true;
	}
	
	public String getString() {
		return text;
	}
	
	public void setString(String text) {
		this.text = text;
	}
	
	public int getChars(char[] data) {
		int size = data.length;
		char[] src = text.toCharArray();
		if (src.length < size) {
			size = src.length;
		}
		System.arraycopy(src, 0, data, 0, size);
		return size;
		
	}
	public void setChars(char[] data, int offset, int length) {
		text = new String(data, offset, length);
	}
	public void insert(String src, int position) {
	}
	
	public void insert(char[] data, int offset, int length, int position) {
	}
	
	public void delete(int offset, int length) {
	}
	
	public int getMaxSize() {
		return maxSize;			
	}
	public int setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		return maxSize;
	}
	
	public int size() {
		return text.length();
	}
	
	public int getCaretPosition() {
		return 0;
	}
	
	public void setConstraints(int constraints) {
		this.constraints = constraints;
	}
	public int getConstraints() {
		return constraints;
	}
	
	public void setInitialInputMode(String characterSubset) {
		
	}
	
	void emuKeyAction(int key, int keyChar, int action) {
		//System.out.println(" char=" + Character.getNumericValue(keyChar));
		if (Character.digit((char)keyChar,0) != -1) {
			if (text == null) {
				text = "";
			}
			text += (char)keyChar;
			emuUpdateScreen();
		} else 
		if (key == 8 && text.length()> 0) {
			text = text.substring(0, text.length()-1);
		} else {
			//filter specific chars
			if (keyChar == 65535 || keyChar == 127) {
				return;
			}
			if (text == null) {
				text = "";
			}
			text += (char)keyChar;
			emuUpdateScreen();
			//System.out.println("pstros: unknown char! " + keyChar + " " + (char)keyChar);
		}
	}
	int emuPaint(NDSGraphics g, int x, int y) {
		NDSFont font = g.getFont();
		int height = font.getSize() + 2;
		int width = Display.WIDTH - x - 2;
		int origY = y;
		//int shiftY = emuGetYSpace(g);
		int paintColor = COLOR_BLACK;
		if (emuActive) {
			paintColor = COLOR_RED;
		}
		
		g.setColor(paintColor);
		if (label != null) {
			g.drawString(label, x+2, y+1);
			y += height + 2;
		}

		if (emuActive) {
			g.setColor(COLOR_HIGHLIGT);
			g.fillRect(x,y-height+2, Display.WIDTH - 4, height + 2);
			g.setColor(paintColor);
		}
		if (text != null) {
			g.drawString(text, x+2, y+1);
		}
		g.drawRect(x,y-height+2, Display.WIDTH - 4, height + 2);
		y += height + 2;
		y += 4;

		return y - origY;
	}
	int emuGetHeight(NDSGraphics g) {
		NDSFont font = g.getFont();
		int height = font.getSize();
		int h = height + 4;
		
		height = h;
		if (label != null && label.length() > 0) {
			height += h;
		}
		return height + 4;
	}
	int emuGetYSpace(NDSGraphics g) {
		NDSFont font = g.getFont();
		return font.getSize() + 3;
		//return 3;
	}

}
