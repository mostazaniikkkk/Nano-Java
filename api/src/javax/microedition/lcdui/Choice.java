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


public interface Choice {
	
	public static final int EXCLUSIVE = 1;
	public static final int MULTIPLE = 2;
	public static final int IMPLICIT  = 3;
	public static final int POPUP = 4;
	
	public static final int TEXT_WRAP_DEFAULT = 0;
	public static final int TEXT_WRAP_ON  = 1;
	public static final int TEXT_WRAP_OFF = 0;
	
	
	public int append(String stringPart, Image imagePart);
	public void insert(int elementNum, String stringPart, Image imagePart);
	public void delete(int elementNum);
	//public void deleteAll();
	public void set(int elementNum, String stringPart, Image imagePart);
	public boolean isSelected(int elementNum);
	public int getSelectedIndex();
	public int getSelectedFlags(boolean[] selectedArray_return);
	public void setSelectedIndex(int elementNum, boolean selected);
	public void setSelectedFlags(boolean[] selectedArray);
	//public void setFitPolicy(int fitPolicy);
	//public int getFitPolicy();
	//public void setFont(int elementNum, Font font);
	//public Font getFont(int elementNum);
	
	

}
