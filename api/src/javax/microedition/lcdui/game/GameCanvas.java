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
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import nds.pstros.EmuCanvas;
import nds.pstros.MainApp;


public abstract class GameCanvas extends Canvas {
	
	public static final int UP_PRESSED = (1 << Canvas.UP); //0x0002;
	public static final int DOWN_PRESSED = (1 << Canvas.DOWN); //0x0040;
	public static final int LEFT_PRESSED = (1 << Canvas.LEFT); //0x0004;
	public static final int RIGHT_PRESSED = (1 << Canvas.RIGHT); //0x0020;
	public static final int FIRE_PRESSED = (1 << Canvas.FIRE); //0x0100;
	public static final int GAME_A_PRESSED = (1 << Canvas.GAME_A); // 0x0200;
	public static final int GAME_B_PRESSED = (1 << Canvas.GAME_B); // 0x0400;
	public static final int GAME_C_PRESSED = (1 << Canvas.GAME_C); //0x0800;
	public static final int GAME_D_PRESSED = (1 << Canvas.GAME_D); //0x1000;
	public static final int SOFT_L_PRESSED = (1 << Canvas.SOFT_L); 
	public static final int SOFT_R_PRESSED = (1 << Canvas.SOFT_R); 
	public static final int SOFT_C_PRESSED = (1 << Canvas.SOFT_C); 
	
	
	private boolean suppresKeyEvents;
	private Graphics g;

	protected GameCanvas(boolean suppressKeyEvents) {
		this.suppresKeyEvents = suppressKeyEvents;
	}
	protected Graphics getGraphics() {
		/*
		if (emuCanvas != null) {
			return emuCanvas.getDeviceGraphics();
		} 
		
		emuSetEmuCanvas(EmuCanvas.getInstance());
		if (emuCanvas != null) {
			return emuCanvas.getDeviceGraphics();
		} 
		System.out.println("Pstros: GameCanvas.getGraphics() - emuCanvas is null!");
		return null;
		*/
		
		if (g == null) {
			EmuCanvas ec = EmuCanvas.getInstance();
			if (ec != null) {
				g = ec.createGraphics();
			}
		}
		return g;
	}
	
	public int getKeyStates() {
		//System.out.println("getKeyStates=" + emuKeyStates);
		return emuKeyStates;

	}

	public void flushGraphics(int x, int y, int width, int height) {
		if (MainApp.verbose) {
			System.out.println("GameCanvas: flushGraphics x=" + x + " y=" + y + " w=" + width + " h=" + height);
		}
		flushGraphics();
	}
	
	public void flushGraphics() {
		EmuCanvas ec = EmuCanvas.getInstance();
		if (ec != null) {
			ec.flushGraphics(g.emuGetGraphicsImage());
			ec.checkPause();
		}
	}
	
	public void paint(Graphics g) {
		
	}
	
}
