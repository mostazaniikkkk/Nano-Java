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

/**
 * Implement the EmuListener to get notified about the emulator state. Events will be passed to the
 * emuUpdate(...) method. Note: pass your instance of the EmuListener to the EmuExecutor.setEmuListener(...)
 * to get notifications.   
 */
public interface EmuListener {
	
	/**
	 * Emulator called repaint() - data object contains current Image of the 
	 * emulator display. Use the passed image for read-only operations only!
	 * If you want to apply effects/filters to the image then create your own copy for that
	 * purpose.   
	 */
	public static final String EMU_EVENT_REFRESH_DISPLAY = "emuRefreshDisplay";
	/**
	 * Emulator was started. 
	 */
	public static final String EMU_EVENT_STARTED = "emuStarted";
	/**
	 * Emulator has ended. 
	 */
	public static final String EMU_EVENT_CLOSED = "emuClosed";
	
	/**
	 * Emulator encountered an error - data object contains String description
	 * of the error. 
	 */
	public static final String EMU_EVENT_ERROR = "emuError";
	
	/**
	 * Emulator encountered a platformRequest call - data object contains String URL of 
	 * the request call. 
	 */
	public static final String EMU_EVENT_PLATFORM_REQUEST = "emuPlatformRequest";
	 
	/**
	 * Implement this method to get basic state-informations about
	 * the emulator. 
	 * @param event - identifier of the event
	 * @param data - additional data of the event
	 */
	public void emuUpadate(String event, Object data);

}
