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
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import nds.pstros.MainApp;


public class Sprite extends Layer {
	private static final int ANCHOR = Graphics.TOP | Graphics.LEFT;
	
	/*   TRANS_NONE          TRANS_ROT_180
 	 *   +------+------+     +------+------+
	 *   | *    |      |     |      |      |
	 *   |      |      |     |      |      |
	 *   |      |      |     |      |    * |
	 *   +------+------+     +------+------+
	 * 
	 *   TRANS_MIRROR        TRANS_MIRROR_ROT_180
 	 *   +------+------+     +------+------+
	 *   |      |    * |     |      |      |
	 *   |      |      |     |      |      |
	 *   |      |      |     | *    |      |
	 *   +------+------+     +------+------+
	 * 
	 * 
	 */
	
	
	public static final int TRANS_NONE = 0;
	public static final int TRANS_MIRROR_ROT180 = 1;
	public static final int TRANS_MIRROR = 2;
	public static final int TRANS_ROT180 = 3;
	public static final int TRANS_MIRROR_ROT270 = 4;
	public static final int TRANS_ROT90 = 5;
	public static final int TRANS_ROT270 = 6;
	public static final int TRANS_MIRROR_ROT90 = 7;
	
	
	private static int[] tmpPoint = new int[4];
	
	Image image;
	int frameW;
	int frameH;
	int[] frameSeq;
	int imgW;
	int imgH;
	int transform;
	int transformDX;	//position correction according current transformation
	int transformDY;
		
	int spotX;
	int spotY;
	private int posX;
	private int posY;
	private int curFrame;	
	
	
	//collision rectangle in un-transformed state  
	int collisionX;
	int collisionY;
	int collisionW;
	int collisionH;
	
	//collision rectangle in transformed state
	int cX;	//collision rect X
	int cY;	//collision rect Y
	int cW;	//collision rect W
	int cH;	//collision rect H
	
	public Sprite(Image image) {
		this(image, image.getWidth(), image.getHeight());
	}
	public Sprite(Image image, int frameWidth, int frameHeight) {
		setImage(image, frameWidth, frameHeight);
		setVisible(true);
	}
	
	public Sprite(Sprite s) {
		this(s.image, s.frameW, s.frameH);
		setFrameSequence(s.frameSeq);
		spotX = s.spotX;
		spotY = s.spotY;
		transform = s.transform;
		transformDX = s.transformDX;
		transformDY = s.transformDY;
		cX = s.cX;
		cY = s.cY;
		cW = s.cW;
		cH = s.cH;
	}
	public void setFrameSequence(int[] sequence) {
		frameSeq = sequence;
	}
	public void defineReferencePixel(int rX, int rY) {
		//System.out.println("Sprite@" + hashCode() + " defineReferencePixel x=" + rX + " y=" + rY );
		posX += rX - spotX;
		posY += rY - spotY;
		spotX = rX;
		spotY = rY; 
	}
	public void setRefPixelPosition(int rX, int rY) {
		//System.out.println("Sprite@" + hashCode() + " setRefPixelPosition x=" + rX + " y=" + rY  + " rpix x=" + spotX + " rpix y="  +spotY);
		posX = rX;
		posY = rY;
		emuUpdateTransformedDistances();
	}
	public int getRefPixelX() {
		return posX;
	}
	public int getRefPixelY() {
		return posY;
	}
	
	
	public void setPosition(int pX, int pY) {
		//System.out.println("Set position: x=" + x + " y=" + y);
		posX = pX + spotX - transformDX;
		posY = pY + spotY - transformDY;
		super.setPosition(pX,pY);
	}
	public void move(int dx, int dy) {
		posX += dx;
		posY += dy;
		super.move(dx, dy);
	}
	public void setFrame(int sequenceIndex) {
		curFrame = sequenceIndex;
	}
	public final int getFrame() {
		return curFrame;
	}
	public int getRawFrameCount() {
		return imgW * imgH;
		//return 1;
	}
	public int getFrameSequenceLength() {
		if (frameSeq == null) {
			return imgW * imgH;
		}
		return frameSeq.length;
	}
	public void nextFrame() {
		int max; 
		if (frameSeq == null) {
			max = imgW * imgH;
		} else {
			max = frameSeq.length;
		}
		curFrame++;
		if (curFrame >= max) {
			curFrame = 0; 
		}
	}
	public void prevFrame() {
		int max; 
		if (frameSeq == null) {
			max = imgW * imgH;
		} else {
			max = frameSeq.length;
		}
		curFrame--;
		if (curFrame < 0) {
			curFrame =  max - 1; 
		}
	}
	
	public void setImage(Image img, int frameWidth, int frameHeight) {
		image = img;
		
		if (frameW != frameWidth || frameH != frameHeight) {
			collisionX = 0;
			collisionY = 0;
			collisionW = cW = frameWidth;
			collisionH = cH = frameHeight;
		}
		
		frameW = frameWidth;
		frameH = frameHeight;
		imgW = image.getWidth() / frameW;
		imgH = image.getHeight() / frameH;
		
		//System.out.println("Sprite@" + hashCode() + " setImage fW=" + frameWidth + " fH=" + frameHeight  + " iW=" + imgW + " imgH=" + imgH);
		layerWidth = frameWidth;
		layerHeight = frameHeight;
		frameSeq = null;
		curFrame = 0;
		
	}
	public void setTransform(int transform) {
		switch(transform) {
			case TRANS_NONE:
			case TRANS_ROT180:
			case TRANS_MIRROR:
			case TRANS_MIRROR_ROT180:
				layerWidth = frameW;
				layerHeight = frameH;
				cW = collisionW;
				cH = collisionH;
				this.transform = transform;
				break;
			case TRANS_ROT90:
			case TRANS_MIRROR_ROT90:
			case TRANS_MIRROR_ROT270:
			case TRANS_ROT270:
				layerWidth = frameH;
				layerHeight = frameW;
				cW = collisionH;
				cH = collisionW;
				this.transform = transform;
				break;
			default:
				throw new IllegalArgumentException("invalid transform=" + transform);
		}
		emuUpdateTransformedDistances();
	}
	
	private void emuUpdateTransformedDistances() {
		switch (transform) {
			case TRANS_NONE :
				transformDX = 0;
				transformDY = 0;
				cX = collisionX;
				cY = collisionY;
				break;
			case TRANS_MIRROR_ROT180: // 1
				transformDX = 0;
				transformDY = -(frameH-1 - 2 * spotY);
				cX = collisionX;
				cY = frameH - collisionY - collisionH; 
				break;
			case TRANS_MIRROR: //2
				transformDX = -(frameW-1 - 2 * spotX);
				transformDY = 0;
				cX = frameW - collisionX - collisionW; 
				cY = collisionY;
				break;
			case TRANS_ROT180: // 3
				transformDX = -(frameW-1 - 2 * spotX);
				transformDY = -(frameH-1 - 2 * spotY);
				cX = frameW - collisionX - collisionW; 
				cY = frameH - collisionY - collisionH; 
				break;
			case TRANS_MIRROR_ROT270: // 4
				transformDX = spotX - spotY;
				transformDY = -(spotX - spotY);
				cX = collisionY;
				cY = collisionX;
				break;
			case TRANS_ROT90:	// 5	
				transformDX =-(frameH - spotX - spotY - 1);
				transformDY =-(spotX - spotY);
				cX = frameH - collisionY - collisionH;
				cY = collisionX; 
				break;
			case TRANS_ROT270:	// 6
				transformDX = spotX - spotY;
				transformDY = -(frameW - spotX - spotY - 1);
				cX = collisionY;
				cY = frameW - collisionX - collisionW;
				break;
			case TRANS_MIRROR_ROT90:	// 7
				transformDX = -(frameH - spotX - spotY - 1);
				transformDY = -(frameW - spotX - spotY - 1);
				cX = frameH - collisionY - collisionH;
				cY = frameW - collisionX - collisionW;
				break;
		}
		//set sprite position
		x = posX - spotX + transformDX;
		y = posY - spotY + transformDY;
	}
	
	public void defineCollisionRectangle(int x, int y, int width, int height) {
		//System.out.println("Sprite@" + hashCode() + " defineCollisionRectangle x=" + x + " y=" + y  + " w=" + width + " h=" + height);

		collisionX = x;
		collisionY = y;
		collisionW = cW = width;
		collisionH = cH = height;
		emuUpdateTransformedDistances();
	}
	
	private boolean emuRectCollision(int sx,int  sw, int sy, int sh, int dx, int dw, int dy, int dh) {
		//source
		int sx1 = sx;
		int sx2 = sx + sw;
		int sy1 = sy;
		int sy2 = sy + sh;
		//destination 
		int dx1 = dx;
		int dx2 = dx + dw;
		int dy1 = dy;
		int dy2 = dy + dh;
		
		
		if(	sx2 >= dx1 && sx1 <= dx2 && 
			sy2 >= dy1 && sy1 <= dy2)
			return true;
		return false;
	}
	
	//assume that the rect collision is true 
	private boolean emuPixelCollision(
		//source collision box
		int sx,int  sw, int sy, int sh,
		//destination collision box 
		int dx, int dw, int dy, int dh,
		
		//source collision data
		int sOffset, int sScanLength,
		boolean[] sSolid,
		
		//destination collision data
		int dOffset, int dScanLength,
		boolean[] dSolid) {
			
		//source
		int sx1 = sx;
		int sx2 = sx + sw;
		int sy1 = sy;
		int sy2 = sy + sh;
		//destination 
		int dx1 = dx;
		int dx2 = dx + dw;
		int dy1 = dy;
		int dy2 = dy + dh;
		
		int rectX = sx > dx ? sx : dx;
		int rectY = sy > dy ? sy : dy;
		
		int rectW = sx2 > dx2 ? dx2 - rectX : sx2 - rectX;
		int rectH = sy2 > dy2 ? dy2 - rectY : sy2 - rectY;
		
		int sOffsX = rectX - sx;
		int dOffsX = rectX - dx;
		int sOffsY = rectY - sy;
		int dOffsY = rectY - dy;
		
		int x, y;
		int sIndex, dIndex;
		
		if (dSolid == null) {
			System.out.println("!!! dsolid is null");
			return false;
		}
		if (sSolid == null) {
			System.out.println("!!! ssolid is null");
			return false;
		}
		
		for (y  = 0; y < rectH; y++) {
			sIndex = sOffset + ((sOffsY + y) * sScanLength) + sOffsX;
			dIndex = dOffset + ((dOffsY + y) * dScanLength) + dOffsX;
			if (y == 0) {
				//System.out.println("offsets: s=" + sIndex + " d=" + dIndex + " sOpaque=" + sSolid[sIndex] + " dOpaque=" + dSolid[dIndex]);
			}
			for (x = 0; x < rectW; x++) {
				if (sIndex > sSolid.length) {
					System.out.println("!! Source: max=" + sSolid.length);
				}
				if (dIndex > dSolid.length) {
					System.out.println("!! Destin: max=" + dSolid.length + " current=" + dIndex +  " rW=" + rectW + " rH=" + rectH + " dOY=" + dOffsY + " y=" + y + " x=" + x + " dScan=" + dScanLength + " dOffs=" + dOffset + " imgW=" + image.getWidth() + " imgH=" + image.getHeight());
				}
				if (sSolid[sIndex++] && dSolid[dIndex++]) {
					return true;
				} 
			} 
		}
		return false;
	}
	
	int emuGetFrameIndex() {
		if (frameSeq == null) {
			return curFrame;
		} else {
			if (curFrame >= frameSeq.length) {
				curFrame = 0;
			}
			return frameSeq[curFrame];
		}
	}
	
	private void emuTransformCoordinate(
		int xSrc,
		int ySrc,
		int width,
		int height,
		int[] result) {
		
		Image img = image;
			int tmp;
			int imgWidth = 0;
			switch (transform) {
				//0
				case TRANS_NONE:
					imgWidth = img.getWidth();
					break;
				//1
				case TRANS_MIRROR_ROT180:
					ySrc = img.getHeight() - ySrc - height;
					imgWidth = img.getWidth();
					break;
				//2
				case TRANS_MIRROR:
					xSrc = img.getWidth() - xSrc - width;
					imgWidth = img.getWidth();
					break; 
				//3
				case TRANS_ROT180:
					ySrc = img.getHeight() - ySrc - height;
					xSrc = img.getWidth() - xSrc - width;
					imgWidth = img.getWidth();
					break;
				//4
				case TRANS_MIRROR_ROT270: 				
				//System.out.println(" x_src=" + x_src + " y_src=" + y_src + " w=" + width + " h=" + height + " transform=" + transform + " x_dst="  + x_dest + " y_dst="  + y_dest + " anchor=" + anchor + " trX=" + trX + " trY=" + trY);
					tmp = width;
					width = height;
					height = tmp;
					tmp = xSrc;
					xSrc = ySrc;
					ySrc = tmp;
					imgWidth = img.getHeight();
					break;
				//5;
				case TRANS_ROT90: 
				//System.out.println(" x_src=" + x_src + " y_src=" + y_src + " w=" + width + " h=" + height + " transform=" + transform + " x_dst="  + x_dest + " y_dst="  + y_dest + " anchor=" + anchor + " trX=" + trX + " trY=" + trY);
					imgWidth = img.getHeight();
					tmp = width;
					width = height;
					height = tmp;
					tmp = xSrc;
					xSrc = ySrc;
					ySrc = tmp;
					xSrc = img.getHeight() - xSrc - width;
					break;
				//6;
				 case TRANS_ROT270:
					imgWidth = img.getHeight();
					tmp = width;
					width = height;
					height = tmp;
					tmp = xSrc;
					xSrc = ySrc;
					ySrc = tmp;
					ySrc = img.getWidth() - ySrc - height;
					break;
				//7
				 case TRANS_MIRROR_ROT90:
					imgWidth = img.getHeight();
					tmp = width;
					width = height;
					height = tmp;
					tmp = xSrc;
					xSrc = ySrc;
					ySrc = tmp;
					ySrc = img.getWidth() - ySrc - height;
					xSrc = img.getHeight() - xSrc - width;
					break;
				default:
					if (transform != 0) {
						System.out.println(" unsupported transform ! " + transform);
						return;
					}
			}
			result[0] = xSrc;
			result[1] = ySrc;
			result[2] = imgWidth;
	}
	
	public final boolean collidesWith(Sprite s, boolean pixelLevel) {
		if (MainApp.verbose) {
			System.out.println("Sprite@" + hashCode() + " collidesWith(Sprite) pixelLevel=" + pixelLevel);
		}
		if (!isVisible() || !s.isVisible()) {
			return false;
		}
		int srcX = getX()+ cX;
		int srcY = getY()+ cY;
		int dstX = s.getX() + s.cX;
		int dstY = s.getY() + s.cY;  
		boolean result = emuRectCollision(srcX, cW, srcY, cH, dstX, s.cW, dstY, s.cH);
		
		
		if (result && pixelLevel) {

			int sImgPosX;
			int sImgPosY;
			int dImgPosX;
			int dImgPosY;
			int sOffset;
			int dOffset;
			int sScanLength;
			int dScanLength;

			int sImgIndex = emuGetFrameIndex();
			int dImgIndex = s.emuGetFrameIndex();
			
			sImgPosX = (sImgIndex % imgW) * frameW;
			sImgPosY = (sImgIndex / imgW) * frameH;
			emuTransformCoordinate(sImgPosX, sImgPosY, frameW, frameH, tmpPoint);
			sImgPosX = tmpPoint[0];
			sImgPosY = tmpPoint[1];
			sScanLength = tmpPoint[2]; //width of the transformed image
			sOffset = (sImgPosY + cY) * sScanLength + (sImgPosX + cX);
			//System.out.println("Src: x=" + sImgPosX + " y=" + sImgPosY + " scan=" + sScanLength  + " offs=" + sOffset);
			
			dImgPosX = (dImgIndex % s.imgW) * s.frameW;
			dImgPosY = (dImgIndex / s.imgW) * s.frameH;
			s.emuTransformCoordinate(dImgPosX, dImgPosY, s.frameW, s.frameH, tmpPoint);
			dImgPosX = tmpPoint[0];
			dImgPosY = tmpPoint[1];
			dScanLength = tmpPoint[2]; //width of the transformed image
			dOffset = (dImgPosY + s.cY) * dScanLength + (dImgPosX + s.cX);
			//System.out.println("Dst: x=" + dImgPosX + " y=" + dImgPosY + " scan=" + dScanLength  + " offs=" + dOffset);
			

			boolean[] srcCollision = image.emuGetCollisionData(transform);
			tmpPoint[0] = transform;
			boolean[] dstCollision = s.image.emuGetCollisionData(s.transform);
			tmpPoint[0] = s.transform;
			 
			
			result = emuPixelCollision(
				srcX, cW, srcY, cH, 
				dstX, s.cW, dstY, s.cH, 
				sOffset, sScanLength,					
				srcCollision,
				dOffset, dScanLength, 
				dstCollision); 			
		} 
		
		return result;
	}
	public final boolean collidesWith(TiledLayer t,  boolean pixelLevel) {
		if (MainApp.verbose) {
			System.out.println("Sprite@" + hashCode() + " collidesWith(TiledLayer)  pixelLevel="+ pixelLevel);
		}
		if (t == null || !t.isVisible() || !isVisible()) {
			return false;
		} 

		int srcX = getX()+ cX;
		int srcY = getY()+ cY;
		//test border points of the collision rectangle
		if (!pixelLevel) {
			if (t.emuCollideWithPoint(srcX, srcY)) return true;
			if (t.emuCollideWithPoint(srcX + cW, srcY)) return true;
			if (t.emuCollideWithPoint(srcX, srcY + cH)) return true;
			if (t.emuCollideWithPoint(srcX + cW, srcY + cH)) return true;
		} else {
			boolean c1 = t.emuCollideWithPoint(srcX, srcY);
			boolean c2 = t.emuCollideWithPoint(srcX + cW, srcY);
			boolean c3 = t.emuCollideWithPoint(srcX, srcY + cH);
			boolean c4 = t.emuCollideWithPoint(srcX + cW, srcY + cH);
			
			//one of the point is colliding - test transparent pixels
 			if (c1 || c2 || c3 || c4 ) {
				int sImgPosX;
				int sImgPosY;
				int sOffset;
				int sScanLength;

				int sImgIndex = emuGetFrameIndex();
			
				sImgPosX = (sImgIndex % imgW) * frameW;
				sImgPosY = (sImgIndex / imgW) * frameH;
				emuTransformCoordinate(sImgPosX, sImgPosY, frameW, frameH, tmpPoint);
				sImgPosX = tmpPoint[0];
				sImgPosY = tmpPoint[1];
				sScanLength = tmpPoint[2]; //width of the transformed image
				sOffset = (sImgPosY + cY) * sScanLength + (sImgPosX + cX);
				 		
				//System.out.println("  pixel collision: srcX=" + srcX + " srcY="+ srcY + " cW=" + cW + " cH=" + cH +" sOffset=" + sOffset + " sl=" + sScanLength + " imgIndex=" + sImgIndex);
				boolean[] srcCollision = image.emuGetCollisionData(transform);
				
 				return t.emuPixelCollision(
					srcX, cW, srcY, cH, 
					sOffset, sScanLength,					
					srcCollision
					);
			} else {
				//no point is colliding - then there is no collision
				return false;
			}
		}

		return false;
	}
	public final boolean collidesWith(Image image, int x, int y, boolean pixelLevel) {
		if (MainApp.verbose) {
			System.out.println("Sprite@" + hashCode() + " collidesWith(Image)");
		}
		return emuRectCollision(getX() + cX, cW, getY() + cY, cH, x, image.getWidth(), y, image.getHeight());
	}
	
	
	
	public void paint(Graphics g) {
		if (!isVisible()) {
			return;
		}
		int imgIndex = emuGetFrameIndex();
		int paintX = getX();  
		int paintY = getY(); 
		
		if (MainApp.verbose) {
			System.out.println("Sprite@" + hashCode() + " paint x=" + paintX + " y=" + paintY  + " spotX=" + spotX + " spotY=" + spotY + " frW=" + frameW + " frameH=" + frameH + " transform=" + transform);
		}
		int imgPosX = (imgIndex % imgW) * frameW;
		int imgPosY = (imgIndex / imgW) * frameH;

		
		//System.out.println("ii=" + imgIndex + " x=" + paintX + " y=" + paintY + " transform=" + transform + " tr x=" + g.getTranslateX() + " tr y=" + g.getTranslateY() + " posX=" + getX() + " posY=" + getY());
		g.drawRegion(image, imgPosX, imgPosY, frameW, frameH, transform, paintX, paintY, ANCHOR);
		
		// ***** debug collision rectangle  ******
		/*
		g.setColor(0xFFFFFF);
		g.drawRect(paintX + cX, paintY + cY, cW, cH);
		*/
	}
	

}
