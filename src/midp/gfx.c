/*
 * gfx.c - software rasterizer: rectangles, lines, triangles, arcs, image
 * blits with transforms and alpha.
 */
#include "gfx.h"

#include <stdlib.h>
#include <string.h>

static inline void plot(Surface *s, const Clip *c, int x, int y, uint16_t color)
{
    if (x >= c->x1 && x < c->x2 && y >= c->y1 && y < c->y2)
        s->px[y * s->w + x] = color;
}

void gfx_fill_rect(Surface *s, const Clip *c, int x, int y, int w, int h, uint16_t color)
{
    int x1 = x > c->x1 ? x : c->x1;
    int y1 = y > c->y1 ? y : c->y1;
    int x2 = x + w < c->x2 ? x + w : c->x2;
    int y2 = y + h < c->y2 ? y + h : c->y2;
    if (x1 >= x2 || y1 >= y2)
        return;
    int n = x2 - x1;
    uint16_t *row = s->px + y1 * s->w + x1;
    for (int i = 0; i < n; i++)
        row[i] = color;
    for (int yy = y1 + 1; yy < y2; yy++)
        memcpy(s->px + yy * s->w + x1, row, (size_t)n * 2);
}

void gfx_line(Surface *s, const Clip *c, int x1, int y1, int x2, int y2, uint16_t color, bool dotted)
{
    if (y1 == y2 && !dotted) {
        if (x1 > x2) { int t = x1; x1 = x2; x2 = t; }
        gfx_fill_rect(s, c, x1, y1, x2 - x1 + 1, 1, color);
        return;
    }
    if (x1 == x2 && !dotted) {
        if (y1 > y2) { int t = y1; y1 = y2; y2 = t; }
        gfx_fill_rect(s, c, x1, y1, 1, y2 - y1 + 1, color);
        return;
    }
    int dx = abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
    int dy = -abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
    int err = dx + dy;
    for (int step = 0;; step++) {
        if (!dotted || (step & 2) == 0)
            plot(s, c, x1, y1, color);
        if (x1 == x2 && y1 == y2)
            break;
        int e2 = 2 * err;
        if (e2 >= dy) { err += dy; x1 += sx; }
        if (e2 <= dx) { err += dx; y1 += sy; }
    }
}

void gfx_rect(Surface *s, const Clip *c, int x, int y, int w, int h, uint16_t color, bool dotted)
{
    if (w < 0 || h < 0)
        return;
    if (w == 0 || h == 0) {
        gfx_line(s, c, x, y, x + w, y + h, color, dotted);
        return;
    }
    gfx_line(s, c, x, y, x + w, y, color, dotted);
    gfx_line(s, c, x, y + h, x + w, y + h, color, dotted);
    gfx_line(s, c, x, y + 1, x, y + h - 1, color, dotted);
    gfx_line(s, c, x + w, y + 1, x + w, y + h - 1, color, dotted);
}

/* Scanline triangle fill with 16.16 fixed-point edges. Pixel centers on
 * each row between the edges are filled. */
void gfx_fill_triangle(Surface *s, const Clip *c, int x1, int y1, int x2, int y2,
                       int x3, int y3, uint16_t color)
{
    int t;
    if (y1 > y2) { t = y1; y1 = y2; y2 = t; t = x1; x1 = x2; x2 = t; }
    if (y2 > y3) { t = y2; y2 = y3; y3 = t; t = x2; x2 = x3; x3 = t; }
    if (y1 > y2) { t = y1; y1 = y2; y2 = t; t = x1; x1 = x2; x2 = t; }
    if (y1 == y3) {
        int lo = x1 < x2 ? x1 : x2, hi = x1 > x2 ? x1 : x2;
        if (x3 < lo) lo = x3;
        if (x3 > hi) hi = x3;
        gfx_fill_rect(s, c, lo, y1, hi - lo + 1, 1, color);
        return;
    }
    int ystart = y1 > c->y1 ? y1 : c->y1;
    int yend = y3 < c->y2 - 1 ? y3 : c->y2 - 1;
    for (int y = ystart; y <= yend; y++) {
        /* Long edge 1-3 and the short edge on this half, in 16.16 fixed
         * point (multiplications: shifting negative values is undefined). */
        int64_t xa = (int64_t)x1 * 65536 + (int64_t)(x3 - x1) * 65536 * (y - y1) / (y3 - y1);
        int64_t xb;
        if (y < y2 || y2 == y3) {
            xb = y2 == y1 ? (int64_t)x2 * 65536
                          : (int64_t)x1 * 65536 + (int64_t)(x2 - x1) * 65536 * (y - y1) / (y2 - y1);
        } else {
            xb = (int64_t)x2 * 65536 + (int64_t)(x3 - x2) * 65536 * (y - y2) / (y3 - y2);
        }
        if (xa > xb) { int64_t tmp = xa; xa = xb; xb = tmp; }
        int l = (int)((xa + 0x8000) >> 16), r = (int)((xb + 0x8000) >> 16);
        gfx_fill_rect(s, c, l, y, r - l + 1, 1, color);
    }
}

/* ------------------------------------------------------------------------ */
/* Arcs                                                                     */

/* Angle in degrees [0, 360) of (dx, -dy), measured counter-clockwise from
 * the positive x axis like MIDP. Uses a cheap octant approximation that is
 * monotonic, which is all the arc clipping needs. */
static int angle_of(double dx, double dy)
{
    double ax = dx < 0 ? -dx : dx, ay = dy < 0 ? -dy : dy;
    double a;
    if (ax == 0 && ay == 0)
        return 0;
    /* atan approximation, max error ~0.3 degrees */
    if (ax >= ay) {
        double z = ay / ax;
        a = (45.0 * z) - z * (z - 1) * (14.0 + 3.83 * z);
    } else {
        double z = ax / ay;
        a = 90.0 - ((45.0 * z) - z * (z - 1) * (14.0 + 3.83 * z));
    }
    if (dx < 0) a = 180 - a;
    if (dy < 0) a = 360 - a;
    int r = (int)(a + 0.5);
    return r >= 360 ? r - 360 : r;
}

static bool in_arc(int ang, int start, int arc)
{
    if (arc >= 360 || arc <= -360)
        return true;
    if (arc < 0) {
        start += arc;
        arc = -arc;
    }
    start %= 360;
    if (start < 0)
        start += 360;
    int d = ang - start;
    if (d < 0)
        d += 360;
    return d <= arc;
}

void gfx_arc(Surface *s, const Clip *c, int x, int y, int w, int h, int start, int arc,
             uint16_t color, bool fill)
{
    if (w < 0 || h < 0 || arc == 0)
        return;
    double rx = w / 2.0, ry = h / 2.0;
    double cx = x + rx, cy = y + ry;
    int y1 = y > c->y1 ? y : c->y1, y2 = y + h < c->y2 - 1 ? y + h : c->y2 - 1;
    int x1 = x > c->x1 ? x : c->x1, x2 = x + w < c->x2 - 1 ? x + w : c->x2 - 1;
    if (rx == 0 || ry == 0) {
        gfx_line(s, c, x, y, x + w, y + h, color, false);
        return;
    }
    for (int py = y1; py <= y2; py++) {
        double ny = (py + (fill ? 0.5 : 0.0) - cy) / ry;
        for (int px = x1; px <= x2; px++) {
            double nx = (px + (fill ? 0.5 : 0.0) - cx) / rx;
            double d = nx * nx + ny * ny;
            bool on;
            if (fill) {
                on = d <= 1.0;
            } else {
                /* Outline: inside the ellipse but a neighbour is outside. */
                double ex = 1.0 / rx, ey = 1.0 / ry;
                double tol = (ex > ey ? ex : ey) * 2.0;
                on = d <= 1.0 + tol * 0.5 && d >= 1.0 - tol;
            }
            if (on && in_arc(angle_of(nx, -ny), start, arc))
                s->px[py * s->w + px] = color;
        }
    }
}

void gfx_round_rect(Surface *s, const Clip *c, int x, int y, int w, int h, int aw, int ah,
                    uint16_t color, bool fill)
{
    if (w < 0 || h < 0)
        return;
    if (aw < 0) aw = -aw;
    if (ah < 0) ah = -ah;
    if (aw > w) aw = w;
    if (ah > h) ah = h;
    int rw = aw / 2, rh = ah / 2;
    if (fill) {
        gfx_fill_rect(s, c, x + rw, y, w - 2 * rw, h, color);
        gfx_fill_rect(s, c, x, y + rh, rw, h - 2 * rh, color);
        gfx_fill_rect(s, c, x + w - rw, y + rh, rw, h - 2 * rh, color);
        gfx_arc(s, c, x, y, aw, ah, 90, 90, color, true);
        gfx_arc(s, c, x + w - aw, y, aw, ah, 0, 90, color, true);
        gfx_arc(s, c, x, y + h - ah, aw, ah, 180, 90, color, true);
        gfx_arc(s, c, x + w - aw, y + h - ah, aw, ah, 270, 90, color, true);
    } else {
        gfx_line(s, c, x + rw, y, x + w - rw, y, color, false);
        gfx_line(s, c, x + rw, y + h, x + w - rw, y + h, color, false);
        gfx_line(s, c, x, y + rh, x, y + h - rh, color, false);
        gfx_line(s, c, x + w, y + rh, x + w, y + h - rh, color, false);
        gfx_arc(s, c, x, y, aw, ah, 90, 90, color, false);
        gfx_arc(s, c, x + w - aw, y, aw, ah, 0, 90, color, false);
        gfx_arc(s, c, x, y + h - ah, aw, ah, 180, 90, color, false);
        gfx_arc(s, c, x + w - aw, y + h - ah, aw, ah, 270, 90, color, false);
    }
}

/* ------------------------------------------------------------------------ */
/* Blits                                                                    */

void gfx_blit(Surface *dst, const Clip *c, const Surface *src, int sx, int sy, int w, int h,
              int transform, int dx, int dy)
{
    bool swap = transform >= 4;   /* the ROT90/ROT270 family swaps axes */
    int  dw = swap ? h : w, dh = swap ? w : h;

    /* Where destination (0,0) reads from, and the source step per
     * destination x and y, in source pixels. */
    int u0, v0, ux, vx, uy, vy;
    switch (transform) {
    default:
    case TRANS_NONE:          u0 = 0;     v0 = 0;     ux = 1;  vx = 0;  uy = 0;  vy = 1;  break;
    case TRANS_MIRROR:        u0 = w - 1; v0 = 0;     ux = -1; vx = 0;  uy = 0;  vy = 1;  break;
    case TRANS_MIRROR_ROT180: u0 = 0;     v0 = h - 1; ux = 1;  vx = 0;  uy = 0;  vy = -1; break;
    case TRANS_ROT180:        u0 = w - 1; v0 = h - 1; ux = -1; vx = 0;  uy = 0;  vy = -1; break;
    case TRANS_ROT90:         u0 = 0;     v0 = h - 1; ux = 0;  vx = -1; uy = 1;  vy = 0;  break;
    case TRANS_ROT270:        u0 = w - 1; v0 = 0;     ux = 0;  vx = 1;  uy = -1; vy = 0;  break;
    case TRANS_MIRROR_ROT90:  u0 = w - 1; v0 = h - 1; ux = 0;  vx = -1; uy = -1; vy = 0;  break;
    case TRANS_MIRROR_ROT270: u0 = 0;     v0 = 0;     ux = 0;  vx = 1;  uy = 1;  vy = 0;  break;
    }

    int x1 = dx > c->x1 ? dx : c->x1;
    int y1 = dy > c->y1 ? dy : c->y1;
    int x2 = dx + dw < c->x2 ? dx + dw : c->x2;
    int y2 = dy + dh < c->y2 ? dy + dh : c->y2;
    if (x1 >= x2 || y1 >= y2)
        return;

    /* Fast path: untransformed opaque copy, row by row (memmove handles
     * copyArea overlap). */
    if (transform == TRANS_NONE && !src->alpha) {
        int n = x2 - x1;
        if (src == dst && dy < sy) {
            for (int y = y1; y < y2; y++)
                memmove(dst->px + y * dst->w + x1,
                        src->px + (sy + y - dy) * src->w + sx + (x1 - dx), (size_t)n * 2);
        } else {
            for (int y = y2 - 1; y >= y1; y--)
                memmove(dst->px + y * dst->w + x1,
                        src->px + (sy + y - dy) * src->w + sx + (x1 - dx), (size_t)n * 2);
        }
        return;
    }

    /* Transformed copies from a surface onto itself go through a copy. */
    uint16_t *tmp = NULL;
    const uint16_t *spx = src->px;
    int sstride = src->w;
    if (src == dst) {
        tmp = malloc((size_t)w * h * 2);
        if (!tmp)
            return;
        for (int y = 0; y < h; y++)
            memcpy(tmp + y * w, src->px + (sy + y) * src->w + sx, (size_t)w * 2);
        spx = tmp;
        sstride = w;
        sx = sy = 0;
    }

    for (int y = y1; y < y2; y++) {
        int ddx = x1 - dx, ddy = y - dy;
        int u = u0 + ux * ddx + uy * ddy;
        int v = v0 + vx * ddx + vy * ddy;
        uint16_t *out = dst->px + y * dst->w + x1;
        for (int x = x1; x < x2; x++, u += ux, v += vx, out++) {
            int si = (sy + v) * sstride + sx + u;
            uint16_t p = spx[si];
            if (src->alpha && !tmp) {
                uint8_t a = src->alpha[si];
                if (a == 255)
                    *out = p;
                else if (a)
                    *out = gfx_blend(*out, p, a);
            } else {
                *out = p;
            }
        }
    }
    free(tmp);
}

void gfx_draw_rgb(Surface *dst, const Clip *c, const int32_t *rgb, int offset, int scan,
                  int x, int y, int w, int h, bool alpha)
{
    int x1 = x > c->x1 ? x : c->x1;
    int y1 = y > c->y1 ? y : c->y1;
    int x2 = x + w < c->x2 ? x + w : c->x2;
    int y2 = y + h < c->y2 ? y + h : c->y2;
    for (int yy = y1; yy < y2; yy++) {
        const int32_t *row = rgb + offset + (yy - y) * scan + (x1 - x);
        uint16_t      *out = dst->px + yy * dst->w + x1;
        for (int xx = x1; xx < x2; xx++, row++, out++) {
            uint32_t argb = (uint32_t)*row;
            unsigned a = alpha ? argb >> 24 : 255;
            if (a)
                *out = gfx_blend(*out, gfx_pack(argb), a);
        }
    }
}
