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



public abstract class Canvas extends Displayable  {
	public static final int UP = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 5;
	public static final int DOWN = 6;
	public static final int FIRE = 8;
	public static final int GAME_A = 9;
	public static final int GAME_B = 10;
	public static final int GAME_C = 11;
	public static final int GAME_D = 12;
	
	public static final int SOFT_L = 24;
	public static final int SOFT_C = 25;
	public static final int SOFT_R = 26;
	
	public static final int KEY_NUM0 = 48;
	public static final int KEY_NUM1 = 49;
	public static final int KEY_NUM2 = 50;
	public static final int KEY_NUM3 = 51;
	public static final int KEY_NUM4 = 52;
	public static final int KEY_NUM5 = 53;
	public static final int KEY_NUM6 = 54;
	public static final int KEY_NUM7 = 55;
	public static final int KEY_NUM8 = 56;
	public static final int KEY_NUM9 = 57;
	
	public static final int KEY_STAR = 42;
	public static final int KEY_POUND = 35;
	
	private static final int MASK_KEY_NUM0 = 1 ; 
	private static final int MASK_KEY_NUM1 = 1 << (1); 
	private static final int MASK_KEY_NUM2 = 1 << (2); 
	private static final int MASK_KEY_NUM3 = 1 << (3); 
	private static final int MASK_KEY_NUM4 = 1 << (4); 
	private static final int MASK_KEY_NUM5 = 1 << (5); 
	private static final int MASK_KEY_NUM6 = 1 << (6); 
	private static final int MASK_KEY_NUM7 = 1 << (7); 
	private static final int MASK_KEY_NUM8 = 1 << (8); 
	private static final int MASK_KEY_NUM9 = 1 << (9); 
	private static final int MASK_KEY_STAR = 1 << (10); 
	private static final int MASK_KEY_CROSS = 1 << (11); 
	
	private boolean fullScreen;
	
	private static final int DEFAULT_CONSOLE_SIZE = 24;
	
	protected Canvas() {
		displaybableAreaWidth = Display.WIDTH;
		//emuCreateBridge();
		emuSetScreenMode(false);
		EmuCanvas ec = EmuCanvas.getInstance();
		//if (ec != null) {
		//	ec.setBridge(emuLcduiBridge);
		//} 	
	}

	public boolean emuIsFullScreen() {
		return fullScreen;
	}
	
	public boolean isDoubleBuffered() {
		return true;
	}
	
	public boolean hasPointerEvents(){
		return true;
	}
	
	public boolean hasPointerMotionEvents(){
		return true;
	}
	
	public boolean hasRepeatEvents() {
		return true;
	}
	
	public int getKeyCode(int gameAction) {
		//System.out.println("getKeyCode called! gameAction=" + gameAction);
		switch(gameAction) {
			case UP: return ConfigData.keyUpArrow;
			case DOWN: return ConfigData.keyDownArrow;
			case LEFT: return ConfigData.keyLeftArrow;
			case RIGHT: return ConfigData.keyRightArrow;
			case FIRE: return ConfigData.keyCenterSoft;
			case GAME_A: return KEY_NUM1;
			case GAME_B: return KEY_NUM3;
			case GAME_C: return KEY_NUM7;
			case GAME_D: return KEY_NUM9;
			default: return 0;		
		}
	}
	
	public String getKeyName(int keyCode) {
		if (MainApp.verbose) {
			System.out.println("Pstros: Canvas. getKeyName called. keyCode=" + keyCode);
		}
		return "key";
	}
	
	public int getGameAction(int keyCode) {
		//System.out.println("getGameAction called! keyCode=" + keyCode);
		if (keyCode == ConfigData.keyUpArrow || keyCode == KEY_NUM2) {
			return UP;
		} else 
		if (keyCode == ConfigData.keyDownArrow || keyCode == KEY_NUM8) {
			return DOWN;
		} else
		if (keyCode == ConfigData.keyLeftArrow || keyCode == KEY_NUM4) {
			return LEFT;
		} else
		if (keyCode == ConfigData.keyRightArrow || keyCode == KEY_NUM6) {
			return RIGHT;
		} else
		if (keyCode == ConfigData.keyCenterSoft || keyCode == KEY_NUM5) {
			return FIRE;
		} else
		if (keyCode == KEY_NUM1) {
			return GAME_A;
		}else
		if (keyCode == KEY_NUM3) {
			return GAME_B;
		}else
		if (keyCode == KEY_NUM7) {
			return GAME_C;
		}else
		if (keyCode == KEY_NUM9) {
			return GAME_D;
		}
		
		return 0;
	}

	public void setFullScreenMode(boolean mode) {
		//newMode is true when FulScreen is requested AND bottom console is not specified (eq 0)
		boolean newMode = (mode && ConfigData.fullScreenSupported);
		//System.out.println("setFullScreen=" + mode + " enableFull=" + newMode + " fs supported=" + ConfigData.fullScreenSupported);
		if (fullScreen != newMode) {
			emuSetScreenMode(newMode);
		}
	}
	
	private void emuSetScreenMode(boolean mode) {
		fullScreen = mode;
		if (fullScreen) {
			//hide consoles in full screen
			displaybableAreaHeight = Display.HEIGHT;
		} else {
						
			int consoleSize = ConfigData.getConsoleSize();
			//if the console size is not set by user - create default bottom
			//console size - 24pix height
			if (consoleSize < 1) {
				consoleSize = DEFAULT_CONSOLE_SIZE;
				ConfigData.bottomConsoleHeight = consoleSize;
			}
			
			displaybableAreaHeight = Display.HEIGHT - consoleSize;
		}
	}

	protected void keyPressed(int keyCode){};
	protected void keyRepeated(int keyCode) {};
	protected void keyReleased(int keyCode){};
	protected void pointerPressed(int x,int y) {};
	protected void pointerReleased(int x, int y) {};
	protected void pointerDragged(int x, int y) {};
	protected void showNotify(){};
	protected void hideNotify(){};
	protected abstract void paint(Graphics g);
	
	void emuPointerPressed(int x, int y) {
		pointerPressed(x,y );	
	}
	void emuPointerReleased(int x, int y) {
		pointerReleased(x,y);	
	}
	void emuPointerDragged(int x, int y) {
		pointerDragged(x,y);	
	}
	
	Graphics emuGetGraphics() {
		if (emuCanvas != null) {
			return emuCanvas.getDeviceGraphics();
		} 
		
		emuSetEmuCanvas(EmuCanvas.getInstance());
		if (emuCanvas != null) {
			return emuCanvas.getDeviceGraphics();
		} 
		System.out.println("Pstros: GameCanvas.getGraphics() - emuCanvas is null!");
		return null;
	}
	int emuGetKeyStates() {
		return emuKeyStates;
	}
	
	void emuFlushGraphics() {
		if (emuCanvas != null) {
			emuCanvas.flushGraphics(null);
			emuCanvas.checkPause();
		}
	}
	
	void emuCleanKeys() {
		emuNumStates = 0;
	}
	
	public boolean emuKeyAction(int key, int keyChar, int modifiers,  int action) {
		int origKeyStates = emuKeyStates;
		boolean processed =  super.emuKeyAction(key, keyChar, modifiers,  action);
		//System.out.println("processed =" + processed);
		if (action == EmuExecutor.KEY_ACTION_PRESS) {
			//originally there was an "switch", but since the keys are configurable
			//we must rely on the "if" statements

			if (key == Display.keyFire || key == Display.keyFire2) {
				if ((origKeyStates & GameCanvas.FIRE_PRESSED) == 0) {
					keyPressed(ConfigData.configActive? ConfigData.keyCenterSoft:FIRE);
				} else {
					keyRepeated(ConfigData.configActive? ConfigData.keyCenterSoft:FIRE);
				}
			} else 
			if (key == Display.keyLeft) {
				if ((origKeyStates & GameCanvas.LEFT_PRESSED) == 0) {
					keyPressed(ConfigData.configActive? ConfigData.keyLeftArrow:LEFT);
				} else {
					keyRepeated(ConfigData.configActive? ConfigData.keyLeftArrow:LEFT);
				}
			}else
			if (key == Display.keyRight) {
				if ((origKeyStates & GameCanvas.RIGHT_PRESSED) == 0) {
					keyPressed(ConfigData.configActive? ConfigData.keyRightArrow: RIGHT);
				} else {
					keyRepeated(ConfigData.configActive? ConfigData.keyRightArrow: RIGHT);
				}
			}else
			if (key == Display.keyUp) {
				if ((origKeyStates & GameCanvas.UP_PRESSED) == 0) {
					keyPressed(ConfigData.configActive? ConfigData.keyUpArrow:UP);
				} else {
					keyRepeated(ConfigData.configActive? ConfigData.keyUpArrow:UP);
				}
			}else
			if (key == Display.keyDown) {
				if ((origKeyStates & GameCanvas.DOWN_PRESSED) == 0) {
					keyPressed(ConfigData.configActive? ConfigData.keyDownArrow:DOWN);
				} else {
					keyRepeated(ConfigData.configActive? ConfigData.keyDownArrow:DOWN);
				}
			}else
			if ((key == Display.keySoftLeft || key == Display.keySoftLeft2) && modifiers == 0) {
				//if not processed by displayable - ie. no system soft button exist 
				if (!processed) {
					if ((origKeyStates & GameCanvas.SOFT_L_PRESSED) == 0) {
						keyPressed(ConfigData.configActive? ConfigData.keyLeftSoft:SOFT_L);
					} else {
						keyRepeated(ConfigData.configActive? ConfigData.keyLeftSoft:SOFT_L);
					}
				}
			}else
			if ((key == Display.keySoftRight || key == Display.keySoftRight2) && modifiers == 0){
				//if not processed by displayable - ie. no system soft button exist 
				if (!processed) {
					if ((origKeyStates & GameCanvas.SOFT_R_PRESSED) == 0) {
						keyPressed(ConfigData.configActive? ConfigData.keyRightSoft:SOFT_R);
					} else {
						keyRepeated(ConfigData.configActive? ConfigData.keyRightSoft:SOFT_R);
					}
				}
			} else
			//numeric keyboard
			if (key == Display.keyNum5 ) {
				if ((emuNumStates & MASK_KEY_NUM5) == 0) {
					emuNumStates |= MASK_KEY_NUM5;
					keyPressed(KEY_NUM5);
				} else {
					keyRepeated(KEY_NUM5);
				}
			} else
			if (key == Display.keyNum1 ) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM7) == 0) {
						emuNumStates |= MASK_KEY_NUM7;
						keyPressed(KEY_NUM7);
					} else {
						keyRepeated(KEY_NUM7);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM1) == 0) {
						emuNumStates |= MASK_KEY_NUM1;
						keyPressed(KEY_NUM1);
					} else {
						keyRepeated(KEY_NUM1);
					}
				}
			} else
			if (key == Display.keyNum2) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM8) == 0) {
						emuNumStates |= MASK_KEY_NUM8;
						keyPressed(KEY_NUM8);
					} else {
						keyRepeated(KEY_NUM8);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM2) == 0) {
						emuNumStates |= MASK_KEY_NUM2;
						keyPressed(KEY_NUM2);
					} else {
						keyRepeated(KEY_NUM2);
					} 
				}
			} else 
			if (key == Display.keyNum3) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM9) == 0) {
						emuNumStates |= MASK_KEY_NUM9;
						keyPressed(KEY_NUM9);
					} else {
						keyRepeated(KEY_NUM9);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM3) == 0) {
						emuNumStates |= MASK_KEY_NUM3;
						keyPressed(KEY_NUM3);
					} else {
						keyRepeated(KEY_NUM3);
					}
				}
			} else
			if (key == Display.keyNum4) {
				if ((emuNumStates & MASK_KEY_NUM4) == 0) {
					emuNumStates |= MASK_KEY_NUM4;
					keyPressed(KEY_NUM4);
				} else {
					keyRepeated(KEY_NUM4);
				}
			} else
			if (key == Display.keyNum6) {
				if ((emuNumStates & MASK_KEY_NUM6) == 0) {
					emuNumStates |= MASK_KEY_NUM6;
					keyPressed(KEY_NUM6);
				} else {
					keyRepeated(KEY_NUM6);
				}
			} else
			if (key == Display.keyNum7) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM1) == 0) {
						emuNumStates |= MASK_KEY_NUM1;
						keyPressed(KEY_NUM1);
					} else {
						keyRepeated(KEY_NUM1);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM7) == 0) {
						emuNumStates |= MASK_KEY_NUM7;
						keyPressed(KEY_NUM7);
					} else {
						keyRepeated(KEY_NUM7);
					} 
				}
			} else
			if (key == Display.keyNum8) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM2) == 0) {
						emuNumStates |= MASK_KEY_NUM2;
						keyPressed(KEY_NUM2);
					} else {
						keyRepeated(KEY_NUM2);
					} 
				} else {
					if ((emuNumStates & MASK_KEY_NUM8) == 0) {
						emuNumStates |= MASK_KEY_NUM8;
						keyPressed(KEY_NUM8);
					} else {
						keyRepeated(KEY_NUM8);
					}
				}
			} else
			if (key == Display.keyNum9) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM3) == 0) {
						emuNumStates |= MASK_KEY_NUM3;
						keyPressed(KEY_NUM3);
					} else {
						keyRepeated(KEY_NUM3);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM9) == 0) {
						emuNumStates |= MASK_KEY_NUM9;
						keyPressed(KEY_NUM9);
					} else {
						keyRepeated(KEY_NUM9);
					}
				}
			}else
			if (key ==  Display.keyNum0) {
				if ((emuNumStates & MASK_KEY_NUM0) == 0) {
					emuNumStates |= MASK_KEY_NUM0;
					keyPressed(KEY_NUM0);
				} else {
					keyRepeated(KEY_NUM0);
				}
			}else
			if (key == Display.keyCross) {
				if ((emuNumStates & MASK_KEY_CROSS) == 0) {
					emuNumStates |= MASK_KEY_CROSS;
					keyPressed(KEY_POUND);
				} else {
					keyRepeated(KEY_POUND);
				}
			} else
			if (key == Display.keyStar) {
				if ((emuNumStates & MASK_KEY_STAR) == 0) {
					emuNumStates |= MASK_KEY_STAR;
					keyPressed(KEY_STAR);
				} else {
					keyRepeated(KEY_STAR);
				}
			}else
			//Virtual keys
			if (key == Display.keyVirtual1) {
				keyPressed(1000000);
			}
			if (key == Display.keyVirtual2) {
				keyPressed(1000001);
			} else
			if (key == Display.keyVirtual3) {
				keyPressed(1000002);
			} 
			
		} else
		if (action == EmuExecutor.KEY_ACTION_RELEASE) {
			if (key == Display.keyFire || key == Display.keyFire2) {
				if ((origKeyStates & GameCanvas.FIRE_PRESSED) != 0) {
					keyReleased(ConfigData.configActive? ConfigData.keyCenterSoft:FIRE);
				}
			} else
			if (key == Display.keyLeft ) {
				if ((origKeyStates & GameCanvas.LEFT_PRESSED) != 0) {
					keyReleased(ConfigData.configActive? ConfigData.keyLeftArrow:LEFT);
				}
			} else
			if (key == Display.keyRight ) {
				if ((origKeyStates & GameCanvas.RIGHT_PRESSED) != 0) {
					keyReleased(ConfigData.configActive? ConfigData.keyRightArrow:RIGHT);
				}
			} else
			if (key == Display.keyUp ) {
				if ((origKeyStates & GameCanvas.UP_PRESSED) != 0) {
					keyReleased(ConfigData.configActive? ConfigData.keyUpArrow:UP);
				}
			} else
			if (key ==  Display.keyDown ) {
				if ((origKeyStates & GameCanvas.DOWN_PRESSED) != 0) {
					keyReleased(ConfigData.configActive? ConfigData.keyDownArrow:DOWN);
				}
			} else
			
			if ((key == Display.keySoftLeft  || key == Display.keySoftLeft2) && modifiers == 0) {
				if ((origKeyStates & GameCanvas.SOFT_L_PRESSED) != 0) {
					keyReleased(ConfigData.configActive? ConfigData.keyLeftSoft:SOFT_L);
				} 
			} else
			if ((key == Display.keySoftRight  || key == Display.keySoftRight2) && modifiers == 0) {
				if ((origKeyStates & GameCanvas.SOFT_R_PRESSED) != 0) {
					keyReleased(ConfigData.configActive? ConfigData.keyRightSoft:SOFT_R);
				} 
			} else
			//numeric keyboard
			if (key ==  Display.keyNum5 ) {
				if ((emuNumStates & MASK_KEY_NUM5) != 0) {
					emuNumStates &= ~MASK_KEY_NUM5;
					keyReleased(KEY_NUM5);
				}
			} else
			if (key ==  Display.keyNum0 ) {
				if ((emuNumStates & MASK_KEY_NUM0) != 0) {
					emuNumStates &= ~MASK_KEY_NUM0;
					keyReleased(KEY_NUM0);
				}
			} else
			if (key ==  Display.keyNum1 ) {
				if (ConfigData.numKeySwap) {	
					if ((emuNumStates & MASK_KEY_NUM7) != 0) {
						emuNumStates &= ~MASK_KEY_NUM7;
						keyReleased(KEY_NUM7);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM1) != 0) {
						emuNumStates &= ~MASK_KEY_NUM1;
						keyReleased(KEY_NUM1);
					} 
				}
			} else
			if (key ==  Display.keyNum2) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM8) != 0) {
						emuNumStates &= ~MASK_KEY_NUM8;
						keyReleased(KEY_NUM8);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM2) != 0) {
						emuNumStates &= ~MASK_KEY_NUM2;
						keyReleased(KEY_NUM2);
					}
				}
			} else
			if (key ==  Display.keyNum3 ) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM9) != 0) {
						emuNumStates &= ~MASK_KEY_NUM9;
						keyReleased(KEY_NUM9);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM3) != 0) {
						emuNumStates &= ~MASK_KEY_NUM3;
						keyReleased(KEY_NUM3);
					}
				}
			} else
			if (key ==  Display.keyNum4 ) {
				if ((emuNumStates & MASK_KEY_NUM4) != 0) {
					emuNumStates &= ~MASK_KEY_NUM4;
					keyReleased(KEY_NUM4);
				}
			} else
			if (key == Display.keyNum6 ) {
				if ((emuNumStates & MASK_KEY_NUM6) != 0) {
					emuNumStates &= ~MASK_KEY_NUM6;
					keyReleased(KEY_NUM6);
				}
			} else
			if (key ==  Display.keyNum7) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM1) != 0) {
						emuNumStates &= ~MASK_KEY_NUM1;
						keyReleased(KEY_NUM1);
					} 
				} else {
					if ((emuNumStates & MASK_KEY_NUM7) != 0) {
						emuNumStates &= ~MASK_KEY_NUM7;
						keyReleased(KEY_NUM7);
					}
				}
			} else
			if (key == Display.keyNum8 ) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM2) != 0) {
						emuNumStates &= ~MASK_KEY_NUM2;
						keyReleased(KEY_NUM2);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM8) != 0) {
						emuNumStates &= ~MASK_KEY_NUM8;
						keyReleased(KEY_NUM8);
					}
				}
			} else
			if (key ==  Display.keyNum9 ) {
				if (ConfigData.numKeySwap) {
					if ((emuNumStates & MASK_KEY_NUM3) != 0) {
						emuNumStates &= ~MASK_KEY_NUM3;
						keyReleased(KEY_NUM3);
					}
				} else {
					if ((emuNumStates & MASK_KEY_NUM9) != 0) {
						emuNumStates &= ~MASK_KEY_NUM9;
						keyReleased(KEY_NUM9);
					}
				}
			} else
			if (key == Display.keyCross ) {
				if ((emuNumStates & MASK_KEY_CROSS) != 0) {
					emuNumStates &= ~MASK_KEY_CROSS;
					keyReleased(KEY_POUND);
				}
			} else
			if (key == Display.keyStar) {
				if ((emuNumStates & MASK_KEY_STAR) != 0) {
					emuNumStates &= ~MASK_KEY_STAR;
					keyReleased(KEY_STAR);
				}
			} else
			//Virtual keys
			if (key == Display.keyVirtual1) {
				keyReleased(1000000);
			}
			if (key == Display.keyVirtual2) {
				keyReleased(1000001);
			} else
			if (key == Display.keyVirtual3) {
				keyReleased(1000002);
			} 
			
		}
		//super.emuKeyAction(key, keyChar, action);
		return true;
	}
	
	public final void repaint(int x, int y, int width, int height) {
		/*
		if (MainApp.verbose) { 
			System.out.println("Pstros: RP 1 ec=" + emuCanvas);
		}
		*/
		
		if (emuCanvas != null ) {
			/*
			if (MainApp.verbose) { 
				System.out.println("Pstros: RP 2 ec=" + emuCanvas);
			}
			*/
			synchronized(emuCanvas.paintLock) {
				/*
				if (MainApp.verbose) { 
					System.out.println("Pstros: RP 3 ec=" + emuCanvas);
				}
				*/
				emuCanvas.paintLock[0]++;
				emuCanvas.setEmuPaintRequest(x,y, width, height);
				if (ConfigData.slaveMode) {
					emuCanvas.update();
				} else {
					/*
					if (MainApp.verbose) { 
						System.out.println("Pstros: RP 4 ec=" + emuCanvas);
					}
					*/
					emuCanvas.repaint(x,y, width, height);
				}
			}
			emuCanvas.checkPause();
		}
		if (MainApp.verbose) { 
			System.out.println("Pstros: Canvas.repaint called emuCanvas=" + emuCanvas);
		}
		
	}
	
	public final void repaint() {
		//System.out.println("Canvas:repaint called..  this=" + this);
		repaint(0,0, Display.WIDTH, Display.HEIGHT);
				
	}
	public final void serviceRepaints() {
		if (MainApp.verbose) { 
			System.out.println("Pstros: Canvas.serviceRepaints called emuCanvas=" + emuCanvas);
		}
		//System.out.println("Canvas: serviceRepaints! emuCanvas=" + emuCanvas + " this=" + this);
		if (emuCanvas!= null) {
			//System.out.print("service repaint called!");
			//emuCanvas.flushGraphics();
			
			//the paint method was not executed yet
			//most probably the thread scheduling is too busy to
			//call repaint. 
			int safeUnlock = 10;
			/*
			while (emuCanvas.paintRequestValid() && safeUnlock > 0) {
				safeUnlock--;
				Thread.yield();
				try {
					Thread.sleep(2);
				} catch(Exception e) {
				}
			}
			*/
			
			synchronized(emuCanvas.paintLock) {
				if (emuCanvas.paintLock[0] > 0) {
					if (MainApp.verbose && emuCanvas.paintLock[0] > 1) {
						System.out.println("Pstros: ?? paint lock=" + emuCanvas.paintLock[0]);
					} 
					safeUnlock = 500;
					while (emuCanvas.isPainting() && safeUnlock > 0) {
						//System.out.print('.');
						try {
							Thread.sleep(2);
							safeUnlock --;
						} catch(Exception e) {
						}
					}
					emuCanvas.paintLock[0]--;
				}
				if (emuCanvas.paintRequestValid()) {
					emuCanvas.flushGraphics(null);
				}
			}
			//puase execution if needed
			emuCanvas.checkPause();
			
			//System.out.println("finished!");
		}
		
	}
	
	protected void sizeChanged(int w, int h) {};
	
	public void emuPaint(Graphics g,  Object o) {
		if (MainApp.verbose) {
			System.out.println("**canvas paint! caller=" + o );
		}
		paint(g);
	}
	void emuPaint(NDSGraphics g) {
	}
	public void emuShowNotify() {
		try {
			showNotify();
		} catch (Exception e) {
			e.printStackTrace();
		}
		emuSetShown(true);
	}
	public void emuHideNotify() {
		try {
			hideNotify();
		} catch (Exception e) {
			e.printStackTrace();
		}
		emuSetShown(false);
	}
	
}
