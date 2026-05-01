package javax.microedition.lcdui;

import java.util.Date;
import java.util.TimeZone;

import nds.pstros.video.NDSFont;
import nds.pstros.video.NDSGraphics;


public class DateField extends Item{
	
	
	public static final int DATE = 1;
	public static final int TIME = 2;
	public static final int DATE_TIME = 3;
	
	
	private TimeZone timeZone;
	private int inputMode;
	
	public DateField(String label, int mode) {
		this(label, mode, null);
	}
	public DateField(String label, int mode, TimeZone timeZone) {
		setLabel(label);
		if (timeZone == null) {
			this.timeZone = TimeZone.getDefault(); 
		} else {
			this.timeZone = timeZone;
		}
		setInputMode(mode); 
	}
	
	public Date getDate() {
		return null;
	}
	
	public void setDate(Date date) {
	}
	
	public int getInputMode() {
		return inputMode;
	}
	public void setInputMode(int mode){
		inputMode = mode;
	}
	int emuPaint(NDSGraphics g, int x, int y) {
		NDSFont font = g.getFont();
		int height = font.getSize() + 2;
		int width = Display.WIDTH - x - 2;
		int origY = y;
		//int shiftY = emuGetYSpace(g);
		int paintColor = COLOR_BLACK;
		if (emuActive) {
			paintColor = COLOR_RED;
		}
		
		g.setColor(paintColor);
		if (label != null) {
			g.drawString(label, x+2, y+1);
			y += height + 2;
		}

		if (emuActive) {
			g.setColor(COLOR_HIGHLIGT);
			g.fillRect(x,y-height+2, Display.WIDTH - 4, height + 2);
			g.setColor(paintColor);
		}
		
		g.drawString("not implemented in Pstros", x+2, y+1);
		g.drawRect(x,y-height+2, Display.WIDTH - 4, height + 2);
		y += height + 2;
		y += 4;

		return y - origY;
	}
	int emuGetYSpace(NDSGraphics g) {
		NDSFont font = g.getFont();
		return font.getSize() + 3;
	}
	int emuGetHeight(NDSGraphics g) {
		NDSFont font = g.getFont();
		int height = font.getSize();
		int h = height + 4;
		
		height = h;
		if (label != null && label.length() > 0) {
			height += h;
		}
		return height + 4;
	}


}
