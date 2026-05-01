/*
 * Created on May 22, 2008
 *
 */
package nds.pstros.video;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import nds.Video;

/**
 * @author ole
 *
 */
public class NDSFont {
	public static final int PLAIN = 0;
	public static final int BOLD = 1;
	
	
	private String name;
	private int style;
	private int size;
	protected int fontHeight;
	
	protected int totalGlyphs;
	protected int firstGlyph;
	protected int baseLine;
	protected short[] glyphData;
	protected NDSImage image;
	
	private static NDSFont systemFont;
	
	public NDSFont(String name, int style, int size) {
		this.name = name;
		this.style = style;
		this.size = size;
		if (systemFont == null) {
			readFont("system.dsf");
			systemFont = this;
		} else {
			fontHeight = systemFont.fontHeight;
			totalGlyphs = systemFont.totalGlyphs;
			firstGlyph = systemFont.firstGlyph;
			glyphData = systemFont.glyphData;
			image = systemFont.image;
			baseLine = systemFont.baseLine;
		}
	}
	
	public int getStringWidth(char[] text, int offs, int length) {
		int w = 0;
		final int max = text.length;
		for (int i = 0; i < max; i++ ) {
			int index = text[i];
			int val = glyphData[index] & 0xFFFF; // ? & 0xFFFF
			w+= ((val >> 12) + 1); 
		}
		return w;
	}
	public int getStringWidth(String text ) {
		return getStringWidth(text.toCharArray(), 0, text.length());
	}
	public int getHeight() {
		return fontHeight;
	}
	
	public int getBaseLine() {
		return baseLine;
	}
	
	public String getFontName() {
		return name;
	}
	public int getSize() {
		return size;
	}
	
	private static native byte[] getSystemFontData();

	//todo - spacing
	private void readFont(String filename) throws RuntimeException{
		InputStream is = new ByteArrayInputStream(getSystemFontData());
		DataInputStream din = new DataInputStream(is);
		try {
			int magic = din.readInt();
			if (magic != 0x6E647366) {
				throw new IllegalStateException("wrong font data!");
			}
			int version = din.readByte();
			totalGlyphs = din.readShort();
			firstGlyph = din.readShort();
			baseLine = din.readByte();
			glyphData = new short[totalGlyphs];
			for (int i = 0; i < totalGlyphs; i++) {
				glyphData[i] = din.readShort();
			}
			int imgSize = din.readShort();
			//image = NDSImage.createImage(din, NDSImageConsumer.CONV_GRAY);
			image = NDSImage.createImage(din);
			fontHeight = image.getHeight();
			
		} catch (IOException e) {
			throw new IllegalStateException(e.toString());
		}
	}
	void drawString(final short[] dst, final int dstW, final int dstH, String text, int x, int y, final short color, final int clX, final int clY, final int clW, final int clH ) {
		if (text == null ) {
			return;
		}
		y -= baseLine;
		//check clip
		if (y > clY + clH || y + fontHeight < clY) {
			return;
		}
		final int clX2 = clX + clW;
		final int imageWidth = image.getWidth();
		int rX;
		int rW;
		
		char[] txt = text.toCharArray();
		final int max = txt.length;
		final byte[] pixelData = image.pixelDataByte;
		for (int i = 0; i < max; i++ ) {
			int correction = 0;
			int index = txt[i];
			int val = glyphData[index] & 0xFFFF; // ? & 0xFFFF
			int w = ((val >> 12));
			val &= 0xFFF;
			
			if (clX > x) {
				rX = clX;
				correction = clX - x;
			} else {
				rX = x;
			}
			rW = (clX2 < x+ w ) ? x + w - clX2 : w;
			
			Video.blitGray(
					dst, dstW, dstH,
					pixelData, imageWidth, fontHeight,
					x - val ,y, color, 
					rX, clY, rW - correction, clH
			);
			
			x+= w + 1; 
		}
		
	}
}
