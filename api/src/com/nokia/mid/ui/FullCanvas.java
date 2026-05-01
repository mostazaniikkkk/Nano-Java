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

import javax.microedition.lcdui.Canvas;

import nds.pstros.ConfigData;
import nds.pstros.MainApp;


public abstract class FullCanvas extends Canvas {
	
	public static final int KEY_UP_ARROW = -1;
	public static final int KEY_DOWN_ARROW = -2;
	public static final int KEY_LEFT_ARROW = -3;
	public static final int KEY_RIGHT_ARROW = -4;
	public static final int KEY_SOFTKEY3 = -5;
	public static final int KEY_SOFTKEY1 = -6;
	public static final int KEY_SOFTKEY2 = -7;
	public static final int KEY_SEND = -10;
	public static final int KEY_END = -11;
	
	protected FullCanvas() {
		super();
		setFullScreenMode(true);
	}
	
	public void addCommand(javax.microedition.lcdui.Command cmd) throws RuntimeException {
		throw new RuntimeException("Commands are not possible in FullCanvas.");
	}
	
	public void setCommandListener(javax.microedition.lcdui.CommandListener l) throws RuntimeException{
		throw new RuntimeException("Commands are not possible in FullCanvas.");
	}
	
	public int getGameAction(int keyCode) {
		if (MainApp.verbose) {
			System.out.println("FullCanvas.getGameAction keyCode=" + keyCode);
		}
		if (keyCode == ConfigData.keyUpArrow) {
			return UP;
		} else 
		if (keyCode == ConfigData.keyDownArrow) {
			return DOWN;
		} else
		if (keyCode == ConfigData.keyLeftArrow) {
			return LEFT;
		} else
		if (keyCode == ConfigData.keyRightArrow) {
			return RIGHT;
		} else
		if (keyCode == ConfigData.keyCenterSoft) {
			return FIRE;
		}
		return 0;
	}

}
