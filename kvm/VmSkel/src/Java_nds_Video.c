#include <nds.h>
#include <nds/arm9/video.h>
#include <nds/registers_alt.h>
#include <kni.h> 
#include <stdio.h> 

//#include <garbage.h>

#include <gxj_image.h>

/* Defined in video.itcm.c */
void drawTriangle(unsigned short *dst, int dstW, int dstH,
                  int x1, int y1, int x2, int y2, int x3, int y3,
                  unsigned short pixel,
                  int clipX, int clipY, int clipW, int clipH);


 
 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_lcdSwap() { 
    lcdSwap();
    KNI_ReturnVoid(); 
} 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_initVideo() { 
	// set the mode for 2 text layers and two extended background layers
	videoSetMode(MODE_5_2D | DISPLAY_BG3_ACTIVE);
	// set the first bank as background memory and the third as sub background memory
	// B and D are not used (if you want a bitmap greater than 256x256 you will need more
	// memory so another vram bank must be used and mapped consecutivly
	vramSetMainBanks(	VRAM_A_MAIN_BG_0x06000000, VRAM_B_LCD,
						VRAM_C_SUB_BG , VRAM_D_LCD);

	// set up our bitmap background

	BG3_CR = BG_BMP16_256x256;

	// these are rotation backgrounds so you must set the rotation attributes:
	// these are fixed point numbers with the low 8 bits the fractional part
	// this basicaly gives it a 1:1 translation in x and y so you get a nice flat bitmap
	BG3_XDX = 1 << 8;
	BG3_XDY = 0;
	BG3_YDX = 0;
	BG3_YDY = 1 << 8; //192
	//BG3_YDY = 278; //virtual height of 208 pixels

	BG3_CX = 0;
	BG3_CY = 0;

	//clean the memory
	memset((short*) 0x06000000,0x0, 256 * 256 * 2);

//	opaqueDraws = 0;
//	transpDraws = 0;
	

	KNI_ReturnVoid(); 
} 




KNIEXPORT KNI_RETURNTYPE_INT Java_nds_Video_decodePngImage() { 
	imageSrcData imageSrc;
	memoryFile imageMem;
	imageDstData imageDst;
	imageData img;
	int result;

	KNI_StartHandles(3); 
	KNI_DeclareHandle(dataBufferHandle); 
	KNI_DeclareHandle(pixelBufferHandle); 
	KNI_DeclareHandle(alphaBufferHandle); 
	
	KNI_GetParameterAsObject(1, dataBufferHandle);
	KNI_GetParameterAsObject(3, pixelBufferHandle);
	KNI_GetParameterAsObject(4, alphaBufferHandle);
	
	int bufSize = KNI_GetParameterAsInt(2);
	
	char* dataBuffer = (char*) KNI_GetRawArrayRegionPtr(dataBufferHandle, 0);
	char* alphaBuffer = (char*) KNI_GetRawArrayRegionPtr(alphaBufferHandle, 0);
	unsigned short* pixelBuffer  = (unsigned short*) KNI_GetRawArrayRegionPtr(pixelBufferHandle, 0);


	imageMem.data = dataBuffer;
	imageMem.pos = 0;
	imageMem.size = bufSize;
	initImageSrcData(&imageSrc, &imageMem);

    img.palette = (unsigned short*) malloc(256 * 2);
    img.pixelBuf = pixelBuffer; //pb
    img.alphaBuf = alphaBuffer; //ab
    initImageDstData(&imageDst, &img);
    
    result = decode_png_image(&imageSrc, &imageDst);
    free(img.palette);
    //free(pb);

    if (result != 0) {
               result = img.imgType + 1;
    }

	KNI_EndHandles(); 
	KNI_ReturnInt(result);
}

/*
    public static native void blit(
	short[] dst, int dstW, int dstH,
	short[] src, int srcW, int srcH,
	int dstX, int dstY,
	int clipX, int clipY, int clipW, int clipH ,
	byte transp, byte[] alpha
    );
*/
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_blit() { 
	int i,j,j2;
	int dstOffset, srcOffset;
	unsigned short pixel;
	int hasAlpha;

	unsigned int A, AM;
	int dR,dG, dB;
	int sR,sG, sB;
	unsigned int backPixel;

	int srcX = 0;
	int srcY = 0;
	int incX = 0;
	int incY = 0;

	KNI_StartHandles(3); 

	KNI_DeclareHandle(srcArrayHandle); 
	KNI_DeclareHandle(dstArrayHandle); 
	KNI_DeclareHandle(alphaArrayHandle); 

	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int srcW = KNI_GetParameterAsInt(5);
	int srcH = KNI_GetParameterAsInt(6);

	int dstX = KNI_GetParameterAsInt(7);
	int dstY = KNI_GetParameterAsInt(8);

	int clipX = KNI_GetParameterAsInt(9);
	int clipY = KNI_GetParameterAsInt(10);
	int clipW = KNI_GetParameterAsInt(11);
	int clipH = KNI_GetParameterAsInt(12);
	//transparency type: 0-opaque 1-transparent
	jbyte transp = KNI_GetParameterAsByte(13);


	int dstMaxX;
	int dstMaxY;

	KNI_GetParameterAsObject(1, dstArrayHandle);
	KNI_GetParameterAsObject(4, srcArrayHandle);
	KNI_GetParameterAsObject(14, alphaArrayHandle);

	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);
	jshort* src  = (jshort *) KNI_GetRawArrayRegionPtr(srcArrayHandle, 0);
	char* alpha = (char*) KNI_GetRawArrayRegionPtr(alphaArrayHandle, 0);

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
	}
	//check the alpha channel exists
	hasAlpha = 1;
	if (alpha < 0xF) { // == NULL
		hasAlpha = 0;
	}


   	//check clip Horizontaly    	
	if (clipX > dstX) {    		
		incX = (clipX - dstX);     		
		srcX += incX;    		
		dstX += incX;    	
	}
	dstMaxX = dstX + srcW - incX;
    	if (clipX + clipW <  dstMaxX) {    		
		dstMaxX = clipX + clipW;      	
	}

    	//check clip Vertically    	
	if (clipY > dstY) {    		
		incY = (clipY - dstY );     		
		srcY += incY;    		
		dstY += incY;    	
	}    	
	dstMaxY = dstY + srcH - incY;    	
	if (clipY + clipH < dstMaxY) {    		
		dstMaxY = clipY + clipH;    	
	}



	//blit inside the destination bounds
	if (dstMaxX >= 0 && dstMaxY >= 0 && dstX < dstW && dstY < dstH) {
		if (dstX < 0) {
			srcX = -dstX;
			dstX = 0;
		}
		if (dstMaxX > dstW) {
			dstMaxX = dstW;
		}
		
		if (dstY < 0) {
			srcY = -dstY;
			dstY = 0;
		}
		if (dstMaxY > dstH) {
			dstMaxY = dstH;
		}
		
		j2 = srcY;
		//contains transparent pixels
		if (transp) {
//			transpDraws++;
			for (j = dstY ; j < dstMaxY; j++, j2++) {
				dstOffset = j * dstW + dstX;
				srcOffset = j2 * srcW + srcX; 
				//use alpha blending
				if (hasAlpha) {
					for (i = dstX; i < dstMaxX; i++) {
						pixel = src[srcOffset];
						A = alpha[srcOffset++];
						//draw pixels
						if (A == 0xFF) { // fully opaque pixel
							dst[dstOffset] = pixel;
						}
						else 
						if (A != 0) {

	       						AM = 255 - A;
		       					backPixel = dst[dstOffset];
		       					dR = ((backPixel >> 10) & 0x1F) * AM;
	       						dG = ((backPixel >> 5) & 0x1F) * AM;
	       						dB = ((backPixel ) & 0x1F) * AM;
	       					
	       						sR = ((pixel >> 10) & 0x1F) * A;
	       						sG = ((pixel >> 5 ) & 0x1F) * A;
	       						sB = ((pixel) & 0x1F) * A;
	       					
	       					
		       					dR += sR;
		       					dG += sG;
	       						dB += sB;
	       					
	       					
	       						dR >>= 8;
	       						dG >>= 8;
	       						dB >>= 8;
	       					
	       						dst[dstOffset] = (short)(0x8000 | (dR<<10 ) | (dG<<5) | (dB)); 		
						}
						dstOffset++;
					}
				} 
				//no alpha blending
				else {
					for (i = dstX; i < dstMaxX; i++) {
						pixel = src[srcOffset++];
						//draw only opaque pixels
						if ((pixel & 0x8000) != 0) { //!=0
							dst[dstOffset] = pixel;
						}
						dstOffset++;
					}
				}
			}       
		} 
		//contains only opaque pixels
		else {
//			opaqueDraws++;
			for (j = dstY ; j < dstMaxY; j++, j2++) {
				dstOffset = j * dstW + dstX;
				srcOffset = j2 * srcW + srcX; 
				for (i = dstX; i < dstMaxX; i++) {
					dst[dstOffset++] = src[srcOffset++];
				}
			}       
		}
	}

//	if ((opaqueDraws + transpDraws) % 10000 == 0) {
//		printf("blit t:%i o:%i \n", transpDraws, opaqueDraws);
//	}
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
}

/*
    public static native void blitRGB(
	short[] dst, int dstW, int dstH,
	int[]   src, int srcW, int srcH, 
	int dstX,int dstY, 
    int offset, int scanlength,
	int clipX, int clipY, int clipW, int clipH, 
	byte[] alpha, boolean  processAlpha
    );
*/

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_blitRGB() { 
	int i,j,j2;
	int dstOffset, srcOffset;
	unsigned int pixel;
	int hasAlpha;

	unsigned int A, AM;
	int dR,dG, dB, dA;
	int sR,sG, sB;
	unsigned int backPixel;

	int srcX = 0;
	int srcY = 0;
	int incX = 0;
	int incY = 0;

	KNI_StartHandles(3); 

	KNI_DeclareHandle(srcArrayHandle); 
	KNI_DeclareHandle(dstArrayHandle); 
	KNI_DeclareHandle(alphaArrayHandle); 

	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int srcW = KNI_GetParameterAsInt(5);
	int srcH = KNI_GetParameterAsInt(6);

	int dstX = KNI_GetParameterAsInt(7);
	int dstY = KNI_GetParameterAsInt(8);

    int offset = KNI_GetParameterAsInt(9);
    int scanlength = KNI_GetParameterAsInt(10);
    
	int clipX = KNI_GetParameterAsInt(11);
	int clipY = KNI_GetParameterAsInt(12);
	int clipW = KNI_GetParameterAsInt(13);
	int clipH = KNI_GetParameterAsInt(14);
	//transparency type: 0-opaque 1-transparent
	jbyte processAlpha = KNI_GetParameterAsByte(16);


	int dstMaxX;
	int dstMaxY;

	KNI_GetParameterAsObject(1, dstArrayHandle);
	KNI_GetParameterAsObject(4, srcArrayHandle);
	KNI_GetParameterAsObject(15, alphaArrayHandle);

	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);
	unsigned int* src  = (unsigned int *) KNI_GetRawArrayRegionPtr(srcArrayHandle, 0);
	unsigned char*  alpha = (unsigned char*) KNI_GetRawArrayRegionPtr(alphaArrayHandle, 0);
	

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}
		//check the alpha channel exists
	hasAlpha = 1;
	if (alpha < 0xF) { // == NULL
		hasAlpha = 0;
	}
	//printf("\x1b[19B dst =%p src=%p ac=%p t=%i a=%i \n", dst, src, alpha, transp, hasAlpha);

   	//check clip Horizontaly    	
	if (clipX > dstX) {    		
		incX = (clipX - dstX);     		
		srcX += incX;    		
		dstX += incX;    	
	}
	dstMaxX = dstX + srcW - incX;
    	if (clipX + clipW <  dstMaxX) {    		
		dstMaxX = clipX + clipW;      	
	}

    	//check clip Vertically    	
	if (clipY > dstY) {    		
		incY = (clipY - dstY );     		
		srcY += incY;    		
		dstY += incY;    	
	}    	
	dstMaxY = dstY + srcH - incY;    	
	if (clipY + clipH < dstMaxY) {    		
		dstMaxY = clipY + clipH;    	
	}



	//blit inside the destination bounds
	if (dstMaxX >= 0 && dstMaxY >= 0 && dstX < dstW && dstY < dstH) {
		if (dstX < 0) {
			srcX = -dstX;
			dstX = 0;
		}
		if (dstMaxX > dstW) {
			dstMaxX = dstW;
		}
		
		if (dstY < 0) {
			srcY = -dstY;
			dstY = 0;
		}
		if (dstMaxY > dstH) {
			dstMaxY = dstH;
		}
		
		j2 = srcY;
		//contains transparent pixels
//			transpDraws++;
			for (j = dstY ; j < dstMaxY; j++, j2++) {
				dstOffset = j * dstW + dstX;
				srcOffset = j2 * scanlength + srcX + offset; 
				//use alpha blending
				if (processAlpha) {
					for (i = dstX; i < dstMaxX; i++) {
						pixel = src[srcOffset++];
						A = (pixel >> 24) & 0xFF;;
						//draw pixels
						if (A == 0xFF) { // fully opaque pixel
							dst[dstOffset] = (unsigned short)(0x8000 | //alpha 
		       							((pixel >> 19) & 0x1f) |
		       							((pixel >> 6) & 0x3e0) |
		       							((pixel << 7) & 0x7c00));
		       				if (hasAlpha) {
                                alpha[dstOffset] = 0xFF;
                            }
						}
						else 
						if (A != 0) {

	       						AM = 255 - A;
		       					backPixel = dst[dstOffset];
		       					dB = ((backPixel >> 10) & 0x1F) * AM;
	       						dG = ((backPixel >> 5) & 0x1F) * AM;
	       						dR = ((backPixel ) & 0x1F) * AM;
	       					
	       						sR = ((pixel >> 19) & 0x1F) * A;
	       						sG = ((pixel >> 11) & 0x1F) * A;
	       						sB = ((pixel >> 3 ) & 0x1F) * A;
	       					
	       					
		       					dR += sR;
		       					dG += sG;
	       						dB += sB;
	       					
	       					
	       						dR >>= 8;
	       						dG >>= 8;
	       						dB >>= 8;
	       					
	       						dst[dstOffset] = (short)(0x8000 | (dB<<10 ) | (dG<<5) | (dR)); 	
                                if (hasAlpha) {
                                    //add the alpha channels of the source and the destination
                                    dA = alpha[dstOffset] + A; 
                                    //clamp the result
                                    if (dA > 0xFF) {
                                        alpha[dstOffset] = 0xFF;
                                    } else {
                                        alpha[dstOffset] = dA;
                                    }
                                }
						}
						dstOffset++;
					}
				} 
				//no alpha blending
				else {
					for (i = dstX; i < dstMaxX; i++) {
						pixel = src[srcOffset++];
						//draw only opaque pixels
						dst[dstOffset] =  (unsigned short)
            						(0x8000 | //alpha 
	       							((pixel >> 19) & 0x1f) |
	       							((pixel >> 6) & 0x3e0) |
	       							((pixel << 7) & 0x7c00));
	       				if (hasAlpha) {
                               alpha[dstOffset] = 0xFF;
                        }
                        dstOffset ++;
	       							
					}
				}
			}       
		 
	
	}

//	if ((opaqueDraws + transpDraws) % 10000 == 0) {
//		printf("blit t:%i o:%i \n", transpDraws, opaqueDraws);
//	}
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
}


/*
    public static native void blitRGB4444(
	short[] dst, int dstW, int dstH,
	short[] src, int srcW, int srcH,
	int dstX,int dstY, int offset, int scan,
	byte[] alpha, boolean  processAlpha,
	int manipX, int manipY, int rotate
    );
*/
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_blitRGB4444() { 
	unsigned int pixel;
	int hasAlpha;


	int x, y;
	int srcPix;
	int dstPix;
	int srcOffset = 0;
	int dstOffset = 0;

	KNI_StartHandles(3); 

	KNI_DeclareHandle(srcArrayHandle); 
	KNI_DeclareHandle(dstArrayHandle); 
	KNI_DeclareHandle(alphaArrayHandle); 

	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int srcW = KNI_GetParameterAsInt(5);
	int srcH = KNI_GetParameterAsInt(6);

	int dstX = KNI_GetParameterAsInt(7);
	int dstY = KNI_GetParameterAsInt(8);

	int offset = KNI_GetParameterAsInt(9);
	int scan = KNI_GetParameterAsInt(10);

	int manipX = KNI_GetParameterAsInt(13);
	int manipY = KNI_GetParameterAsInt(14);
	int rotate = KNI_GetParameterAsInt(15);
	
	int rotManipX = manipX;

    
	//transparency type: 0-opaque 1-transparent
	jbyte processAlpha = KNI_GetParameterAsByte(12);


	KNI_GetParameterAsObject(1, dstArrayHandle);
	KNI_GetParameterAsObject(4, srcArrayHandle);
	KNI_GetParameterAsObject(11, alphaArrayHandle);

	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);
	unsigned short* src  = (unsigned short *) KNI_GetRawArrayRegionPtr(srcArrayHandle, 0);
	unsigned char*  alpha = (unsigned char*) KNI_GetRawArrayRegionPtr(alphaArrayHandle, 0);
	unsigned char a;
	

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}
	//check the alpha channel exists
	hasAlpha = 1;
	if (alpha < 0xF) { // == NULL
		hasAlpha = 0;
	}
	
	if (rotate) {
    	rotManipX = - manipY * srcH;
    }

	for (y = 0; y < srcH; y++) {
		srcOffset = offset + (y * scan);
		if (manipY > 0) {
			if (rotate) {
				dstOffset = (srcW - 1) * srcH + y; 
			} else {
				dstOffset = y * srcW;
			}
		} else {
			if (rotate) {
				dstOffset = y;
			} else {
				dstOffset = (srcH - y - 1) * srcW;
			}
		}
		if (manipX < 0) {
			if (rotate) {
				dstOffset += (srcH-1 - 2*y);
			} else {
				dstOffset += (srcW - 1);
			}
		}		
		
		if (processAlpha) {
			for (x = 0; x < srcW; x++) {
				srcPix = src[srcOffset++];
				//blue
				dstPix = (srcPix & 0x0F) << 11;
				//green
				dstPix |= (srcPix & 0xF0)<< 2;
				//red
				dstPix |= (srcPix & 0xF00) >> 7;
				//alpha
				dstPix |= 0x8000;
                a = (unsigned char)((srcPix & 0xF000) >> 8);
                //don't touch totaly transparent pixels
                if (a) {
                    a |= 0xF;
                } 
				alpha[dstOffset] = a;
				dst[dstOffset] = (unsigned short)dstPix;
				dstOffset += rotManipX;
			}
		} else 
		{
			for (x = 0; x < srcW; x++) {
				srcPix = src[srcOffset++];
				//blue
				dstPix = (srcPix & 0x0F) << 11;
				//green
				dstPix |= (srcPix & 0xF0)<< 2;
				//red
				dstPix |= (srcPix & 0xF00) >> 7;
				//alpha
				dstPix |= 0x8000; 
					
				dst[dstOffset] = (unsigned short)dstPix;
				dstOffset += rotManipX;
			}
		}
	}
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
}

/*
    public static native void blitRGB8888(
	short[] dst, int dstW, int dstH,
	int[] src, int srcW, int srcH,
	int dstX,int dstY, int offset, int scan,
	byte[] alpha, boolean  processAlpha,
	int manipX, int manipY, int rotate
    );

*/

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_blitRGB8888() { 
	unsigned int pixel;
	int hasAlpha;


	int x, y;
	int srcPix;
	int dstPix;
	int srcOffset = 0;
	int dstOffset = 0;

	KNI_StartHandles(3); 

	KNI_DeclareHandle(srcArrayHandle); 
	KNI_DeclareHandle(dstArrayHandle); 
	KNI_DeclareHandle(alphaArrayHandle); 

	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int srcW = KNI_GetParameterAsInt(5);
	int srcH = KNI_GetParameterAsInt(6);

	int dstX = KNI_GetParameterAsInt(7);
	int dstY = KNI_GetParameterAsInt(8);

	int offset = KNI_GetParameterAsInt(9);
	int scan = KNI_GetParameterAsInt(10);

	int manipX = KNI_GetParameterAsInt(13);
	int manipY = KNI_GetParameterAsInt(14);
	
	int rotate = KNI_GetParameterAsInt(15);
	int rotManipX = manipX;

    
	//transparency type: 0-opaque 1-transparent
	jbyte processAlpha = KNI_GetParameterAsByte(12);


	KNI_GetParameterAsObject(1, dstArrayHandle);
	KNI_GetParameterAsObject(4, srcArrayHandle);
	KNI_GetParameterAsObject(11, alphaArrayHandle);

	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);
	unsigned int* src  = (unsigned int *) KNI_GetRawArrayRegionPtr(srcArrayHandle, 0);
	unsigned char*  alpha = (unsigned char*) KNI_GetRawArrayRegionPtr(alphaArrayHandle, 0);
	unsigned char a;
	

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}
	//check the alpha channel exists
	hasAlpha = 1;
	if (alpha < 0xF) { // == NULL
		hasAlpha = 0;
	}
	if (rotate) {
    	rotManipX = - manipY * srcH;
    }

	for (y = 0; y < srcH; y++) {
		srcOffset = offset + (y * scan);
		if (manipY > 0) {
			if (rotate) {
				dstOffset = (srcW - 1) * srcH + y; 
			} else {
				dstOffset = y * srcW;
			}
		} else {
			if (rotate) {
				dstOffset = y;
			} else {
				dstOffset = (srcH - y - 1) * srcW;
			}
		}
		if (manipX < 0) {
			if (rotate) {
				dstOffset += (srcH-1 - 2*y);
			} else {
				dstOffset += (srcW - 1);
			}
		}		
				
		if (processAlpha) {
			for (x = 0; x < srcW; x++) {
				srcPix = src[srcOffset++];
				alpha[dstOffset] = (byte)((srcPix & 0xFF000000) >> 24);
				dst[dstOffset] = 
   					 (short)(0x8000 | //alpha 
   							((srcPix >> 19) & 0x1f) |
   							((srcPix >> 6) & 0x3e0) |
   							((srcPix << 7) & 0x7c00));
				dstOffset += rotManipX;
			}
		} else 
		{
			for (x = 0; x < srcW; x++) {
				srcPix = src[srcOffset++];
				dst[dstOffset] = 
   					 (short)(0x8000 | //alpha 
   							((srcPix >> 19) & 0x1f) |
   							((srcPix >> 6) & 0x3e0) |
   							((srcPix << 7) & 0x7c00));
				dstOffset += rotManipX;
			}
		}
	}
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
}

/*
    public static native void getRGB(
	short[] dst, int dstW, int dstH,
	int[] src, int srcW, int srcH, 
	int dstX,int dstY, int offset, int scanlength,
	byte[] alpha
    );
*/
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_getRGB() { 
	int i,j,j2;
	int dstOffset, srcOffset;
	unsigned int pixel;
	int hasAlpha;

	unsigned int A, AM;

	int srcX = 0;
	int srcY = 0;
	int incX = 0;
	int incY = 0;

	KNI_StartHandles(3); 

	KNI_DeclareHandle(srcArrayHandle); 
	KNI_DeclareHandle(dstArrayHandle); 
	KNI_DeclareHandle(alphaArrayHandle); 

	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int srcW = KNI_GetParameterAsInt(5);
	int srcH = KNI_GetParameterAsInt(6);

	int dstX = KNI_GetParameterAsInt(7);
	int dstY = KNI_GetParameterAsInt(8);

	int offset = KNI_GetParameterAsInt(9);
	int scanlength = KNI_GetParameterAsInt(10);

	int dstMaxX;
	int dstMaxY;

	KNI_GetParameterAsObject(1, dstArrayHandle);
	KNI_GetParameterAsObject(4, srcArrayHandle);
	KNI_GetParameterAsObject(11, alphaArrayHandle);

	unsigned short* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);
	unsigned int* src  = (unsigned int *) KNI_GetRawArrayRegionPtr(srcArrayHandle, 0);
	char* alpha = (char*) KNI_GetRawArrayRegionPtr(alphaArrayHandle, 0);

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}
	//check the alpha channel exists
	hasAlpha = 1;
	if (alpha < 0xF) { // == NULL
		hasAlpha = 0;
	}
	dstMaxX = dstX + srcW;
	dstMaxY = dstY + srcH ;    	
	
	/*
	printf("\x1b[19B dst =%p src=%p ac=%p  a=%i  dstX=%i dstY=%i w=%i h=%i o=%i sl=%i dstMaxX=%i\n", 
        dst, src, alpha, hasAlpha, dstX, dstY, srcW, srcH, offset, scanlength ,dstMaxX);
        */

	//blit inside the destination bounds
	if (dstMaxX >= 0 && dstMaxY >= 0 && dstX < dstW && dstY < dstH) {
		if (dstX < 0) {
			srcX = -dstX;
			dstX = 0;
		}
		if (dstMaxX > dstW) {
			dstMaxX = dstW;
		}
		
		if (dstY < 0) {
			srcY = -dstY;
			dstY = 0;
		}
		if (dstMaxY > dstH) {
			dstMaxY = dstH;
		}
		j2 = srcY;
		for (j = dstY ; j < dstMaxY; j++, j2++) {
			dstOffset = j * dstW + dstX;
			srcOffset = j2 * scanlength + srcX + offset; 
			//store alpha
			if (hasAlpha) {
				for (i = dstX; i < dstMaxX; i++) {
    				pixel = dst[dstOffset];
    				A = (alpha[dstOffset++] & 0xFF);
    				src[srcOffset++] = 
	       					 	(	(A<<24)  | //alpha
	       							((pixel & 0x1F) << 19) |  // R
	       							((pixel & 0x3E0) << 6) |  // G
	       							((pixel & 0x7C00) >> 7)); // B
    			}
			} 
			//no alpha processing
			else {
    			for (i = dstX; i < dstMaxX; i++) {
    				pixel = dst[dstOffset++];
    				A =  (pixel & 0x8000) == 0 ?  0 : 0xFF000000;
    				src[srcOffset++] =  
				 		(    A | 
                            ((pixel & 0x1F) << 19) | 
                            ((pixel & 0x3E0) << 6) |  
                            ((pixel & 0x7C00) >> 7));
    			}
			}
		}      
	} 
	/*
	else {
            printf("early exit !\n", cnt);	
        } 
    */

	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
}


/*
    public static native void blitGray(
	short[] dst, int dstW, int dstH,
	byte[] src, int srcW, int srcH,
	int dstX, int dstY, int color
	int clipX, int clipY, int clipW, int clipH 
    );
*/

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_blitGray() { 
	int i,j,j2;
	int dstOffset, srcOffset;

	int srcX = 0;
	int srcY = 0;
	int incX = 0;
	int incY = 0;

	KNI_StartHandles(2); 

	KNI_DeclareHandle(srcArrayHandle); 
	KNI_DeclareHandle(dstArrayHandle); 

	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int srcW = KNI_GetParameterAsInt(5);
	int srcH = KNI_GetParameterAsInt(6);

	int dstX = KNI_GetParameterAsInt(7);
	int dstY = KNI_GetParameterAsInt(8);

	unsigned short color = KNI_GetParameterAsShort(9);

	int clipX = KNI_GetParameterAsInt(10);
	int clipY = KNI_GetParameterAsInt(11);
	int clipW = KNI_GetParameterAsInt(12);
	int clipH = KNI_GetParameterAsInt(13);


	int dstMaxX;
	int dstMaxY;

	KNI_GetParameterAsObject(1, dstArrayHandle);
	KNI_GetParameterAsObject(4, srcArrayHandle);

	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);
	jbyte* src  = (jbyte *) KNI_GetRawArrayRegionPtr(srcArrayHandle, 0);

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}

   	//check clip Horizontaly    	
	if (clipX > dstX) {    		
		incX = (clipX - dstX);     		
		srcX += incX;    		
		dstX += incX;    	
	}
	dstMaxX = dstX + srcW - incX;
    	if (clipX + clipW <  dstMaxX) {    		
		dstMaxX = clipX + clipW;      	
	}

    	//check clip Vertically    	
	if (clipY > dstY) {    		
		incY = (clipY - dstY );     		
		srcY += incY;    		
		dstY += incY;    	
	}    	
	dstMaxY = dstY + srcH - incY;    	
	if (clipY + clipH < dstMaxY) {    		
		dstMaxY = clipY + clipH;    	
	}



	//blit inside the destination bounds
	if (dstMaxX >= 0 && dstMaxY >= 0 && dstX < dstW && dstY < dstH) {
		if (dstX < 0) {
			srcX = -dstX;
			dstX = 0;
		}
		if (dstMaxX > dstW) {
			dstMaxX = dstW;
		}
		
		if (dstY < 0) {
			srcY = -dstY;
			dstY = 0;
		}
		if (dstMaxY > dstH) {
			dstMaxY = dstH;
		}
		
		j2 = 0;
		for (j = dstY ; j < dstMaxY; j++, j2++) {
			dstOffset = j * dstW + dstX;
			srcOffset = (srcY +j2) * srcW + srcX; 
			for (i = dstX; i < dstMaxX; i++) {
				if (src[srcOffset++] != 0) {
					dst[dstOffset] = color;
				}
				dstOffset++;
			}
		}
	}
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
}

/*

    public static void fillRect(    	
	short[] dst, int dstW, int dstH,			//surface data 		
	int dstX, int dstY, int srcW, int srcH, short color //command data	
	int clipX, int clipY, int clipW, int clipH,
	int alpha
    ) ;
*/
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_fillRect() { 
	int i,j,j2;
	int dstOffset, srcOffset;

	int srcX = 0;
	int srcY = 0;
	int incX = 0;
	int incY = 0;
	
   	int dR,dG,dB;
   	int sR,sG,sB;
   	unsigned short backPixel;

	KNI_StartHandles(1); 

	KNI_DeclareHandle(dstArrayHandle); 
	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int dstX = KNI_GetParameterAsInt(4);
	int dstY = KNI_GetParameterAsInt(5);

	int srcW = KNI_GetParameterAsInt(6);
	int srcH = KNI_GetParameterAsInt(7);

	unsigned short color = KNI_GetParameterAsShort(8);

	int clipX = KNI_GetParameterAsInt(9);
	int clipY = KNI_GetParameterAsInt(10);
	int clipW = KNI_GetParameterAsInt(11);
	int clipH = KNI_GetParameterAsInt(12);
	int alpha = KNI_GetParameterAsInt(13);
	int am = 255 - alpha; //reverse alpha

	int dstMaxX;
	int dstMaxY;
	
	int opaque = 0;
	KNI_GetParameterAsObject(1, dstArrayHandle);
	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);

   	if (alpha != 0) {

    if (alpha == 0xFF) {
        opaque = 1;
    } 


	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}


   	//check clip Horizontaly    	
	if (clipX > dstX) {    		
		incX = (clipX - dstX);     		
		srcX += incX;    		
		dstX += incX;    	
	}
	dstMaxX = dstX + srcW - incX;
    	if (clipX + clipW <  dstMaxX) {    		
		dstMaxX = clipX + clipW;      	
	}

    	//check clip Vertically    	
	if (clipY > dstY) {    		
		incY = (clipY - dstY );     		
		srcY += incY;    		
		dstY += incY;    	
	}    	
	dstMaxY = dstY + srcH - incY;    	
	if (clipY + clipH < dstMaxY) {    		
		dstMaxY = clipY + clipH;    	
	}


	//blit inside the destination bounds
	if (dstMaxX >= 0 && dstMaxY >= 0 && dstX < dstW && dstY < dstH) {
		if (dstX < 0) {
			srcX = -dstX;
			dstX = 0;
		}
		if (dstMaxX > dstW) {
			dstMaxX = dstW;
		}
		
		if (dstY < 0) {
			srcY = -dstY;
			dstY = 0;
		}
		if (dstMaxY > dstH) {
			dstMaxY = dstH;
		}
		
		j2 = 0;
		
		//opaque pixels
		if (opaque) {
    		for (j = dstY ; j < dstMaxY; j++, j2++) {
	       		dstOffset = j * dstW + dstX;
		      	for (i = dstX; i < dstMaxX; i++) {
			     	dst[dstOffset++] = color;
    			}
	       	}
        }
        //blending
          else {
        		for (j = dstY ; j < dstMaxY; j++, j2++) {
        			dstOffset = j * dstW + dstX;
        			for (i = dstX; i < dstMaxX; i++) {
	       					backPixel = dst[dstOffset];
	       					dR = ((backPixel >> 10) & 0x1F) * am;
	       					dG = ((backPixel >> 5) & 0x1F) * am;
	       					dB = ((backPixel ) & 0x1F) * am;
	       					
	       					sR = ((color >> 10) & 0x1F) * alpha;
	       					sG = ((color >> 5 ) & 0x1F) * alpha;
	       					sB = ((color) & 0x1F) * alpha;
	       					
	       					dR += sR;
	       					dG += sG;
	       					dB += sB;
	       					
	       					dR >>= 8;
	       					dG >>= 8;
	       					dB >>= 8;
	       					
	       					dst[dstOffset++] = (short)(0x8000 | (dR<<10 ) | (dG<<5) | (dB)); 
        			}
        		}
        }
	}
	
    } //endif (alpha != 0)
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
}

/*
    public static native void drawLine(
       	short[] dst, int dstW, int dstH,			//surface data
	int x1, int y1, int x2, int y2, short color, //command data			
	int clipX, int clipY, int clipW, int clipH     
    ) ;
*/

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_drawLine() { 
	int xinc1;    	
	int xinc2;    	
	int yinc1;    	
	int yinc2;    	    	

	KNI_StartHandles(1); 

	KNI_DeclareHandle(dstArrayHandle); 
	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int x1 = KNI_GetParameterAsInt(4);
	int y1 = KNI_GetParameterAsInt(5);

	int x2 = KNI_GetParameterAsInt(6);
	int y2 = KNI_GetParameterAsInt(7);

	unsigned short pixel = KNI_GetParameterAsShort(8);

	int clipX = KNI_GetParameterAsInt(9);
	int clipY = KNI_GetParameterAsInt(10);
	int clipW = KNI_GetParameterAsInt(11);
	int clipH = KNI_GetParameterAsInt(12);

    	int den;    	
	int num;    	
	int numadd;    	
	int numpixels;    	
	int deltax = abs(x2 - x1);        // The difference between the x's    	
	int deltay = abs(y2 - y1);        // The difference between the y's    	
	int x = x1;                       // Start x off at the first pixel    
	int y = y1;                       // Start y off at the first pixel    	
	int clipX2 = clipX + clipW;    	
	int clipY2 = clipY + clipH;
	int curpixel;

	KNI_GetParameterAsObject(1, dstArrayHandle);

	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}

    	if (x2 >= x1){                 // The x-values are increasing
    	  xinc1 = 1;
    	  xinc2 = 1;
    	} else {                          // The x-values are decreasing
    	  xinc1 = -1;
    	  xinc2 = -1;
    	}

    	if (y2 >= y1) {                 // The y-values are increasing
    	  yinc1 = 1;
    	  yinc2 = 1;
    	} else {                         // The y-values are decreasing
    	  yinc1 = -1;
    	  yinc2 = -1;
    	}

    	if (deltax >= deltay){         // There is at least one x-value for every y-value
    	  xinc1 = 0;                  // Don't change the x when numerator >= denominator
    	  yinc2 = 0;                  // Don't change the y for every iteration
    	  den = deltax;
    	  num = deltax / 2;
    	  numadd = deltay;
    	  numpixels = deltax;         // There are more x-values than y-values
    	} else {                          // There is at least one y-value for every x-value
    	  xinc2 = 0;                  // Don't change the x for every iteration
    	  yinc1 = 0;                  // Don't change the y when numerator >= denominator
    	  den = deltay;
    	  num = deltay / 2;
    	  numadd = deltax;
    	  numpixels = deltay;         // There are more y-values than x-values
    	}

    	//java.lang.System.out.println("----------------");
    	for (curpixel = 0; curpixel <= numpixels; curpixel++) {
    		//clip check
    		if (
    			x >= 0 && x >= clipX && x < dstW && x < clipX2 &&
			y >= 0 && y >= clipY && y < dstH && y < clipY2
		) {
			dst[y* dstW + x] = pixel;
    		}
    	  num += numadd;              // Increase the numerator by the top of the fraction
    	  if (num >= den)             // Check if numerator >= denominator
    	  {
    	    num -= den;               // Calculate the new numerator value
    	    x += xinc1;               // Change the x as appropriate
    	    y += yinc1;               // Change the y as appropriate
    	  }
    	  x += xinc2;                 // Change the x as appropriate
    	  y += yinc2;                 // Change the y as appropriate
    	}
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 

}




/*
   public static void fillTriangle(
    		short[] dst, int dstW, int dstH,
			int x1, int y1, int x2, int y2, int x3, int y3, short color, 
			int clipX, int clipY, int clipW, int clipH 
    ) 
*/



/*
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_fillTriangle() { 
    	int i,j;
    	int dstOffset;

    	int srcX = 0;
    	int srcY = 0;
    	int incX = 0;
    	int incY = 0;

    	signed short bounds[512]; // 2 * 256 of height ? FIXME ?

	KNI_StartHandles(1); 

	KNI_DeclareHandle(dstArrayHandle); 
	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int x1 = KNI_GetParameterAsInt(4);
	int y1 = KNI_GetParameterAsInt(5);

	int x2 = KNI_GetParameterAsInt(6);
	int y2 = KNI_GetParameterAsInt(7);

	int x3 = KNI_GetParameterAsInt(8);
	int y3 = KNI_GetParameterAsInt(9);

	unsigned short pixel = KNI_GetParameterAsShort(10);

	int dstMaxX;
	int dstMaxY;

    	int srcW, srcH;
    	int dstX = x1;
    	int dstY = y1;
    	int dstX2 = x1;
    	int dstY2 = y1;

	int clipX = KNI_GetParameterAsInt(11);
	int clipY = KNI_GetParameterAsInt(12);
	int clipW = KNI_GetParameterAsInt(13);
	int clipH = KNI_GetParameterAsInt(14);

	KNI_GetParameterAsObject(1, dstArrayHandle);

	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}


    	//find the extent of the triangle
    	//min extent
    	if (x2 < dstX) {
    		dstX = x2;
    	}
    	if (x3 < dstX) {
    		dstX = x3;
    	}
    	if (y2 < dstY) {
    		dstY = y2;
    	}
    	if (y3 < dstY) {
    		dstY = y3;
    	}
    	//max extent
    	if (x2 > dstX2) {
    		dstX2 = x2;
    	}
    	if (x3 > dstX2) {
    		dstX2 = x3;
    	}
    	if (y2 > dstY2) {
    		dstY2 = y2;
    	}
    	if (y3 > dstY2) {
    		dstY2 = y3;
    	}
    	srcW = dstX2 - dstX;
    	srcH = dstY2 - dstY;




   	//check clip Horizontaly    	
	if (clipX > dstX) {    		
		incX = (clipX - dstX);     		
		srcX += incX;    		
		dstX += incX;    	
	}
	dstMaxX = dstX + srcW - incX;
    	if (clipX + clipW <  dstMaxX) {    		
		dstMaxX = clipX + clipW;      	
	}

    	//check clip Vertically    	
	if (clipY > dstY) {    		
		incY = (clipY - dstY );     		
		srcY += incY;    		
		dstY += incY;    	
	}    	
	dstMaxY = dstY + srcH - incY;    	
	if (clipY + clipH < dstMaxY) {    		
		dstMaxY = clipY + clipH;    	
	}


	//blit inside the destination bounds
	if (dstMaxX >= 0 && dstMaxY >= 0 && dstX < dstW && dstY < dstH) {

    		if (dstX < 0) {
    			srcX = -dstX;
    			dstX = 0;
    		}
    		if (dstMaxX > dstW) {
    			dstMaxX = dstW;
    		}
    		
    	   	//initialize bounds
        	for (i = 0; i < 512; i++) {
        		bounds[i++] = (signed short)1000; //dstMaxX; //min
        		bounds[i] =  (signed short)-1000; // dstX ; //max
        	}
        	
        	//virtually draw the 3 lines of the triangle 
        	//and compute exact bounds of the triangle
        	computeLine( dstW, dstH,			//surface data 
        			x1, y1, x2, y2, bounds, //command data
        			clipX, clipY, clipW, clipH 
	            );
        	computeLine( dstW, dstH,			//surface data 
        			x2, y2, x3, y3, bounds, //command data
        			clipX, clipY, clipW, clipH 
        	    );
        	computeLine( dstW, dstH,			//surface data 
        			x3, y3, x1, y1, bounds, //command data
        			clipX, clipY, clipW, clipH 
	            );
    		
    		for (j = 0; j < 256;  j++) {
    			int minX = bounds[j*2];
    			int maxX = bounds[j*2 +1] + 1;
    			dstOffset = j * dstW + minX; 
   			for (i = minX; i < maxX; i++) {
   				dst[dstOffset++] = pixel;
   			}
    		}
	}    	
    	

	KNI_EndHandles(); 
	KNI_ReturnVoid(); 

}

*/

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_fillTriangle() { 

	KNI_StartHandles(1); 

	KNI_DeclareHandle(dstArrayHandle); 
	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int x1 = KNI_GetParameterAsInt(4);
	int y1 = KNI_GetParameterAsInt(5);

	int x2 = KNI_GetParameterAsInt(6);
	int y2 = KNI_GetParameterAsInt(7);

	int x3 = KNI_GetParameterAsInt(8);
	int y3 = KNI_GetParameterAsInt(9);

	unsigned short pixel = KNI_GetParameterAsShort(10);


	int clipX = KNI_GetParameterAsInt(11);
	int clipY = KNI_GetParameterAsInt(12);
	int clipW = KNI_GetParameterAsInt(13);
	int clipH = KNI_GetParameterAsInt(14);

	KNI_GetParameterAsObject(1, dstArrayHandle);

	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}

    drawTriangle(dst, dstW, dstH, 
        x1,y1,x2,y2,x3,y3, pixel,
        clipX, clipY, clipW, clipH);    	

	KNI_EndHandles(); 
	KNI_ReturnVoid(); 

}

inline void  drawPoint(
        	unsigned short* dst, int dstW, int dstH, 
			int x, int y, short color,
			int clipX, int clipY, int clipX2, int clipY2 
    ) {
		//clip check
		if (
			x >= 0 && x >= clipX && x < dstW && x < clipX2 &&
			y >= 0 && y >= clipY && y < dstH && y < clipY2
		) {
			dst[y* dstW + x] = color;
		}
    }

void drawEllipse(
        	unsigned short* dst, int dstW, int dstH,			//surface data 
			int x1, int y1, int width, int height, short color, //command data
			int clipX, int clipY, int clipW, int clipH 
    ) {
    	
    	int clipX2 = clipX + clipW;
    	int clipY2 = clipY + clipH;
    	
    	int a = width / 2;
    	int b = height / 2;
    	int xc = x1 + a;
    	int yc = y1 + b;
    	
		int x = 0, y = b;
		int a2 = a*a, b2 = b*b;
		int crit1 = -(a2/4 + a%2 + b2);
		int crit2 = -(b2/4 + b%2 + a2);
		int crit3 = -(b2/4 + b%2);
		int t = -a2*y; /* e(x+1/2,y-1/2) - (a^2+b^2)/4 */
		int dxt = 2*b2*x, dyt = -2*a2*y;
		int d2xt = 2*b2, d2yt = 2*a2;
		
		int shiftX = 1 - (width & 0x1);
		int shiftY = 1 - (height & 0x1);

		while (y>=0 && x<=a) {
			//draw bottom right quadrant
			drawPoint(dst, dstW, dstH, xc+x - shiftX, yc+y - shiftY, color, clipX, clipY, clipX2, clipY2);
			//draw top left quadrant
			if (x!=0 || y!=0)
				drawPoint(dst, dstW, dstH, xc-x, yc-y, color, clipX, clipY, clipX2, clipY2);
			if (x!=0 && y!=0) {
				//top right quadrant
				drawPoint(dst, dstW, dstH, xc+x- shiftX, yc-y, color, clipX, clipY, clipX2, clipY2);
				//bottom left quadrant
				drawPoint(dst, dstW, dstH, xc-x, yc+y - shiftY, color, clipX, clipY, clipX2, clipY2);
			}
			if (t + b2*x <= crit1 ||   /* e(x+1,y-1/2) <= 0 */
			    t + a2*y <= crit3){     /* e(x+1/2,y) <= 0 */
				//incX
				x++;
				dxt += d2xt;
				t += dxt;
			}
			else if (t - a2*y > crit2){ /* e(x+1/2,y-1) > 0 */
				//incY
				y--;
				dyt += d2yt;
				t += dyt;
			}
			else {
				//incX
				x++;
				dxt += d2xt;
				t += dxt;
				//incY
				y--;
				dyt += d2yt;
				t += dyt;
			}
		}    	
    }

/*
   public static void drawArc(
        	short[] dst, int dstW, int dstH,			//surface data 
			int x, int y, int width, int height, int startAngle, int arcAngle, short color, //command data
			int clipX, int clipY, int clipW, int clipH 
    )
*/

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_drawArc() { 
	KNI_StartHandles(1); 

	KNI_DeclareHandle(dstArrayHandle); 
	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int x = KNI_GetParameterAsInt(4);
	int y = KNI_GetParameterAsInt(5);

	int width = KNI_GetParameterAsInt(6);
	int height = KNI_GetParameterAsInt(7);

    int startAngle = KNI_GetParameterAsInt(8);
	int arcAngle = KNI_GetParameterAsInt(9);

	unsigned short color = KNI_GetParameterAsShort(10);

	int clipX = KNI_GetParameterAsInt(11);
	int clipY = KNI_GetParameterAsInt(12);
	int clipW = KNI_GetParameterAsInt(13);
	int clipH = KNI_GetParameterAsInt(14);

	
	KNI_GetParameterAsObject(1, dstArrayHandle);
	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}

   	if (arcAngle >= 360) {
    		drawEllipse(
    				dst, dstW, dstH,
					x, y, width, height, color,
					clipX, clipY, clipW, clipH
    		);
   	} else {
    		//TODO - impelement arc
   	}
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
    
}

inline void  drawLineHoriz(
        	unsigned short* dst, int dstW, int dstH, 
			int x1, int x2, int y, short color,
			int clipX, int clipY, int clipX2, int clipY2 
    ) {
        int offset, i;
    	//check Y clip
    	if (y < clipY || y < 0 || y >= clipY2 || y >= dstH ) {
    		return;
    	}
    	//set x1 and x2 according the clip
    	if (x1 < clipX) {
    		x1 = clipX;
    	}
    	if (x1 < 0) {
    		x1 = 0;
    	}
    	x2 ++;
    	if (x2 > clipX2) {
    		x2 = clipX2;
    	}
    	if (x2 > dstW) {
    		x2 = dstW;
    	}
    	offset = dstW * y + x1;
    	for (i = x1; i < x2; i++) {
    		dst[offset++] = color;
    	}
}


void fillEllipse(
        	unsigned short* dst, int dstW, int dstH,			//surface data 
			int x1, int y1, int width, int height, short color, //command data
			int clipX, int clipY, int clipW, int clipH 
    ) {
    	
    	int clipX2 = clipX + clipW;
    	int clipY2 = clipY + clipH;
    	
    	int a = width / 2;
    	int b = height / 2;
    	int xc = x1 + a;
    	int yc = y1 + b;
    	
		int x = 0, y = b;
		int a2 = a*a, b2 = b*b;
		int crit1 = -(a2/4 + a%2 + b2);
		int crit2 = -(b2/4 + b%2 + a2);
		int crit3 = -(b2/4 + b%2);
		int t = -a2*y;  // e(x+1/2,y-1/2) - (a^2+b^2)/4 
		int dxt = 2*b2*x, dyt = -2*a2*y;
		int d2xt = 2*b2, d2yt = 2*a2;
		
		int shiftX = 1 - (width & 0x1);
		int shiftY = 1 - (height & 0x1);

		int lastY = -1;
		while (y>=0 && x<=a) {
			//
			if (lastY != y) {
				lastY = y;
				drawLineHoriz(dst, dstW, dstH, xc-x, xc+x-shiftX, yc+y - shiftY, color, clipX, clipY, clipX2, clipY2);
				drawLineHoriz(dst, dstW, dstH, xc-x, xc+x-shiftX, yc-y, color, clipX, clipY, clipX2, clipY2);
			} else {
				//draw bottom right quadrant
				drawPoint(dst, dstW, dstH, xc+x - shiftX, yc+y - shiftY, color, clipX, clipY, clipX2, clipY2);
				//draw top left quadrant
				if (x!=0 || y!=0)
					drawPoint(dst, dstW, dstH, xc-x, yc-y, color, clipX, clipY, clipX2, clipY2);
				if (x!=0 && y!=0) {
					//top right quadrant
					drawPoint(dst, dstW, dstH, xc+x- shiftX, yc-y, color, clipX, clipY, clipX2, clipY2);
					//bottom left quadrant
					drawPoint(dst, dstW, dstH, xc-x, yc+y - shiftY, color, clipX, clipY, clipX2, clipY2);
				}
			}
			if (t + b2*x <= crit1 ||   // e(x+1,y-1/2) <= 0 
			    t + a2*y <= crit3){    // e(x+1/2,y) <= 0 
				//incX
				x++;
				dxt += d2xt;
				t += dxt;
			}
			else if (t - a2*y > crit2){ // e(x+1/2,y-1) > 0 
				//incY
				y--;
				dyt += d2yt;
				t += dyt;
			}
			else {
				//incX
				x++;
				dxt += d2xt;
				t += dxt;
				//incY
				y--;
				dyt += d2yt;
				t += dyt;
			}
		}    	
		
}


/*
   public static void fillArc(
        	short[] dst, int dstW, int dstH,			//surface data 
			int x, int y, int width, int height, int startAngle, int arcAngle, short color, //command data
			int clipX, int clipY, int clipW, int clipH 
    )
*/

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_fillArc() { 
	KNI_StartHandles(1); 

	KNI_DeclareHandle(dstArrayHandle); 
	int dstW = KNI_GetParameterAsInt(2);
	int dstH = KNI_GetParameterAsInt(3);

	int x = KNI_GetParameterAsInt(4);
	int y = KNI_GetParameterAsInt(5);

	int width = KNI_GetParameterAsInt(6);
	int height = KNI_GetParameterAsInt(7);

    int startAngle = KNI_GetParameterAsInt(8);
	int arcAngle = KNI_GetParameterAsInt(9);

	unsigned short color = KNI_GetParameterAsShort(10);

	int clipX = KNI_GetParameterAsInt(11);
	int clipY = KNI_GetParameterAsInt(12);
	int clipW = KNI_GetParameterAsInt(13);
	int clipH = KNI_GetParameterAsInt(14);

	
	KNI_GetParameterAsObject(1, dstArrayHandle);
	jshort* dst  = (jshort *) KNI_GetRawArrayRegionPtr(dstArrayHandle, 0);

	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (jshort*) BG_BMP_RAM(0);
		// printf("\x1b[19B dst =%p src=%p \n", dst, src);
	}

   	if (arcAngle >= 360) {
    		fillEllipse(
    				dst, dstW, dstH,
					x, y, width, height, color,
					clipX, clipY, clipW, clipH
    		);
   	} else {
    		//TODO - impelement arc
   	}
	KNI_EndHandles(); 
	KNI_ReturnVoid(); 
    
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetMainBanks() {
    KNI_ReturnInt(
    vramSetMainBanks(
            KNI_GetParameterAsInt(1),
            KNI_GetParameterAsInt(2),
            KNI_GetParameterAsInt(3),
            KNI_GetParameterAsInt(4)
            )
    );
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramRestoreMainBanks() {
    vramRestoreMainBanks(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankA() {
    vramSetBankA(KNI_GetParameterAsInt(1));
    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankB() {
    vramSetBankB(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankC() {
    vramSetBankC(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankD() {
    vramSetBankD(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankE() {
    vramSetBankE(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankF() {
    vramSetBankF(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankG() {
    vramSetBankG(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankH() {
    vramSetBankH(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Video_vramSetBankI() {
    vramSetBankI(KNI_GetParameterAsInt(1));

    KNI_ReturnVoid();
}


