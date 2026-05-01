/*
 * Created on May 21, 2008
 *
 */
package nds.pstros;

import javax.microedition.lcdui.Display;
import javax.microedition.media.Manager;
import javax.microedition.midlet.MIDlet;

import nds.pstros.rms.RmsManager;

/**
 * @author ole
 *
 */
public class MainApp {
	public static final boolean verbose = false;
	public static boolean soundVerbose;
	public static MIDlet midlet;
	public static Class root;
	
	private static String appName = "app";
	private static String jarFileName;
	private static String basePath;
	private static String execClassName;
	
	private static int fps;
	
	public void runMain(String[]args, Class rootClass) {
		root = rootClass;
		main(args);
		//verbose = true;
	}
	
	public static void main(String[] args) {
		if (args.length < 1) {
			String value = System.getProperty("MIDlet-1");
			if (value != null) {
				int index = value.indexOf(',', 0);
				appName = value.substring(0, index).trim();
				index = value.indexOf(',', index+1);
				String[] newArgs = new String[]{"-C" + value.substring(index+1).trim()};
				args = newArgs;
			}
		}
		printHeader();
		if (verbose) { 
			System.out.println("pstros started!");
		}

		checkArguments(args);
		executeEmu();
	}
	private static void checkArguments(String[] args) {
		//set special parameters taken from jad file
		String val;


		val = System.getProperty("NDS-resolution");
		if (val != null) {
			int index = val.indexOf('x');
			if (index > 0) {
				int resX = Integer.parseInt(val.substring(0, index).trim());
				int resY = Integer.parseInt(val.substring(index+1).trim());
				
				if (resX >= 64 && resX <=  256) {
					Display.WIDTH = resX;
				}
				if (resY >= 64 && resY <= 256) {
					Display.HEIGHT = resY;
				}
				//System.out.println("Set resolution: w=" + Display.WIDTH + " h=" + Display.HEIGHT);
			}
		}

		EmuCanvas.screenPosX = (256 - Display.WIDTH)  / 2;
		EmuCanvas.screenPosY = (192 - Display.HEIGHT)  / 2;

		
		val = System.getProperty("NDS-screen-X");
		if (val != null) {
			EmuCanvas.screenPosX = Integer.parseInt(val.trim()); 
		} 
		val = System.getProperty("NDS-screen-Y");
		if (val != null) {
			EmuCanvas.screenPosY = Integer.parseInt(val.trim()); 
		} 
		
		//remap keys
		setKeys();
		
		
		if (args == null || args.length < 1) {
			return;
		}
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			setEmuParameter(arg,i);
		}
	}
	
	private static void setKeys() {
		EmuCanvas.keyA = setKey("NDS-key-A", EmuCanvas.keyA);
		EmuCanvas.keyB = setKey("NDS-key-B", EmuCanvas.keyB);
		EmuCanvas.keyX = setKey("NDS-key-X", EmuCanvas.keyX);
		EmuCanvas.keyY = setKey("NDS-key-Y", EmuCanvas.keyY);
		EmuCanvas.keyL = setKey("NDS-key-L", EmuCanvas.keyL);
		EmuCanvas.keyR = setKey("NDS-key-R", EmuCanvas.keyR);
		
		EmuCanvas.keyStart = setKey("NDS-key-Start", EmuCanvas.keyStart);
		EmuCanvas.keySelect = setKey("NDS-key-Select", EmuCanvas.keySelect);
		
		EmuCanvas.keyLeft = setKey("NDS-key-Left", EmuCanvas.keyLeft);
		EmuCanvas.keyRight = setKey("NDS-key-Right", EmuCanvas.keyRight);
		EmuCanvas.keyUp = setKey("NDS-key-Up", EmuCanvas.keyUp);
		EmuCanvas.keyDown = setKey("NDS-key-Down", EmuCanvas.keyDown);
		
	}
	private static int setKey(String keyName, int defaultKeyValue) {
		String val = System.getProperty(keyName);
		if (val == null) {
			return defaultKeyValue;
		}
		int key = remapKey(val.trim()); 
		if ( key < -10000) {
			return defaultKeyValue;
		}
		return key;
	}
	private static int remapKey(String keyName) {
		if (keyName == null || keyName.length() < 3) {
			return -10001;
		} else
		if (keyName.equals("keyNum0")) {
			return Display.keyNum0;
		} else
		if (keyName.equals("keyNum1")) {
			return Display.keyNum1;
		} else
		if (keyName.equals("keyNum2")) {
			return Display.keyNum2;
		} else
		if (keyName.equals("keyNum3")) {
			return Display.keyNum3;
		} else
		if (keyName.equals("keyNum4")) {
			return Display.keyNum4;
		} else
		if (keyName.equals("keyNum5")) {
			return Display.keyNum5;
		} else
		if (keyName.equals("keyNum6")) {
			return Display.keyNum6;
		} else
		if (keyName.equals("keyNum7")) {
			return Display.keyNum7;
		} else
		if (keyName.equals("keyNum8")) {
			return Display.keyNum8;
		} else
		if (keyName.equals("keyNum9")) {
			return Display.keyNum9;
		} else
		if (keyName.equals("keySoftLeft")) {
			return Display.keySoftLeft;
		} else
		if (keyName.equals("keySoftRight")) {
			return Display.keySoftRight;
		} else
		if (keyName.equals("keyFire")) {
			return Display.keyFire;
		} else
		if (keyName.equals("keyLeft")) {
			return Display.keyLeft;
		} else
		if (keyName.equals("keyRight")) {
			return Display.keyRight;
		} else
		if (keyName.equals("keyUp")) {
			return Display.keyUp;
		} else
		if (keyName.equals("keyDown")) {
			return Display.keyLeft;
		} else
		if (keyName.equals("keyStar")) {
			return Display.keyStar;
		} else
		if (keyName.equals("keyCross")) {
			return Display.keyCross;
		} 
		return -10001;
	}
	
	private static boolean executeEmu() {
		//execute JAR 
		if (execClassName != null ) {
			//System.out.println("jarName   :" + jarFileName);
			//System.out.println("jarPath   :" + basePath);
			System.out.println("className :" + execClassName);
			System.out.println("appName   :" + appName);

			RmsManager manager =  RmsManager.getInstance();
			if (manager != null) {
				manager.readData();
			}
		
			executeClass();
			return true;
		} else {
			//printHelp();
			return false;
		}
	}
	private static void printHeader() {
		System.out.println("Nano Java - MIDP Runtime v. 0.7.3");
		System.out.println();
	}
	
	private static void setEmuParameter(String arg, int index) {

		//width  
		if (arg.startsWith("-w")) {
			Display.WIDTH = Integer.parseInt(arg.substring(2));
		} else
		//height
		if (arg.startsWith("-h")) {
			Display.HEIGHT = Integer.parseInt(arg.substring(2));
		}else
		if (arg.equals("-ro")) {
			ConfigData.readOnly = true;
		} else 

		if (arg.startsWith("-C")) {
			execClassName = arg.substring(2);
		} else
		
		//application name
		if (arg.startsWith("-A")) {
			appName = arg.substring(2);
		} else
		

		if (arg.startsWith("-dn=")) {
			ConfigData.deviceName = arg.substring(4);
		} else

		if (arg.equals("-mute")) {
			ConfigData.forceMute = true;
		} else
		//dont run update thread
		if (arg.startsWith("-U")) {
			ConfigData.updateSerialRunner  = false;
		}
		/*
		else
		//verbose
		if (arg.startsWith("-v")) {
			verbose = true;
		}
		*/
	}	
	private static void executeClass() {
		if (execClassName == null) {
			return;
		}
		System.out.println("DBG: loading " + execClassName);
		try {
			Class execClass = Class.forName(execClassName);
			System.out.println("DBG: class loaded");
			Display.emuRunEmulation();
			executeMidlet(execClass);
		} catch (Throwable e) {
			System.out.println("ERR: " + e);
		}
	}

	private static void executeMidlet(final Class execClass) {
		final EmuCanvas emuCanvas = EmuCanvas.getInstance();
		new Thread("pstros") { public void run() {
		try {
			final MIDlet md = (MIDlet) execClass.newInstance();
			midlet = md;
			
			/*
			int priority = getPriority() * 2;
			if (priority < 1) {
				priority = 1;
			}
			setPriority(priority);
			*/
			
			
			midlet.emuRun();
			//System.out.println("midlet=" + midlet);
			int monitorCounter = 0;
			
			if (ConfigData.updateSerialRunner) {
				//showThreads();
				long time = 500;
				int checkCount = 1000 / (int)time;
				boolean runnerExists = false;
				//check requests for Display.callSerially
				while (midlet.emuIsRunning()) {
					//showThreads();
					//System.out.println(" Zzz...");
					synchronized (emuCanvas) {
						runnerExists = emuCanvas.callRunner();
					} 
					if (runnerExists) { //runner exists ->check more often
						//System.out.println("runner exists...");
						time = 10;
						checkCount = 1000 / (int)time;
					}
					//update sound and music players
					if (checkCount-- < 0) { 
						Manager.emuUpdatePlayers();
						checkCount = 1000 / (int)time;
						emuCanvas.checkKeys();
					}
					Thread.sleep(5/*time*/);
				}
			} else {
				while (midlet.emuIsRunning()) {
					Thread.sleep(100);
					refreshFrameTitle(false);
				}				
			}
			Manager.emuStopPlayers();
			System.out.println("Nano Java: execution ended.");
			
			//allow 500 ms for cleanup of threads
			//if active threads remain in the TG then stop the threads
			try {
				Thread.sleep(50);
			} catch(Exception e) {
				System.out.println("Nano Java: Thread stop:" + e);					
			}

			
		} catch (Throwable e) {
			System.out.println("Error: NanoJava:" + e);
			e.printStackTrace();
		}
		}}.start();
	}
	
	private static void refreshFrameTitle(boolean force) {
		int currentFps = EmuCanvas.getInstance().getFps();
		if (force ||  currentFps != fps) {
			//todo - show fps somewhere
			fps = currentFps;
		}
	}
	
	private static boolean updateReferenceInfo() {
		return false;
	}
	public static String getApplicationName() {
		return appName;
	}
	
}
