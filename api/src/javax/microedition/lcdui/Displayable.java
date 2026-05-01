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

import javax.microedition.lcdui.game.GameCanvas;

import nds.pstros.ConfigData;
import nds.pstros.EmuCanvas;
import nds.pstros.EmuExecutor;
import nds.pstros.MainApp;
import nds.pstros.video.NDSGraphics;


public abstract class Displayable {
	
	Command leftCommand;
	Command rightCommand;
	CommandListener listener;
	
	Form commandForm;
	EmuCommandListener commandFormListener;
	
	int displaybableAreaWidth;
	int displaybableAreaHeight;
	
	boolean shown;
	
	String title;
	Ticker ticker;
	
	public EmuCanvas emuCanvas;
	public int emuKeyStates;	// keys - arrows , soft buttons, fire 
	public int emuNumStates;	// keys - numeric keys


	abstract void emuPaint(NDSGraphics g);


	private boolean isOkCommandtype(Command c) {
		return c.getCommandType() == Command.OK;
	}
	
	public void addCommand(Command cmd) throws RuntimeException {
		if (MainApp.verbose) {
			System.out.println("Displayable.addCommand =" + cmd.getLabel() + " type=" + cmd.getCommandType() + " command=" + cmd );
		}
		if (cmd == null ) {
			throw new NullPointerException("command is null!");
		}
		
		if ( leftCommand == cmd || rightCommand == cmd ) {
			return;
		}
		boolean update = false;
		int commandType = cmd.getCommandType(); 
		
		//there are too many commands on the screen -> use auxiliary commandForm
		if ((leftCommand != null && rightCommand != null) || commandForm != null) {
			if (commandForm == null) {
				commandForm = new Form(title);
				commandForm.append(leftCommand.getLabel());
				commandForm.append(rightCommand.getLabel());
				commandForm.addCommand(new Command("Select", Command.OK,1));
				commandForm.addCommand(new Command("Back", Command.BACK,1));
				commandFormListener = new EmuCommandListener(this, EmuCommandListener.RUN_COMMAND);
				commandFormListener.addCommand(leftCommand);
				commandFormListener.addCommand(rightCommand);
				
				commandForm.listener = commandFormListener;

				leftCommand = new Command("Options", Command.SCREEN, 1);
				rightCommand = null;
				listener =  new EmuCommandListener(commandForm, EmuCommandListener.SET_SCREEN);
				
				update = true;
			} 
			if (!commandForm.emuHasStringItem(cmd.getLabel())) {
				commandForm.append(cmd.getLabel());
				commandFormListener.addCommand(cmd);
			}
		} else 
		if (commandType == Command.OK) {
			update = addOkCommand(cmd);			
		} else
		if (commandType == Command.BACK || commandType == Command.CANCEL || 
			commandType == Command.EXIT ||  commandType == Command.ITEM) {
			update = addBackCommand(cmd);
		} else
		if (leftCommand == null) {
			leftCommand = cmd;
			update = true;
		} else 
		if (rightCommand == null) {
			rightCommand = cmd;
			update = true;
		}
		
		if (update) {
			emuUpdateScreen();
		}
	} 
	private boolean addOkCommand(Command cmd) {
		if (ConfigData.keyLeftSoft == ConfigData.keySelect) {
			if (leftCommand == null || !isOkCommandtype(leftCommand)) {
				leftCommand = cmd;
				return true;
			} 
		} else {
			if (rightCommand == null) {
				rightCommand = cmd;
				return true;
			}
		}
		return false;
	}
	private boolean addBackCommand(Command cmd) {
		if (ConfigData.keyLeftSoft != ConfigData.keySelect) {
			if (leftCommand == null) {
				leftCommand = cmd;
				return true;
			} else {
				return addOkCommand(cmd);
			}
		} else {
			if (rightCommand == null) {
				rightCommand = cmd;
				return true;
			} else {
				return addOkCommand(cmd);
			}
		}
		//return false;
	}
			
	
	public void removeCommand(Command cmd) {
		if (cmd == null) {
			return;
		}
		boolean update = false;
		if (cmd == leftCommand) {
			leftCommand = null;
			update = true;			
		} else
		if (cmd == rightCommand) {
			rightCommand = null;
			update = true;
		}
		if (update) {
			emuUpdateScreen();
		}
	}

	public void setCommandListener(CommandListener l) {
		if (MainApp.verbose) {
			System.out.println("Displayable: setCommandListener to=" + this + " listener=" + l);
		}
		if (commandForm != null) {
			commandFormListener.setCommandListener(l);
		} else {
			listener = l;
		}
	}	


	public int getWidth() {
		return displaybableAreaWidth;
	}
	
	public int getHeight() {
		return displaybableAreaHeight;
	}
	
	protected void sizeChanged(int w,  int h) {
	}
	
	public boolean isShown() {
		//System.out.println("isShown=" + shown + " Displayable=" + this);
		return shown;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String s) {
		title = s;
		emuUpdateScreen();
	}
	
	public Ticker getTicker() {
		return ticker;
	}
	public void setTicker(Ticker t) {
		ticker = t;
		emuUpdateScreen();
	}
	

	public void emuSetEmuCanvas(EmuCanvas c) {
		//System.out.println("Setting emu canvas. Displayable=" + this);
		emuKeyStates = 0;
		emuCanvas = c;
	}	
	void emuUpdateScreen() {
		if (emuCanvas!= null) {
			if (ConfigData.slaveMode) {
				emuCanvas.update();
			} else {
				emuCanvas.repaint();
			}
		}
	}
	
	public Command emuGetLeftCommand() {
		return leftCommand;
	}
	public Command emuGetRightCommand() {
		return rightCommand;
	}
	public boolean emuIsFullScreen() {
		return false;
	}
	
	//returns true if key was processed
	public boolean emuKeyAction(int key, int keyChar, int modifiers,  int action) {
		if (MainApp.verbose) {
			System.out.println("Displayable.emuKeyAction key=" + key + " action=" + action + " ecs=" + Integer.toHexString(emuKeyStates));
		}
		//System.out.println("l=" + leftCommand);
		if (action == EmuExecutor.KEY_ACTION_PRESS) {
			if ((key == Display.keySoftLeft  || key == Display.keySoftLeft2) && modifiers == 0) {
				if ((emuKeyStates & GameCanvas.SOFT_L_PRESSED) == 0) {
					emuKeyStates |= GameCanvas.SOFT_L_PRESSED;
					if (leftCommand != null) {
						if (MainApp.verbose) {
							System.out.println("pressed LEFT, command=" + leftCommand);
						}
						ItemCommandListener itemListener = null;
						Object owner = leftCommand.owner;
						Item item = null;
						if (owner instanceof Item) {
							item = (Item) owner; 
							itemListener = item.emuGetItemCommandListener();
						}
						if (itemListener != null) {
							itemListener.commandAction(leftCommand, item);
						}
						else 
						if (listener != null) {
							listener.commandAction(leftCommand, this);
							return true;
						}
					}
				} 
			} else
			if ((key == Display.keySoftRight || key ==  Display.keySoftRight2) && modifiers == 0) { 
				if ((emuKeyStates & GameCanvas.SOFT_R_PRESSED) == 0) {
					emuKeyStates |= GameCanvas.SOFT_R_PRESSED;
					if (rightCommand != null) {
						if (MainApp.verbose) {
							System.out.println("pressed RIGHT, command=" + rightCommand);
						}
						ItemCommandListener itemListener = null;
						Object owner = rightCommand.owner;
						Item item = null;
						if (owner instanceof Item) {
							item = (Item) owner; 
							itemListener = item.emuGetItemCommandListener();
						}
						if (itemListener != null) {
							itemListener.commandAction(rightCommand, item);
						}
						else
						if (listener != null) {  
							listener.commandAction(rightCommand, this);
						}
						return true;
					}
				}
			} else
			if (key == Display.keyFire2 || key == Display.keyFire ) {
				if ((emuKeyStates & GameCanvas.FIRE_PRESSED) == 0) {
					emuKeyStates |= GameCanvas.FIRE_PRESSED;
				}
			} else
			if (key == Display.keyLeft ) {
				if ((emuKeyStates & GameCanvas.LEFT_PRESSED) == 0) {
					emuKeyStates |= GameCanvas.LEFT_PRESSED;
				}
			} else
			if (key ==  Display.keyRight ) {
				if ((emuKeyStates & GameCanvas.RIGHT_PRESSED) == 0) {
					emuKeyStates |= GameCanvas.RIGHT_PRESSED;
				}
			} else
			if (key ==  Display.keyUp ) {
				if ((emuKeyStates & GameCanvas.UP_PRESSED) == 0) {
					emuKeyStates |= GameCanvas.UP_PRESSED;
				}
			} else
			if (key ==  Display.keyDown ) {
				if ((emuKeyStates & GameCanvas.DOWN_PRESSED) == 0) {
					emuKeyStates |= GameCanvas.DOWN_PRESSED;
				}
			} 
		} else 
		if (action == EmuExecutor.KEY_ACTION_RELEASE) {
			if ((key == Display.keySoftLeft  || key == Display.keySoftLeft2 ) && modifiers == 0) { 
				if ((emuKeyStates & GameCanvas.SOFT_L_PRESSED) != 0) {
					emuKeyStates &= ~GameCanvas.SOFT_L_PRESSED;
					if (listener != null && leftCommand != null) {
						return true;
					}
				}
			} else
			if ((key == Display.keySoftRight || key == Display.keySoftRight2) && modifiers == 0) { 
				if ((emuKeyStates & GameCanvas.SOFT_R_PRESSED) != 0) {
					emuKeyStates &= ~GameCanvas.SOFT_R_PRESSED;
					if (listener != null && leftCommand != null) {
						return true;
					}
				}
			} else
			if (key ==  Display.keyLeft ) {
				if ((emuKeyStates & GameCanvas.LEFT_PRESSED) != 0) {
					emuKeyStates &= ~GameCanvas.LEFT_PRESSED;
				}
			} else
			if (key ==  Display.keyRight ) {
				if ((emuKeyStates & GameCanvas.RIGHT_PRESSED) != 0) {
					emuKeyStates &= ~GameCanvas.RIGHT_PRESSED;
				}
			} else
			if (key ==  Display.keyUp) {
				if ((emuKeyStates & GameCanvas.UP_PRESSED) != 0) {
					emuKeyStates &= ~GameCanvas.UP_PRESSED;
				}
			} else
			if (key ==  Display.keyDown) {
				if ((emuKeyStates & GameCanvas.DOWN_PRESSED) != 0) {
					emuKeyStates &= ~GameCanvas.DOWN_PRESSED;
				}
			} else
			if (key == Display.keyFire2 || key == Display.keyFire ) {
				if ((emuKeyStates & GameCanvas.FIRE_PRESSED) != 0) {
					emuKeyStates &= ~GameCanvas.FIRE_PRESSED;
				}
			}

		}
		return false;
	}
	public void emuSetShown(boolean state) {
		shown = state;
	}
	
	public String toString() {
		return getClass().getName() +  "@" + hashCode();
	}
}
