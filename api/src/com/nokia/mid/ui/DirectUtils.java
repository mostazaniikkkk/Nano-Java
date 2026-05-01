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

import java.io.IOException;

import javax.microedition.lcdui.Image;


public class DirectUtils {
	//private static javax.microedition.lcdui.Graphics baseGraphics;
	
	public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
		if (imageData == null) {
			throw new NullPointerException("imageData is null.");
		}
		if (imageOffset >= imageData.length || imageOffset + imageLength > imageData.length) {
			throw new ArrayIndexOutOfBoundsException("imageOffset and imageLength specify an invalid range.");
		}  
		try {
			return Image.createImage(imageData, imageOffset, imageLength);
		} catch (IOException e) {
			throw new IllegalArgumentException(e.toString());
		}
	}
	
	public static Image createImage(int width, int height, int ARGBcolor) {
		if (width < 1 || height < 1) {
			throw new IllegalArgumentException ("either width or height is zero or less.");	
		}
		return Image.createImage(width, height, ARGBcolor);
	}
	
	public static DirectGraphics getDirectGraphics(javax.microedition.lcdui.Graphics g) {
		/*
		if (directGraphics == null || g != baseGraphics) {
			directGraphics = new EmuExtendedGraphics(g);
			baseGraphics = g;
		}
		
		//System.out.println("Pstros: DirectUtils.getDirectGraphics  src=" + g.getEmuGraphics().hashCode() + " dst=" + directGraphics.getEmuGraphics().hashCode());
		return directGraphics;
		*/
		return (DirectGraphics) g;
	}
}
