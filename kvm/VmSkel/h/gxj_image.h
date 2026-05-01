/*
 * gxj_image.h - stub PNG image decoder for NDS port
 * The original Pstros GBA relied on an external GXJ library for PNG decoding.
 * This stub provides type definitions so Java_nds_Video.c compiles; actual
 * PNG decoding returns -1 (unsupported) at runtime.
 */
#ifndef GXJ_IMAGE_H
#define GXJ_IMAGE_H

#include <stdlib.h>

typedef struct {
    const char *data;
    int         pos;
    int         size;
} memoryFile;

typedef struct {
    unsigned short *palette;
    unsigned short *pixelBuf;
    unsigned char  *alphaBuf;
    int             width;
    int             height;
    int             imgType;
} imageData;

typedef struct { memoryFile *src; } imageSrcData;
typedef struct { imageData  *dst; } imageDstData;

static inline void initImageSrcData(imageSrcData *s, memoryFile *m)
    { s->src = m; }
static inline void initImageDstData(imageDstData *d, imageData *img)
    { d->dst = img; }

/* Implemented in nds_png.c */
extern int decode_png_image(imageSrcData *s, imageDstData *d);

#endif /* GXJ_IMAGE_H */
