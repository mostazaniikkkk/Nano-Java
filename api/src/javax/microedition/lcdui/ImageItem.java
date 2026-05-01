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

import nds.pstros.video.NDSGraphics;


public class ImageItem extends Item{
	
	public static final int LAYOUT_DEFAULT = 0;
	public static final int LAYOUT_LEFT =  1;
	public static final int LAYOUT_RIGHT = 2;
	public static final int LAYOUT_CENTER = 3;
	public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
	public static final int LAYOUT_NEWLINE_AFTER = 0x200;
	
	
	private int mode;
	private Image image;
	private int layout;
	private String text; 
	
	public ImageItem(String label, Image img, int layout, String altText) {
		this(label, img, layout, altText, PLAIN);
	}
	
	public ImageItem(String label, Image image, int layout, String altText, int appearanceMode) {
		setLabel(label);
		this.image  = image;
		this.layout = layout;
		text = altText;
		mode = appearanceMode;
	}
	
	public Image getImage() {
		return image;
	}
	
	public void setImage(Image img) {
		image = img;
		emuUpdateScreen();
	}
	
	public String getAltText() {
		return text;
	}
	
	public void setAltText(String text) {
		this.text = text;
	}
	
	public int getLayout() {
		return layout;
	}
	
	public void setLayout(int layout) {
		this.layout = layout;
	}
	
	public int getAppearanceMode() {
		return mode;
	}
	int emuPaint(NDSGraphics g, int x, int y) {
		int hLayout = layout & 0xf;
		int vLayout = layout & 0xf0;
		
		//System.out.println("ii x=" + x + " y=" + y + " layout=" + layout + " hex=" + Integer.toHexString(layout) + " hL=" + hLayout + " vL=" + vLayout);
		
		switch (hLayout) {
			case LAYOUT_RIGHT: 
				x = Display.WIDTH - image.getWidth();
				break;
			case LAYOUT_CENTER:
				x = (Display.WIDTH - image.getWidth()) / 2;
				break;
		}
		int imageHeight = image.getHeight();
		switch (vLayout) {
			case LAYOUT_BOTTOM:
				y -= imageHeight;
				break;
			case LAYOUT_VCENTER:
				y -= (imageHeight / 2);
				break;
		} 
		//System.out.println("   x=" + x + " y=" + y);
		g.drawImage(image.emuGetImage(0),x,y);
		return imageHeight+ 2;
	}
	int emuGetHeight(NDSGraphics g) {
		return image.getHeight()+ 2;
	}
	int emuGetYSpace(NDSGraphics g) {
		int imageHeight = image.getHeight();
		int vLayout = layout & 0xf0;
		int result = 1;
		switch (vLayout) {
			case LAYOUT_BOTTOM:
				result += imageHeight;
				break;
			case LAYOUT_VCENTER:
				result += (imageHeight / 2);
				break;
		}
		return result;
	}


}
