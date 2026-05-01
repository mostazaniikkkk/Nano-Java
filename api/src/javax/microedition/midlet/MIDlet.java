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

package javax.microedition.midlet;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Display;

import nds.pstros.MainApp;


public abstract class MIDlet {
	
	
	
	private boolean running;
	private boolean internalDestroy;
	

	public MIDlet() {
//		//FIXME - NDS ?
//		/*
//		final String platform = "microedition.platform";
//		String val = System.getProperty(platform);
//		if (val == null) {
//			System.setProperty(platform, "nds_pstros");
//		}
//		*/
	}

	public final boolean emuIsRunning() {
		return running; 
	}
	
	public final int checkPermission(String permission) {
		return 0;
	}
	protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

	public final String getAppProperty(String key) {
		//String result = JadFileParser.getValue(key);
		String result = null;
		if (result == null ) {
			result = System.getProperty(key);
		}
		if (MainApp.verbose) {
			System.out.println("Pstros: MIDlet.getAppProperty key=" + key + " result=" + result);
		}
		return result;
	}
	public final void notifyDestroyed(){
		internalDestroy = true;
		running = false;
		if (MainApp.verbose) {
			System.out.println("Pstros: notifyDestroyed called!");
		}
		Display.emuDestroyFrame();
	}
	
	public final void notifyPaused() {
		if (MainApp.verbose) {
			System.out.println("Pstros: notifyPaused called!");
		}
	}
	protected abstract void pauseApp();
	
	public final boolean platformRequest(String URL)  throws ConnectionNotFoundException {
			System.out.println("Pstros: platformRequest called.  URL="  + URL);
			throw new ConnectionNotFoundException("Feature not implemented in emulator");
	}
	public final void resumeRequest() {
		System.out.println("Pstros: resume request called!");
	}
	
	protected abstract void startApp() throws MIDletStateChangeException;
	
	public final void emuRun() throws MIDletStateChangeException {
		running = true;
		Display.emuRunEmulation();
		startApp();
	}
	
	public final void emuDestroy() {
		if (internalDestroy) {
			//System.out.println("Pstros: midlet call destroyApp(). Internal!");
			return;
		}
		try {
			//System.out.println("Pstros: midlet call destroyApp()");
			destroyApp(true);
			running = false;
		} catch (Exception e) { }
	} 
	
	public final void emuPauseApp() {
		pauseApp();
	}
	public final void emuStartApp() throws MIDletStateChangeException{
		startApp();
	}

	
}
