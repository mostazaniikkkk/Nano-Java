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



public class ConfigData {
	
	public static boolean configActive = true;
	
	//Slave mode: don't provide our own WindowFrame, just process into the image
	//Master mode: create Window frame and emulate interactively 
	public static boolean slaveMode = false;
	
	public static int keyLeftSoft = -6;
	public static int keyRightSoft = -7;
	public static int keyCenterSoft = -5;
	public static int keyUpArrow = -1;
	public static int keyDownArrow = -2;
	public static int keyLeftArrow = -3;
	public static int keyRightArrow = -4;
	public static int keySelect = -6;
	public static int keyBack = -7;
	public static int keyClear = -8;
	
	public static int topConsoleHeight = 0;
	public static int bottomConsoleHeight = 0;
	public static boolean fullScreenSupported = true;

	public static int skinScreenX = 10;
	public static int skinScreenY = 20;
	public static int skinWidth = 0;
	public static int skinHeight = 0;
	
	public static int windowPositionX = 0;
	public static int windowPositionY = 0;
	public static int scale = 1;
	public static boolean externalScaler = false;
	
	public static String deviceName;
	
	public static int captureHeight = 0;
	public static int captureOffsetY = 0; 
	public static int captureMotionPrecision = 6;
	
	public static final int VIDEO_MEMORY_DEFAULT = 1024 << 10;	//1024 kpixels
	public static int videoMemoryLimit = VIDEO_MEMORY_DEFAULT;	
	
	public static String captureFile = "./capt";
	
	public static boolean storeImages = false;
	public static String  storeImagePath = null;
	
	public static int drawWait = -1;	//wait 5ms after each repaint by default
	public static boolean updateSerialRunner = true;
	
	public static boolean saveParams = true;
	//public static boolean rmsHack = false;
	
	public static boolean controlConfig = false;	//custom controls
	
	public static boolean classMonitor = false;	//clas monitor enabled
	public static boolean forceMute = false;
	
	public static boolean readOnly = false;			//read only access
	public static int displayGamma = 0;
	public static int zoomSize = 1;
	
	public static String fileConnectionMapping;
	public static String callEvent;
	
	public static boolean numKeySwap = false;

	public static int readSpeed = -1;


	/** helper methods *****/
	public static int getConsoleSize() {
		int consoleSize = 0;
		if (configActive) {
			if (ConfigData.topConsoleHeight > 0) {
				consoleSize += topConsoleHeight;
			}
			if (ConfigData.bottomConsoleHeight > 0) {
				consoleSize += bottomConsoleHeight;
			}
		}
		return consoleSize;
	}
	public static Object getParameter(String name) {
		
		return null;
	}

}
