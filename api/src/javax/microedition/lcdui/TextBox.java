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

import nds.pstros.EmuExecutor;
import nds.pstros.video.NDSGraphics;


public class TextBox extends Screen {

	private static int MAX_SIZE = 1024*8;

	private String text;
	private int maxSize;
	private int constraints;
	private int caretPosition;	
	private String charSubset;
	
	public TextBox(String title, String text, int maxSize, int constraints) {
		setTitle(title);
		setMaxSize(maxSize);
		this.text = text;
		this.constraints = constraints;
		
	}
	
	public String getString() {
		return text;
	}
	
	public void setString(String text) {
		this.text = text;
	}
	
	public int getChars(char[] data) {
		if (text == null) {
			return 0;
		}
		int length = text.length(); 
		text.getChars(0, length, data, 0);
		return length;
	}
	
	public void setChars(char[] data, int offset, int length) {
		String newText = new String(data, offset, length);
		text = newText;
	}
	public void insert(String src, int position) {
		StringBuffer sb = new StringBuffer(text);
		sb.insert(position, src);
		text = sb.toString();
	}
	
	public void insert(char[] data, int offset, int length, int position) {
		String newString = new String(data, offset, length);
		insert(newString, position);
	}
	
	
	public void delete(int offset, int length) {
		StringBuffer sb = new StringBuffer(text);
		sb.delete(offset, offset+ length);
		text = sb.toString();
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	public int setMaxSize(int maxSize) {
		if (maxSize > MAX_SIZE) {
			maxSize = MAX_SIZE;
		}
		this.maxSize = maxSize;
		
		if (text != null && text.length() > maxSize) {
			text = text.substring(0, maxSize);
		}
		return maxSize;
	}
	public int size() {
		return  text == null ? 0 : text.length();
	}
	
	public int getCaretPosition() {
		return caretPosition;
	}
	public void setConstraints(int constraints) {
		this.constraints = constraints;
	}
	public int getConstraints() {
		return constraints;
	}
	public void setInitialInputMode(String characterSubset) {
		charSubset = characterSubset;		
	}
	void emuPaintScreenContent(NDSGraphics g) {
		int y = 30;
		if (text != null) {
			g.drawString(text, 0, y);
		}
	}
	
	public boolean emuKeyAction(int key, int keyChar, int modifiers,  int action) {
		super.emuKeyAction(key, keyChar, modifiers, action);
		if (action == EmuExecutor.KEY_ACTION_PRESS) {
			if (key == Display.keyLeft ) {
				if (text != null) {
					int length = text.length();
					caretPosition++;
					if (caretPosition >length) {
						caretPosition = length;
					}
				}
			} else
			if (key == Display.keyRight )  {
				if (text != null) {
					caretPosition--;
					if (caretPosition < 0) {
						caretPosition = 0;
					}
				}
			}
				
		}
		return true;
	}
}
