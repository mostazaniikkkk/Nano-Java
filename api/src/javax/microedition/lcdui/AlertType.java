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


public class AlertType {


	private static final int TYPE_ALARM = 1;
	private static final int TYPE_CONFIRMATION = 2;
	private static final int TYPE_ERROR = 3;
	private static final int TYPE_INFO = 4;
	private static final int TYPE_WARNING = 5;
	
	public static final AlertType ALARM = new AlertType(TYPE_ALARM); 
	public static final AlertType CONFIRMATION = new AlertType(TYPE_CONFIRMATION); 
	public static final AlertType ERROR = new AlertType(TYPE_ERROR); 
	public static final AlertType INFO = new AlertType(TYPE_INFO); 
	public static final AlertType WARNING = new AlertType(TYPE_WARNING); 
	
	int type;

	protected AlertType() {
	}
	protected AlertType(int t) {
		type = t;
	}
	
	public boolean playSound(Display display) {
		return true; 
	}

	
}
