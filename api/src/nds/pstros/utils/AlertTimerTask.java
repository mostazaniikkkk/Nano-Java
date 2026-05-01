package nds.pstros.utils;

import java.util.TimerTask;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;


public class AlertTimerTask extends TimerTask {
	
	private Display display;
	private Displayable d;
	public AlertTimerTask(Display display, Displayable d) {
		this.display = display;
		this.d = d;
	}
	
	public void run() {
		if (display == null || d == null) {
			return;
		}
		display.setCurrent(d);
	}

}
