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
package nds.pstros.utils;


public class Pair {
	
	private Object o1;
	private Object o2;
	
	public Pair() {
	}
	
	public Pair(Object first, Object second) {
		set(first, second);
	}
	
	public void set(Object first, Object second) {
		o1 = first;
		o2 = second;
	}
	public void setFirst(Object first) {
		o1 = first;
	}
	public void setSecond(Object second) {
		o2 = second;
	}
	
	public Object getFirst() {
		return o1;
	}
	public Object getSecond() {
		return o2;
	}

}
