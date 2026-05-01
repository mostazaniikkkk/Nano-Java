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

import nds.pstros.ConfigData;
import nds.pstros.video.NDSGraphics;


public abstract class CustomItem extends Item {
	
	protected static final int TRAVERSE_HORIZONTAL = 1;
	protected static final int TRAVERSE_VERTICAL = 2;
	protected static final int KEY_PRESS = 4;
	protected static final int KEY_RELEASE = 8;
	protected static final int KEY_REPEAT = 0x10;
	protected static final int POINTER_PRESS = 0x20;
	protected static final int POINTER_RELEASE = 0x40;
	protected static final int POINTER_DRAG = 0x80;
	protected static final int NONE = 0;
	
	protected CustomItem(String label) {
		this.label = label;
	}
	
	public int getGameAction(int keyCode) {
		if (keyCode == ConfigData.keyUpArrow) {
			return Canvas.UP;
		} else 
		if (keyCode == ConfigData.keyDownArrow) {
			return Canvas.DOWN;
		} else
		if (keyCode == ConfigData.keyLeftArrow) {
			return Canvas.LEFT;
		} else
		if (keyCode == ConfigData.keyRightArrow) {
			return Canvas.RIGHT;
		} else
		if (keyCode == ConfigData.keyCenterSoft) {
			return Canvas.FIRE;
		}
		return 0;
	}
	
	protected final int getInteractionModes() {
		//TODO - implement
		return 0;
	}
	protected abstract int getMinContentWidth();
	protected abstract int getMinContentHeight();
	protected abstract int getPrefContentWidth(int height);
	protected abstract int getPrefContentHeight(int width);
	
	protected void sizeChanged(int w, int h) {};
	protected final void invalidate() {};
	protected abstract void paint(Graphics g, int w, int h);
	
	protected final void repaint() {
		//emuUpdateScreen();
	};
	
	protected final void repaint(int x, int y, int w, int h) {
		repaint();
	};
	
	protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout) {
		//TODO - implement if you desire :)
		return false;
	}
	protected void traverseOut() {};
	protected void keyPressed(int keyCode) {};
	protected void keyReleased(int keyCode){};
	protected void keyRepeated(int keyCode) {};
	protected void pointerPressed(int x, int y) {};
	protected void pointerReleased(int x, int y) {};
	protected void pointerDragged(int x, int y) {};
	protected void showNotify() {};
	protected void hideNotify() {};
	
	int emuGetHeight(NDSGraphics g) {
		return getMinContentHeight();
	}
		
	
}
