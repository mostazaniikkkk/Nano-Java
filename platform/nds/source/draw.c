/*
 * draw.c - drawing helpers for the DS's own screens; see draw.h.
 */
#include <string.h>

#include "draw.h"
#include "../../../src/midp/gfx.h"

uint16_t draw_buf_main[DRAW_W * DRAW_H];
uint16_t draw_buf_sub[DRAW_W * DRAW_H];

static Surface target = {NULL, NULL, DRAW_W, DRAW_H};
static Clip    whole = {0, 0, DRAW_W, DRAW_H};

void draw_target(uint16_t *buf)
{
    target.px = buf;
}

void draw_flush(volatile uint16_t *vram, int y1, int y2)
{
    const uint32_t    *src = (const uint32_t *)(target.px + y1 * DRAW_W);
    volatile uint32_t *dst = (volatile uint32_t *)(vram + y1 * DRAW_W);
    for (int i = 0; i < (y2 - y1) * DRAW_W / 2; i++)
        dst[i] = src[i];
}

void draw_rect(int x, int y, int w, int h, uint32_t rgb)
{
    gfx_fill_rect(&target, &whole, x, y, w, h, gfx_pack(rgb));
}

void draw_round_rect(int x, int y, int w, int h, int r, uint32_t fill, uint32_t edge)
{
    gfx_round_rect(&target, &whole, x, y, w - 1, h - 1, r, r, gfx_pack(fill), true);
    gfx_round_rect(&target, &whole, x, y, w - 1, h - 1, r, r, gfx_pack(edge), false);
}

void draw_vgradient(int x, int y, int w, int h, uint32_t top, uint32_t bottom)
{
    for (int i = 0; i < h; i++) {
        uint32_t c = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int a = (top >> shift) & 255, b = (bottom >> shift) & 255;
            c |= (uint32_t)(a + (b - a) * i / (h > 1 ? h - 1 : 1)) << shift;
        }
        draw_rect(x, y + i, w, 1, c);
    }
}

void draw_triangle(int x1, int y1, int x2, int y2, int x3, int y3, uint32_t rgb)
{
    gfx_fill_triangle(&target, &whole, x1, y1, x2, y2, x3, y3, gfx_pack(rgb));
}

void draw_disc(int cx, int cy, int r, uint32_t fill, uint32_t edge)
{
    gfx_arc(&target, &whole, cx - r, cy - r, 2 * r, 2 * r, 0, 360, gfx_pack(fill), true);
    gfx_arc(&target, &whole, cx - r, cy - r, 2 * r, 2 * r, 0, 360, gfx_pack(edge), false);
}

void draw_arc(int x, int y, int w, int h, int start, int arc, uint32_t rgb)
{
    gfx_arc(&target, &whole, x, y, w, h, start, arc, gfx_pack(rgb), false);
}

void draw_menu_icon(int x, int y, uint32_t rgb)
{
    for (int i = 0; i < 3; i++)
        draw_rect(x, y + i * 5, 10, 2, rgb);
}

void draw_dim(int x, int y, int w, int h)
{
    for (int yy = y; yy < y + h && yy < DRAW_H; yy++) {
        uint16_t *row = target.px + yy * DRAW_W;
        for (int xx = x; xx < x + w && xx < DRAW_W; xx++)
            row[xx] = (uint16_t)(0x8000 | ((row[xx] >> 2) & 0x1CE7));
    }
}

/* ------------------------------------------------------------------------ */
/* Text                                                                     */

/* Decodes UTF-8 (manifests are often UTF-8) into Latin-1 font characters. */
static int decode(const char *t, uint16_t *out, int max)
{
    int n = 0;
    const uint8_t *p = (const uint8_t *)t;
    while (*p && n < max) {
        uint16_t c = *p++;
        if ((c & 0xE0) == 0xC0 && (*p & 0xC0) == 0x80) {
            c = (uint16_t)(((c & 0x1F) << 6) | (*p++ & 0x3F));
        } else if ((c & 0xF0) == 0xE0 && (p[0] & 0xC0) == 0x80 && (p[1] & 0xC0) == 0x80) {
            c = (uint16_t)(((c & 0x0F) << 12) | ((p[0] & 0x3F) << 6) | (p[1] & 0x3F));
            p += 2;
        }
        out[n++] = c;
    }
    return n;
}

int draw_text_width(const char *t, int size, int style)
{
    uint16_t buf[128];
    int      n = decode(t, buf, 128);
    return font_chars_width(size, style, buf, n);
}

int draw_text_height(int size)
{
    return font_height(size);
}

void draw_text_in(const char *t, int x, int y, int size, int style, uint32_t rgb, int x1, int x2)
{
    uint16_t buf[128];
    int      n = decode(t, buf, 128);
    Clip     c = {x1, 0, x2, DRAW_H};
    font_draw(&target, &c, size, style, buf, n, x, y, gfx_pack(rgb));
}

void draw_text(const char *t, int x, int y, int size, int style, uint32_t rgb)
{
    draw_text_in(t, x, y, size, style, rgb, 0, DRAW_W);
}

void draw_text_centered(const char *t, int cx, int y, int size, int style, uint32_t rgb)
{
    draw_text(t, cx - draw_text_width(t, size, style) / 2, y, size, style, rgb);
}

int draw_text_wrapped(const char *t, int x, int y, int w, int size, int style, uint32_t rgb,
                      int max_lines)
{
    uint16_t buf[256];
    int      n = decode(t, buf, 256);
    int      lines = 0, start = 0;
    int      lh = font_height(size);
    while (start < n && lines < max_lines) {
        while (start < n && buf[start] == ' ')
            start++;
        /* Take as many words as fit. */
        int end = start, last_space = -1, width = 0;
        while (end < n && buf[end] != '\n') {
            int cw = font_char_width(size, style, buf[end]);
            if (width + cw > w)
                break;
            if (buf[end] == ' ')
                last_space = end;
            width += cw;
            end++;
        }
        if (end < n && buf[end] != '\n' && last_space > start)
            end = last_space;
        bool last = lines == max_lines - 1 && end < n;
        int  len = end - start;
        if (last) {
            /* Make room for an ellipsis. */
            int dots = 3 * font_char_width(size, style, '.');
            while (len > 0 && font_chars_width(size, style, buf + start, len) + dots > w)
                len--;
        }
        Clip c = {x, 0, x + w, DRAW_H};
        font_draw(&target, &c, size, style, buf + start, len, x, y + lines * lh, gfx_pack(rgb));
        if (last) {
            uint16_t dots[3] = {'.', '.', '.'};
            font_draw(&target, &c, size, style, dots, 3,
                      x + font_chars_width(size, style, buf + start, len), y + lines * lh,
                      gfx_pack(rgb));
        }
        lines++;
        start = end < n && buf[end] == '\n' ? end + 1 : end;
    }
    return lines;
}

/* ------------------------------------------------------------------------ */
/* Images                                                                   */

void draw_argb(const uint32_t *px, int w, int h, int x, int y, int dw, int dh)
{
    for (int yy = 0; yy < dh; yy++) {
        int sy = yy * h / dh, ty = y + yy;
        if (ty < 0 || ty >= DRAW_H)
            continue;
        uint16_t *row = target.px + ty * DRAW_W;
        for (int xx = 0; xx < dw; xx++) {
            int tx = x + xx;
            if (tx < 0 || tx >= DRAW_W)
                continue;
            uint32_t p = px[sy * w + xx * w / dw];
            unsigned a = p >> 24;
            if (a)
                row[tx] = gfx_blend(row[tx], gfx_pack(p), a);
        }
    }
}
