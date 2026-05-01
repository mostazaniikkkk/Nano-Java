/*
 * Created on May 22, 2008
 *
 */
package nds.pstros.video;

/**
 * @author ole
 *
 */
public class NDSCanvas {
	
	
	protected NDSImage canvas;
	protected NDSGraphics g;
	protected boolean direct;
	
	
	public NDSCanvas(boolean direct) {
		this.direct = direct;
		if (!direct) {
			canvas = NDSImage.createImage(256, 192, 0);
			g = canvas.getGraphics();
		} else {
			g = new NDSGraphics(); //direct graphics
		}
	}
	
	public NDSGraphics getGraphics() {
		return g;
	}
	
	public void repaint() {
		update(g);
		//flush the graphics
	}
	public void repaint(int x, int y, int w, int h) {
		update(g);
	}
	
	public void update(NDSGraphics g) {
	}

	public int  getWidth() {
		return 256;
	}
	public int getHeight() {
		return 192;
	}
}
