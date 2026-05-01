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

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import nds.pstros.video.NDSGraphics;
import nds.pstros.video.NDSImage;


public class TiledLayer extends Layer{
	private static final int MAX_ANIM_TILES = 128;
	private static final int ANCHOR = Graphics.TOP | Graphics.LEFT; 	
	
	private int maxX;		//number of tile columns
	private int maxY;		//number of tile rows
	
	private int imageX;		//width - size in tiles
	private int imageY;		//height - size in tiles
	private int tileW;		//tile width		
	private int tileH;		//tile height
	private NDSImage tiles[];	//image separated to tiles  - its 30x faster than having only one image
	private boolean[][] solid; //pixel collision data
	
	private short[] tileMap;		//tile map;
	private int mapSize;
	
	private short[] animationMap;		//animation map;
	private int animationMapMax;		//animation map - last index	
	

	public TiledLayer(int columns, int rows, Image image, int tileWidth, int tileHeight) {
		if (image == null) {
			throw new NullPointerException("image is null!");
		}
		if (columns < 1 || rows < 1) {
			throw new IllegalArgumentException("columns=" + columns + " rows=" + rows);
		}
		maxX = columns;
		maxY = rows;
		
		mapSize = maxX * maxY;
		tileMap = new short[mapSize];
		
		animationMapMax = 0;	//no animation tiles specified
		animationMap = new short[MAX_ANIM_TILES];	
		
		setStaticTileSet(image, tileWidth, tileHeight);
		setVisible(true);
	}
	public void setStaticTileSet(Image image, int tileWidth, int tileHeight) {
		if (image == null) {
			throw new NullPointerException("image is null!");
		}
		if (tileWidth < 1 || tileHeight < 1) {
			throw new IllegalArgumentException("tileWidth=" + tileWidth + " tileHeight=" + tileHeight);
		}
		int oldTileCount = imageX * imageY;
		int imageW = image.getWidth();
		int imageH = image.getHeight();
		if (imageW % tileWidth != 0) {
			throw new IllegalArgumentException("not an integer multiple! tileWidth=" + tileWidth + " imageWidth=" + imageW);
		}
		if (imageH % tileHeight != 0) {
			throw new IllegalArgumentException("not an integer multiple! tileHeight=" + tileHeight + " imageHeight=" + imageH);
		}
		tileW = tileWidth;
		tileH = tileHeight;
		
		imageX = imageW / tileW;
		imageY = imageH / tileH;
		
		layerWidth = maxX * tileW;
		layerHeight = maxY * tileH;

		//now create single tiles - rip them from the image		
		int size = imageX * imageY;
		int tileX, tileY;
		tiles = new NDSImage[size];
		solid = new boolean[size][];
		NDSGraphics g;
		NDSImage img = image.emuGetImage(0);
		int imgW = img.getWidth();
		NDSImage dstImage;
		byte[] srcAlpha = img.getPixelAlphaData();
		for (int i = 0; i < size; i++) { 
			dstImage = tiles[i] = NDSImage.createImage(tileW, tileH, 0);
			if (srcAlpha != null) {
				dstImage.createAlphaChannel();
			}
			//region of the tile within image
			tileX = (i % imageX) * tileW;
			tileY = (i / imageX) * tileH;
			if (!dstImage.setPixels(0,0, tileW, tileH, img.getPixelData(), img.getPixelAlphaData(), tileY *  imgW + tileX, imgW)) {
				dstImage.deleteAlphaChannel();
				dstImage.checkTransparency();
			}
		}
		
		//new tileset image has less number of tiles 
		if (size < oldTileCount) {
			// clear the tile map
			for (int i = 0; i < mapSize; i++) {
				tileMap[i] = 0;
			}
			//clear animation map
			for (int i = 0; i < animationMapMax; i++) {
				animationMap[i] = 0;
			}
			animationMapMax = 0;	//no animation mapping 
		}
		
		
	}
	
	public int createAnimatedTile(int staticTileIndex) {
		if (staticTileIndex < 1 || staticTileIndex > imageX * imageY ) {
			throw new IndexOutOfBoundsException("staticTileIndex=" + staticTileIndex + " max allowed value=" + (imageX*imageY));
		}
		//TODO check for array out of bounds
		animationMap[animationMapMax] = (short)staticTileIndex;
		animationMapMax++;
		return -animationMapMax;
	}
	
	public void setAnimatedTile(int animatedTileIndex, int staticTileIndex){
		if (staticTileIndex < 1 || staticTileIndex > imageX * imageY ) {
			throw new IndexOutOfBoundsException("staticTileIndex=" + staticTileIndex + " max allowed value=" + (imageX*imageY));
		}
		if (-animatedTileIndex > animationMapMax) {
			throw new IndexOutOfBoundsException("animatedTileIndex=" + animatedTileIndex + " max allowed value=" + (-animationMapMax));			
		}
		animationMap[-animatedTileIndex -1] = (short) staticTileIndex;		
	}
	public int getAnimatedTile(int animatedTileIndex) {
		if (-animatedTileIndex > animationMapMax) {
			throw new IndexOutOfBoundsException("animatedTileIndex=" + animatedTileIndex + " max allowed value=" + (-animationMapMax));			
		}
		return animationMap[-animatedTileIndex -1];
	}
	public void setCell(int col, int row, int tileIndex) {
		if (col >= maxX || row >= maxY) {
			throw new IndexOutOfBoundsException("col=" + col + " max allowed column=" + maxX + " row=" + row + " max alowed row=" + maxY);
		}
		tileMap[row*maxX + col] = (short)tileIndex;
	}
	public int getCell(int col, int row) {
		if (col >= maxX || row >= maxY) {
			throw new IndexOutOfBoundsException("col=" + col + " max allowed column=" + maxX + " row=" + row + " max alowed row=" + maxY);
		}
		return tileMap[row*maxX + col];
	}
	public final int getCellWidth() {
		return tileW;
	}
	public final int getCellHeight() {
		return tileH;
	}
	public final int getColumns() {
		return maxX;
	}
	public final int getRows() {
		return maxY;
	}
	public void fillCells(int col, int row, int numCols, int numRows, int tileIndex) {
		int j, i;
		int maxRow = row + numRows;
		int maxCol = col + numCols;
		int idx;
		
		if (numCols < 1 || numRows < 0 || col >= maxX || maxCol > maxX || row >= maxY || maxRow > maxY) {
			throw new IllegalArgumentException("invalid region specified!");
		}
		for (j = row; j < maxRow; j++) {
			idx = j * maxX + col;
			for (i = col; i < maxCol; i++) {
				tileMap[idx] = (short)tileIndex;
				idx++;				
			} 
		} 
	}
	public void paint(Graphics g) {
		if (!isVisible()) {
			return;
		}
		int x = getX();
		int y = getY();
		int posX, posY;
		int j, i;
		int mapIndex;
		int tileIndex;
		int tileX, tileY;

		int trX = g.getTranslateX();
		int trY = g.getTranslateY();

		//compute rendering bounds - measured in tiles
		int x1 = (-x -trX ) / tileW;
		int x2 = x1 + (Display.WIDTH / tileW) + 2;
		
		int y1 = (-y - trY) / tileH;
		int y2 = y1 + (Display.HEIGHT / tileH) + 2;
		
		if (x1 < 0) {
			x1 = 0;
		}
		if (x2 > maxX) {
			x2 = maxX;
		}
		
		if (y1 < 0) {
			y1 = 0;
		}
		if (y2 > maxY) {
			y2 = maxY;
		}

		//System.out.println("TiledLayer.paint()  maxX=" + maxX + " maxY=" + maxY + " tileW=" + tileW + " tileH=" + tileH + " trX=" + trX + " trY=" + trY +  " x=" + x + " y=" + y + " x1=" + x1 + " x2=" +x2);
		//go through the rows of the tilemap 
		for (j = y1; j < y2; j++) {
			mapIndex = j * maxX + x1;
			//compute tile position in pixels
			posY = y + j * tileH;
			posX = x + x1 * tileW;
			//go through the columns of the tilemap 
			for (i = x1; i < x2; i++) {
				tileIndex = tileMap[mapIndex];
				//if tile is not empty
				if (tileIndex != 0) {
					//tile is animated -> substitute animated index 
					if (tileIndex < 0) {
						tileIndex = animationMap[-tileIndex -1];
					} 
					tileIndex--;

					//tile is NOT empty - fully transparent
					if (tileIndex > -1) {
						//render the tile
//						if (tileIndex < tiles.length) {
							g.emuDrawImage(tiles[tileIndex], posX, posY);
//						} else {
//							System.out.println("Pstros: internal error: TiledLayer: oti=" + tMap[mapIndex] + " max=" + tiles.length);
//						}
					}
				}
				//pick next index in the row
				mapIndex++;
				//set X postiion of the next tile
				posX += tileW;
			}
		}
		
	}
	
	private boolean[] emuCreateSolid(int tileIndex) {
		NDSImage bi =  tiles[tileIndex];
		boolean[] data = new boolean[tileW * tileH];
		short rgb[] = bi.getPixelData();
		for (int i = 0; i < data.length; i++) {
			data[i] = ((rgb[i] & 0x8000) == 0x8000);
		}
		return data;
	}

	boolean emuCollideWithPoint(int pointX, int pointY) {
		int x = getX();
		int y = getY();
		
		int x1 = (-x + pointX ) / tileW;
		int y1 = (-y + pointY ) / tileH;
		
		//System.out.println("x1=" + x1 + " y1=" + y1 + " maxX=" + maxX + " maxY=" + maxY);
		//the point is out of the layer boundaries
		if (x1 < 0 || y1 < 0 || x1 >= maxX || y1 >= maxY) {
			//System.out.println(" Out of bounds !");
			return false;
		}
				
		int mapIndex = y1 * maxX + x1;
		int tileIndex = tileMap[mapIndex];
				
		//System.out.println("tl x=" + x + " y=" + y + " point x=" + pointX + " y=" + pointY + " tileIndex=" + tileIndex);
		
		return tileIndex != 0;	
	}
	
	boolean emuPixelCollision(int sX, int sW, int sY, int sH, int sOffset, int sScanLength, boolean[] sSolid) {
		
		int cX = getX();
		int cY = getY();
		int x1, y1;		//tile position within the layer
		int x2, y2;		//local position of the pixel within the tile
		int pixelX, pixelY;
		
		int mapIndex, tileIndex;
		boolean[] dSolid;
		  
		int sIndex;
		for (int pointY = 0; pointY < sH; pointY++) {
			sIndex = sOffset  + (sScanLength * pointY);
			pixelY = y1 = (-cY + pointY + sY);	//global pixel position Y
			
			y2 = y1 % tileH;
			//the source point is out of the layer boundaries - Y axis
			y1 /= tileH;
			if (pixelY < 0 || y1 >= maxY) {
				continue;
			}
			for (int pointX = 0; pointX < sW; pointX++) {
				//the source point is solid 
				if (sSolid[sIndex]) {
					pixelX = x1 = (-cX + pointX + sX);	//global pixel position X
					x2 = x1 % tileW;	
					x1 /= tileW;
					//the source point is inside the layer boundaries - X axis
					if (pixelX >= 0  &&   x1 < maxX ) {
				
						mapIndex = y1 * maxX + x1;
						tileIndex = tileMap[mapIndex];
				
						//if tile is not empty
						if (tileIndex != 0) {
							//substitute animated index 
							if (tileIndex < 0) {
								tileIndex = animationMap[-tileIndex -1];
							} 
							tileIndex--;
							if (tileIndex >= 0) {
								dSolid = solid[tileIndex];
								if (dSolid == null) {
									dSolid = emuCreateSolid(tileIndex);
									solid[tileIndex] = dSolid;
								}
								//try {
								if (dSolid[y2 * tileW + x2]) {
									return true;
								}	
								//} catch (Exception e) {
								//	System.out.println("y1=" + y1 + " x1=" + x1 + " maxX=" + maxX + " maxY=" + maxY + " x2=" + x2 + " y2=" + y2 + " pixX=" + pixelX + " pixY=" + pixelY);
								//	e.printStackTrace();
								//}					
							}
						}
					}
				}
				sIndex++;				
			}
		}
		
		return false;
	}
}
