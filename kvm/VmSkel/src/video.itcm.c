#include <nds.h>
#include <nds/arm9/video.h>

/*
void myITCMfunction() ITCM_CODE int x ; 
 
void myITCMfunction( int x)
{ 
    int a, b;
    a = 0;
    b = a;    
} 
*/

//void computeLine( int dstW, int dstH,  int x1, int y1, int x2, int y2, signed short* bounds, int clipX, int clipY, int clipW, int clipH ) ITCM_CODE;
inline void  computeLine( int dstW, int dstH,  int x1, int y1, int x2, int y2, signed short* bounds, int clipX, int clipY, int clipW, int clipH )  { 
	int xinc1;    	
	int xinc2;    	
	int yinc1;    	
	int yinc2;    	    	

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
	int testMin;
	int testMax;
	int boundIndex;


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

    	testMin = clipX;
    	testMax = clipX2;
    	if (testMin < 0) {
    		testMin = 0;
    	}
    	if (testMax >= dstW ) {
    		testMax = dstW-1;
    	}

    	for (curpixel = 0; curpixel <= numpixels; curpixel++) {
    		//clip check  (only the Y position
    		if (y >= 0 && y >= clipY && y < dstH && y < clipY2 ) {
    			//store bounds - min X and maxX for the line (determined by Y position)
			boundIndex = y * 2;
    			if (x < bounds[boundIndex]) {
    				if (x < testMin) {
    					bounds[boundIndex] = (short)testMin;
    				} else {
    					bounds[boundIndex] = (short)x;
    				}
    			} 
    			if (x > bounds[boundIndex+1]) {
    				if (x > testMax) {
    					bounds[boundIndex+1] = (short)testMax;
    				} else {
    					bounds[boundIndex+1] = (short)x;
    				}
    			}

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
}

//void drawTriangle( unsigned short* dst, int dstW, int dstH, int x1, int y1, int x2, int y2, int x3, int y3,  unsigned short pixel,  int clipX, int clipY, int clipW, int clipH ) ITCM_CODE;
void drawTriangle( unsigned short* dst, int dstW, int dstH, int x1, int y1, int x2, int y2, int x3, int y3,  unsigned short pixel,  int clipX, int clipY, int clipW, int clipH  ) {
        
    int i,j;
    int dstOffset;

    int srcX = 0;
    int srcY = 0;
    int incX = 0;
    int incY = 0;

    signed short bounds[512]; // 2 * 256 of height ? FIXME ?


    int dstMaxX;
	int dstMaxY;

    int srcW, srcH;
    int dstX = x1;
    int dstY = y1;
    int dstX2 = x1;
    int dstY2 = y1;


	//direct rendering to the screen
	if (dst < 0xF) { // == NULL
		dst = (unsigned short*) BG_BMP_RAM(0);
		// iprintf("\x1b[19B dst =%p src=%p \n", dst, src);
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
    	

}
