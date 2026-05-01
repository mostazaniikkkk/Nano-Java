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

import java.util.StringTokenizer;

import nds.pstros.MainApp;
import nds.pstros.video.NDSFont;
import nds.pstros.video.NDSGraphics;


public class Item {
	private static char[] SPACE_CHAR = {' '};
	private static char[] NEWLINE_CHAR = {'\n'};
	
	
	public static final int LAYOUT_DEFAULT = 0;
	public static final int LAYOUT_LEFT = 1;
	public static final int LAYOUT_RIGHT = 2;
	public static final int LAYOUT_CENTER = 3;
	
	public static final int LAYOUT_TOP = 0x10;
	public static final int LAYOUT_BOTTOM = 0x20;
	public static final int LAYOUT_VCENTER = 0x30;
	
	public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
	public static final int LAYOUT_NEWLINE_AFTER = 0x200;
	public static final int LAYOUT_SHRINK = 0x400;
	public static final int LAYOUT_EXPAND = 0x800;
	public static final int LAYOUT_VSHRINK	= 0x1000;
	public static final int LAYOUT_VEXPAND = 0x2000;
	public static final int LAYOUT_2 = 0x4000;
	
	public static final int PLAIN = 0;
	public static final int HYPERLINK = 1;
	public static final int BUTTON = 2;
	
	static final int  COLOR_HIGHLIGT  = 0xFFDCDCDC; 
	static final int  COLOR_RED  = 0xFFFF0000; 
	static final int  COLOR_BLACK  = 0xFF000000; 
	
	protected String label;
	protected int layout;
	protected Command command;
	protected ItemCommandListener listener;
	
	private Displayable emuDisplayable;
	boolean emuInteractive;		//true if item responds to user keypress
	boolean emuActive;
	boolean emuMultiElement;		//true if item has more elements
	
	void emuSetDisplayable(Displayable d) {
		emuDisplayable = d;
	}
	Displayable emuGetDisplayable() {
		return emuDisplayable;
	}
	protected void emuUpdateScreen() {
		if (emuDisplayable != null) {
			emuDisplayable.emuUpdateScreen();
		}
	}
	boolean emuIsInteractive() {
		return emuInteractive;
	}
	
	boolean emuIsMultiElement() {
		return emuMultiElement;
	}
	void emuSetActive(boolean state) {
		emuActive = state;
		if (command != null && listener != null && state) {
			command.owner = this ;
			//System.out.println("owner=" + command.owner);
		}
	}
	
	void emuKeyAction(int key, int keyChar,  int action) {
	}
		
	public void setLabel(String l){
		label = l;
	}
	
	public String getLabel() {
		return label;
	}
	
	public int getLayout() {
		return layout;
	}
	
	public void setLayout(int l) throws Exception {
		layout = l;		
	}
	
	
	/**
	 * draw Item
	 * @param g - grapgics
	 * @param x - x position
	 * @param y - y position
	 * @return height of the item
	 */
	
	int emuPaint(NDSGraphics g, int x, int y) {
		if (MainApp.verbose) {
			System.out.println("Item.emuPaint()  not implemented for item=" + this);
		}
		return 0;
	}
	
	int emuGetHeight(NDSGraphics g) {
		if (MainApp.verbose) {
			System.out.println("Item.emuGetHeight()  not implemented for item=" + this);
		}
		return 0;
	}
	int emuGetElementHeight(NDSGraphics g) {
		//if this item is not multi element use full height as the element height
		if (!emuMultiElement) {
			return emuGetHeight(g);
		}
		if (MainApp.verbose) {
			System.out.println("Item.emuGetHeight()  not implemented for item=" + this);
		}
		return 0;
	}
	
	int emuGetYSpace(NDSGraphics g) {
		return 0;
	}
	
	void emuActionPressed() {
	}
	
	boolean emuMoveSelectionDown() {
		return true;
	}
	boolean emuMoveSelectionUp() {
		return true;
	}
	
	public void addCommand(Command cmd) {
		command = cmd;
		if (command != null) {
			command.owner = this;
		}
	}
	public void removeCommand(Command cmd) {
		if (command == cmd) {
			command = null;
		}
	}
	public void setDefaultCommand(Command cmd) {
		command = cmd;
	}
	
	public void setItemCommandListener(ItemCommandListener l) {
		listener = l;
	}
	ItemCommandListener emuGetItemCommandListener() {
		return listener;
	}
	
	
	private static int[] emuResult = new int[3];
	static int[] emuDrawMultiLine(NDSGraphics g, String text, int x, int y, boolean draw) {
		emuResult[0] = 0;
		emuResult[1] = 0;
		emuResult[2] = 0;
		if (text == null) {
			return emuResult;
		}
		int origX = x;
		int origY = y;
		NDSFont font = g.getFont();
		int spaceWidth = font.getStringWidth(SPACE_CHAR,0,1);
		int fontHeight = font.getHeight();
		//y += fontHeight+2;

			
		StringTokenizer tokenizer = new StringTokenizer(text,"\n");
		int size = tokenizer.countTokens();
		//go through all lines of the text
		for (int j = 0; j < size; j++) {
			StringTokenizer lineTokenizer = new StringTokenizer(tokenizer.nextToken()," ");
			int lineSize = lineTokenizer.countTokens();
			int wordLength;
			String word;
			//go through all words of the line
			for (int i = 0; i < lineSize; i++) {
				word = lineTokenizer.nextToken();
				wordLength = font.getStringWidth(word.toCharArray(), 0, word.length()); 
				x += wordLength;
				if (x > Display.WIDTH) {
					x = 0;
					y += fontHeight+ 2; 
				}else {
					x-=wordLength; 
				}
				if (draw) {
					g.drawString(word, x,y);
				}
				x+= wordLength;
				x+= spaceWidth;
			}
			//new line
			if (j < size-1) {
				x = 0;
				y+= fontHeight + 2;
			}
			
		}
		emuResult[0] = x - origX;
		emuResult[1] = y - origY; 
		emuResult[2] = fontHeight + 2;
		return emuResult;
	}
	
	Command emuGetDefaultCommand() {
		return command;
	}

}
