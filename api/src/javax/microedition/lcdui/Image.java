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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.game.Sprite;

import nds.pstros.MainApp;
import nds.pstros.video.NDSGraphics;
import nds.pstros.video.NDSImage;


public class Image {
	//private static final int IMAGE_DELAY = 50;
	
	private static int[] tmpPixels;
	
	private NDSImage[] emuImages;
	private int width;
	private int height;
	private boolean[] emuValid;
	
	private boolean[][] collisionData;
	Graphics graphics;
	

	private Image(NDSImage image) {
		if (image == null) {
			throw new IllegalArgumentException();
		}
		emuImages = new NDSImage[8];
		collisionData = new boolean[8][];
		emuValid = new boolean[8];
		emuImages[0] = image;
		//System.out.println("Image constructor emuImage=" + emuImage);
		if (emuImages[0] == null) {
			System.out.println("Image constructor: null emuImage!" );
		} 
		width = emuImages[0].getWidth();
		height = emuImages[0].getHeight();
	}
	
	public NDSImage emuGetImage(int transform) {
		NDSImage result = emuImages[transform]; 
		if (transform != 0 && !emuValid[transform]) {
			emuCreateTransformedImage(transform);
			result = emuImages[transform];
			emuValid[transform] = true;
		}
		return result;
	}
	
	public boolean[] emuGetCollisionData(int transform) {
		boolean[] result = collisionData[transform];
		if (result == null) {
			emuCreateCollisionData(transform);
			result = collisionData[transform];
		}
		return result;		
	}
	
	public Object getObject() {
		//return non transformed native image
		return emuImages[0];
	}
	
	private void emuCreateCollisionData(int transform) {
		int imgW = getWidth();
		int imgH = getHeight();
		boolean[] data = new boolean[imgW * imgH];
		
		if (transform > 3) {
			int tmp = imgW;
			imgW = imgH;
			imgH = tmp;
		}
		
		short[] rgb = emuImages[transform].getPixelData();
		for (int i = 0; i < data.length; i++) {
			data[i] = ((rgb[i] & 0x8000) == 0x8000);
		}
		collisionData[transform] = data;		
	}
	
	private void emuCreateTransformedImage(int transform) {
		NDSImage src = emuImages[0];
		int w = src.getWidth();
		int h = src.getHeight();
		emuImages[transform] = emuCreateTransformedImage(src, 0, 0, w,h, transform);
	}
	
	private static NDSImage emuCreateTransformedImage(NDSImage src, int x, int y,  int w, int h,  int transform) {
		NDSImage dst = null;
		if (src == null) {
			return null;
		}
		byte srcTransparency = src.getTransparency();
		//create temporary image
		NDSImage tmpImage = new NDSImage(w,h, (short) 0);

		NDSGraphics g = tmpImage.createGraphics();
		
		//copy original (non transformed) image to buffered image
		g.drawImage(src, -x, -y);
		
		//copy alpha channel
		if (srcTransparency == NDSImage.TYPE_ALPHA) {
			tmpImage.createAlphaChannel(src.getPixelAlphaData());
		}

		if (transform == 0) {
			tmpImage.setTransparency(srcTransparency);
			return tmpImage;
		}
		
		//create pixel data arrays
		short[] dataSrc = new short[w*h];
		short[] dataDst = new short[w*h];
		byte[] alphaSrc = null;
		byte[] alphaDst = null;
		//store pixels of the source image into dataSrc array 
		//src.getRGB(0,0,w,h, dataSrc, 0, w);
		System.arraycopy(src.getPixelData(), 0, dataSrc, 0, dataSrc.length);
		if (srcTransparency == NDSImage.TYPE_ALPHA) {
			alphaSrc = new byte[w*h];
			alphaDst = new byte[w*h];
			System.arraycopy(src.getPixelAlphaData(), 0, alphaSrc, 0, alphaSrc.length);
		}
		
		 
		//make transformation of the dataSrc and store it in the dataDst
		//then create final image (dst)
		//System.out.println("Trans =" + transform + " srcImage=" + src +"  w=" + w + " h=" + h);
		switch (transform) {
			//TRANS_MIRROR_ROT180
			case Sprite.TRANS_MIRROR_ROT180: 
				emuHorizontalSwap(dataDst, dataSrc, alphaDst, alphaSrc, w,h); 
				dst =  new NDSImage(w,h, dataDst, alphaDst);
				break;
			//TRANS_MIRROR_ROT90
				case Sprite.TRANS_MIRROR_ROT90: 
				emuHorizontalSwap(dataDst, dataSrc, alphaDst, alphaSrc, w,h);
				System.arraycopy(dataDst,0, dataSrc,0 ,w*h);
				if (srcTransparency == NDSImage.TYPE_ALPHA) {
					System.arraycopy(alphaDst,0, alphaSrc,0 ,w*h);
				}
				emuRotate270(dataDst, dataSrc, alphaDst, alphaSrc,w, h);
				dst =  new NDSImage(h,w, dataDst, alphaDst);
				break;
			//TRANS_MIRROR_ROT270
				case Sprite.TRANS_MIRROR_ROT270: 
				emuHorizontalSwap(dataDst, dataSrc, alphaDst, alphaSrc,w,h);
				System.arraycopy(dataDst,0, dataSrc,0 ,w*h); 
				if (srcTransparency == NDSImage.TYPE_ALPHA) {
					System.arraycopy(alphaDst,0, alphaSrc,0 ,w*h);
				}
				emuRotate90(dataDst, dataSrc, alphaDst, alphaSrc,w, h);
				dst =  new NDSImage(h,w, dataDst, alphaDst);
				break;
			//TRANS_MIRROR
			case Sprite.TRANS_MIRROR: 
				emuVerticalSwap(dataDst, dataSrc, alphaDst, alphaSrc,w,h);
				dst =  new NDSImage(w,h, dataDst, alphaDst);
				break;
			//TRANS_ROT180
			case Sprite.TRANS_ROT180: 
				emuHorizontalSwap(dataDst, dataSrc, alphaDst, alphaSrc,w,h);
				System.arraycopy(dataDst,0, dataSrc,0 ,w*h);
				if (srcTransparency == NDSImage.TYPE_ALPHA) {
					System.arraycopy(alphaDst,0, alphaSrc,0 ,w*h);
				}
				emuVerticalSwap(dataDst, dataSrc, alphaDst, alphaSrc,w,h);
				dst =  new NDSImage(w,h, dataDst, alphaDst);
				break;
			case Sprite.TRANS_ROT90:
				emuRotate90(dataDst, dataSrc, alphaDst, alphaSrc,w, h);
				dst = new NDSImage(h,w, dataDst, alphaDst);
				break;
			case Sprite.TRANS_ROT270:
				emuRotate270(dataDst, dataSrc, alphaDst, alphaSrc,w, h);
				dst = new NDSImage(h,w, dataDst, alphaDst);
				break;
				
			default:
				dst = tmpImage;
				System.out.println("Pstros: Image.emuCreateTransformedImage() - Unsupported transform! " + transform);
		}
		dst.setTransparency(srcTransparency);
		return dst;
	}
	
	private static void emuHorizontalSwap(short[] dataDst, short[] dataSrc, byte[] alphaDst, byte[] alphaSrc, int w, int h) {
		for (int y = 0; y < h; y++) {
			int baseSrc = y*w;
			int baseDst = (h-y-1) * w; 
			for (int x = 0; x< w; x++) {
				dataDst[baseDst + x] = dataSrc[baseSrc + x];
			}
		}
		if (alphaSrc == null) {
			return;
		}
		for (int y = 0; y < h; y++) {
			int baseSrc = y*w;
			int baseDst = (h-y-1) * w; 
			for (int x = 0; x< w; x++) {
				alphaDst[baseDst + x] = alphaSrc[baseSrc + x];
			}
		}
	}
	private static void emuVerticalSwap(short[] dataDst, short[] dataSrc,  byte[] alphaDst, byte[] alphaSrc, int w, int h) {
		for (int x = 0; x< w; x++) {
			int baseSrc = x;
			int baseDst = w-x-1; 
			for (int y = 0; y < h; y++) {
				dataDst[baseDst] = dataSrc[baseSrc];
				baseDst+= w;
				baseSrc+= w;
			}
		}
		if (alphaSrc == null) {
			return;
		}
		for (int x = 0; x< w; x++) {
			int baseSrc = x;
			int baseDst = w-x-1; 
			for (int y = 0; y < h; y++) {
				alphaDst[baseDst] = alphaSrc[baseSrc];
				baseDst+= w;
				baseSrc+= w;
			}
		}
	}
	private static void emuRotate90(short[] dataDst, short[] dataSrc,  byte[] alphaDst, byte[] alphaSrc, int w, int h) {
		for (int y = 0; y < h; y++) {
			int baseSrc = y*w;
			int baseDst = (h-y-1); 
			for (int x = 0; x< w; x++) {
				dataDst[baseDst + x*h] = dataSrc[baseSrc + x];
			}
		}
		if (alphaSrc == null) {
			return;
		}
		for (int y = 0; y < h; y++) {
			int baseSrc = y*w;
			int baseDst = (h-y-1); 
			for (int x = 0; x< w; x++) {
				alphaDst[baseDst + x*h] = alphaSrc[baseSrc + x];
			}
		}
	}	
	private static void emuRotate270(short[] dataDst, short[] dataSrc,  byte[] alphaDst, byte[] alphaSrc, int w, int h) {
		for (int y = 0; y < h; y++) {
			int baseSrc = y*w;
			int baseDst = (h * (w-1)) + y; 
			for (int x = 0; x< w; x++) {
				dataDst[baseDst - x*h] = dataSrc[baseSrc + x];
			}
		}
		if (alphaSrc == null) {
			return;
		}
		for (int y = 0; y < h; y++) {
			int baseSrc = y*w;
			int baseDst = (h * (w-1)) + y; 
			for (int x = 0; x< w; x++) {
				alphaDst[baseDst - x*h] = alphaSrc[baseSrc + x];
			}
		}
	}	

	
	public static Image createImage(int w, int h) { 
		if (w < 1 || h < 1) {
			throw new IllegalArgumentException("wrong image size");
		}
		Image image = new Image(new NDSImage(w,h));
		if (MainApp.verbose) {
			System.out.println("Image.createImage(w, h) w=" +w + " h=" + h + " image=" + image.hashCode());
		}
		return image;
	}
	
	public static Image createImage(Image source){
		if (MainApp.verbose) {
			System.out.println("Image.createImage(Image source).. ");
		}
		if (source == null) {
			throw new NullPointerException("source image is null"); 
		}
		NDSImage bi = emuCreateTransformedImage(source.emuGetImage(0), 0,0, source.getWidth(), source.getHeight(), 0); 
		return new Image(bi);
	}
	
	public static Image createImage(String name) throws IOException {
		if (MainApp.verbose) {
			System.out.println("Image.createImage(String name) =" + name);
		}
		
		if (name == null) {
			if (MainApp.verbose) {
				System.out.println("resource name is null" );
			}
			throw new NullPointerException("resource name is null");
		}
		
		Image result = new Image(NDSImage.createImage(name, MainApp.midlet.getClass()));
//		if (true) {
//			NDSImage bi = result.emuGetImage(0);
//			System.out.println("" + name + " type=" + bi.getTransparency() + " pd=" + bi.getPixelData()[0]);
//		}
		if (MainApp.verbose) {
			System.out.println(" image created=" + result + " w=" + result.getWidth() + " h=" + result.getHeight());
		}
		return result;
		 
	}
	
	
	
	public static Image createImage(byte[] imageData, int imageOffset, int imageLength)throws IOException{
		if (MainApp.verbose) {
			System.out.println("Image.createImage(byte[] imageData, int imageOffset, int imageLength)  data.length=" + imageData.length + " offset=" + imageOffset + " length=" + imageLength);
		}

		if (imageData == null) {
			throw new NullPointerException("imageData is null");
		}
		byte[] data;
		if (imageOffset == 0 && imageData.length == imageLength) {
			data = imageData;
		} else  {
			data = new byte[imageLength];
			System.arraycopy(imageData, imageOffset, data, 0, imageLength);
		}
		Image result = new Image(NDSImage.createImage(new ByteArrayInputStream(data)));
		if (MainApp.verbose) {
			System.out.println(" image created=" + result + " w=" + result.getWidth() + " h=" + result.getHeight());
		}
		return result;
	}

	public static Image createImage(Image image, int x, int y, int width, int height, int transform){
		if (MainApp.verbose) {
			System.out.println("Image.createImage(Image image, int x, int y, int width, int height, int transform) x=" + x + " y=" + y + " transform=" + transform);
		}
		NDSImage bi = emuCreateTransformedImage(image.emuGetImage(0), x,y, width, height, transform);
		if (bi == null) {
			return null;
		}		
		return new Image(bi);		
	}
	public static Image createImage(int width, int height, int colorARGB) {
		Image result =  new Image(NDSImage.createImage(width, height, colorARGB));
		if (MainApp.verbose) {
			System.out.println("Image.createImage(w, h, colorARGB) w=" + width + " h=" + height + " image=" + result);
		}
		return result;
	}
	
	public static Image createImage(InputStream stream) throws IOException {
		if (stream == null) {
			throw new NullPointerException("input stream is null");
		}
		
		Image result = new Image(NDSImage.createImage(stream));
		if (MainApp.verbose) {
			System.out.println("Image.createImage(w, h, colorARGB) w=" + result.getWidth() + " h=" + result.getHeight() + " image=" + result);
		}
		return result;		
	}
	
	
	public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
		if (width < 1 || height < 1) {
			throw new IllegalArgumentException("wrong image size");
		}
		NDSImage bi = new NDSImage(width, height, (short)0xFFFF);
		
		
		NDSGraphics g = bi.getGraphics();
		if (processAlpha) {
			bi.createAlphaChannel(); //transparency is set to ALPHA
			g.drawRGB8888(rgb, width, height, 0, width, 1,1, 0);
			//FIXME - optimise (remove) unused alpha data if 
		} else {
			g.drawRGB888(rgb, width, height, 0, width, 1,1, 0);
		}
		
		Image image = new Image(bi);
		//System.out.println("cRGBIm w=" +width + " h=" + height + " a=" + processAlpha);
		if (MainApp.verbose) {
			System.out.println("Image.createRGBImage(w, h) w=" +width + " h=" + height + " a=" + processAlpha + " image=" + image.hashCode());
		}
		return image;
	}
	
	
	public Graphics getGraphics() throws IllegalStateException {
		if (emuImages[0] == null) {
			System.out.println("Error: getGraphics: emuImage  is null!");
			return null;
		}
		if (graphics != null) {
			return graphics; 
		}
		Graphics result = new Graphics();
		result.emuSetGraphics(emuImages[0].getGraphics());
		result.emuSetGraphicsImage(emuImages[0]);
		result.emuSetImage(this);
		//hack - the image is too large - probably a background cache
		//disable the invalidate after every drawImage()
		if (width >= Display.WIDTH && height >= Display.HEIGHT / 2) {
			result.emuSetFixed(true);
		}
		result.setClip(0,0,width,height);
		result.setColor(0);
		result.setFont(Font.getDefaultFont());
		result.setStrokeStyle(Graphics.SOLID);
		if (MainApp.verbose) {
			System.out.println("Image: get graphics =" + result.hashCode() +" srcImage=@" + hashCode());
		}
		graphics = result;
		return result;
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public boolean isMutable() {
		return false;
	}

	//package private 
	void emuInvalidate() {
		for (int i = 1; i < 7; i++) {
			emuValid[i] = false;
			collisionData[i] = null;
		} 		
	}

	private static int cacheImageW;
	private static int cacheImageH;
	
	
	public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
		NDSImage src = emuImages[0];
		if (MainApp.verbose) {
			System.out.println("getRGB  x=" + x + " y=" + y + " w=" + width + " h=" + height + " off=" + offset + " scanl=" + scanlength + " ? alpha=" + (src.getPixelAlphaData() != null) + " src=" + src);
		}
		//store pixels of the source image into rgbData array 
		src.getRGB8888(x, y, width, height, rgbData, offset, scanlength);
	}
	


	
}
