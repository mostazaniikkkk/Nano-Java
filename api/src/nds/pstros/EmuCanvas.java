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
package nds.pstros;


import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Screen;

import nds.Bios;
import nds.Key;
import nds.Video;
import nds.pstros.rms.RmsManager;
import nds.pstros.utils.NDSKeyEvent;
import nds.pstros.video.NDSCanvas;
import nds.pstros.video.NDSFont;
import nds.pstros.video.NDSGraphics;
import nds.pstros.video.NDSImage;
import nds.pstros.video.NDSRectangle;


public class EmuCanvas extends NDSCanvas {
	public static final int COLOR_BLACK = 0xFF000000;
	public static final int COLOR_WHITE = 0xFFffffff;
	public static final int COLOR_BLUE = 0xFF0000ff;
	public static final int COLOR_LIGHT_GRAY = 0xFFBBBBBB;
	public static final int COLOR_GRAY = 0xFF909090;

	public static final int MASK_ALT = 8;
	
	//remaping of the keys 
	public static int keyLeft = Display.keyLeft;
	public static int keyRight = Display.keyRight;
	public static int keyUp = Display.keyUp;
	public static int keyDown = Display.keyDown;
	public static int keyA = Display.keyNum3;
	public static int keyB = Display.keyNum5;
	public static int keyY = Display.keyNum1;
	public static int keyX = Display.keyNum0;
	public static int keyL = Display.keySoftLeft;
	public static int keyR = Display.keySoftRight;
	public static int keyStart = Display.keyCross;
	public static int keySelect = Display.keyStar;

	public static int screenPosX;
	public static int screenPosY;

	public static EmuCanvas instance; 
	private static NDSRectangle tmpClipRect = new NDSRectangle();

	private Displayable displayable;
	private NDSImage emuImage;
	private NDSGraphics emuGraph;
	private javax.microedition.lcdui.Graphics deviceGraph; 
	private static int emuFrameCounter;
	
	private boolean painting;
	private boolean emuPaintRequest;
	private boolean hideNotifyTest;
	private boolean debugKeys = false;
	private boolean distanceMode = false;
	private int mouseX , mouseY;
	
	private int repaintX, repaintY, repaintW, repaintH;
	
	private NDSFont consoleFont;
	
	public int[] paintLock = new int[1];
	private int[] pauseLock = new int[1];

	private int[] tmpInt = new int[4];
	
//	private Frame imageViewerFrame;
//	private Frame zoomFrame;
//	private ZoomViewer zoomViewer;
//	private MonitorFrame classMonitorFrame;
	private long lastTime;
	private int fps;
	private boolean paused;
	
	private byte screenShiftY;
	
	
	private short[] frameBackupRGB;
	private boolean restoreFrame;
	
	
	public static EmuCanvas getInstance() {
		if (instance == null) {
			instance = new EmuCanvas();
			//System.out.println("EC: getInstance Thread=" + Thread.currentThread().hashCode());
		}
		return instance;
	}

	private EmuCanvas() {
		super(true);
		//setBackground(COLOR_WHITE);
		//addKeyListener(this);
	}
	
	/*
	public void setParentComponent(Component c) {
		parentComponent = c;
	}
	*/
	
	public void backupImage() {
		final int w = emuImage.getWidth();
		final int h = emuImage.getHeight();
		if (frameBackupRGB == null || frameBackupRGB.length != w*h) {
			frameBackupRGB = new short[w * h];
		}
		System.arraycopy(emuImage.getPixelData(), 0, frameBackupRGB, 0, frameBackupRGB.length );
	}
	
	public void restoreImage() {
		restoreFrame = true;
	}
	
	public javax.microedition.lcdui.Graphics createGraphics() {
		NDSImage img = NDSImage.createImage(Display.WIDTH, Display.HEIGHT);

		NDSGraphics eg = img.getGraphics();
		eg.setColor(COLOR_BLACK);
		eg.fillRect(0,0,Display.WIDTH, Display.HEIGHT);
		
		javax.microedition.lcdui.Graphics dg = new javax.microedition.lcdui.Graphics();
		dg.emuSetGraphics(eg);
		dg.emuSetGraphicsImage(img);
		dg.setClip(0,0,Display.WIDTH,Display.HEIGHT);
		return dg;
	}
	
	public void init() {
		//int consoleSize = ConfigData.getConsoleSize();
		
		emuImage = NDSImage.createImage(Display.WIDTH, Display.HEIGHT);

		emuGraph = emuImage.getGraphics();
		//emuGraph.setColor(COLOR_BLUE);
		emuGraph.setColor(COLOR_BLACK);
		emuGraph.fillRect(0,0,Display.WIDTH, Display.HEIGHT);
		
		
		deviceGraph = new javax.microedition.lcdui.Graphics();
		deviceGraph.emuSetGraphics(emuGraph);
		deviceGraph.emuSetGraphicsImage(emuImage);
		deviceGraph.setClip(0,0,Display.WIDTH,Display.HEIGHT);
		//System.out.println("EmuCanvas.init() deviceGraphics=" + deviceGraph.hashCode() + " w=" + Display.WIDTH + " h=" + Display.HEIGHT);
		if (ConfigData.bottomConsoleHeight > 0) {
			consoleFont = new NDSFont(null, NDSFont.PLAIN,  ConfigData.bottomConsoleHeight - 4);
			//System.out.println("Font=" + font.hashCode());
		} else {
			consoleFont = new NDSFont(null, NDSFont.PLAIN,  12);
		}
		mouseX = -1;
		mouseY = -1;
	}
	
	
	public javax.microedition.lcdui.Graphics getDeviceGraphics() {
		return deviceGraph;
	}
	
	public NDSImage getEmuImage() {
		return  emuImage;
	}
	public void setEmuPaintRequest(int x, int y, int w, int h) {
		repaintX = x;
		repaintY = y;
		repaintW = w;
		repaintH = h;
		emuPaintRequest = true;
	}
	
	
	public void setContent(Displayable d) {
		
		//hide current displayable
		if (displayable instanceof javax.microedition.lcdui.Canvas) {
			((javax.microedition.lcdui.Canvas)displayable).emuHideNotify();
		}
		displayable = d;
		if (displayable != null) {
			displayable.emuSetEmuCanvas(this);
		}
		
		//reset new display
		deviceGraph.translate(-deviceGraph.getTranslateX(), -deviceGraph.getTranslateY());
		deviceGraph.setClip(0,0, 1000, 1000);
				
		if (displayable instanceof javax.microedition.lcdui.Canvas) {
			//System.out.println("EmuCanvas: calling showNotify() on :" + displayable);
			synchronized(displayable) {
				((javax.microedition.lcdui.Canvas)displayable).emuShowNotify();
				//System.out.println("show notify done");
			}
		} else {
			//System.out.println("EmuCanvas: warning setContent on :" + displayable);
		}
		//System.out.println("setContent:" + d);
		if (ConfigData.slaveMode) {
			if (d !=null) {
				setEmuPaintRequest(0,0,d.getWidth(), d.getHeight());
				update();
			}
		} else {
			if (d!= null) {
				setEmuPaintRequest(0,0,d.getWidth(), d.getHeight());
				repaint();
			}
		}
	}
	
	public void update(NDSGraphics g) {
		//System.out.println("update:" + g);
		if (hideNotifyTest) {
			g.setColor(0xFFFF00FF);
			g.setClip(0,0, 1000, 1000);
			g.fillRect(0,0,1000,1000);
			
			g.setColor(0xFFFF0000);
			for (int i = 0; i < 100; i++) {
				g.fillRect(0,i*20, 1000, 10);
			}
		} else {
			paintContent(g);
		}
		checkKeys();
		Bios.swiWaitForVBlank();

	}
	
	public void update() {
		paintContent(null);
	}
	
	public int getFps() {
		return fps;
	}
	public void paint() {
		System.out.println("EmuCanvas: paint()...");
		NDSGraphics g = getGraphics();
		paintContent(g);
		//g.dispose();
	}
	
	public boolean paintRequestValid() {
		return emuPaintRequest;
	}

	public void paint(NDSGraphics g) {
		System.out.println("EmuCanvas: paint(g)...");
		paintContent(g);
	}
	private void paintContent(NDSGraphics g) {
		synchronized(paintLock) {
			painting = true;
			//System.out.println("EmuCanvas:paintContent START time=" + System.currentTimeMillis() + " displayable=" + displayable);
		
			if (emuGraph == null) {
				if (MainApp.verbose) {
					System.out.println("EmuCanvas: paintContent() emuGraph is null.");
				}
				return;
			}
			if (restoreFrame) {
				if (frameBackupRGB != null) {
					final int w = emuImage.getWidth();
					final int h = emuImage.getHeight();
					System.arraycopy(frameBackupRGB, 0, emuImage.getPixelData(), 0, w*h);
					frameBackupRGB = null;
					//System.out.println("DRAW RGB!");
				}
				restoreFrame = false;
			}
			
			if (displayable instanceof Screen) {
				//System.out.println("EmuCanvas: paint Screen.");
				paintScreen(g, (Screen) displayable);
			} else
			if (displayable instanceof javax.microedition.lcdui.Canvas) {
				//System.out.println("EmuCanvas: paint Canvas.");
				paintCanvas(g, (javax.microedition.lcdui.Canvas) displayable);
			} else 
			if (MainApp.verbose){
				System.out.println("Nano Java: paint unknown displayable! " + displayable);
			}
			//System.out.println("EmuCanvas:paintContent END time_of_start=" + time);
			painting = false;
		}
	}
	public boolean callRunner() {
		if (!painting) {
			//System.out.println("call runner - go!..");
			return Display.emuRunSerialRunner();
		}
		/* 
		else {
			System.out.println("call runner - painting...");
		}
		*/
		return false;
	}
	
	public boolean isPainting() {
		return painting;
	}
	
	private void togglePause() {
		boolean stop;
		synchronized (pauseLock) {
			paused = !paused;
			stop = paused;
		}
	}
	
	public void flushGraphics(NDSImage img) {
		if (img == null) {
			img = emuImage;
		}

		//System.out.println("EmuCanvas: flushGraphics");
		NDSGraphics g = getGraphics();
		paintCanvasContent(g, displayable, img);
		//updateFps();
			
		if (ConfigData.drawWait > 0) {
			try{
				Thread.sleep(ConfigData.drawWait);
			} catch (Exception e) {
				//System.out.println("thread.sleep interrupted! " + e);
			}
		}
		checkKeys();
		Bios.swiWaitForVBlank();

	}
	public void checkKeys() {
		Key.scan();
		checkKeyPress(Key.down());
		checkKeyRelease(Key.up());
	}
	private void checkKeyPress(int keys) {
		if ((keys & Key.LEFT) != 0) {
			keyPressed(keyLeft);
		}
		if ((keys & Key.RIGHT) != 0) {
			keyPressed(keyRight);
		}
		if ((keys & Key.UP) != 0) {
			keyPressed(keyUp);
		}
		if ((keys & Key.DOWN) != 0) {
			keyPressed(keyDown);
		}
		if ((keys & Key.B) != 0) {	//fire 17 / 101
			keyPressed(keyB); 
		}
		if ((keys & Key.L) != 0) {
			keyPressed(keyL);
		}
		if ((keys & Key.R) != 0) {
			keyPressed(keyR);
		}
		if ((keys & Key.SELECT) != 0) {
			keyPressed(keySelect);
		}
		if ((keys & Key.START) != 0) {
			keyPressed(keyStart);		
		}
		if ((keys & Key.X) != 0) {
			keyPressed(keyX);		
		}
		if ((keys & Key.A) != 0) {
			keyPressed(keyA);		
		}
		if ((keys & Key.Y) != 0) {
			keyPressed(keyY);		
		}
	}
	private void checkKeyRelease(int keys) {
		if ((keys & Key.LEFT) != 0) {
			keyReleased(keyLeft);
		}
		if ((keys & Key.RIGHT) != 0) {
			keyReleased(keyRight);
		}
		if ((keys & Key.UP) != 0) {
			keyReleased(keyUp);
		}
		if ((keys & Key.DOWN) != 0) {
			keyReleased(keyDown);
		}
		if ((keys & Key.B) != 0) { //fire 17 / 101
			keyReleased(keyB);
		}
		if ((keys & Key.L) != 0) {
			keyReleased(keyL);
		}
		if ((keys & Key.R) != 0) {
			keyReleased(keyR);
		}
		if ((keys & Key.SELECT) != 0) {
			keyReleased(keySelect);		
		}
		if ((keys & Key.START) != 0) {
			keyReleased(keyStart);		
		}
		if ((keys & Key.X) != 0) {
			keyReleased(keyX);		// 0
		}
		if ((keys & Key.A) != 0) {
			keyReleased(keyA);		//  phone 3
		}
		if ((keys & Key.Y) != 0) {
			keyReleased(keyY);		// phone 1
		}
		
	}
	public void checkPause() {
		if (ConfigData.slaveMode) {
			return;
		}
		//pause execution 
		boolean stop;
		synchronized (pauseLock) {			
			stop = paused;
		}
		while (stop) { 
			synchronized (pauseLock) {
				stop = paused;
			}
				
			try {
				Thread.sleep(5);
				//processEvent(new KeyEvent(this,0,0,0,0,' '));
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}
	
	private void paintScreen(NDSGraphics g, Screen screen) {
		//refresh content of the playfield only on emulator request  
		//if (emuPaintRequest) {
			g.setClip(0,0,256, 256);
			screen.emuPaint(emuGraph);
		//}
		if (g != null) {
			g.drawImage(emuImage, screenPosX, screenPosY);
		} else {
			//FIXME -  NDS ?
			//MainApp.getInstance().setEvent(EmuListener.EMU_EVENT_REFRESH_DISPLAY, emuImage);
		}
		
		//System.out.println("paint screen!");
		//checkKeys();
		//Bios.swiWaitForVBlank();

	}
	private void updateFps() {
		emuFrameCounter++;
		//compute fps - after 40 frames
		if (emuFrameCounter % 40 == 0) {
			long now = System.currentTimeMillis();
			if (lastTime > 0) {
				int divisor = (int)(now - lastTime);
				if (divisor == 0) {
					divisor = 5;
					try {
						Thread.sleep(5);
					} catch (Exception e) {}
				}
				fps =(int)(40000 / divisor);
			}
			lastTime = now;
		}
		
	}
	
	private void paintCanvas(NDSGraphics g, javax.microedition.lcdui.Canvas canvas) {
		//refresh content of the playfield only on emulator request  
		//System.out.println("GameCanvas? " + (canvas instanceof javax.microedition.lcdui.game.GameCanvas) );
		//System.out.println("EmuCanvas.paintCanvas() emuPaintRequest=" + emuPaintRequest + " time=" + System.currentTimeMillis());
		if (emuPaintRequest) {
			emuPaintRequest = false;
			try{
				if (MainApp.verbose) {
					System.out.println("--------Nano Java: Canvas.paint ------ frame=" + emuFrameCounter);
				}

				updateFps();
				//reset drawing environment
				deviceGraph.setColor(0);
				deviceGraph.translate(-deviceGraph.getTranslateX(), -deviceGraph.getTranslateY());
				deviceGraph.setClip(repaintX,repaintY,repaintW,repaintH);
				deviceGraph.setFont(javax.microedition.lcdui.Font.getDefaultFont());
				deviceGraph.setStrokeStyle(javax.microedition.lcdui.Graphics.SOLID);
				canvas.emuSetShown(true);
				canvas.emuPaint(deviceGraph, this);
				
				if (ConfigData.drawWait > 0) {
					try{
						Thread.sleep(ConfigData.drawWait);
					} catch (Exception e) {
						System.out.println("thread.sleep interrupted! " + e);
					}
				}
			} catch (Exception e) {
				System.out.println("Nano Java: unhandled exception in J2ME code! Error:" + e);
				e.printStackTrace();
			}
		} 
		paintCanvasContent(g, canvas, emuImage);
	}
	
	private void paintCanvasContent(NDSGraphics g, Displayable disp, NDSImage img) {
		int topH = ConfigData.topConsoleHeight;
		int botH = ConfigData.bottomConsoleHeight;
		//final int x = (getWidth() - emuImage.getWidth()) >> 1;
		//final int y = (getHeight() - emuImage.getHeight()) >> 1;
		int w =  Display.WIDTH * ConfigData.scale;
		int h =  (Display.HEIGHT - topH) * ConfigData.scale;
		
		if (disp == null) {
			return;
		}
		if (disp.emuIsFullScreen()) {
			topH = 0;
			botH = 0;
			h = (Display.HEIGHT) * ConfigData.scale; 			
		}
		
		int  currColor = 0; 
		boolean topDrawn = false;
		//draw top console - we don'd draw anything on the top console - draw it directly  
		if (topH > 0 && g != null) {
			currColor = emuGraph.getColor();
			emuGraph.getClipBounds(tmpClipRect);
			
			g.setColor(COLOR_LIGHT_GRAY);
			g.setClip(screenPosX, screenPosY,Display.WIDTH, Display.HEIGHT);
			g.fillRect(screenPosX, screenPosY, w, ConfigData.topConsoleHeight * ConfigData.scale);
			topDrawn = true;
		}
		//draw bottom console
		if (botH > 0 ) {
			if (!topDrawn) {
				currColor = emuGraph.getColor();
				emuGraph.getClipBounds(tmpClipRect);
			}
			topDrawn = true;
			//System.out.println("dispH=" + Display.HEIGHT + " botH=" + botH + " topH=" + topH + " x=" + x +  " y=" + y  + " color=" + currColor);
			emuGraph.setClip(0,0, Display.WIDTH, Display.HEIGHT);
			emuGraph.setColor(COLOR_LIGHT_GRAY);
			emuGraph.fillRect(0, Display.HEIGHT - botH - topH, Display.WIDTH, botH);
			
			Command lc = disp.emuGetLeftCommand();
			Command rc = disp.emuGetRightCommand();
			//System.out.println("lc=" + lc + " rc=" + rc);
			//left softKey
			if (lc != null) {
				NDSFont currentFont = emuGraph.getFont();
				emuGraph.setColor(COLOR_BLACK);
				emuGraph.setFont(consoleFont);
				emuGraph.drawString(((Command)lc).getLabel(), 0, Display.HEIGHT-4); //4
				emuGraph.setFont(currentFont);
			}
			//right soft key
			if (rc != null) {
				String label = ((Command) rc).getLabel();
				int boundW = consoleFont.getStringWidth(label);
				NDSFont currentFont = emuGraph.getFont();
				emuGraph.setColor(COLOR_BLACK);
				emuGraph.setFont(consoleFont);
				emuGraph.drawString(label, Display.WIDTH - boundW - 1, Display.HEIGHT-4);
				emuGraph.setFont(currentFont);
			}
			
		}
		//set back emuGraph properties
		if (topDrawn) {
			emuGraph.setClip(tmpClipRect);
			emuGraph.setColor(currColor);			
		}
		
		if (g != null) {
			//draw play field
			g.setClip(screenPosX, screenPosY, Display.WIDTH , Display.HEIGHT);
			g.drawImage(img, screenPosX, screenPosY + topH);
			
		} else {
			//FIXME ? NDS
			//MainApp.getInstance().setEvent(EmuListener.EMU_EVENT_REFRESH_DISPLAY, emuImage);			
		}
		
	}


	
	
	public void keyAction(int actionType, int key) {
		//System.out.println("keyAction type=" + actionType + " key=" + key + " displayable=" + displayable);
		if (displayable != null) {
			displayable.emuKeyAction(key, ' ', 0, actionType);
		}
	} 


	
	private void runCallEvent(javax.microedition.lcdui.Canvas gCanvas) {
		//no callTest event sequence specified - just call hide and show notify
		if (ConfigData.callEvent == null) {
			gCanvas.emuHideNotify();

			hideNotifyTest = true;
			//MainApp.repaintDisplay();
			repaint();
			try {
				Thread.sleep(2000);
			} catch (Exception ex) {	
			}
			Thread.yield();
			hideNotifyTest = false;
			gCanvas.emuShowNotify();
		}
		/*
		else {
			if (MainApp.verbose) {
				System.out.println("Pstros: device.callEvent=" + ConfigData.callEvent);
			}
			StringTokenizer st = new StringTokenizer(ConfigData.callEvent, ",; ");
			int size =  st.countTokens();
			for (int i = 0; i < size; i++) {
				String command = st.nextToken();
				if (MainApp.verbose) {
					System.out.println("pstros: event command=" + command);
				}
				//pause application
				if ( command.equals("pauseApp") || command.equals("pa")) {
					if (MainApp.midlet != null) {
						MainApp.midlet.emuPauseApp();
					}
				} else
				//start app
				if ( command.equals("startApp") || command.equals("sa")) {
					if (MainApp.midlet != null) {
						try {
							MainApp.midlet.emuStartApp();
						} catch (Exception e) {
							System.out.println("error:" + e);
						}
					}
				} else
				//hide notify 
				if ( command.equals("hideNotify") || command.equals("hn")) {
					gCanvas.emuHideNotify();
				} else
				//show notify 
				if ( command.equals("showNotify") || command.equals("sn")) {
					gCanvas.emuShowNotify();
				} else
				//screen off - show test pattern 
				if ( command.equals("screenOff") || command.equals("s0")) {
					hideNotifyTest = true;
					//repaint screen with the abstract pattern
					repaint();
					//g.dispose();
				} else
				if ( command.equals("screenOn") || command.equals("s1")) {
					hideNotifyTest = false;
				} else
				if ( command.startsWith("wait") || command.startsWith("w")) {
					String number;
					if (command.startsWith("wait") ) {
						number = command.substring(4);
					} else
					{
						number = command.substring(1);
					}
					if (number == null || number.length() < 1) {
						number = "1000";
					} 
					int value = Integer.parseInt(number);
					try {
						Thread.sleep(value);
					} catch (Exception e) {
					}
				} 
			}
			 
		}
		*/
	}
	
	public void keyPressed(int keyCode) {
		//char keyChar = e.getKeyChar();
		//int modifiers = e.getModifiers();
		if (debugKeys) {
			System.out.println("EmuCanvas.keyPressed key=" + keyCode + " diplayable=" + displayable );
		}
		if (displayable != null) {
			displayable.emuKeyAction(keyCode, ' ', 0,  EmuExecutor.KEY_ACTION_PRESS);
		}
		//scale Alt + 1,2,3
		//Togle key debug - CapsLock
		if (keyCode == 20) {
			debugKeys = !debugKeys;
		} else
		//Toggle verbose mode - Alt+V
		/*
		if (keyCode == 86 && (modifiers & MASK_ALT) != 0) {
			MainApp.verbose = !MainApp.verbose;			
		} else
		*/
//		//Call GG
//		if (keyCode == 71 && (modifiers & MASK_ALT) != 0) {
//			MainApp.forceGC();
//		} else
		//toggle pause F5
		if (keyCode == Display.keyPause) {
			togglePause();
			//System.out.println("Paused =" + paused);
		} else
		//F9 - hide/ show notify test
		if (keyCode == Display.keyShowHideNotify) {	
			if (displayable instanceof javax.microedition.lcdui.Canvas) {
				javax.microedition.lcdui.Canvas gCanvas = (javax.microedition.lcdui.Canvas) displayable;
				runCallEvent(gCanvas);
				
			}
		}
		if (keyCode == 8) { //Backspace
			setEmuPaintRequest(repaintX, repaintY, repaintW, repaintH);
			repaint();
		} else 
		if (keyCode == 9){ //tabulator
			callRunner();
		}else 
		if (keyCode == 27) { //ESC
			Display.emuDestroyFrame();
		}
	}

	public void keyReleased(int keyCode) {

		//System.out.println("keyrelease=" + e.getKeyCode());
		if (displayable != null) {
			displayable.emuKeyAction(keyCode, ' ' , 0, EmuExecutor.KEY_ACTION_RELEASE);
		}
	}

	public void keyTyped(NDSKeyEvent e) {
	}

	/*
	public void windowClosed(WindowEvent e) {
	}
	*/

	public void quit() {
		//System.out.println("Closing!");
		if (MainApp.midlet != null) {
			MainApp.midlet.emuDestroy();
		}
		RmsManager.getInstance().saveData();
		setContent(null);
		emuGraph.dispose();
		Video.initVideo(); //clear the screen
		try {
			System.exit(0);
		} catch (Exception ex) {
		}
	}
	
	
	/////////// HIDE / SHOW NOTIFY
	public void hideNotify() {
		if (displayable instanceof javax.microedition.lcdui.Canvas) {
			((javax.microedition.lcdui.Canvas)displayable).emuHideNotify();
		}
	}
	public void showNotify() {
		if (displayable instanceof javax.microedition.lcdui.Canvas) {
			((javax.microedition.lcdui.Canvas)displayable).emuShowNotify();
		}
	}
	
	

}
