/*
 * gfx.h - software rasterizer behind javax.microedition.lcdui.Graphics.
 *
 * Pixels are 15-bit colors with red in the low bits and bit 15 set (the
 * native DS bitmap layout, where bit 15 marks a visible pixel). Source images may carry a separate 8-bit alpha plane; drawing
 * targets (the screen and mutable images) are always opaque.
 */
#ifndef NJ_GFX_H
#define NJ_GFX_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

typedef struct Surface {
    uint16_t      *px;
    const uint8_t *alpha;    /* NULL when fully opaque */
    int            w, h;
} Surface;

/* Clip rectangle in absolute surface coordinates, x2/y2 exclusive. */
typedef struct Clip {
    int x1, y1, x2, y2;
} Clip;

static inline uint16_t gfx_pack(uint32_t rgb)
{
    return (uint16_t)(0x8000 | ((rgb >> 19) & 31) | (((rgb >> 11) & 31) << 5) | (((rgb >> 3) & 31) << 10));
}

static inline uint32_t gfx_unpack(uint16_t p)
{
    uint32_t r = p & 31, g = (p >> 5) & 31, b = (p >> 10) & 31;
    r = (r << 3) | (r >> 2);
    g = (g << 3) | (g >> 2);
    b = (b << 3) | (b >> 2);
    return (r << 16) | (g << 8) | b;
}

static inline uint16_t gfx_blend(uint16_t dst, uint16_t src, unsigned a)
{
    if (a >= 255) return src;
    if (a == 0) return dst;
    unsigned ia = 255 - a;
    unsigned r = ((src & 31) * a + (dst & 31) * ia) / 255;
    unsigned g = (((src >> 5) & 31) * a + ((dst >> 5) & 31) * ia) / 255;
    unsigned b = (((src >> 10) & 31) * a + ((dst >> 10) & 31) * ia) / 255;
    return (uint16_t)(0x8000 | r | (g << 5) | (b << 10));
}

/* Sprite / drawRegion transforms (javax.microedition.lcdui.game.Sprite). */
enum {
    TRANS_NONE = 0, TRANS_MIRROR_ROT180 = 1, TRANS_MIRROR = 2, TRANS_ROT180 = 3,
    TRANS_MIRROR_ROT270 = 4, TRANS_ROT90 = 5, TRANS_ROT270 = 6, TRANS_MIRROR_ROT90 = 7
};

void gfx_fill_rect(Surface *s, const Clip *c, int x, int y, int w, int h, uint16_t color);
void gfx_line(Surface *s, const Clip *c, int x1, int y1, int x2, int y2, uint16_t color, bool dotted);
void gfx_rect(Surface *s, const Clip *c, int x, int y, int w, int h, uint16_t color, bool dotted);
void gfx_fill_triangle(Surface *s, const Clip *c, int x1, int y1, int x2, int y2,
                       int x3, int y3, uint16_t color);
void gfx_arc(Surface *s, const Clip *c, int x, int y, int w, int h, int start, int arc,
             uint16_t color, bool fill);
void gfx_round_rect(Surface *s, const Clip *c, int x, int y, int w, int h, int aw, int ah,
                    uint16_t color, bool fill);

/* Copies a region of src to dst at (dx, dy) applying transform and src
 * alpha. src may be dst (copyArea), in which case overlap is handled. */
void gfx_blit(Surface *dst, const Clip *c, const Surface *src, int sx, int sy, int w, int h,
              int transform, int dx, int dy);

/* Draws ARGB pixels (MIDP drawRGB). */
void gfx_draw_rgb(Surface *dst, const Clip *c, const int32_t *rgb, int offset, int scan,
                  int x, int y, int w, int h, bool alpha);

/* Text (font.c). size: 0 small, 1 medium, 2 large; style bits as in
 * javax.microedition.lcdui.Font. */
int  font_height(int size);
int  font_baseline(int size);
int  font_char_width(int size, int style, int ch);
int  font_chars_width(int size, int style, const uint16_t *s, int n);
void font_draw(Surface *s, const Clip *c, int size, int style, const uint16_t *text, int n,
               int x, int y, uint16_t color);

#endif
