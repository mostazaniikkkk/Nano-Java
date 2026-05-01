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
package javax.microedition.lcdui.game;

import java.util.Vector;

import javax.microedition.lcdui.Graphics;

import nds.pstros.MainApp;


public class LayerManager {

	private Vector layers;
	
	//view window coordinates
	private int winX;
	private int winY;
	private int winW;
	private int winH;

	public LayerManager() {
		layers = new Vector();
		setViewWindow(0,0, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}
	
	public void append(Layer l) {
		if (l == null) {
			throw new NullPointerException();
		}
		layers.addElement(l);
	}
	public void insert(Layer l, int index) {
		if (l == null) {
			throw new NullPointerException();
		}
		int currentIndex = layers.indexOf(l);
		if (currentIndex > -1) {
			layers.removeElementAt(currentIndex);
		}
		layers.insertElementAt(l, index);		
	}
	public Layer getLayerAt(int index) {
		return (Layer) layers.elementAt(index);
	}
	public int getSize() {
		return layers.size();
	}
	
	public void remove(Layer l) {
		if (l == null) {
			throw new NullPointerException();
		}
		layers.removeElement(l);
	}
	
	public void setViewWindow(int x, int y, int width, int height) {
		winX = x;
		winY = y;
		winW = width;
		winH = height;
		if (MainApp.verbose) {
			System.out.println("LayerManager.setViewWindow() x=" + x + " y=" + y + " w=" + width + " h=" + height);
		}

	}
	
	public void paint(Graphics g, int x, int y) {
		int trX = g.getTranslateX();
		int trY = g.getTranslateY();
		int size = layers.size();
		if (MainApp.verbose) {
			System.out.println("Pstros:LayerManager.paint() x=" + x + " y=" + y + " wX=" + winX + " wY=" + winY + " wW=" + winW + " wH=" + winH + " tr X=" + trX + " Y=" + trY + " layer count=" + size);
		}
		//ther's no layer to paint -> exit 
		if (size < 1) {
			return;
		}		
		
		//store original clip rectangle
		int origClipX = g.getClipX();
		int origClipY = g.getClipY();
		int origClipW = g.getClipWidth();
		int origClipH = g.getClipHeight();
		int lX, lY, lW, lH;	//layer coordinates
		Layer l;
		int boundX = winX + winW;	//right boundary of the view window -> x 
		int boundY = winY + winH;	//bottom boundary of the view window-> y

		//set the clip that corresponds to our view window		
		g.setClip(/*winX + */ x, /*winY + */y, winW, winH);
		
		//do the view window translation
		g.translate(-winX + x , -winY  + y );
		
		//go through all layers and paint it -> backwards
		for (int i = size-1; i > -1; i--) {
			//get layer and its coordinates
			l = (Layer) layers.elementAt(i);
			/*
			lX = l.getX();
			lY = l.getY();
			lW = l.getWidth();
			lH = l.getHeight();
			//System.out.println("layer : " + i + " x=" + lX + " y=" + lY + " w=" + lW + " h=" + lH);
			//test wether the layer is visible in our view vindow
			//test of the far boundary (right, bottom)
			
			if (lX > boundX || lY > boundY) {
				System.out.println("Skip layer Far: " + i + " x=" + lX + " y=" + lY + " w=" + lW + " h=" + lH);
				continue;
			}
			//test of the near boundary (left, top)
			if (winX > lX + lW || winY > lY + lH) {
				System.out.println("Skip layer Near: " + i + " x=" + lX + " y=" + lY + " w=" + lW + " h=" + lH);
				continue;
			} 
			//the layer intersects with our view window -> paint it
			  
			 */
			l.paint(g);
			//System.out.println(" paint layer=" + i);
		}		

		//set back original translation
		g.translate(winX - x, winY - y); 		
		//set back the original clip rectangle
		g.setClip(origClipX, origClipY, origClipW, origClipH);
		
	}
	
}
