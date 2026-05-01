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
package javax.microedition.media.control;

import javax.microedition.media.Control;


public interface ToneControl extends Control {
	public static final byte VERSION = -2;
	public static final byte TEMPO = -3;
	public static final byte RESOLUTION = -4;
	public static final byte BLOCK_START = -5;
	public static final byte BLOCK_END = -6;
	public static final byte PLAY_BLOCK  = -7;
	public static final byte SET_VOLUME = -8;
	public static final byte REPEAT = -9;
	public static final byte C4 = 60;
	public static final byte SILENCE = -1;
	
	public void setSequence(byte[] sequence);

}
