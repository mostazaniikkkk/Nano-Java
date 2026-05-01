package javax.microedition.lcdui;

import java.util.Vector;

import nds.pstros.MainApp;


class EmuCommandListener implements CommandListener {
	public static final int SET_SCREEN = 0;
	public static final int RUN_COMMAND = 1;
	
	CommandListener listener;
	Displayable displayable;
	int type; 
	Vector commands;
	
	public EmuCommandListener (Displayable d, int type) {
		displayable = d;
		this.type = type;
	}
	

	public void commandAction(Command c, Displayable d) {
		//System.out.println("Command Action ! command=" + c + " displayable=" + d + " type=" + type);
		if (type == SET_SCREEN) {
			Display display = Display.getDisplay(MainApp.midlet);
			if (display != null) {
				display.emuBackupFrame();
				display.setCurrent(displayable);
			}
		} else
		if (type == RUN_COMMAND) {
			if (c.getCommandType() == Command.BACK) {
				Display display = Display.getDisplay(MainApp.midlet);
				if (display != null) {
					display.setCurrent(displayable);
					display.emuRestoreFrame();
					display.emuRepaintDisplay();
				}
			} else {
				int index = 0;
				if (d instanceof Screen) {
					index = ((Screen) d).selected;
				}
				//System.out.println("selected index =" + index);
				Command cmd = (Command) commands.elementAt(index);
				listener.commandAction(cmd, displayable);
			}
		}
	}
	
	public void setCommandListener(CommandListener l) {
		listener = l;
	}
	
	public boolean addCommand(Command cmd) {
		if (commands == null) {
			commands = new Vector();
		}
		int index = commands.indexOf(cmd);
		//command already exists
		if (index >= 0) {
			return false;
		}
		commands.addElement(cmd);
		return true;
	}

}
