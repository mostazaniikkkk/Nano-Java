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
import nds.pstros.video.NDSGraphics;

public class Alert extends Screen{
	public static final Command DISMISS_COMMAND = new Command("Dismiss", Command.OK, 0);
	public static final int FOREVER = -2;
	
	private String text;
	private Image image;
	private AlertType alertType;
	private int timeout;
	
	private Displayable emuNextDisplayable;
	
	public Alert(String title) {
		this(title, null, null, null);
	}
	
	public Alert(String title, String alertText, Image alertImage, AlertType alertType) {
		setTitle(title);
		this.text = alertText;
		this.image = alertImage;
		this.alertType = alertType;
		timeout = 3000;
		if (MainApp.verbose) {
			System.out.println("Alert created: type=" + alertType.type + " title=" + title);
		}
		leftCommand = DISMISS_COMMAND;
	}

	public int getDefaultTimeout() {
		return 3000;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int time) {
		if (MainApp.verbose) {
			System.out.println("Alert: setTimeout=" + time);
		} 
		timeout = time;
	}
	public AlertType getType() {
		return alertType;
	}
	public void setType(AlertType type) {
		if (MainApp.verbose) {
			System.out.println("Alert: setType=" + alertType);
		}
		alertType = type;
	}
	public String getString() {
		return text;
	}
	public void setString(String str) {
		text = str;
	}
	public Image getImage() {
		return image;
	}
	public void setImage(Image img) {
		image = img;
	}
	
	public void addCommand(Command cmd) {
		if (leftCommand == DISMISS_COMMAND) {
			leftCommand = null;
		}
		super.addCommand(cmd);
	}
	public void removeCommand(Command cmd) {
		leftCommand = DISMISS_COMMAND;
	}
	public void setCommandListener(CommandListener l) {
		super.setCommandListener(l);
	}
	
	void emuPaintScreenContent(NDSGraphics g) {
		int y = 28; //18
		if (image != null) {
			g.drawImage(image.emuGetImage(0), (Display.WIDTH - image.getWidth()) / 2, y);
			y += image.getHeight() + 4; 
		}
		if (text != null) {
			Item.emuDrawMultiLine(g, text, 0, y, true);
		}
		
	} 

	void emuSetNextDisplayable(Displayable d) {
		emuNextDisplayable = d;
		EmuCommandListener ecl = new EmuCommandListener(emuNextDisplayable, EmuCommandListener.SET_SCREEN);
		setCommandListener(ecl);
	}
	Displayable emuGetNextDisplayable() {
		return emuNextDisplayable;
	}
	

}
