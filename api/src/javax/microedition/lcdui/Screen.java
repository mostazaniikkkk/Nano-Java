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

import javax.microedition.lcdui.game.GameCanvas;

import nds.pstros.EmuCanvas;
import nds.pstros.EmuExecutor;
import nds.pstros.video.NDSFont;
import nds.pstros.video.NDSGraphics;


public class Screen extends Displayable{

	protected Vector items;
	int selected;
	
	static NDSFont screenFont;

	public Screen() {
		items = new Vector();
		selected = 0;
		if (screenFont == null) {
			screenFont = new NDSFont("Dialog", 0, 12);
		}
	}
	
	public void emuPaint(NDSGraphics g) {
		NDSFont oldFont = g.getFont();
		g.setFont(screenFont);
		
		//clean background
		g.setColor(EmuCanvas.COLOR_LIGHT_GRAY);
		g.fillRect(0,0, Display.WIDTH, Display.HEIGHT);
		
		//draw title bar
		g.setColor(EmuCanvas.COLOR_GRAY);
		g.fillRect(0,0, Display.WIDTH, 16);
		//draw bottom console
		g.fillRect(0,Display.HEIGHT -16, Display.WIDTH, 16);
		
		//draw title string
		g.setColor(EmuCanvas.COLOR_BLACK);
		if (title != null) {
			g.drawString(title, 0, 12); 
		}
		//System.out.println("l=" + leftCommand + " r=" + rightCommand);
		//left softKey
		if (leftCommand != null) {
			g.drawString(leftCommand.getLabel(), 0, Display.HEIGHT-4);
		}
		//right soft key
		if (rightCommand != null) {
			int width = screenFont.getStringWidth(rightCommand.getLabel() );
			g.drawString(rightCommand.getLabel(), Display.WIDTH - width - 1, Display.HEIGHT-4);
		}
		
		//draw items
		g.setClip(0, 16, Display.WIDTH, Display.HEIGHT - 16 - 16);
		
		emuPaintScreenContent(g);
		
		//remove clip
		g.setClip(0, 0, Display.WIDTH, Display.HEIGHT);
		g.setFont(oldFont);

	}
	
	//override to show content on the screen
	void emuPaintScreenContent(NDSGraphics g) {
		System.out.println("Pstros: Screen: implement emuPaintContent for:" + this);
	} 

	public boolean emuKeyAction(int key, int keyChar, int modifiers, int action) {
		super.emuKeyAction(key, keyChar, modifiers, action);
		if (action == EmuExecutor.KEY_ACTION_PRESS) {
			if ((emuKeyStates & GameCanvas.UP_PRESSED) != 0) {
				moveSelectionUp();
			} else  
			if ((emuKeyStates & GameCanvas.DOWN_PRESSED) != 0) {
				moveSelectionDown();
			} else
			if ((emuKeyStates & GameCanvas.FIRE_PRESSED) != 0) {
				emuFirePressed();
			}
		}
		return true;
	}
	
	private void moveSelectionDown() {
		if (items.size() < 1) {
			return;
		}
		Object item = items.elementAt(selected);
		boolean nextItem = true;
		if (item instanceof Item) {
			if (((Item)item).emuIsInteractive()) {
				nextItem = ((Item)item).emuMoveSelectionDown();
			}
		}		
		//fixme - assume there is no sequential placement of spacers   
		if (nextItem) {		
			int max = items.size();
			if (selected < max-1) {
				selected ++;
				item = items.elementAt(selected);
				if (item instanceof Spacer) {
					if (selected < max-1) {
						selected++;
					} else {
						selected --; //the spacer is the last item
					}
				}
				
				emuUpdateScreen();
			}
		}
	}
	private void moveSelectionUp() {
		if (items.size() < 1) {
			return;
		}
		Object item = items.elementAt(selected);
		boolean previousItem = true;
		boolean nextIsSpacer = false;
		if (item instanceof Item) {
			if (((Item)item).emuIsInteractive()) {
				previousItem = ((Item)item).emuMoveSelectionUp();
			}
			nextIsSpacer = item instanceof Spacer;
		}
		//fixme - assume there is no sequential placement of spacers   
		if (previousItem) {		
			if (selected > 0) {
				selected --;
				item = items.elementAt(selected);
				
				if (item instanceof Spacer) {
					if (selected > 0) {
						selected --;
					} else {
						selected ++;
					}
				}
				emuUpdateScreen();
			}
		}
	}
	
	void emuFirePressed() {
		int max = items.size();
		if (max < 1 || selected < 0 || selected > max-1) {
			return;
		}
		Object item = items.elementAt(selected);
		if (item instanceof Item) {
			if (((Item)item).emuIsInteractive()) {
				((Item)item).emuActionPressed();
			}
		}
	}
	
}
