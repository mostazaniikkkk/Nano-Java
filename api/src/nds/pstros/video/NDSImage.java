package nds.pstros.video;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import nds.Video;


/*
 * Created on May 20, 2008
 *
 */

/**
 * @author ole
 *
 */
public class NDSImage {
	public static final byte TYPE_OPAQUE = 0; //the image is fully opaque (contains only opaque pixels)
	public static final byte TYPE_TRANSP = 1; //the image is transparnt (contains some transparent pixels)
	public static final byte TYPE_ALPHA =  2; //the image is semitransparent (contains some semitransparent pixels)
	
	private static final int MIN_BUF_SIZE = 16 * 1024;
	private static byte[] tmpBuf;
	
	short[] pixelData;
	byte[] pixelDataByte;	//alpha or grayscaled images
	int width;
	int height;
	byte transp;
	NDSGraphics graphics;

	
	public static NDSImage createImage(String filename, Class cls) throws IOException {

		InputStream is = cls.getResourceAsStream(filename);
		DataInputStream din = new DataInputStream(is);
		int size = is.available();
		initTmpBuff(size, false);
		din.readFully(tmpBuf, 0, size);
		din.close();

		return decodeImage(size);
		
	}
	
	private static NDSImage decodeImage(int size) {
		int wh = Video.getImageSize(tmpBuf);
		int w = (wh >> 16) & 0xFFFF;
		int h = wh & 0xFFFF;
		
		NDSImage result = new NDSImage(w,h);
		result.createAlphaChannel();
		int imgType = Video.decodePngImage(tmpBuf, size, result.pixelData, result.pixelDataByte);

		if (imgType == 0) {
			throw new RuntimeException("unknown image format");
		} else
		//Grayscale
		if (imgType == 1) {
			result.pixelData = null;
		} else
		// Palette
		if (imgType == 4) {
			result.pixelDataByte = null;
			result.transp = TYPE_TRANSP;
		}
		return result;

	}
	private static void initTmpBuff(int size, boolean keepData) {
		if (tmpBuf == null) {
			if (size < MIN_BUF_SIZE) {
				size = MIN_BUF_SIZE;
			}
			tmpBuf = new byte[size];
		} else 
		if (size > tmpBuf.length) {
			if (keepData) {
				byte[] newBuf = new byte[(size * 120) / 100];
				System.arraycopy(tmpBuf, 0, newBuf, 0, tmpBuf.length);
				tmpBuf = newBuf;
			} else {
				tmpBuf = new byte[size];
			}
		}
	}
	
	
	public static NDSImage createImage(InputStream is) throws IOException {
		initTmpBuff(0, false);
		int size = readChunks(is);
		if (size < 1) {
			return null;
		}
		return decodeImage(size);
		
		//return createImage(is, NDSImageConsumer.CONV_ARGB_1555);
	}
	
	private static int readChunks(InputStream is) throws IOException {
		int size = 0;
		int pos = 0;
		int blockSize;
		DataInputStream din;
		if (is instanceof DataInputStream) {
			din = (DataInputStream) is;
		} else {
			din = new DataInputStream(is);
		}
		//read png header
		size += 8;
		initTmpBuff(size, true);
		din.readFully(tmpBuf, pos, size);
		//check header
		if ((tmpBuf[0] & 0xFF) != 0x89 || tmpBuf[1] != 0x50 || tmpBuf[2] != 0x4E || tmpBuf[3] != 0x47) {
			//System.out.println("wrong png header!");
			return -1;
		}
		pos += 8;
		//System.out.println("-----------------");
		int chunkSize = 0;
		int chunkId = 0;
		int tmp;
		boolean finish = false;
		
		do {
			//read chunk ( 4 bytes content size, 4 bytes id, 0-XX bytes content, 4 bytes CRC)
			size += 8;
			initTmpBuff(size, true);
			din.readFully(tmpBuf, pos, 8);
			
			//read content size
			chunkSize = 0;
			tmp = tmpBuf[pos++] & 0xFF;
			chunkSize = (tmp << 24);
			tmp = tmpBuf[pos++] & 0xFF;
			chunkSize |= (tmp << 16);
			tmp = tmpBuf[pos++] & 0xFF;
			chunkSize |= (tmp << 8);
			tmp = tmpBuf[pos++] & 0xFF;
			chunkSize |= tmp;
			chunkSize += 4; // + 4 bytes of CRC
			
			//read the chunk id
			chunkId = 0;
			tmp = tmpBuf[pos++] & 0xFF;
			chunkId = (tmp << 24);
			tmp = tmpBuf[pos++] & 0xFF;
			chunkId |= (tmp << 16);
			tmp = tmpBuf[pos++] & 0xFF;
			chunkId |= (tmp << 8);
			tmp = tmpBuf[pos++] & 0xFF;
			chunkId |= tmp; 
//			System.out.println("Chunk: " + Character.toString((char)tmpBuf[pos-4]) +  Character.toString((char)tmpBuf[pos-3]) + Character.toString((char)tmpBuf[pos-2]) + Character.toString((char)tmpBuf[pos-1])
//					+ " size=" + chunkSize);
//			
//			if (chunkSize > 65535) {
//				dumpBuffer(pos);
//			}
			
			//read the chunk content + crc
			size+= chunkSize;
			initTmpBuff(size, true);
			din.readFully(tmpBuf, pos, chunkSize);
			pos+= chunkSize;
		} while (chunkId != 0x49454E44);  //IEND chunk 
		//System.out.println("read chunks size:" + size);
		return size;
	}
	
//	private static void dumpBuffer(int size) {
//		try {
//		DataOutputStream dout = new DataOutputStream(new FileOutputStream(new File ("/dump.img")));
//		dout.write(tmpBuf, 0, size );
//		dout.flush();
//		dout.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//	}
	
	public static NDSImage createImage(InputStream is, int type) throws IOException {
		initTmpBuff(0, false);
		int size = readChunks(is);
		if (size < 1) {
			return null;
		}
		return decodeImage(size);
	}
	
	public static NDSImage createImage(int width, int height) {
		return new NDSImage(width, height);
	}
	public static NDSImage createImage(int width, int height, int ARGBcolor) {
		//FIXME - convert color
		return new NDSImage(width, height, (short)ARGBcolor);
	}
	public static NDSImage createImage(int width, int height, short[] data) {
		return new NDSImage(width, height, data);
	}

	
	
	public NDSImage(int w, int h ) {
		width = w;
		height = h;
		if (w < 1 || h < 1) {
			throw new IllegalArgumentException();
		}
		initPixelData();
	}
	public NDSImage(int w, int h , short color) {
		width = w;
		height = h;
		if (w < 1 || h < 1) {
			throw new IllegalArgumentException();
		}
		initPixelData(color);
	}
	public NDSImage(int w, int h, short[] data ) {
		width = w;
		height = h;
		if (w < 1 || h < 1 ) {
			throw new IllegalArgumentException();
		}
		pixelData = data;
	}
	public NDSImage(int w, int h, short[] data, byte[] alphaData ) {
		width = w;
		height = h;
		if (w < 1 || h < 1 ) {
			throw new IllegalArgumentException();
		}
		pixelData = data;
		pixelDataByte = alphaData;
	}
	
	private void initPixelData() {
		initPixelData((short) 0xFFFF);
	}
	private void initPixelData(final short color) {
		
		if (pixelData == null || pixelData.length < width * height) {
			pixelData = new short[width * height];
		}
		final int size = pixelData.length;
		for (int i = 0; i < size ;i++) {
			pixelData[i] = color;
		}
		
	}
	
	public boolean setPixels(int x, int y, int w, int h, short[] pixels, byte[] alpha, int off, int scansize) {
		final int maxY = y + h;
		int srcIndex = off;
		boolean hasAlpha = false;
		byte a;
		if (alpha == null) {
			for (int j = y; j < maxY; j++) {
				srcIndex = scansize * (j-y) + off;
				int index = width * j + x;
				for (int i = 0; i < w; i++) {
					pixelData[index++] = pixels[srcIndex++];
				}
			}
		} else {
			for (int j = y; j < maxY; j++) {
				srcIndex = scansize * (j-y) + off;
				int index = width * j + x;
				for (int i = 0; i < w; i++) {
					pixelData[index] = pixels[srcIndex];
					a = alpha[srcIndex++];
					if (a == 0) {
						pixelData[index] &= ~0x8000; 
					} else
					if (a != -1 && a != 0) {
						hasAlpha = true;
					}
					pixelDataByte[index++] = a;
				}
			}
		}
		return hasAlpha;
	}

	public short[] getPixelData() {
		return pixelData;
	}
	public byte[] getPixelAlphaData() {
		return pixelDataByte;
	}

	public NDSGraphics getGraphics() {
		return createGraphics();
	}
	public NDSGraphics createGraphics() {
		if (graphics == null) {
			graphics = new NDSGraphics(this);
		} 
		return graphics;
	}
	
	public int getHeight() {
		return height;
	}
	public int getWidth() {
		return width;
	}
	
	public void getRGB8888(int dstX, int dstY, int srcW, int srcH, int[]rgb, int offset, int scanlength) {
		Video.getRGB(
				pixelData, width, height,
				rgb, srcW, srcH,
				dstX, dstY, offset, scanlength,
				pixelDataByte
				);
	}
	
	/*
	 * Checks whether the image contains transparent pixels
	 */
	public void checkTransparency() {
		if (pixelData == null || transp == TYPE_ALPHA) {
			return;
		}
		transp = TYPE_OPAQUE;
		//check the corner pixels of the image first, most of transparent images has the
		//corner pixels transparent. It's just a speed optimization
		if (
				pixelTransparent(0) ||	//top left
				pixelTransparent(width-1) ||	//top right
				pixelTransparent((height-1) * width) || 	//bottom left
				pixelTransparent(width * height - 1) // bottom right
				
		){
			transp = TYPE_TRANSP;
			return;
		}
		
		final int size = pixelData.length;
		for (int i = 0; i < size; i++) {
			if ((pixelData[i] & 0x8000) != 0) {
				transp = TYPE_TRANSP;
				return;
			}
		}
	}
	
	private boolean pixelTransparent(int index) {
		return (pixelData[index] & 0x8000) != 0; 
	}
	
	public byte getTransparency() {
		return transp;
	}
	public void setTransparency(byte trn) {
		transp = trn;
	}
	public void createAlphaChannel(byte[] src) {
		createAlphaChannel();
		if (src != null) {
			System.arraycopy(src, 0, pixelDataByte, 0, pixelDataByte.length);
		}
	}
	public void createAlphaChannel() {
		pixelDataByte = new byte[width * height];
		transp = TYPE_ALPHA;
	}
	public void deleteAlphaChannel() {
		pixelDataByte = null;
	}
	
	public void setSize(int w, int h, boolean resetGraphics) {
		if (w*h <= pixelData.length) {
			width = w;
			height = h;
		} else {
			if (pixelData != null) {
				pixelData = new short[w*h];
			}
			if (pixelDataByte != null) {
				pixelDataByte = new byte[w*h];
			}
		}
		//reset the graphics object if it was already required 
		if (resetGraphics && graphics != null) {
			graphics = new NDSGraphics(this);
		}
	}
}
