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


import javax.microedition.lcdui.game.Sprite;

import nds.pstros.MainApp;
import nds.pstros.video.NDSGraphics;
import nds.pstros.video.NDSImage;
import nds.pstros.video.NDSRectangle;

import com.nokia.mid.ui.DirectGraphics;


public class Graphics implements DirectGraphics {
	private static final int EMU_BUFF_WIDTH = 256;
	private static final int EMU_BUFF_HEIGHT = 256;
	
	private static NDSImage bi;	//temporary image
	private static int biWidth;
	private static int biHeight;
	private static NDSGraphics biGraphics;
	//private static int[] pixelData;
	
	private static int[] polygonX = new int[32];
	private static int[] polygonY = new int[32];

	private static int[] polygonPointsX;
	private static int[] polygonPointsY;

	private static int[] tmpRgbData;
	private static final String lock = "Lock";
	
	public static final int HCENTER = 1;
	public static final int VCENTER = 2;
	public static final int LEFT = 4;
	public static final int RIGHT = 8;
	public static final int TOP = 16;
	public static final int BOTTOM = 32;
	public static final int BASELINE = 64;
	
	public static final int SOLID = 0;
	public static final int DOTTED = 1;
	
	private static NDSRectangle emuTmpRect = new NDSRectangle();
	
	protected int trX;			//translation coordinates
	protected int trY;
	private int color;			//current color
	private int strokeStyle;
	private int clX;			//clipping coordinates
	private int clY;
	private int clW;
	private int clH;
	private Font font;
	
	private boolean fixed;
	
	
	//Emu specific
	protected NDSGraphics emuGraphics;
	protected NDSImage emuGraphicsImage;
	protected Image emuImage;
	int emuActionCounter;
	int emuDrawRegionCounter;
	
	public Graphics() {
		if (bi == null) {
			initBufferedImage(EMU_BUFF_WIDTH, EMU_BUFF_HEIGHT);		
		}
		font = Font.getDefaultFont();
	}
	
	private void initBufferedImage(int w, int h) {
		int maxPixels = EMU_BUFF_WIDTH * EMU_BUFF_HEIGHT;
		
		if (w*h > maxPixels) {
				h = maxPixels / w;
		} 
		//biWidth = w;
		//biHeight = h;
		
		if (bi == null) {
			bi = new NDSImage(w, h);
			bi.createAlphaChannel();
		} else {
			bi.setSize(w,h, false);
		}
		if (biGraphics == null) {
			biGraphics = bi.getGraphics();
		} else {
			biGraphics.resetSize();
		}
	}
	
	public void emuSetImage(Image img) {
		emuImage = img;
	}
	
	public void emuSetGraphics(NDSGraphics eg) {
		emuGraphics = eg;
	}
	public NDSGraphics emuGetGraphics() {
		return emuGraphics;
	}
	
	public void emuSetGraphicsImage(NDSImage bi) {
		//System.out.println("setting bi =" + bi + " to=" + getClass().getName() + "@" + hashCode());
		emuGraphicsImage = bi;
	}
	public NDSImage emuGetGraphicsImage() {
		//System.out.println("getting bi =" + emuGraphicsImage + " from=" + getClass().getName() + "@" + hashCode());
		return emuGraphicsImage;
	}
	public void emuSetFixed(boolean state) {
		fixed = state;
	}
	
	public void translate(int x, int y) {
		if (MainApp.verbose) {
			System.out.println("Graphics: translate x=" + x + " y=" + y);
		}
		trX += x;
		trY += y;
	}
	
	public int getTranslateX() {
		return trX;
	}
	public int getTranslateY() {
		return trY;
	}

	public int getColor() {
		return color;
	}
	public int getDisplayColor(int col) {
		return col;
	}
	
	public int getRedComponent() {
		return ((color >> 16) & 0xFF);
	}
	public int getGreenComponent() {
		return ((color >> 8) & 0xFF);
	}
	public int getBlueComponent() {
		return (color & 0xFF);
	}
	public int getGrayScale() {
		return (getRedComponent() + getGreenComponent() + getBlueComponent()) / 3; 
	}
	public void setColor(int red, int green, int blue) {
		if (MainApp.verbose) {
			System.out.println("Graphics: setColor: r=" + red + " g=" + green + " b=" + blue);
		}
		setColor( ((red & 0xFF) << 16) + ((green & 0xFF) << 8) + (blue & 0xFF)); 
	}	
	public void setColor(int RGB) {
		if (MainApp.verbose) {
			System.out.println("Graphics: setColor: color=" + RGB);
		}
		color = 0xFF000000 | RGB;
		emuGraphics.setColor(color);
	}
	
	public void setGrayScale(int value) {
		setColor(value, value, value);
	}
	
	public Font getFont() {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  getFont() " + font);
		}
		return font;
	}

	public void setStrokeStyle(int style) {
		strokeStyle = style;
	}
	public int getStrokeStyle() {
		return strokeStyle;
	}
	
	public void setFont(Font f) {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  setFont="  + font);
		}
		if (f == null) {
			font = Font.getDefaultFont();
		} else {
			font = f;
		}
	}
	public int getClipX() {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  getClipX="  + clX + " trX=" + trX);
		}
		return clX - trX;
	}
	public int getClipY() {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  getClipY="  + clY + " trY=" + trY);
		}
		return clY - trY;
	}
	public int getClipWidth() {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  getClipW="  + clW);
		}
		return clW;
	}
	public int getClipHeight() {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  getClipH="  + clH);
		}

		return clH;
	}
	public void setClip(int x,int y, int width, int height) {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + " setClip"  + " x=" + x + " y=" + y + " w=" + width + " h=" + height +  " trX=" + trX + " trY=" + trY);
		}
		//return;
		
		clX = trX + x;
		clY = trY + y;
		clW = width;
		clH = height;
		emuGraphics.setClip(clX,clY,width,height);
		
		
	}
	public void clipRect(int x, int y, int width, int height) {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  clipRct"  + " x=" + x + " y=" + y + " w=" + width + " h=" + height +  " trX=" + trX + " trY=" + trY);
		}
		//return;
		
		int clX1 = clX  + clW;
		int clY1 = clY  + clH;
		
		int clX2 = trX + x + width;
		int clY2 = trY + y + height;
		
		if (trX + x > clX)  clX = trX + x;
		if (clX2 < clX1) clX1 = clX2;
		clW = clX1 - clX;
		
		if (trY + y > clY)  clY = trY + y;
		if (clY2 < clY1) clY1 = clY2;
		clH = clY1 - clY;
		emuGraphics.clipRect(trX + x, trY + y,width, height);
		
	}
	public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) {
		//FIXME - ? NDS
		/*
		int srcX = x_src + trX;
		int srcY = y_src + trY;
		if (emuImage == null) {
			throw new IllegalStateException();
		}
		if (srcX < 0 || srcY < 0 || srcX + width > emuImage.getWidth() || srcY + height > emuImage.getHeight()) {
			throw new IllegalArgumentException();
		} 
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  copyArea x_src=" + x_src + " y_src=" + y_src + " w=" + width + " h="  + height + " x_dest=" + x_dest + " y_dest=" + y_dest + " anchor=" + anchor + " trX=" + trX + " trY=" + trY);
		}

		synchronized(lock) {
			int anX = 0;
			int anY = 0;
			int size = width * height;
			if (tmpRgbData == null ||  tmpRgbData.length < size) {
				tmpRgbData = new int[size];
			}
			emuImage.getRGB(tmpRgbData, 0, width, srcX, srcY, width, height);

			//compute anchor
			if ((anchor & HCENTER) != 0) {
				anX = width >> 1;
			} else
			if ((anchor & RIGHT) != 0) {
				anX = width;
			}
		
			if ((anchor & VCENTER) != 0) {
				anY = height >> 1;
			} else
			if ((anchor & BOTTOM) != 0) {
				anY = height;
			}
			//Note: translation is done inside the drawRGB method
			drawRGB(tmpRgbData, 0, width, x_dest-anX, y_dest-anY, width, height, false);
		}
		emuActionCounter++;
		*/
	}
	
	public void drawLine(int x1, int y1, int x2, int y2) {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  drawLine x1=" + x1 + " y1=" + y1 + " x2=" + x2 + " y2="  + y2);
		}
		emuGraphics.drawLine(x1+trX, y1 + trY, x2 + trX, y2 + trY);
		emuActionCounter++;
	}
	
	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
		emuGraphics.fillTriangle(x1 + trX,y1 + trY,x2 + trX,y2 + trY,x3 + trX,y3 + trY);
	}
	
	public void fillRect(int x, int y, int width, int height) {
		if (MainApp.verbose) {
			System.out.println("fill rect x=" + x + " y=" + y + " w=" + width + " h=" + height);
		}
		emuGraphics.fillRect(x + trX,y + trY,width, height);
		emuActionCounter++;
	}
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		if (MainApp.verbose) {
			System.out.println("fill round rect x=" + x + " y=" + y + " w=" + width + " h=" + height);
		}
		emuGraphics.fillRoundRect(x + trX,y + trY,width, height, arcWidth, arcHeight);
		emuActionCounter++;
	}
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		if (MainApp.verbose) {
			System.out.println("fill arc x=" + x + " y=" + y + " w=" + width + " h=" + height + " sAngle=" + startAngle + " eAngle=" + arcAngle);
		}
		//test
		emuGraphics.fillArc(x + trX,y + trY,width, height, startAngle, arcAngle);
		emuActionCounter++;
	}
	
	public void drawRect(int x, int y, int width, int height) {
		if (MainApp.verbose) {
			//System.out.println("draw rect clip=" + emuGraphics.getClip());
		}
		emuGraphics.drawRect(x + trX,y + trY,width,height);
		emuActionCounter++;
	}
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		if (MainApp.verbose) {
			System.out.println("draw round rect x=" + x + " y=" + y + " w=" + width + " h=" + height);
		}
		emuGraphics.drawRoundRect(x + trX,y + trY,width,height, arcWidth, arcHeight);
		emuActionCounter++;
	}
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		if (MainApp.verbose) {
			System.out.println("draw arc x=" + x + " y=" + y + " w=" + width + " h=" + height + " sAngle=" + startAngle + " eAngle=" + arcAngle);
		}
		emuGraphics.drawArc(x + trX,y + trY,width, height, startAngle, arcAngle);
		emuActionCounter++;
	}
	
	public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
		String line = new String(data, offset, length);
		drawString(line, x ,y , anchor);
		emuActionCounter++;
	}
	public void drawChar(char character, int x, int y, int anchor) {
		String line = String.valueOf(character);
		drawString(line, x ,y , anchor);
		emuActionCounter++;
	}
	
	public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
		drawString(str.substring(offset, offset+ len), x, y, anchor);
		emuActionCounter++;
	}
	
	public void drawString(String str, int x, int y, int anchor) {
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + " draw string=" + str + " x=" + x + " y=" + y + " anchor=" + anchor + " trX=" + trX + " trY=" + trY + " clX=" + clX + " clY=" + clY + " clW=" + clW + " clH=" + clH);
		}
		int anX = 0;
		int anY = 0;
		if ((anchor & HCENTER) != 0) {
			anX = font.stringWidth(str) >> 1;
		} else
		if ((anchor & RIGHT) != 0) {
			anX = font.stringWidth(str);
		}
		
		if ((anchor & VCENTER) != 0) {
			anY = font.getHeight() >> 1;
		} else 
		if ((anchor & BOTTOM) != 0) {
			anY = font.getHeight();
		} else 
		if ((anchor & BASELINE) != 0) {
			anY = font.getBaselinePosition();
		} 

		emuGraphics.setFont(font.emuGetFont());
		emuGraphics.drawString(str, x + trX - anX, y + trY - anY + font.getBaselinePosition());
		//emuGraphics.drawString(str, x + trX - anX, y + trY - anY);
		//emuGraphics.drawRect(x+ trX - anX, y + trY - anY + font.getHeight() - 4, font.stringWidth(str), font.getHeight());
		emuActionCounter++;
	}
	
	public void drawImage(Image img, int x, int y, int anchor) {
		if (img == null) {
			//System.out.println("Graphics:" + hashCode() + "  drawImage: img= null!" );
			return;
		}
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  drawImage: img=" + img.hashCode()  + " x=" + x + " y=" + y + " anchor=" + anchor + " trX=" + trX + " trY=" + trY + " w=" + img.getWidth() + " h=" + img.getHeight());
		}
		emuDrawImage(img.emuGetImage(0), x,y, anchor, img.getWidth(), img.getHeight());
		emuActionCounter++;
	}
	
	void emuDrawImage(NDSImage image, int x, int y, int anchor, int width, int height) {
		int anX = 0;
		int anY = 0;
		if (MainApp.verbose) {
			System.out.println("Graphics:" + hashCode() + "  drawImage: img=" + image.hashCode()  + " x=" + x + " y=" + y + " anchor=" + anchor + " trX=" + trX + " trY=" + trY + " w=" + image.getWidth() + " h=" + image.getHeight());
		}
		if ((anchor & HCENTER) != 0) {
			anX = width >> 1;
		} else
		if ((anchor & RIGHT) != 0) {
			anX = width;
		}
		
		if ((anchor & VCENTER) != 0) {
			anY = height >> 1;
		} else 
		if ((anchor & BOTTOM) != 0) {
			anY = height;
		} 
		emuGraphics.drawImage(image, x + trX - anX, y + trY - anY);
		//debug
		//emuGraphics.setColor(new Color(this.hashCode()*2));
		//emuGraphics.drawRect(x + trX - anX, y + trY - anY, image.getWidth(null), image.getHeight(null));
		
		//we have to invalidate all the rotated and precached versions
		//because they doesn't match the freshly updated base image  
		if (!fixed && emuImage != null) {
			emuImage.emuInvalidate();
		}
	}
	
	public void drawRGB(int[] rgbData,
						int offset,
						int scanlength,
						int x,
						int y,
						int width,
						int height,
						boolean processAlpha) {
		
		if (MainApp.verbose) {
			System.out.println("Graphics: drawRGB() x=" +x + " y=" + y + " w=" + width + " h="  + height + " offset=" + offset + " scanl=" + scanlength + " alpha=" + processAlpha + " trX=" + trX + " trY=" + trY);
			/*
			for (int j = 0; j < height; j++ ) {
				for (int i= 0; i < width; i++) {
					int pixel = rgbData[j*width + i] >> 24; //alpha
					int color = rgbData[j*width + i] & 0xFFFFFF; //alpha
					if (pixel < 16) {
						System.out.print("  " + Integer.toHexString(pixel));
					} else {
						System.out.print(" " + Integer.toHexString(pixel));
					}
					System.out.print(color);
				}
				System.out.println();
			}
			*/
		}
		
		emuGraphics.drawRGB(
				rgbData, offset, scanlength,
				x + trX,y + trY, width, height, processAlpha
				);
	}
	
	/*
	private int[] emuGetOpaqueRGB(int[] srcData, int offset, int width, int height, int scanlength) {
		int srcOffset = offset;
		int dstOffset = 0;
		
		int size = height * width;
		int skip = scanlength - width;
		int i;
		//safety check
		if (pixelData.length < size) {
			pixelData = new int[size];
		}
		while (dstOffset < size) {
			for (i = 0; i < width; i++) {
				pixelData[dstOffset] = srcData[srcOffset] | 0xFF000000;
				dstOffset++;
				srcOffset++;
			}
			srcOffset += skip;
		} 
		return pixelData;
	}
	*/
	
	protected void emuPushClip() {
		emuGraphics.getClipBounds(emuTmpRect);
	}
	
	protected void emuPopClip() {
		emuGraphics.setClip(emuTmpRect.x, emuTmpRect.y, emuTmpRect.width, emuTmpRect.height);
	}
	
	
	
	public void drawRegion(Image img,
						   int x_src,
						   int y_src,
						   int width,
						   int height,
						   int transform,
						   int x_dest,
						   int y_dest,
						   int anchor) {
			
		//Note! this will also throw null pointer exception when img is null
		//which is required by implementation 			   	
		if (x_src < 0 || y_src < 0 || (x_src + width) > img.getWidth() || (y_src + height) > img.getHeight()) {
			throw new IllegalArgumentException("region to be copied exceeds the bounds of the source image");
		}
						   	
		emuGraphics.getClipBounds(emuTmpRect);
		
		if (MainApp.verbose /*|| transform != 0*/) {
			System.out.println("Graphics:" + hashCode() + " draw region! image=" + img.hashCode() + " x_src=" + x_src + " y_src=" + y_src + " w=" + width + " h=" + height + " transform=" + transform + " x_dst="  + x_dest + " y_dst="  + y_dest + " anchor=" + anchor + " trX=" + trX + " trY=" + trY);
			System.out.println(" current clip x=" + emuTmpRect.x + " y=" + emuTmpRect.y + " w=" + emuTmpRect.width + " h=" +  emuTmpRect.height );
		}
		int anX = 0;
		int anY = 0;
		int tmp;
		switch (transform) {
			//TRANS_MIRROR_ROT180
			case 1: y_src = img.getHeight() - y_src - height;
				break;
			//TRANS_MIRROR
			case 2: x_src = img.getWidth() - x_src - width;
				break; 
			//TRANS_ROT180
			case 3: y_src = img.getHeight() - y_src - height;
				x_src = img.getWidth() - x_src - width;
				break;
			//TRANS_MIRROR_ROT270
			case 4: 				
			//System.out.println(" x_src=" + x_src + " y_src=" + y_src + " w=" + width + " h=" + height + " transform=" + transform + " x_dst="  + x_dest + " y_dst="  + y_dest + " anchor=" + anchor + " trX=" + trX + " trY=" + trY);
				tmp = width;
				width = height;
				height = tmp;
				tmp = x_src;
				x_src = y_src;
				y_src = tmp;
				break;
			//TRANS_ROT90;
			case 5: 
			//System.out.println(" x_src=" + x_src + " y_src=" + y_src + " w=" + width + " h=" + height + " transform=" + transform + " x_dst="  + x_dest + " y_dst="  + y_dest + " anchor=" + anchor + " trX=" + trX + " trY=" + trY);
				tmp = width;
				width = height;
				height = tmp;
				tmp = x_src;
				x_src = y_src;
				y_src = tmp;
				x_src = img.getHeight() - x_src - width;
				break;
			//TRANS_ROT270 ;
			 case 6:
				tmp = width;
				width = height;
				height = tmp;
				tmp = x_src;
				x_src = y_src;
				y_src = tmp;
				y_src = img.getWidth() - y_src - height;
				break;
			//TRANS_MIRROR_ROT90
			 case 7:
				tmp = width;
				width = height;
				height = tmp;
				tmp = x_src;
				x_src = y_src;
				y_src = tmp;
				y_src = img.getWidth() - y_src - height;
				x_src = img.getHeight() - x_src - width;
				break;
			default:
				if (transform != 0) {
					throw new IllegalArgumentException("unsupported transform ! " + transform);
				}
		}
		if ((anchor & HCENTER) != 0) {
			anX = width >> 1;
		} else
		if ((anchor & RIGHT) != 0) {
			anX = width;
		}
		
		if ((anchor & VCENTER) != 0) {
			anY = height >> 1;
		} else
		if ((anchor & BOTTOM) != 0) {
			anY = height;
		}
		
		//emuGraphics.setClip(x_dest + trX - anX, y_dest + trY - anY, width, height);
		emuGraphics.clipRect(x_dest + trX - anX, y_dest + trY - anY,  width, height);
		
		//test wether wee need to update the transformed image
		if (transform != 0 && img.graphics != null && (img.graphics.emuDrawRegionCounter != img.graphics.emuActionCounter ) ) {
			img.graphics.emuDrawRegionCounter = img.graphics.emuActionCounter;
			img.emuInvalidate();
		}
		
		emuGraphics.drawImage(img.emuGetImage(transform), x_dest + trX - anX - x_src, y_dest + trY - anY - y_src);
		/*
		if (emuTmpRect.width == 0) {
			emuTmpRect.width = Display.WIDTH;
			emuTmpRect.height = Display.HEIGHT;
		}
		*/
		emuGraphics.setClip(emuTmpRect.x, emuTmpRect.y, emuTmpRect.width, emuTmpRect.height);
		emuActionCounter++;
	}
	
	public void emuDrawImage(NDSImage img, int x, int y) {
		emuGraphics.drawImage(img, x + trX,y + trY);
	}
	public void emuDrawRegionSimple(NDSImage img,
						   int x_src,
						   int y_src,
						   int width,
						   int height,
						   int x_dest,
						   int y_dest
						   ) {
		emuGraphics.getClipBounds(emuTmpRect);
		if (MainApp.verbose) {
			//System.out.println("emuDrawRegionSimple! image=" + img + " x_src=" + x_src + " y_src=" + y_src + " w=" + width + " h=" + height + " x_dst="  + x_dest + " y_dst="  + y_dest);
		}
		emuGraphics.setClip(x_dest + trX, y_dest + trY , width, height);
		emuGraphics.drawImage(img, x_dest + trX - x_src, y_dest + trY - y_src);
		if (emuTmpRect.width == 0) {
			emuTmpRect.width = Display.WIDTH;
			emuTmpRect.height = Display.HEIGHT;
		}
		emuGraphics.setClip(emuTmpRect.x, emuTmpRect.y, emuTmpRect.width, emuTmpRect.height);
	}
	
	
	/***** extended graphics *****/
	
	public void drawImage(
		Image img,
		int x,
		int y,
		int anchor,
		int manipulation) {
		//drawImage(img, x,y, anchor);
		int transform = 0;
		int color = -1;
		//Note: tested and compared on No_6234 device
		switch (manipulation) {
			case FLIP_HORIZONTAL: 
				transform = Sprite.TRANS_MIRROR; 
				color = 0xFF; //BLUE;
				break;
			case FLIP_VERTICAL: 
				transform = Sprite.TRANS_MIRROR_ROT180;
				color = 0xFFFF00; //Color.YELLOW; 
				break;
			case ROTATE_180: 
				transform = Sprite.TRANS_ROT180;
				color = 0xFF00FF; //Color.MAGENTA;
				break;
			case ROTATE_90:
				transform = Sprite.TRANS_ROT270;
				color = 0xFFFF; //Color.CYAN;
				break;
			case ROTATE_270:
				transform = Sprite.TRANS_ROT90;
				break;
			case 8282: //FLIP_HORIZONTAL + ROTATE_90
				transform = Sprite.TRANS_MIRROR_ROT90;
				break;
			case 8372: //FLIP_HORIZONTAL + ROTATE_180
				transform = Sprite.TRANS_MIRROR_ROT180;
				break;
			case 8462: //FLIP_HORIZONTAL + ROTATE_270
				transform = Sprite.TRANS_MIRROR_ROT270;
				color = 0xFF00; //Color.GREEN;
				break;
			case 16474: //FLIP_VERTICAL + ROTATE_90;
				transform = Sprite.TRANS_MIRROR_ROT270;
				break;
			case 16564: //FLIP_VERTICAL + ROTATE_180;
				transform = Sprite.TRANS_MIRROR;
				break;
			case 16654: //FLIP_VERTICAL + ROTATE_270 
				transform = Sprite.TRANS_MIRROR_ROT90;
				color = 0xFF0000; //Color.RED;
				break;
			case 24576: // FLIP HORIZONTAL + FLIP_VERTICAL
				transform = Sprite.TRANS_ROT180;
				break; 
			case 24666: // FLIP HORIZONTAL + FLIP_VERTICAL + ROT_90 
				transform = Sprite.TRANS_ROT90;
				break; 
			case 24756: // FLIP HORIZONTAL + FLIP_VERTICAL + ROT_180
				transform = Sprite.TRANS_NONE;
				break; 
			case 24846: // FLIP HORIZONTAL + FLIP_VERTICAL + ROT_270
				transform = Sprite.TRANS_ROT270;
				break; 
				
			default:
				if (manipulation != 0) {
					System.out.println("Pstros: ExtendedGraphics.drawImage: unknow transform! " + manipulation);
				}
				
				
		}
		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: " + hashCode() +  " drawImage...) " + img.hashCode() + " x=" + x + " y="  + y + " manipulation="  + manipulation + " trX=" + trX + " trY=" + trY + " anchor=" + anchor + " w=" + img.getWidth() + " h=" + img.getHeight() + " transform=" + transform);
		}
		//emuDrawImage(img.emuGetImage(transform), x,y, anchor, img.getWidth(), img.getHeight()); 
		drawRegion(img, 0,0, img.getWidth(), img.getHeight(), transform, x,y, anchor);
		
		/*		
		if (color != -1) {
			emuGraphics.setColor(color);
			emuGraphics.drawRect(x + trX,y + trY, img.getWidth()-1, img.getHeight()-1);
		} 
		*/
		emuActionCounter++;
		
	}

	public void drawPixels(
		byte[] pixels,
		byte[] transparencyMask,
		int offset,
		int scanlength,
		int x,
		int y,
		int width,
		int height,
		int manipulation,
		int format) {
		// TODO Auto-generated method stub
		System.out.println("ExtendedGraphics: drawPixels(byte[],...) format="  + format + " NOT IMPLEMENTED!");

	}

	public void drawPixels(
		int[] pixels,
		boolean transparency,
		int offset,
		int scanlength,
		int x,
		int y,
		int width,
		int height,
		int manipulation,
		int format) {
		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: drawPixels(int[],...) format="  + format);
		}
		
		byte type = NDSImage.TYPE_OPAQUE;
		int manipX = 1;
		int manipY = 1;
		int rotation = manipulation & 0x1FF;
		//rotation is counter clock wise
		switch (rotation) {
			case ROTATE_180:
				manipX = -1;
				manipY = -1;
				rotation = 0;
				break;
			case ROTATE_270:
				manipX = -1;
				manipY = -1;
				rotation = 1;
				break;
			case ROTATE_90:
				rotation = 1;
				break;
		}
		
		switch (manipulation & 0xFFFE00) {
		case FLIP_HORIZONTAL:
			manipX = -manipX;
			break;
		case FLIP_VERTICAL:
			manipY = -manipY;
			break;
		case FLIP_HORIZONTAL | FLIP_VERTICAL:
			manipX = -manipX;
			manipY = -manipY;
			break;
		}

		bi.setSize(width,height, false);
		biGraphics.resetSize();

		
		switch (format) {
			case 888:  
				biGraphics.drawRGB888(pixels, width, height, offset, scanlength, manipX, manipY, rotation);
			break;
			case 8888: 
				type = NDSImage.TYPE_ALPHA;
				biGraphics.drawRGB8888(pixels, width, height, offset, scanlength, manipX, manipY, rotation);
			break;
		}
		bi.setTransparency(type);
		if (rotation == 0) {
			//bi.setSize(width, height, false);
			emuDrawImage(bi, x, y , 0, width, height ); //Note: translation is done inside emuDrawImage
		} else {
			emuDrawImage(bi, x, y , 0, height, width); //Note: translation is done inside emuDrawImage
		}
		emuActionCounter++;
	}

	public void drawPixels(
			
		short[] pixels,
		boolean transparency,
		int offset,
		int scanlength,
		int x,
		int y,
		int width,
		int height,
		int manipulation,
		int format) {
		
			
		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: drawPixels(short[],...) format="  + format + " tr=" + transparency +  
			" offset=" + offset + " scan=" + scanlength + " x=" + x + " y=" + y + " w=" + width + " h=" + height + " man=" + manipulation );
		}
		
		byte type = NDSImage.TYPE_OPAQUE;
		int manipX = 1;
		int manipY = 1;
		int rotation = manipulation & 0x1FF;
		//rotation is counter clock wise
		switch (rotation) {
			case ROTATE_180:
				manipX = -1;
				manipY = -1;
				rotation = 0;
				break;
			case ROTATE_270:
				manipX = -1;
				manipY = -1;
				rotation = 1;
				break;
			case ROTATE_90:
				rotation = 1;
				break;
		}
		
		switch (manipulation & 0xFFFE00) {
			case FLIP_HORIZONTAL:
				manipX = -manipX;
				break;
			case FLIP_VERTICAL:
				manipY = -manipY;
				break;
			case FLIP_HORIZONTAL | FLIP_VERTICAL:
				manipX = -manipX;
				manipY = -manipY;
				break;
		}

		bi.setSize(width,height, false);
		biGraphics.resetSize();
		
		switch (format) {
			case 444:  
				biGraphics.drawRGB444(pixels, width, height, offset, scanlength, manipX, manipY, rotation);
			break;
			case 4444: 
				biGraphics.drawRGB4444(pixels, width, height, offset, scanlength, manipX, manipY, rotation);
				type = NDSImage.TYPE_ALPHA;
			break;
		}
		bi.setTransparency(type);
		if (rotation == 0) {
			//bi.setSize(width, height, false);
			emuDrawImage(bi, x, y , 0, width, height ); //Note: translation is done inside emuDrawImage
		} else {
			emuDrawImage(bi, x, y , 0, height, width); //Note: translation is done inside emuDrawImage
		}
		emuActionCounter++;
	}

	public void drawPolygon(
		int[] xPoints,
		int xOffset,
		int[] yPoints,
		int yOffset,
		int nPoints,
		int argbColor) {
		
		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: drawPolygon()" );
		}

		if (xPoints == null || yPoints == null || nPoints == 0) {
			return;
		}
		int minX, maxX, valX;
		int minY, maxY, valY;
		
		if (polygonX.length < nPoints) {
			polygonX = new int[nPoints];
			polygonY = new int[nPoints];
		}

		//compute maximum and minimum bounds to speedup draw operations
		minX = maxX  = polygonX[0] = xPoints[xOffset]+ trX;
		minY = maxY  = polygonY[0] = yPoints[yOffset]+ trY;
		
		for (int i = 1; i < nPoints; i++) {
			valX = polygonX[i] = xPoints[i + xOffset]+ trX;
			valY = polygonY[i] = yPoints[i + yOffset]+ trY; 
			if (valX < minX) {
				minX = valX;
			} else 
			if (valX > maxX) {
				maxX = valX;
			}
				
			if (valY < minY) {
				minY = valY;
			}else
			if (valY > maxY) {
				maxY = valY;
			}
		}
		//wire polygons are drawn one pixel to the right and down -> so make clip larger 
		maxX++;
		maxY++;
		if (minX < clX) {
			minX = clX;
		}
		if (minY < clY) {
			minY = clY;
		}
		if (maxX > clX + clW) {
			maxX = clX + clW;
		}
		if (maxY > clY + clH) {
			maxY = clY + clH;
		}
		//is it Correct ? What if the graphics is not screen graphics and is larger than display?		
		if (minX < 0) {
			minX = 0;
		}
		if (maxX > Display.WIDTH) {
			maxX = Display.WIDTH;
		}
		if (minY < 0) {
			minY = 0;
		}
		if (maxY > Display.HEIGHT) {
			maxY = Display.HEIGHT;
		}

		
		//emuCleanBi();
		emuCleanBi(minX, minY, maxX, maxY);

		//biGraphics.setColor(new Color(argbColor));
		biGraphics.setColor(0xFFFFFFFF);
		biGraphics.drawPolygon(polygonX, polygonY, nPoints);
		
		emuApplyAlpha(minX, minY, maxX, maxY, argbColor);
		
		emuPushClip();
		emuGraphics.clipRect(minX+trX,minY+trY,maxX-minX, maxY-minY);
		emuGraphics.drawImage(bi, trX, trY);
		emuPopClip();
		emuActionCounter++;


	}

	public void drawTriangle(
		int x1,
		int y1,
		int x2,
		int y2,
		int x3,
		int y3,
		int argbColor) {

		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: drawTriangle()" );
		}

		int minX, maxX, valX;
		int minY, maxY, valY;
		
						
		polygonX[0] = x1 + trX;
		polygonX[1] = x2 + trX;
		polygonX[2] = x3 + trX;
		polygonY[0] = y1 + trY;
		polygonY[1] = y2 + trY;
		polygonY[2] = y3 + trY;
		
		//compute maximum and minimum bounds to speedup draw operations
		minX = maxX  = polygonX[0];
		minY = maxY  = polygonY[0];
		for (int i = 1; i < 3; i++) {
			valX = polygonX[i];
			valY = polygonY[i]; 
			if (valX < minX) {
				minX = valX;
			} else 
			if (valX > maxX) {
				maxX = valX;
			}
				
			if (valY < minY) {
				minY = valY;
			}else
			if (valY > maxY) {
				maxY = valY;
			}
		}
		//wire polygons are drawn one pixel to the right and down -> so make clip larger 
		maxX++;
		maxY++;
		if (minX < clX) {
			minX = clX;
		}
		if (minY < clY) {
			minY = clY;
		}
		if (maxX > clX + clW) {
			maxX = clX + clW;
		}
		if (maxY > clY + clH) {
			maxY = clY + clH;
		}

		//is it Correct ?		
		if (minX < 0) {
			minX = 0;
		}
		if (maxX > Display.WIDTH) {
			maxX = Display.WIDTH;
		}
		if (minY < 0) {
			minY = 0;
		}
		if (maxY > Display.HEIGHT) {
			maxY = Display.HEIGHT;
		}
		
		
		//System.out.println("minX=" + minX + " minY=" + minY + " maxX=" + maxX + " maxY=" + maxY);

		//emuCleanBi();
		emuCleanBi(minX, minY, maxX, maxY);
		
		biGraphics.setColor(0xFFFFFFFF);
		biGraphics.drawPolygon(polygonX, polygonY, 3);
		
		//emuApplyAlpha((argbColor & 0xFF000000));
		emuApplyAlpha(minX, minY, maxX, maxY, argbColor);
		
		emuPushClip();
		emuGraphics.clipRect(minX+trX,minY+trY,maxX-minX, maxY-minY);
		emuGraphics.drawImage(bi, trX, trY);
		emuPopClip();
		emuActionCounter++;
	}

	public void fillPolygon(
		int[] xPoints,
		int xOffset,
		int[] yPoints,
		int yOffset,
		int nPoints,
		int argbColor) {

		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: fillPolygon()  coloralpha=" + (argbColor>>24) + " c=" + argbColor+  " xOffset=" + xOffset + " yOffset=" + yOffset + " nPoints=" + nPoints + " size=" + xPoints.length);
//			for (int i = 0; i < nPoints; i++) {
//				System.out.println(Integer.toString(i) + " x=" + xPoints[xOffset + i] + " y=" + yPoints[yOffset + i]);
//			}
		}
		
		if (xPoints == null || yPoints == null || nPoints == 0) {
			return;
		}
		int minX, maxX, valX;
		int minY, maxY, valY;
		
		if (polygonX.length < nPoints) {
			polygonX = new int[nPoints];
			polygonY = new int[nPoints];
		}

		//compute maximum and minimum bounds to speedup draw operations
		minX = maxX  = polygonX[0] = xPoints[xOffset]+ trX;
		minY = maxY  = polygonY[0] = yPoints[yOffset]+ trY;
		
		for (int i = 1; i < nPoints; i++) {
			valX = polygonX[i] = xPoints[i + xOffset]+ trX;
			valY = polygonY[i] = yPoints[i + yOffset]+ trY; 
			if (valX < minX) {
				minX = valX;
			} else 
			if (valX > maxX) {
				maxX = valX;
			}
				
			if (valY < minY) {
				minY = valY;
			}else
			if (valY > maxY) {
				maxY = valY;
			}
		}
		//maxX++;
		//maxY++;
		if (minX < clX) {
			minX = clX;
		}
		if (minY < clY) {
			minY = clY;
		}
		if (maxX > clX + clW) {
			maxX = clX + clW;
		}
		if (maxY > clY + clH) {
			maxY = clY + clH;
		}
		//is it Correct ? What if the graphics is not screen graphics and is larger than display?		
		if (minX < 0) {
			minX = 0;
		}
		if (maxX > Display.WIDTH) {
			maxX = Display.WIDTH;
		}
		if (minY < 0) {
			minY = 0;
		}
		if (maxY > Display.HEIGHT) {
			maxY = Display.HEIGHT;
		}

		
		emuCleanBi(minX, minY, maxX, maxY);

		//biGraphics.setColor(new Color(argbColor));
		biGraphics.setColor(0xFFFFFFFF);
		biGraphics.fillPolygon(polygonX, polygonY, nPoints);
		
		emuApplyAlpha(minX, minY, maxX, maxY, argbColor);
		
		emuPushClip();
		emuGraphics.clipRect(minX, minY ,maxX-minX, maxY-minY);
		emuGraphics.drawImage(bi, 0,  0);
		emuPopClip();
		emuActionCounter++;
		
	}
	public void fillTriangle(
		int x1,
		int y1,
		int x2,
		int y2,
		int x3,
		int y3,
		int argbColor) {
		if (MainApp.verbose){
			System.out.println("ExtendedGraphics: fillTriangle() alpha=" + (argbColor>>24));
		}
		int minX, maxX, valX;
		int minY, maxY, valY;
		
						
		polygonX[0] = x1 + trX;
		polygonX[1] = x2 + trX;
		polygonX[2] = x3 + trX;
		polygonY[0] = y1 + trY;
		polygonY[1] = y2 + trY;
		polygonY[2] = y3 + trY;
		
		//compute maximum and minimum bounds to speedup draw operations
		minX = maxX  = polygonX[0];
		minY = maxY  = polygonY[0];
		for (int i = 1; i < 3; i++) {
			valX = polygonX[i];
			valY = polygonY[i]; 
			if (valX < minX) {
				minX = valX;
			} else 
			if (valX > maxX) {
				maxX = valX;
			}
				
			if (valY < minY) {
				minY = valY;
			}else
			if (valY > maxY) {
				maxY = valY;
			}
		}
		//maxX++;
		//maxY++;
		if (minX < clX) {
			minX = clX;
		}
		if (minY < clY) {
			minY = clY;
		}
		if (maxX > clX + clW) {
			maxX = clX + clW;
		}
		if (maxY > clY + clH) {
			maxY = clY + clH;
		}

		//is it Correct ?		
		if (minX < 0) {
			minX = 0;
		}
		if (maxX > Display.WIDTH) {
			maxX = Display.WIDTH;
		}
		if (minY < 0) {
			minY = 0;
		}
		if (maxY > Display.HEIGHT) {
			maxY = Display.HEIGHT;
		}
		
		
		//System.out.println("minX=" + minX + " minY=" + minY + " maxX=" + maxX + " maxY=" + maxY);

		//emuCleanBi();
		emuCleanBi(minX, minY, maxX, maxY);
		
		biGraphics.setColor(0xFFFFFFFF);
		biGraphics.fillPolygon(polygonX, polygonY, 3);
		
		//emuApplyAlpha((argbColor & 0xFF000000));
		emuApplyAlpha(minX, minY, maxX, maxY, argbColor);
		
		emuPushClip();
		emuGraphics.clipRect(minX, minY,maxX-minX, maxY-minY);
		//System.out.println("minX=" + minX + " minY=" + minY + " maxX=" + maxX + " maxY=" + maxY);
		emuGraphics.drawImage(bi, 0, 0);
		emuPopClip();
		emuActionCounter++;
	}	

/*	
	private void emuCleanBi() {
		int length = pixelData.length;
		
		for (int i = 0; i < length; i++) {
			pixelData[i] = 0;
		}
		
		bi.setRGB(0,0,EMU_BUFF_WIDTH, EMU_BUFF_HEIGHT, pixelData, 0,EMU_BUFF_WIDTH);
		biGraphics = bi.getGraphics();
	}
*/	
	
	
	//make fully transparent block in the image
	private void emuCleanBi(int x1, int y1, int x2, int y2) {
		//int length = pixelData.length;
		int i,j;
		int base;
		short[] rgb = bi.getPixelData();
		byte[] alpha = bi.getPixelAlphaData();
		for (j = y1; j < y2; j++) {
			base = j* EMU_BUFF_WIDTH + x1;
			for (i = x1; i < x2; i++) {
				rgb[base] = 0;
				alpha[base++] = 0;
				//pixelData[base++] = 0;
			}
		}
	}

/*	
	private void emuApplyAlpha(int alpha) {
		int length = pixelData.length;
		int pixel;
		bi.getRGB(0,0, EMU_BUFF_WIDTH, EMU_BUFF_HEIGHT, pixelData, 0, EMU_BUFF_WIDTH);		
		for (int i = 0; i < length; i++) {
			pixel = pixelData[i];
			if ((pixel & 0xFF000000) == 0xFF000000) {
				pixelData[i] = alpha | (pixel & 0xFFFFFF); 
			}  
		}
		bi.setRGB(0,0,EMU_BUFF_WIDTH, EMU_BUFF_HEIGHT, pixelData, 0,EMU_BUFF_WIDTH);
		//biGraphics = bi.getGraphics();
	}
*/	
	private void emuApplyAlpha(int x1, int y1, int x2, int y2,int color) {
		int i, j, base;
		int height = y2-y1;
		int width = x2-x1;
		short[] pixelData = bi.getPixelData();
		byte[] alphaData = bi.getPixelAlphaData();
		
		final byte alpha = (byte)(color >> 24); 
		final short colorNative = (short)( //0x800 |
			((color >> 19) & 0x1f) |
			((color >> 6) & 0x3e0) |
			((color << 7) & 0x7c00));
		
		for (j = 0; j < height; j++) {
			base = (j + y1) * EMU_BUFF_WIDTH + x1; 		
			for (i = 0; i < width; i++) {
				if (pixelData[base] != 0) {
					pixelData[base] = colorNative;
					alphaData[base] = alpha;
				}
				base++; 
			}
		}
	}


	public int getAlphaComponent() {
		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: getAlphaComponent()" );
		}
		return ((color & 0xFF000000) >>> 24);
	}

	public int getNativePixelFormat() {
		//System.out.println("ExtendedGraphics: getNativePixelFormat()" );
		return TYPE_USHORT_4444_ARGB;
		//return 0;
	}

	public void getPixels(
		byte[] pixels,
		byte[] transparencyMask,
		int offset,
		int scanlength,
		int x,
		int y,
		int width,
		int height,
		int format) {
		// TODO implement
		System.out.println("ExtendedGraphics: getPixels(byte[])  format" + format + " !NOT IMPLEMENTED!");

	}

	public void getPixels(
		int[] pixels,
		int offset,
		int scanlength,
		int x,
		int y,
		int width,
		int height,
		int format) {
		
		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: getPixels(int[])  format" + format);
		}
		NDSImage img = emuGetGraphicsImage();
		int imgH =img.getHeight();
		
		if (y  >= imgH) {
			return;
		}
		if (y  + height > imgH) {
			height = imgH - y;
		}
		int startOffset = y * img.getWidth() + x; 

		switch (format) {
			case 8888:
			case 888 :
				emuStorePixelData8888(pixels, offset, scanlength, width, height, img.getPixelData(), startOffset);
				break; 
		}
	}

	public void getPixels(
		short[] pixels,
		int offset,
		int scanlength,
		int x,
		int y,
		int width,
		int height,
		int format) {

		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: getPixels(short[])  format=" + format + " x=" + x + " y=" + y + " w=" + width + " h=" + height +  " offset=" + offset);
		}
		NDSImage img = emuGetGraphicsImage();
		int imgH =img.getHeight();
		
		if (y  >= imgH) {
			return;
		}
		if (y  + height > imgH) {
			height = imgH - y;
		}
		int startOffset = y * img.getWidth() + x; 
		

		switch (format) {
			case 4444 :
				byte[] alphaData = img.getPixelAlphaData();
				if (alphaData == null) {
					emuStorePixelData444(pixels, offset, scanlength, width, height, img.getPixelData(), startOffset);
				} else {
					emuStorePixelData4444(pixels, offset, scanlength, width, height, img.getPixelData(), alphaData, startOffset);
				}
				break;
			case 444 :
				emuStorePixelData444(pixels, offset, scanlength, width, height, img.getPixelData(), startOffset);
				break; 
		}

	}

	public void setARGBColor(int argbColor) {
		if (MainApp.verbose) {
			System.out.println("ExtendedGraphics: setARGBColor() " + Integer.toHexString(argbColor) );
		}
		color = argbColor;
		emuGraphics.setColorAlpha(color);
	}

	/*
	private static void emuConvertPixelData4444(short[] pixels, int width, int height, int offset, int scan,  int manipX, int manipY) {
		int x, y;
		int srcPix;
		int dstPix;
		int srcOffset = 0;
		int dstOffset = 0;
		
		boolean debug = false;
		if (MainApp.verbose && width == 16 && height == 16 ) {
			debug = true;
			System.out.println("!pixel debug:");
		} 
		
		for (y = 0; y < height; y++) {
			srcOffset = offset + (y * scan);
			if (manipY > 0) {
				dstOffset = y * width;
			} else {
				dstOffset = (height - y - 1) * width;
			}
			if (manipX < 0) {
				dstOffset += (width - 1);
			}
			for (x = 0; x < width; x++) {
				srcPix = pixels[srcOffset++];
				if (debug) {
					System.out.print(Integer.toHexString(srcPix));
				}
				//blue
				dstPix = (srcPix & 0x0F) << 4;
				//green
				dstPix |= (srcPix & 0xF0)<< 8;
				//red
				dstPix |= (srcPix & 0xF00) << 12;
				//alpha 
				dstPix |= (srcPix & 0xF000) << 16;
				//final addition / shift - correct the colors (F0 -> FF, 30 -> 33 etc)
				dstPix |= dstPix >> 4;
				//dstPix |= 0xFF000000; //no transparency
				pixelData[dstOffset] = dstPix;
				dstOffset += manipX;
			}
		}
		if (debug) {
			System.out.println();
		}
	}
	
	private static void emuConvertPixelData444(short[] pixels, int width, int height) {
		int x, y;
		int srcPix;
		int dstPix;
		int offset = 0;
		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				srcPix = pixels[offset];
				//blue
				dstPix = (srcPix & 0x0F) << 4;
				//green
				dstPix |= (srcPix & 0xF0)<< 8;
				//red
				dstPix |= (srcPix & 0xF00) << 12;
				//alpha - full opacity 
				dstPix |= 0xF0000000;
				//final shift
				dstPix |= dstPix >> 4;
				//dstPix |= 0xFF000000; //no transparency
				pixelData[offset++] = dstPix;
			}
		} 
	}
	*/
	private void emuStorePixelData444(short[] pixels, int offset, int scan, int width, int height, short[] pixelData, int startOffset) {
		int srcPix;
		short dstPix;
		int x, y;
		int srcOffset = 0;
		int dstOffset = 0;
		
		for (y = 0; y < height; y++) {
			srcOffset = y* width + startOffset;
			dstOffset = offset + (y * scan);
			for (x = 0; x < width; x++) {
				srcPix = pixelData[srcOffset++];
				pixels[dstOffset++] = (short)(0xF000 |
				(((srcPix >> 11) & 0xF)) |
				(((srcPix >> 6) & 0xF) << 4) |  
				(((srcPix >> 1) & 0xF) << 8)
				);
			}
		} 
	}
	private void emuStorePixelData4444(short[] pixels, int offset, int scan, int width, int height, short[] pixelData, byte[] alphaData, int startOffset) {
		int srcPix;
		short dstPix;
		int x, y;
		int srcOffset = 0;
		int dstOffset = 0;
		int alpha;
		
		for (y = 0; y < height; y++) {
			srcOffset = y* width + startOffset;
			dstOffset = offset + (y * scan);
			for (x = 0; x < width; x++) {
				alpha = alphaData[srcOffset] >> 4;
				srcPix = pixelData[srcOffset++];
				pixels[dstOffset++] = (short)((alpha << 12) |
						(((srcPix >> 11) & 0xF)) |
						(((srcPix >> 6) & 0xF) << 4) |  
						(((srcPix >> 1) & 0xF) << 8)
						);
			}
		} 
	}
	private void emuStorePixelData8888(int[] pixels, int offset, int scan, int width, int height, short[] pixelData, int startOffset ) {
		int srcPix;
		short dstPix;
		int x, y;
		int srcOffset = 0;
		int dstOffset = 0;
		
		for (y = 0; y < height; y++) {
			srcOffset = startOffset + (y* width);
			dstOffset = offset + (y * scan);
			for (x = 0; x < width; x++) {
				srcPix = pixelData[srcOffset++];
				pixels[dstOffset++] = 
					(((srcPix >> 10) & 0x1f) << 3) |
					(((srcPix >> 5) & 0x1f) << (11)) |  // 3 + 8
					(((srcPix  ) & 0x1f) << (19)) |  // 3 + 16
					0xFF000000;
			}
		} 
	}
	/*
	private static void emuConvertPixelData8888(int[] pixels, int width, int height, int offset, int scan,  int manipX, int manipY) {
		int x, y;
		int srcPix;
		int dstPix;
		int srcOffset = 0;
		int dstOffset = 0;
		
		for (y = 0; y < height; y++) {
			srcOffset = offset + (y * scan);
			if (manipY > 0) {
				dstOffset = y * width;
			} else {
				dstOffset = (height - y - 1) * width;
			}
			if (manipX < 0) {
				dstOffset += (width - 1);
			}
			for (x = 0; x < width; x++) {
				srcPix = pixels[srcOffset++];
				pixelData[dstOffset] = srcPix;
				dstOffset += manipX;
			}
		}
	}
	private static void emuConvertPixelData888(int[] pixels, int width, int height) {
		int x, y;
		int srcPix;
		int dstPix;
		int offset = 0;
		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				srcPix = pixels[offset];
				pixelData[offset++] = 0xFF000000 | (srcPix & 0xFFFFFF);
			}
		} 
	}
*/	
	public Object getObject() {
		//return the native graphics 
		return emuGraphics;
	}

}
