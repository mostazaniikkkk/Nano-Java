/*
 * Created on May 21, 2008
 *
 */
package nds.pstros.video;

import nds.Video;

/**
 * @author ole
 *
 */
public class NDSGraphics {
	
	NDSFont currentFont;
	NDSImage baseImage;
	int currentColor;
	int currentColorAlpha;
	short currentColorNative;
	boolean disposed;
	
	int clX, clY, clW, clH; //clip data;
	int dstW; int dstH;

	//create direct graphics  - for rendering directly tow hw screen
	public NDSGraphics() {
		baseImage = new NDSImage(1,1);
		baseImage.pixelData = null;
		dstW = baseImage.width = 256;
		dstH = baseImage.height = 192;
		clW = 256;
		clH = 192;
	}

	public NDSGraphics(NDSImage img) {
		if (img == null) {
			throw new IllegalArgumentException("image is null");
		}
		baseImage = img;
		dstW = clW = baseImage.width;
		dstH = clH = baseImage.height;
	}
	public void resetSize() {
		if (baseImage != null) {
			dstW = clW = baseImage.width;
			dstH = clH = baseImage.height;
		}
	}
	

	public void setColorAlpha(int color) {
		currentColor = color;
		currentColorAlpha = (color >> 24) & 0xFF;

		currentColorNative = (short)(0x8000 | //alpha 
		((color >> 19) & 0x1f) |
		((color >> 6) & 0x3e0) |
		((color << 7) & 0x7c00));

	}

	public void setColor(int color) {
		currentColor = color | 0xFF000000;
		currentColorAlpha = 0xFF;

		currentColorNative = (short)(0x8000 | //alpha 
		((color >> 19) & 0x1f) |
		((color >> 6) & 0x3e0) |
		((color << 7) & 0x7c00));

	}
	public int getColor() {
		return currentColor;
	}
	
	public void setFont(NDSFont font) {
		currentFont = font;
	}
	
	public NDSFont getFont() {
		return currentFont;
	}
	
	public void setClip(NDSRectangle rectangle) {
	}
	
	public void getClipBounds(NDSRectangle rectangle) {
		rectangle.x = clX;
		rectangle.y = clY;
		rectangle.width = clW;
		rectangle.height = clH;
	}
	
	public void drawString(String line, int x, int y) {
		currentFont.drawString(
				baseImage.pixelData, baseImage.getWidth(), baseImage.getHeight(),
				line, x , y, currentColorNative,
				clX, clY, clW, clH
		);
		
	}
	
	public void fillRect(int x, int y, int width, int height) {
		//System.out.println("fill rect");
		Video.fillRect(
				baseImage.pixelData, 
				dstW, dstH, x,y, width, height, currentColorNative,
				clX, clY, clW, clH,
				currentColorAlpha
		);
	}
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		fillRect(x,y,width, height);
	}
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		Video.fillArc(
				baseImage.pixelData, dstW, dstH, 
				x,y, width, height, startAngle, arcAngle,  currentColorNative,
				clX, clY, clW, clH
		);
		
	}
	public void drawRect(int x, int y, int width, int height) {
		final int x2 = x + width;
		final int y2 = y + height;
		drawLine(x,y, x2, y);
		drawLine(x, y2, x2, y2);
		drawLine(x,y, x, y2);
		drawLine(x2,y, x2,y2);
	}
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		drawRect(x,y,width, height);
	}
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		Video.drawArc(
				baseImage.pixelData, dstW, dstH, 
				x,y, width, height, startAngle, arcAngle,  currentColorNative,
				clX, clY, clW, clH
		);
	}
	public void setClip(int x, int y, int width, int height) {
		clX = x;
		clY = y;
		clW = width;
		clH = height;
	}
	public void clipRect(int x, int y, int width, int height) {
		int clX2 = clX + clW;
		int clY2 = clY + clH;
		
		if (x > clX) {
			clW -= (x-clX);
			clX = x;
		}
		if (x + width < clX2) {
			clX2 = x+width;
			clW = clX2 - clX; 
		}

		if (y > clY) {
			clH -= (y-clY);
			clY = y;
		}
		if (y + height < clY2) {
			clY2 = y+height;
			clH = clY2 - clY; 
		}
	}
	public void drawLine(int x1, int y1, int x2, int y2) {
		//System.out.println("draw line");
		Video.drawLine(
				baseImage.pixelData, dstW, dstH, 
				x1,y1, x2, y2, currentColorNative,
				clX, clY, clW, clH
		);
		
	}
	public void fillPolygon(int[] x, int[] y, int vertexCount) {
		if (vertexCount == 3) {
			Video.fillTriangle(
					baseImage.pixelData, dstW, dstH,
					x[0],y[0], x[1], y[1], x[2], y[2], currentColorNative,
					clX, clY, clW, clH
			);
		} else
		if (vertexCount == 4) {
			Video.fillTriangle(
					baseImage.pixelData, dstW, dstH,
					x[0],y[0], x[1], y[1], x[2], y[2], currentColorNative,
					clX, clY, clW, clH
			);
			Video.fillTriangle(
					baseImage.pixelData, dstW, dstH,
					x[0],y[0], x[2], y[2], x[3], y[3], currentColorNative,
					clX, clY, clW, clH
			);
		}
	}
	public void drawPolygon(int[] x, int[] y, int vertexCount) {
	}
	
	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
		Video.fillTriangle(
				baseImage.pixelData, dstW, dstH,
				x1,y1, x2, y2, x3, y3, currentColorNative,
				clX, clY, clW, clH
		);
	}
	
	public void drawImage(NDSImage img, int x, int y) {
		//System.out.println("NDSGraphics.drawImage @" + hashCode() + " direct="  + baseImage.pixelData );
		/*
		if (baseImage == null) {
			System.out.println("baseImage null!!");
			return;
		}
		*/
		int srcW = img.getWidth();
		int srcH = img.getHeight();

		//clipping is preformed inside the blit
		Video.blit(
				baseImage.pixelData, dstW, dstH,
				img.pixelData, srcW, srcH,
				x,y,
				clX, clY, clW, clH,
				img.transp, img.pixelDataByte
		);
	}
	
	public void drawRGB(int[] rgbData,
			int offset,
			int scanlength,
			int x,
			int y,
			int width,
			int height,
			boolean processAlpha) {
		
		Video.blitRGB(
				baseImage.pixelData, dstW, dstH,
				rgbData, width, height, 
				x,y, offset, scanlength,
				clX, clY, clW, clH, 
				baseImage.pixelDataByte, processAlpha
		);
	}
	public void drawRGB444(short[] pixels, int width, int height, int offset, int scan,  int manipX, int manipY, int rotate) {
		//java.lang.System.out.println("Blit4444 dstLength=" + baseImage.pixelData.length + " @" + hashCode() +  " bi@" + baseImage.hashCode() + " data="  + baseImage.pixelData);
		Video.blitRGB4444(
				baseImage.pixelData, dstW, dstH,
				pixels, width, height, 
				0,0, offset, scan,	//position
				null, false,
				manipX, manipY, rotate
		);
		if (rotate == 1) {
			dstW = clW = baseImage.height;
			dstH = clH = baseImage.width;
			baseImage.setSize(dstW, dstH, false);
		}
		
	}
	
	public void drawRGB4444(short[] pixels, int width, int height, int offset, int scan,  int manipX, int manipY, int rotate) {
		//java.lang.System.out.println("Blit4444 dstLength=" + baseImage.pixelData.length + " @" + hashCode() +  " bi@" + baseImage.hashCode() + " data="  + baseImage.pixelData);
		Video.blitRGB4444(
				baseImage.pixelData, dstW, dstH,
				pixels, width, height, 
				0,0, offset, scan,	//position
				baseImage.pixelDataByte, true,
				manipX, manipY, rotate 
		);
		if (rotate == 1) {
			dstW = clW = baseImage.height;
			dstH = clH = baseImage.width;
			baseImage.setSize(dstW, dstH, false);
		}
	}

	public void drawRGB888(int[] pixels, int width, int height, int offset, int scan,  int manipX, int manipY, int rotate) {
		//java.lang.System.out.println("Blit4444 dstLength=" + baseImage.pixelData.length + " @" + hashCode() +  " bi@" + baseImage.hashCode() + " data="  + baseImage.pixelData);
		Video.blitRGB8888(
				baseImage.pixelData, dstW, dstH,
				pixels, width, height, 
				0,0, offset, scan,	//position
				null, false,
				manipX, manipY, rotate
		);
		if (rotate == 1) {
			dstW = clW = baseImage.height;
			dstH = clH = baseImage.width;
			baseImage.setSize(dstW, dstH, false);
		}
	}
	
	public void drawRGB8888(int[] pixels, int width, int height, int offset, int scan,  int manipX, int manipY, int rotate) {
		//java.lang.System.out.println("Blit4444 dstLength=" + baseImage.pixelData.length + " @" + hashCode() +  " bi@" + baseImage.hashCode() + " data="  + baseImage.pixelData);
		Video.blitRGB8888(
				baseImage.pixelData, dstW, dstH,
				pixels, width, height, 
				0,0, offset, scan,	//position
				baseImage.pixelDataByte, true,
				manipX, manipY, rotate
		);
		if (rotate == 1) {
			dstW = clW = baseImage.height;
			dstH = clH = baseImage.width;
			baseImage.setSize(dstW, dstH, false);
		}
	}

	public void dispose() {
		//System.out.println("Dispose graphics!");
		baseImage = null;
		disposed = true;
	}
}
