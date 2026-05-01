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
package com.nokia.mid.ui;


public class DeviceControl {

	public static void setLights(int num, int level) {
		if (num > 0 || num < 0) {
			throw new IllegalArgumentException("light num is not supported. num=" + num); 
		} 
		if (level < 0 || level > 100) {
			throw new IllegalArgumentException("level is not between 0-100 :" + level);
		}
	}
	
	public static void flashLights(long duration) {
		if (duration < 0) {
			throw new IllegalArgumentException("duration < 0 :" + duration );
		}
	}
	
	public static void startVibra(int freq, long duration) {
	}
	public static void stopVibra() {
	}
	
}
