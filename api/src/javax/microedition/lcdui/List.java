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
import nds.pstros.utils.Pair;
import nds.pstros.video.NDSFont;
import nds.pstros.video.NDSGraphics;
import nds.pstros.video.NDSRectangle;


public class List extends Screen  implements Choice{
	public static final Command SELECT_COMMAND = new Command("Select", Command.SCREEN, 0);
	
	private static NDSRectangle tmpRect = new NDSRectangle();
	
	private int type;
	private Command selectCommand;
	private int emuOffsY;
	
	public List() {
		super();
		addCommand(SELECT_COMMAND);
	}
	
	public List(String title, int listType) {
		this();
		setTitle(title);
		type = listType;
	}
	
	public List(String title, int listType, String[] stringElements, Image[] imageElements) {
		this(title, listType);
		int size = stringElements.length;
		Image img;
		for (int i = 0; i < size; i++) {
			img = null;
			if (imageElements != null) {
				img  = imageElements[i];
			} 
			append(stringElements[i], img);
		}
	}
	

	public int append(String stringPart, Image imagePart) {
		if (selected == -1) {
			selected = 0;
		}
		if (MainApp.verbose) {
			System.out.println("append: " + stringPart + " image=" + imagePart);
		}
		Pair p = new Pair(stringPart, imagePart);
		items.addElement(p);
		return items.indexOf(p);
	}

	public void delete(int elementNum) {
		items.removeElementAt(elementNum);
		selected = 0;
		if (items.size() < 1) {
			selected = 0;
		}
	}
	public void deleteAll() {
		items.removeAllElements();
		selected = 0;
	}

	public int getSelectedFlags(boolean[] selectedArray_return) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getSelectedIndex() {
		return selected;
	}

	public void insert(int elementNum, String stringPart, Image imagePart) {
		if (!MainApp.verbose) {
			System.out.println("append: " + stringPart + " image=" + imagePart);
		}
		Pair p = new Pair(stringPart, imagePart);
		items.insertElementAt(p, elementNum);
	}

	public boolean isSelected(int elementNum) {
		return selected == elementNum;
	}

	public void set(int elementNum, String stringPart, Image imagePart) {
		Pair p = (Pair) items.elementAt(elementNum);
		p.set(stringPart, imagePart);
	}

	public void setSelectedFlags(boolean[] selectedArray) {
		// TODO Auto-generated method stub

	}

	public void setSelectedIndex(int elementNum, boolean selected) {
		if (this.selected == elementNum && selected == false) {
			this.selected = 0;
		} else {
			this.selected = elementNum;
		}
	}
	public void setSelectCommand(Command c) {
		selectCommand = c;
	}

	public int size() {
		return items.size();
	}
	public String getString(int elementNum) {
		Pair pair = (Pair) items.elementAt(elementNum);
		if (pair == null) {
			return null;
		} 
		return (String) pair.getFirst();
	}
	public Image getImage(int elementNum) {
		Pair pair = (Pair) items.elementAt(elementNum);
		if (pair == null) {
			return null;
		} 
		return (Image) pair.getSecond();
	}
	void emuFirePressed() {
		int max = items.size();
		if (max < 1 || selected < 0 || selected > max-1) {
			return;
		}
		if (selectCommand != null) {
			if (listener != null) {
				listener.commandAction(selectCommand, this);
			}
		}
	}
	
	void emuPaintScreenContent(NDSGraphics g) {
		int size = items.size();
		Pair item;
		int y;
		int sliderY = 0;
		int sliderH = 0;

		
		NDSFont font = g.getFont();
		int itemHeight = font.getSize() + 4;

		g.getClipBounds(tmpRect);
		//System.out.println("selected=" + selected + " offsY=" + emuOffsY);
		y =  selected * itemHeight;
		sliderY = y;
		sliderH = itemHeight;
		
		if (emuOffsY + y + itemHeight > tmpRect.height) {
			emuOffsY = tmpRect.height - y - itemHeight;
		} else
		if (emuOffsY + y < 0) {
			emuOffsY = -y;
		}
 
		int i;
		y = 0;
		//paint items
		for (i = 0; i < size; i++) {
			item = (Pair) items.elementAt(i);
			y += emuPaintItem(g, 0, emuOffsY + y + tmpRect.y + itemHeight, (String)item.getFirst(), (Image) item.getSecond(), i == selected, itemHeight);
		}
		//paint slider
		if (y > tmpRect.height) {
			g.setColor(Item.COLOR_HIGHLIGT);
			g.fillRect(Display.WIDTH - 2,tmpRect.y, 2, tmpRect.height);
			
			int posY = (sliderY * tmpRect.height) / y;
			int sliderSize = ((sliderH * tmpRect.height) / y) + 1;
			
			g.setColor(Item.COLOR_RED);
			g.fillRect(Display.WIDTH - 2,tmpRect.y + posY, 2, sliderSize);
		}

		
	}
	private int emuPaintItem(NDSGraphics g, int x, int y, String text, Image icon, boolean selected, int itemHeight) {
		if (text == null) {
			return 0;
		}
		if (selected) {
			//System.out.println("Selected = " + text);
			g.setColor(Item.COLOR_HIGHLIGT);
			g.fillRect(x,y-itemHeight, Display.WIDTH - 4, itemHeight );
			g.setColor(Item.COLOR_RED); 	
		} else {
			g.setColor(Item.COLOR_BLACK);
		} 	
		if (icon != null) {
			g.drawImage(icon.emuGetImage(0), x+2, y-itemHeight);
			x += icon.getWidth();
		} 
		g.drawString(text, x +2, y-2);
		
		return itemHeight;
	} 
	

}
