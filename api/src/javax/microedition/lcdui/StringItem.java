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


public class StringItem extends Item {
	private static final int[] DEFAULT_POINT = new int[3];
	private String text;
	private int mode;
	private javax.microedition.lcdui.Font emuFont;
	private int height;
	
	public StringItem(String label, String text) {
		setLabel(label);
		this.text = text;
		height = -1;
		/*
		if (text == null) {
			this.text = getLabel();
		}
		*/
		
		
		//System.out.println("String item: label=" + label + " text=" + text);
	}
	public StringItem(String label, String text, int appearanceMode)  {
		this(label, text);
		mode = appearanceMode;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
		emuUpdateScreen();
	}
	
	public int getAppearanceMode() {
		return mode;
	}
	
	public void setFont(javax.microedition.lcdui.Font font) {
		emuFont = font; 
	}
	public javax.microedition.lcdui.Font getFont() {
		return emuFont;
	}
	
	int emuPaint(NDSGraphics g, int x, int y) {
		if (text == null && label == null) {
			return 0;
		}
		String l = getLabel();
		int[] point = DEFAULT_POINT;
		int origX = x;
		int origY = y;
		x++;
		
		if (!emuActive) {
			g.setColor(0xFF000000);
		} else {
			int areaH = emuGetHeight(g);
			int fontH = g.getFont().getHeight();
			g.setColor(Item.COLOR_HIGHLIGT);
			g.fillRect(x,y - fontH, Display.WIDTH - 4, areaH);
			g.setColor(Item.COLOR_RED);
		}
		if (l != null) {
			NDSFont origFont = g.getFont();
			NDSFont font = new NDSFont(origFont.getFontName(), NDSFont.BOLD, origFont.getSize());
			g.setFont(font);
			point = Item.emuDrawMultiLine(g, l, x, y-1, true);  //NDS: y + 2
			x += point[0];
			y += point[1];
			g.setFont(origFont);
			//System.out.println(l + " sizeY=" + point[1]);
		}
		if (text != null) {
			point = Item.emuDrawMultiLine(g, text, x, y-1, true); //g.drawString(text, x, y+2);
			y += point[1];
			x += point[0];
		}
		
		if (getAppearanceMode() == BUTTON) {
			g.drawRect(origX, origY - point[2]/2-3, x - origX,  y - origY + point[2] );
		}
		return y - origY + point[2];
	}
	int emuGetHeight(NDSGraphics g) {
		if (text == null && label == null) {
			return 0;
		}
		//the height was already computed
		if (height >= 0) {
			return height;
		}
		//return font.getSize() + 4;
		int[] point = DEFAULT_POINT;
		String l = getLabel();
		int x = 0;
		int y = 0;
		if (l != null) {
			NDSFont origFont = g.getFont();
			NDSFont font = new NDSFont(origFont.getFontName(), NDSFont.BOLD, origFont.getSize());
			g.setFont(font);
			point = Item.emuDrawMultiLine(g, l, x, y + 2, false); 
			x += point[0];
			y += point[1];
			g.setFont(origFont);
		}
		if (text != null) {
			point = Item.emuDrawMultiLine(g, text, x, y+2, false);
			y += point[1];
			x += point[0];
		}
		height = y + point[2];
		return height;
		
	}
	int emuGetYSpace(NDSGraphics g) {
		if (text == null && label == null) {
			return 0;
		}
		NDSFont font = g.getFont();
		return font.getSize()+2;
		//return 2;
	}

}
