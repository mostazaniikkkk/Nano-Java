package javax.microedition.lcdui;

import nds.pstros.video.NDSGraphics;



public class Spacer extends Item {
	
	int minWidth;
	int minHeight;
	
	public Spacer(int minWidth,  int minHeight) {
		setMinimumSize(minWidth, minHeight);
	}
	public void setMinimumSize(int minWidth, int minHeight) {
		if (minWidth < 0 || minHeight < 0) {
			throw new IllegalArgumentException();
		}
		this.minHeight = minHeight;
		this.minWidth = minWidth;
	}
	public void addCommand(Command cmd) {
		throw new RuntimeException();
	}
	public void setDefaultCommand(Command cmd) {
		throw new RuntimeException();
	}
	public void setLabel(String label){
		throw new RuntimeException();
	}
	
	int emuGetHeight(NDSGraphics g) {
		return minHeight;
	}
	int emuPaint(NDSGraphics g, int x, int y) {
		return minHeight;
	}

}
