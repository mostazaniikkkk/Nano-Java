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

import nds.pstros.video.NDSFont;
import nds.pstros.video.NDSGraphics;


public class Gauge extends Item {
	
	public static final int INDEFINITE = -1;
	public static final int CONTINUOUS_IDLE = 0;
	public static final int INCREMENTAL_IDLE = 1;
	public static final int CONTINUOUS_RUNNING = 2;
	public static final int INCREMENTAL_UPDATING = 3;
	
	
	private boolean interactive;
	private int maxVal;
	private int curVal;
	
	private int w = 100;
	private int h = 6;
	
	public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
		setLabel(label);
		this.interactive = interactive;
		maxVal = maxValue;
		curVal = initialValue;
		
	}
	
	public int getMaxValue() {
		return maxVal;
	}
	public boolean isInteractive() {
		return interactive;
	}
	
	public void setMaxValue(int maxValue) {
		maxVal = maxValue; 
	}
	
	public int getValue() {
		if (curVal == INDEFINITE) {
			return CONTINUOUS_IDLE;
		}
		return curVal;
	}
	public void setValue(int value) {
		if (value < 0) {
			value = 0;
		} else 
		if (value > maxVal && maxVal > 0) {
			value = maxVal;
		}
		curVal = value;
		emuUpdateScreen();
	}
	
	public void setPreferredSize(int width, int height) {
		w = width;
		h = height;
	}
	
	int emuPaint(NDSGraphics g, int x, int y) {
		int totalH = 0;
		g.setColor(COLOR_BLACK);
		//draw label
		if (label != null) {
			g.drawString(label, x, y);
			NDSFont font = g.getFont();
			totalH = font.getSize() + 2;
			y += totalH;
		}
		//draw gauge
		if (w > Display.WIDTH - x - 4) {
			w = Display.WIDTH - x - 4;
		}
		if (h > 16) {
			h = 16;
		}
		g.drawRect(x, y, w, h);
		int gauge = curVal * w / maxVal;
		g.fillRect(x,y,gauge, h);   
		
		return totalH + h + 2;

	}
	int emuGetHeight(NDSGraphics g) {
		int totalH = 0;
		//get label height
		if (label != null) {
			NDSFont font = g.getFont();
			totalH = font.getSize() + 2;
		}
		//get gauge height
		if (h > 16) {
			h = 16;
		}
		return totalH + h + 2;
	}
	

}
