/*
 * font.c - text rendering with the built-in bitmap fonts. Bold is drawn by
 * doubling each glyph one pixel to the right; underline is a line just
 * below the baseline. Italic is rendered upright.
 */
#include "gfx.h"
#include "font_data.h"

#define STYLE_BOLD       1
#define STYLE_UNDERLINED 4

static const FontData *font(int size)
{
    return &font_data[size < 0 || size > 2 ? 1 : size];
}

static int glyph_index(int ch)
{
    if (ch < FONT_FIRST || ch > FONT_LAST)
        ch = '?';
    return ch - FONT_FIRST;
}

int font_height(int size)
{
    return font(size)->height;
}

int font_baseline(int size)
{
    return font(size)->baseline;
}

int font_char_width(int size, int style, int ch)
{
    return font(size)->widths[glyph_index(ch)] + ((style & STYLE_BOLD) ? 1 : 0);
}

int font_chars_width(int size, int style, const uint16_t *s, int n)
{
    int w = 0;
    for (int i = 0; i < n; i++)
        w += font_char_width(size, style, s[i]);
    return w;
}

static void draw_glyph(Surface *s, const Clip *c, const FontData *f, int g, int x, int y,
                       uint16_t color)
{
    const uint8_t *rows = f->bits + (size_t)g * f->height * f->bytes_per_row;
    int            w = f->widths[g];
    for (int ry = 0; ry < f->height; ry++, rows += f->bytes_per_row) {
        int py = y + ry;
        if (py < c->y1 || py >= c->y2)
            continue;
        uint16_t *out = s->px + py * s->w;
        for (int rx = 0; rx < w; rx++) {
            if (!(rows[rx >> 3] & (1 << (rx & 7))))
                continue;
            int px = x + rx;
            if (px >= c->x1 && px < c->x2)
                out[px] = color;
        }
    }
}

void font_draw(Surface *s, const Clip *c, int size, int style, const uint16_t *text, int n,
               int x, int y, uint16_t color)
{
    const FontData *f = font(size);
    int             start = x;
    if (y >= c->y2 || y + f->height <= c->y1)
        return;
    for (int i = 0; i < n && x < c->x2; i++) {
        int g = glyph_index(text[i]);
        int w = f->widths[g];
        if (x + w + 1 > c->x1) {
            draw_glyph(s, c, f, g, x, y, color);
            if (style & STYLE_BOLD)
                draw_glyph(s, c, f, g, x + 1, y, color);
        }
        x += w + ((style & STYLE_BOLD) ? 1 : 0);
    }
    if (style & STYLE_UNDERLINED)
        gfx_fill_rect(s, c, start, y + f->baseline + 1, x - start, 1, color);
}
