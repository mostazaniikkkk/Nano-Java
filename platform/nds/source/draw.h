/*
 * draw.h - drawing helpers for the DS's own screens (launcher, keypad,
 * menu). They draw into a RAM buffer chosen with draw_target(); draw_flush()
 * copies it to VRAM with 32-bit writes (VRAM ignores byte writes).
 *
 * Colors are 24-bit RGB. Text is UTF-8 and uses the built-in fonts (size
 * 0 small, 1 medium, 2 large; style bit 1 = bold).
 */
#ifndef NJ_DRAW_H
#define NJ_DRAW_H

#include <stdbool.h>
#include <stdint.h>

#define DRAW_W 256
#define DRAW_H 192

#define BOLD 1

/* Screen-sized RAM buffers shared by the launcher and the touch UI (they
 * never run at the same time), for the top (main) and touch (sub) screens. */
extern uint16_t draw_buf_main[DRAW_W * DRAW_H];
extern uint16_t draw_buf_sub[DRAW_W * DRAW_H];

void draw_target(uint16_t *buf);
void draw_flush(volatile uint16_t *vram, int y1, int y2);

void draw_rect(int x, int y, int w, int h, uint32_t rgb);
void draw_round_rect(int x, int y, int w, int h, int r, uint32_t fill, uint32_t edge);
void draw_vgradient(int x, int y, int w, int h, uint32_t top, uint32_t bottom);
void draw_triangle(int x1, int y1, int x2, int y2, int x3, int y3, uint32_t rgb);
void draw_disc(int cx, int cy, int r, uint32_t fill, uint32_t edge);
void draw_arc(int x, int y, int w, int h, int start, int arc, uint32_t rgb);
void draw_menu_icon(int x, int y, uint32_t rgb);
/* Darkens the rectangle (for content behind an overlay). */
void draw_dim(int x, int y, int w, int h);

int  draw_text_width(const char *t, int size, int style);
int  draw_text_height(int size);
/* Draws text clipped horizontally to [x1, x2). */
void draw_text_in(const char *t, int x, int y, int size, int style, uint32_t rgb, int x1, int x2);
void draw_text(const char *t, int x, int y, int size, int style, uint32_t rgb);
void draw_text_centered(const char *t, int cx, int y, int size, int style, uint32_t rgb);
/* Word-wraps text into width w, at most max_lines lines (the last one ends
 * with "..." if cut). Returns the number of lines drawn. */
int  draw_text_wrapped(const char *t, int x, int y, int w, int size, int style, uint32_t rgb,
                       int max_lines);

/* Draws ARGB pixels (alpha blended) scaled to dw x dh. */
void draw_argb(const uint32_t *px, int w, int h, int x, int y, int dw, int dh);

#endif
