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

import nds.pstros.MainApp;


public class Command {
	
	public static final int SCREEN = 1;
	public static final int BACK = 2;
	public static final int CANCEL = 3;
	public static final int OK = 4;
	public static final int HELP = 5;
	public static final int STOP = 6;
	public static final int EXIT = 7;
	public static final int ITEM = 8;
	
	private String shortLabel;
	private String longLabel;
	private int commandType;
	private int priority;
	
	Object owner;
	
	
	public Command(String label, int commandType,  int priority) {
		this (label, label, commandType, priority);	
	}
	
	public Command(String shortLabel, String longLabel, int commandType, int priority) {
		if (MainApp.verbose) {
			System.out.println("Command:<init> sl=" + shortLabel + " ll=" + longLabel +" ct=" + commandType + " pri=" + priority);
		}
		this.shortLabel = shortLabel;
		this.longLabel = longLabel;
		this.commandType = commandType;
		this.priority = priority;
	}
	
	public String getLabel() {
		return shortLabel;
	}

	public String getLongLabel() {
		return longLabel;
	}
	
	public int getCommandType() {
		return commandType;
	}

	public int getPriority(){
		return priority;
	}
	
	public String toString() {
		return "Command@" + hashCode() +" sl=" + shortLabel + " ll=" + longLabel + " ct=" + commandType;
	}
}
