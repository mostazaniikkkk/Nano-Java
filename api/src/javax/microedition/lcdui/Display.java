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

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.midlet.MIDlet;

import nds.Video;
import nds.pstros.EmuCanvas;
import nds.pstros.MainApp;
import nds.pstros.utils.AlertTimerTask;


public class Display {
	
	public static final int COLOR_BACKGROUND = 0;
	public static final int COLOR_FOREGROUND = 1;
	public static final int COLOR_HIGHLIGHTED_BACKGROUND = 2;
	public static final int COLOR_HIGHLIGHTED_FOREGROUND = 3;
	public static final int COLOR_BORDER = 4;
	public static final int COLOR_HIGHLIGHTED_BORDER = 5;	
	
	public static int keySoftLeft  = 'Z';
	public static int keySoftLeft2  = 112; //F1
	public static int keySoftRight  = 'C';
	public static int keySoftRight2  = 113; //F2
	public static int keySoftCenter  = 'X';
	public static int keyLeft  = 37;
	public static int keyUp  = 38;
	public static int keyRight  = 39;
	public static int keyDown  = 40; 
	public static int keyFire  = 17;		//Ctrl
	public static int keyFire2  = 10;	//Enter
	
	public static int keyNum0 = 96; //0
	public static int keyNum7 = 97; //1
	public static int keyNum8 = 98; //2
	public static int keyNum9 = 99; //3
	public static int keyNum4 = 100; //4
	public static int keyNum5 = 101; //5
	public static int keyNum6 = 102; //6
	public static int keyNum1 = 103; //7
	public static int keyNum2 = 104; //8
	public static int keyNum3 = 105; //9
	public static int keyStar = 106;  // numeric * 
	public static int keyCross = 111; // numeric # - mapped as numeric /
	
	public static int keyPause = 116;	//F5
	public static int keyVirtual1 = 117;	//F6
	public static int keyVirtual2 = 118;	//F7
	public static int keyVirtual3 = 119;	//F8
	
	public static int keyScreenShot = 122;	//F11
	public static int keyCaptureVideo = 123; //F12
	public static int keyShowHideNotify = 120; //F9
	
	public static int WIDTH = 176;
	public static int HEIGHT = 192;
	
//	public static int WIDTH = 208;
//	public static int HEIGHT = 208;

	
	private static final String titleName = "Nano Java";
	
	private static Hashtable displays = new Hashtable();
	
	private static Displayable displayable;
	//private static Frame emuFrame;
	private static EmuCanvas emuCanvas;
	private static Runnable emuSerialRunner;
	
	private static boolean emulationStarted = false;
	
	public static void emuDestroyFrame() {
		emuCanvas.quit();
	}
	
	void emuBackupFrame() {
		emuCanvas.backupImage();
	}
	void emuRestoreFrame() {
		emuCanvas.restoreImage();
	}

	public static synchronized boolean emuRunSerialRunner() {
		if (emuSerialRunner != null) {
			if (MainApp.verbose) {
				System.out.println("EmuCanvas: calling the runner=" + emuSerialRunner);
			}
			Runnable tmp = emuSerialRunner;
			emuSerialRunner = null;
			tmp.run();
			return true;
		}
		/*
		 else {
			if (MainApp.verbose) {
				System.out.println("EmuCanvas: calling the runner= null");
			}
		} 
		*/
		return false;
	}
	
	public static Display getDisplay(MIDlet m) {
		Object d = displays.get(m);
		if (d == null) {
			d = new Display();
			displays.put(m, d);
		}
		if (MainApp.verbose) {
			System.out.println("EmuCanvas: getDisplay=" + d);
		}
		
		return (Display) d;
	}
	
	public void emuRepaintDisplay() {
		if (emuCanvas != null) {
			emuCanvas.setEmuPaintRequest(0,0, Display.WIDTH, Display.HEIGHT);
			emuCanvas.repaint();
		}		
	}


	private Display() {
	}
	
	public boolean isColor() {
		return true;
	}
	
	public int numColors() {
		return 0x65536;
	}
	
	public int numAlphaLevels() {
		return 256;
	}
	
	public void setCurrent(Alert alert, Displayable nextDisplayable) {
		if (alert == null || nextDisplayable == null) {
			throw new NullPointerException();
		}
		if (alert == nextDisplayable) {
			throw new IllegalArgumentException();
		}
		if (MainApp.verbose) {
			System.out.println("Display: setCurrent Alert! nextDisplayable=" + nextDisplayable);
		}
		alert.emuSetNextDisplayable(nextDisplayable);
		setCurrent(alert);
	}
	public void setCurrent(Displayable nextDisplayable) {
		if (MainApp.verbose) {
			if (nextDisplayable != null) { 
				System.out.println("Display: setCurrent displayable=" + nextDisplayable.toString() + " old displayable=" + displayable);
			} else {
				System.out.println("Display: setCurrent displayable=null");
			}
		}
		
		//set shown flag for curent displayable
		if (displayable != null) {
			displayable.shown = false;
		}
		
		if (nextDisplayable == null) {
			return; 
		}
		//Alert fixup 
		if (nextDisplayable instanceof Alert) {
			Alert alert = (Alert) nextDisplayable;
			if (alert.getTimeout() > 0) { 
				Displayable displayableAfterAlert = displayable;
				//alert has a next displayable already set -> use it
				if (alert.emuGetNextDisplayable() != null) {
					displayableAfterAlert =  alert.emuGetNextDisplayable();
				} 
				TimerTask tt = new AlertTimerTask (this, displayableAfterAlert);
				Timer alertTimer = new Timer();
				alertTimer.schedule(tt, alert.getTimeout());
			} else {
				if (alert.emuGetNextDisplayable() == null && displayable != null) {
					//create listener for dismiss command
					alert.emuSetNextDisplayable(displayable);
				}
			}
		}
		
		
		displayable = nextDisplayable;
		//set shown flag for nextDisplayable
		if (displayable != null) {
			displayable.shown = true;
		} 
		
		emuRunEmulation();
		emuCanvas.setContent(displayable);
	}
	public Displayable getCurrent() {
		return displayable;
	}
	
	public void setCurrentItem(Item item) {
		Displayable d = item.emuGetDisplayable();
		if (d instanceof Alert) {
			throw new RuntimeException("item owned by Alert");
		} 
		if (d == null) {
			throw new RuntimeException("item not owned by a container");
		}
		if (d != getCurrent()) {
			setCurrent(d);
		}
	}
	
	public boolean vibrate(int duration) {
		if (duration < 0) {
			throw new IllegalArgumentException("Duration is negative : value=" + duration);
		}
		return true;
	}
	
	public synchronized void  callSerially(Runnable r) {
		if (MainApp.verbose){
			System.out.println("Nano Java: Display.callSerially r=" + r);
		}
		emuSerialRunner = r;
	}
	
	public boolean flashBacklight(int duration) {
		if (MainApp.verbose){
			System.out.println("Nano Java: flashBacklight duration=" + duration);
		}
		return emulationStarted;
	}
	
	public int getBestImageWidth(int imageType) {
		return 0;
	}
	public int getBestImageHeight(int imageType) {
		return 0;
	}
	public int getBorderStyle(boolean highlighted) {
		return Graphics.SOLID;
	}
	public int getColor(int colorSpecifier) {
		switch (colorSpecifier) {
			case COLOR_BACKGROUND: 
				return 0x808080;
			case COLOR_FOREGROUND:
				return 0x00; 
			case COLOR_HIGHLIGHTED_BACKGROUND:
				return 0x989898; 
			case COLOR_HIGHLIGHTED_FOREGROUND:
				return 0xFF0000;
			case COLOR_BORDER:
				return 0x606060;
			case COLOR_HIGHLIGHTED_BORDER:
				return 0xFF0000;
			default:
			throw new IllegalArgumentException();
		}
	}
	
	//show emulator frame onscreen
	public static void  emuRunEmulation() {
		if (emulationStarted) {
			return ; 
		}
		emulationStarted = true;
		if (MainApp.verbose) {
			System.out.println("runEmuFrame!");
		}

		emuCanvas = EmuCanvas.getInstance();
		//emuCanvas.setParentComponent(MainApp.parentComponent);
		emuCanvas.init();
		
		Video.initVideo();
	}
	
	
	
}
