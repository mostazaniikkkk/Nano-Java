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
import nds.pstros.video.NDSRectangle;



public class Form  extends Screen{
	protected static NDSRectangle tmpRect = new NDSRectangle();
	
	private ItemStateListener itemListener;
	private int emuOffsY;
	
	public Form(String title) {
		super();
		setTitle(title);
	}
	
	public Form(String title, Item[] it) {
		this(title);
		setItems(it);
	}
	boolean emuHasStringItem(String text) {
		int max = items.size();
		for (int i = 0; i < max;i++) {
			Object item = items.elementAt(i);
			if (item instanceof StringItem) {
				if (((StringItem)item).getText().equals(text)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public int append(Item item) {
		int position;
		items.addElement(item);
		item.emuSetDisplayable(this);
		position = items.indexOf(item);
		//it's the first item - select it by default
		if (position == 0) {
			item.emuSetActive(true);
			Command itemCommand = item.emuGetDefaultCommand();
			if (itemCommand != null) {
				addCommand(itemCommand);
			}
		} 
		return position;
	}
	
	public int append(String str) {
		Item i = new StringItem(null, str);
		i.emuSetDisplayable(this);
		return append(i);
	}
	

	public int append(Image img) {
		Item i = new ImageItem(null, img, ImageItem.LAYOUT_DEFAULT, null);
		i.emuSetDisplayable(this);
		return append(i);
	}	
	
	public void insert(int itemNum, Item item) {
		item.emuSetDisplayable(this);
		items.insertElementAt(item, itemNum);
	}
	
	public void delete(int itemNum) {
		items.removeElementAt(itemNum);
	}
	
	public void deleteAll() {
		items.removeAllElements();
	}
	
	public void set(int itemNum, Item item) {
		item.emuSetDisplayable(this);
		items.setElementAt(item, itemNum);
	}
	
	public Item get(int itemNum) {
		return (Item) items.elementAt(itemNum);
	}
	
	public void setItemStateListener(ItemStateListener iListener) {
		itemListener = iListener;
	}
	
	public int size() {
		return items.size();
	}
	
	
	private void setItems(Item[] it) {
		if (it == null || it.length < 1) {
			return;
		}
		items.removeAllElements();
		for (int i = 0; i < it.length; i++) {
			it[i].emuSetActive(i==0);
			items.addElement(it[i]);
		}
	}
	
	void emuPaintScreenContent(NDSGraphics g) {
		int size = items.size();
		Item item;
		int y;
		int itemHeight;
		int height;
		int shiftY;
		
		int sliderY = 0;
		int sliderH = 0;

		if (size < 1) {
			return;
		}
		g.getClipBounds(tmpRect);
		y = 0; 
		int i;
		//find selected item
		for (i = 0; i < size && i < selected; i++) {
			item = (Item) items.elementAt(i);
			itemHeight = item.emuGetHeight(g); 
			y += itemHeight; 
		}
		sliderY = y;
		//check height of the selected item
		item = (Item) items.elementAt(i);
		if (item.emuIsMultiElement()) {
			itemHeight = item.emuGetElementHeight(g);
			sliderH = item.emuGetHeight(g);
		} else {
			itemHeight = item.emuGetHeight(g);
			sliderH = itemHeight;
			
		} 


		//System.out.println("selected item=" + selected + " offsY=" + emuOffsY +  " y="  + y + " iH="+ itemHeight + " rH=" + tmpRect.height);
		//now decide where to paint
		if (emuOffsY + y + itemHeight > tmpRect.height) {
			//System.out.println("  -> a");
			emuOffsY = tmpRect.height - y - itemHeight;
		} else
		if (emuOffsY + y < 0) {
			//System.out.println("  -> b");
			emuOffsY = -y;
		}
		//System.out.println("  -> offsY=" + emuOffsY +  " y="  + y + " tmpRect.h=" + tmpRect.height);

		y = 0;
		final int maxHeight = tmpRect.y + tmpRect.height;
		for (i = 0; i < size; i++) {
			item = (Item) items.elementAt(i);
			shiftY  = item.emuGetYSpace(g);
			int posY = emuOffsY + y + shiftY + tmpRect.y;
			//note: we must go through the all of the items to get the total height
			if (posY < tmpRect.y || posY > maxHeight) {
				height = item.emuGetElementHeight(g);
			} else {
				height = item.emuPaint(g, 0, emuOffsY + y + shiftY + tmpRect.y);
			}
			y +=height;
			//g.drawLine(0,emuOffsY + y + tmpRect.y, 512, emuOffsY + y + tmpRect.y);
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
	
	public boolean emuKeyAction(int key, int keyChar, int modifiers, int action) {
		//System.out.println("Form key action! action=" + action + " item state listener=" + itemListener);
		if (action == EmuExecutor.KEY_ACTION_RELEASE) {
			super.emuKeyAction(key, keyChar, modifiers,  action);
			return true;
		}
		Item item = null;
		
		if (selected >= 0) {
			try {
				item = (Item) items.elementAt(selected);
			} catch (Exception e) {
			}
			if (item != null) {
				item.emuSetActive(false);
			}
		}
		int oldSelected = selected;
		super.emuKeyAction(key, keyChar, modifiers, action);

		//remove old item command
		if (oldSelected >= 0 && selected != oldSelected) {
			item = (Item) items.elementAt(oldSelected);
			if (item != null) {
				Command itemCommand = item.emuGetDefaultCommand();
				if (itemCommand != null) {
					removeCommand(itemCommand);
				}
			}
			
		}
		//get new selected item - it might be different one after the keypress
		if (selected >= 0) {
			item = (Item) items.elementAt(selected);
			if (item != null) {
				Command itemCommand = item.emuGetDefaultCommand();
				if (itemCommand != null) {
					addCommand(itemCommand);
				}
				item.emuSetActive(true);
				if (item.emuIsInteractive()) {
					item.emuKeyAction(key, keyChar, action);
				}
			}
		}
		emuUpdateScreen(); 
		return true;
	}
	void emuFirePressed() {
		int max = items.size();
		if (max < 1 || selected < 0 || selected > max-1) {
			return;
		}
		Item item = (Item) items.elementAt(selected);
		if (item.emuIsInteractive()) {
			item.emuActionPressed();
			if (itemListener != null) {
				itemListener.itemStateChanged(item);
			}
		}
	}

}
