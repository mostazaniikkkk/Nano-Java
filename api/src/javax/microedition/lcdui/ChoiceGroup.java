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

import java.util.Vector;

import nds.pstros.MainApp;
import nds.pstros.utils.Pair;
import nds.pstros.video.NDSFont;
import nds.pstros.video.NDSGraphics;


public class ChoiceGroup extends Item implements Choice {
	private static final String NONE = "";
	
	
	private int type;
	protected Vector items;
	protected int selected;
	private int fitPolicy;
	private Font font;
	
	private boolean[] flags;
	
	private static final int[] triangleX = {0, 8, 4};
	private static final int[] triangleY = {-4, -4, -0};
	
	public ChoiceGroup(String label, int choiceType) {
		this.label = label;
		type = choiceType;
		items = new Vector();
		selected = -1;
		font = Font.getDefaultFont();
		emuInteractive = true;
		if (choiceType != POPUP) {
			emuMultiElement = true;
		}
	}
	public ChoiceGroup(String label, int choiceType, String[] stringElements, Image[] imageElements) {
		this(label, choiceType);
		int size = stringElements.length;
		Image img;
		for (int i = 0; i < size; i++) {
			img = null;
			if (imageElements != null) {
				img  = imageElements[i];
			} 
			append(stringElements[i], img);
		}
		if (choiceType != POPUP) {
			emuMultiElement = true;
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
		updateFlags(-1);
		return items.indexOf(p);
	}

	public void delete(int elementNum) {
		items.removeElementAt(elementNum);
		selected = 0;
		if (items.size() < 1) {
			selected = -1;
		}
	}
	public void deleteAll() {
		items.removeAllElements();
		selected = -1;
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
	public void insert(int elementNum, String stringPart, Image imagePart) {
		if (selected == -1) {
			selected = 0;
		}
		Pair p = new Pair(stringPart, imagePart);
		items.insertElementAt(p, elementNum);
		updateFlags(elementNum);
		
	}

	public void set(int elementNum, String stringPart, Image imagePart) {
		Pair p = (Pair) items.elementAt(elementNum);
		p.set(stringPart, imagePart);
	}
	public boolean isSelected(int elementNum) {
		return selected == elementNum;
	}
	public int getSelectedIndex() {
		return selected;
	}
	public int getSelectedFlags(boolean[] selectedArray_return) {
		//System.out.println("getSelectedFlags index! cg=" + label);
		int result = 0;
		for (int i = 0; i < flags.length;i++) {
			selectedArray_return[i] = flags[i];
			if (flags[i]) {
				result ++;
			}
		}
		for (int i = flags.length; i < selectedArray_return.length; i++) {
			selectedArray_return[i] = false;
		}
		
		if (type == POPUP || type == EXCLUSIVE && selected >= 0) {
			selectedArray_return[selected] = true;
			return 1;
		}
		return result;
	}
	public void setSelectedIndex(int elementNum, boolean selected) {
		//System.out.println("setSelected index! i=" + elementNum + " active=" + selected + " cg=" + label);
		boolean update = false;
		if (type == MULTIPLE || type == EXCLUSIVE) {
			flags[elementNum] = selected;
			update = true;
		} else 
		if (selected && elementNum >= 0 && elementNum < items.size() ){
			this.selected = elementNum;
			update = true;
		}
		if (update) {
			emuUpdateScreen();
		}
	}
	public void setSelectedFlags(boolean[] selectedArray) {
		if (flags == null) {
			flags = new boolean[items.size()];
		}
		if (type == MULTIPLE || type == EXCLUSIVE) {
			for (int i = 0; i < flags.length; i++) {
				flags[i] = selectedArray[i];
			}
		} else {
			for (int i = 0; i < flags.length; i++) {
				if (selectedArray[i]) {
					selected = i;
					break;
				}
			}
			
		}
		emuUpdateScreen();
	}

	public void setFitPolicy(int fitPolicy) {
		this.fitPolicy = fitPolicy;	
	}
	public int getFitPolicy() {
		return fitPolicy;
	}
	public void setFont(int elementNum, Font font) {
		if (font == null) {
			font = Font.getDefaultFont();
		} else {
			this.font = font;
		}
	}
	public Font getFont(int elementNum) {
		return font;
	}
	
	private void updateFlags(int startIndex) {
		int size = items.size();
		if (size == 1) {
			flags = new boolean[1];	
			return;
		} 
		boolean[] tmpFlags = new boolean[size];
		if (startIndex < 0) {
			System.arraycopy(flags,0, tmpFlags, 0, flags.length);
		} else {
			if (startIndex > 0) {
				System.arraycopy(flags,0, tmpFlags, 0, startIndex);
			}
			System.arraycopy(flags,startIndex, tmpFlags, startIndex+1, flags.length - startIndex);
		}
		flags = tmpFlags; 
	}
	
	int emuPaint(NDSGraphics g, int x, int y) {
		NDSFont font = g.getFont();
		int height = font.getSize() + 2;
		int width = Display.WIDTH - x - 2;
		int origY = y;
		int paintColor = COLOR_BLACK; 
		if (emuActive) {
			paintColor = COLOR_RED;
		}
		g.setColor(paintColor);
		if (label != null) {
			g.drawString(label, x+2, y+1);
			y += height + 2;
		}
		Pair p = (Pair) items.elementAt(selected);
		if (type == POPUP) {
			if (emuActive) {
				g.setColor(COLOR_HIGHLIGT);
				g.fillRect(x,y-height+2, Display.WIDTH - 4, height + 2);
				g.setColor(paintColor);
			}
			g.drawString((String)p.getFirst(), x+2, y+1);
			g.drawRect(x,y-height+2, Display.WIDTH - 4, height + 2);
			//FIXME! NDS
			/*
			g.translate(Display.WIDTH - 16, y);
			g.fillPolygon(triangleX, triangleY, 3);
			g.translate(-(Display.WIDTH - 16), -y);
			*/
			y += height + 2;
			y += 4;
		} else
		if (type == MULTIPLE) {
			//System.out.println("has command ? " + listener);
			int size = items.size();
			for (int i = 0; i < size; i++) {
				p = (Pair) items.elementAt(i);
				//draw background rectangle
				if (i == selected && emuActive) {
					g.setColor(COLOR_HIGHLIGT);
					g.fillRect(x,y-height+2, Display.WIDTH - 4, height + 2);
					g.setColor(paintColor);
					//g.drawRect(x,y-height+2, Display.WIDTH - 4, height + 2);
				}
				//draw checkbox
				g.drawRect(x+2, y-height+4, 8, height -2);
				//draw checkbox active
				if (flags[i]) {
					g.drawLine(x+4, y-height +6, x+8, y );
					g.drawLine(x+4, y, x+8, y -height +6 );
					//g.fillRect(x+4, y-height +7, 5, height - 7);
				}
				g.drawString((String)p.getFirst(), x+12, y+1);
				y+= height + 2;
			}
			y +=2;
		} else
		if (type == EXCLUSIVE) {
			//System.out.println("has command ? " + listener);
			int size = items.size();
			for (int i = 0; i < size; i++) {
				p = (Pair) items.elementAt(i);
				//draw background rectangle
				if (i == selected && emuActive) {
					g.setColor(COLOR_HIGHLIGT);
					g.fillRect(x,y-height+2, Display.WIDTH - 4, height + 2);
					g.setColor(paintColor);
					//g.drawRect(x,y-height+2, Display.WIDTH - 4, height + 2);
				}

				//draw checkbox
				g.drawRect(x+2, y-height+4, 8, height -2);
				//draw checkbox active
				if (flags[i]) {
					g.fillRect(x+4, y-height +7, 5, height - 7);
				}
				g.drawString((String)p.getFirst(), x+12, y+1);
				y+= height + 2;
			}
			y +=2;
		}

		return y - origY;
	}
	
	int emuGetHeight(NDSGraphics g) {
		NDSFont font = g.getFont();
		int height = font.getSize();
		int h = height + 4;
		if (type == POPUP) {
			height = h;
			if (label != null && label.length() > 0) {
				height += h;
			}
			height += 4;
		} else
		if (type == MULTIPLE || type == EXCLUSIVE) {
			height = 0;
			if (label != null && label.length() > 0) {
				height = h;
			}
			int size = items.size();
			height += (h * size); 
			height += 2;
		}

		return height;
	}
	int emuGetElementHeight(NDSGraphics g) {
		//if this item is not multi element use full height as the element height
		if (!emuMultiElement) {
			return emuGetHeight(g);
		}
		NDSFont font = g.getFont();
		int height = font.getSize();

		int h = height + 4;
		height = 0;
		if (label != null && label.length() > 0) {
			height = h;
		}
		height += (h * (selected +1));
		height += 2; 
		
		return height;
	}
	int emuGetYSpace(NDSGraphics g) {
		NDSFont font = g.getFont();
		return font.getSize() + 3;
	}
	void emuActionPressed() {
		boolean update = false;
		if (type == POPUP) {
			int max = items.size();
			selected ++;
			if (selected >= max) {
				selected = 0;
			} 
			update = true;
		} else
		if (type == MULTIPLE) {
			flags[selected] = !flags[selected];
		} else 
		if (type == EXCLUSIVE) {
			for (int i = 0; i < flags.length; i++) {
				flags[i] = false;
			}
			flags[selected] = true;
		}
		
		if (update) {
			emuUpdateScreen();
		}
	}	
		
	boolean emuMoveSelectionDown() {
		if (type == MULTIPLE || type == EXCLUSIVE) {
			if (selected == items.size()-1) {
				return true;
			} else {
				selected++;
				emuUpdateScreen();
			}
			return false;
		}
		return true;
	}
	boolean emuMoveSelectionUp() {
		if (type == MULTIPLE || type == EXCLUSIVE) {
			if (selected == 0) {
				return true;
			} else {
				selected--;
				emuUpdateScreen();
			}
			return false;
		}
		return true;
	}

}
