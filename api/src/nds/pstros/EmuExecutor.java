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

import nds.pstros.utils.NDSKeyEvent;

/**
 * Use the EmuExecutor to controll execution and termination of the pstros emualator. 
 * You can also set and get emulator parameters, send direct keycode events and/or send
 * virtual key actions to controll the emulated application.
 * <p>
 * To obtain the instance of the EmuExecutor call the ole.pstros.MainApp.getEmuExecutor() method.
 * Typical usage might look that way:
 * </p>
 * <br>
 * <pre>
 * 	executor = ole.pstros.MainApp.getEmuExecutor();
 *	executor.setEmuListener(this);
 *	executor.setParameter(jadPath);
 *	executor.setParentComponent(this);
 * 	executor.execute();
 * </pre>
 * </br>     
 */
public interface EmuExecutor {
	
	/**
	 * action type, value 0
	 */
	public static final int KEY_ACTION_PRESS = 0;
	
	/**
	 * action type, value 1
	 */
	public static final int KEY_ACTION_RELEASE = 1;
	
	/**
	 * register emulator listener
	 */
	public void setEmuListener(EmuListener listener);
	
	
	/**
	 * Set parameter to the emulator. Use this method prior execution of the emulation.
	 * @param  parameter -  parameter and its value. Use the same syntax as for command line parameter. 
	 * Example: "-w=128" sets the emulator screen width to 128 pixels.    
	 */
	public void setParameter(String parameter);
		
		
	/**
	 * Get value of the emualation environment parameter 
	 * Example: parameter "-w" gets the actual screen width)  
	 */
	public Object getParameter(String parameter); 
		
	/**
	 * Start execution of the emulator
	 * @return - true if execution started
	 */
	public boolean execute(); 
	
	/**
	 * Terminate execution of the emulator 
	 */
	public void terminate();
	
	/**
	 * Call pauseApp on midlet
	 */
	public void pauseApp();


	/**
	 * Call startApp on midlet
	 */
	public void startApp();
	
	
	/**
	 * Call showNotify on current canvas (that was set by Display.setCurrent(...)). 
	 * If current displayable object is not a Canvas instance then the request is ignored. 
	 */
	public void showNotify();
	
	/**
	 * Call hideNotify on current canvas (that was set by Display.setCurrent(...)). 
	 * If current displayable object is not a Canvas instance then the request is ignored.
	 *
	 */
	public void hideNotify();
	
	/**
	 * Send key press notification to the emulator. Call this method to notify 
	 * emulator that key was pressed.      
	 */
	public void keyPressed(NDSKeyEvent e);
	
	/**
	 * Send key release notification to the emulator. Call this method to notify 
	 * emulator that key was released.      
	 */
	public void keyReleased(NDSKeyEvent e);

	/**
	 * Send key action to the emulator. Use it if you want to simulate 
	 * key press or release (for example to implement virtual buttons).
	 * You can get the appropriate keycode by calling getParameter() method
	 * and passing one of these parameter names:
	 * <br>keySoftLeft, keySoftRight, keySoftCenter </br>
	 * <br>keyLeft, keyUp, keyRight, keyDown, keyFire </br>
	 * <br>keyNum0 ... keyNum9, keyStar, keyCross </br>
	 * @param actionType - either KEY_ACTION_PRESS or KEY_ACTION_RELEASE
	 * @param key - key code
	 */
	public void keyAction(int actionType, int key); 
	
	
	void setEvent(String event, Object data) ;
}
