/*
 * Created on May 22, 2008
 *
 */
package nds.pstros.utils;

/**
 * @author ole
 *
 */
public class NDSKeyEvent {
	private int keyCode;
	
	public int getKeyCode() {
		return keyCode;
	}
	public char getKeyChar() {
		return ' ';
	}
	
	public int getModifiers() {
		return 0;
	}

	
	public void setKeyCode(int keyCode) {
		this.keyCode = keyCode; 
	}
}
