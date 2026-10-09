/*
 * png.c - PNG decoder. The concatenated IDAT stream is inflated in one go,
 * then each scanline is unfiltered and converted to ARGB.
 */
#include "png.h"
#include "inflate.h"

#include <stdlib.h>
#include <string.h>

typedef struct {
    int      w, h, depth, ctype, interlace;
    uint32_t palette[256];
    int      n_palette;
    bool     has_trns;
    uint16_t trns_gray, trns_r, trns_g, trns_b;
    uint8_t *idat;
    uint32_t idat_len;
} Png;

static uint32_t be32(const uint8_t *p)
{
    return ((uint32_t)p[0] << 24) | ((uint32_t)p[1] << 16) | ((uint32_t)p[2] << 8) | p[3];
}

bool png_is_png(const uint8_t *data, uint32_t len)
{
    static const uint8_t sig[8] = {137, 80, 78, 71, 13, 10, 26, 10};
    return len >= 8 && memcmp(data, sig, 8) == 0;
}

static int channels(int ctype)
{
    switch (ctype) {
    case 0: return 1;
    case 2: return 3;
    case 3: return 1;
    case 4: return 2;
    case 6: return 4;
    default: return 0;
    }
}

/* Parses the chunks. With collect set, IDAT data is gathered into p->idat. */
static bool parse(const uint8_t *data, uint32_t len, Png *p, bool collect)
{
    memset(p, 0, sizeof *p);
    if (!png_is_png(data, len))
        return false;
    for (int i = 0; i < 256; i++)
        p->palette[i] = 0xFF000000u;
    uint32_t pos = 8;
    bool     have_ihdr = false;
    while (pos + 12 <= len) {
        uint32_t clen = be32(data + pos);
        const uint8_t *type = data + pos + 4;
        const uint8_t *body = data + pos + 8;
        if (clen > len - pos - 12)
            return false;
        if (memcmp(type, "IHDR", 4) == 0 && clen >= 13) {
            p->w = (int)be32(body);
            p->h = (int)be32(body + 4);
            p->depth = body[8];
            p->ctype = body[9];
            p->interlace = body[12];
            have_ihdr = true;
            if (p->w <= 0 || p->h <= 0 || p->w > 4096 || p->h > 4096 || !channels(p->ctype))
                return false;
        } else if (memcmp(type, "PLTE", 4) == 0) {
            p->n_palette = (int)(clen / 3 > 256 ? 256 : clen / 3);
            for (int i = 0; i < p->n_palette; i++)
                p->palette[i] = 0xFF000000u | ((uint32_t)body[i * 3] << 16) |
                                ((uint32_t)body[i * 3 + 1] << 8) | body[i * 3 + 2];
        } else if (memcmp(type, "tRNS", 4) == 0) {
            p->has_trns = true;
            if (p->ctype == 3) {
                for (uint32_t i = 0; i < clen && i < 256; i++)
                    p->palette[i] = (p->palette[i] & 0x00FFFFFFu) | ((uint32_t)body[i] << 24);
            } else if (p->ctype == 0 && clen >= 2) {
                p->trns_gray = (uint16_t)((body[0] << 8) | body[1]);
            } else if (p->ctype == 2 && clen >= 6) {
                p->trns_r = (uint16_t)((body[0] << 8) | body[1]);
                p->trns_g = (uint16_t)((body[2] << 8) | body[3]);
                p->trns_b = (uint16_t)((body[4] << 8) | body[5]);
            }
        } else if (memcmp(type, "IDAT", 4) == 0 && collect) {
            uint8_t *n = realloc(p->idat, p->idat_len + clen + 1);
            if (!n)
                return false;
            p->idat = n;
            memcpy(p->idat + p->idat_len, body, clen);
            p->idat_len += clen;
        } else if (memcmp(type, "IEND", 4) == 0) {
            break;
        }
        pos += clen + 12;
    }
    return have_ihdr;
}

bool png_info(const uint8_t *data, uint32_t len, PngInfo *info)
{
    Png p;
    if (!parse(data, len, &p, false))
        return false;
    info->width = p.w;
    info->height = p.h;
    info->has_alpha = p.ctype == 4 || p.ctype == 6 || p.has_trns;
    return true;
}

static int paeth(int a, int b, int c)
{
    int p = a + b - c;
    int pa = abs(p - a), pb = abs(p - b), pc = abs(p - c);
    if (pa <= pb && pa <= pc) return a;
    if (pb <= pc) return b;
    return c;
}

static bool unfilter(uint8_t *row, const uint8_t *prev, size_t n, int bpp, int type)
{
    switch (type) {
    case 0:
        break;
    case 1:
        for (size_t i = (size_t)bpp; i < n; i++)
            row[i] = (uint8_t)(row[i] + row[i - bpp]);
        break;
    case 2:
        if (prev)
            for (size_t i = 0; i < n; i++)
                row[i] = (uint8_t)(row[i] + prev[i]);
        break;
    case 3:
        for (size_t i = 0; i < n; i++) {
            int left = i >= (size_t)bpp ? row[i - bpp] : 0;
            int up = prev ? prev[i] : 0;
            row[i] = (uint8_t)(row[i] + ((left + up) >> 1));
        }
        break;
    case 4:
        for (size_t i = 0; i < n; i++) {
            int left = i >= (size_t)bpp ? row[i - bpp] : 0;
            int up = prev ? prev[i] : 0;
            int ul = (prev && i >= (size_t)bpp) ? prev[i - bpp] : 0;
            row[i] = (uint8_t)(row[i] + paeth(left, up, ul));
        }
        break;
    default:
        return false;
    }
    return true;
}

static int sample(const uint8_t *row, int i, int depth)
{
    switch (depth) {
    case 1:  return (row[i >> 3] >> (7 - (i & 7))) & 1;
    case 2:  return (row[i >> 2] >> ((3 - (i & 3)) * 2)) & 3;
    case 4:  return (row[i >> 1] >> ((1 - (i & 1)) * 4)) & 15;
    case 8:  return row[i];
    default: return (row[i * 2] << 8) | row[i * 2 + 1];
    }
}

static void convert(const Png *p, const uint8_t *row, int n, uint32_t *out)
{
    int d = p->depth;
    int max = (1 << d) - 1;
    for (int x = 0; x < n; x++) {
        uint32_t a = 255, r, g, b;
        switch (p->ctype) {
        case 0: {
            int v = sample(row, x, d);
            r = g = b = (uint32_t)(v * 255 / max);
            if (p->has_trns && v == p->trns_gray)
                a = 0;
            break;
        }
        case 2: {
            int vr = sample(row, x * 3, d), vg = sample(row, x * 3 + 1, d), vb = sample(row, x * 3 + 2, d);
            if (p->has_trns && vr == p->trns_r && vg == p->trns_g && vb == p->trns_b)
                a = 0;
            r = (uint32_t)(vr * 255 / max);
            g = (uint32_t)(vg * 255 / max);
            b = (uint32_t)(vb * 255 / max);
            break;
        }
        case 3:
            out[x] = p->palette[sample(row, x, d) & 255];
            continue;
        case 4:
            r = g = b = (uint32_t)(sample(row, x * 2, d) * 255 / max);
            a = (uint32_t)(sample(row, x * 2 + 1, d) * 255 / max);
            break;
        default:
            r = (uint32_t)(sample(row, x * 4, d) * 255 / max);
            g = (uint32_t)(sample(row, x * 4 + 1, d) * 255 / max);
            b = (uint32_t)(sample(row, x * 4 + 2, d) * 255 / max);
            a = (uint32_t)(sample(row, x * 4 + 3, d) * 255 / max);
            break;
        }
        out[x] = (a << 24) | (r << 16) | (g << 8) | b;
    }
}

bool png_decode(const uint8_t *data, uint32_t len, PngRowFn fn, void *ctx)
{
    static const int ax0[7] = {0, 4, 0, 2, 0, 1, 0}, ay0[7] = {0, 0, 4, 0, 2, 0, 1};
    static const int adx[7] = {8, 8, 4, 4, 2, 2, 1}, ady[7] = {8, 8, 8, 4, 4, 2, 2};

    Png p;
    if (!parse(data, len, &p, true) || !p.idat) {
        free(p.idat);
        return false;
    }
    int bits_pp = channels(p.ctype) * p.depth;
    int bpp = (bits_pp + 7) / 8;
    int passes = p.interlace ? 7 : 1;

    /* Size of the raw (filtered) image data over all passes. */
    size_t raw_len = 0;
    for (int ps = 0; ps < passes; ps++) {
        int pw = p.interlace ? (p.w - ax0[ps] + adx[ps] - 1) / adx[ps] : p.w;
        int ph = p.interlace ? (p.h - ay0[ps] + ady[ps] - 1) / ady[ps] : p.h;
        if (pw > 0 && ph > 0)
            raw_len += (size_t)ph * (1 + ((size_t)pw * bits_pp + 7) / 8);
    }
    uint8_t  *raw = malloc(raw_len + 1);
    uint32_t *argb = malloc(sizeof(uint32_t) * (size_t)p.w);
    bool      ok = raw && argb &&
                   nj_zlib_inflate(raw, (uint32_t)raw_len, p.idat, p.idat_len) == (int32_t)raw_len;
    free(p.idat);

    uint8_t *cur = raw;
    for (int ps = 0; ok && ps < passes; ps++) {
        int x0 = p.interlace ? ax0[ps] : 0, y0 = p.interlace ? ay0[ps] : 0;
        int dx = p.interlace ? adx[ps] : 1, dy = p.interlace ? ady[ps] : 1;
        int pw = (p.w - x0 + dx - 1) / dx, ph = (p.h - y0 + dy - 1) / dy;
        if (pw <= 0 || ph <= 0)
            continue;
        size_t   rowbytes = ((size_t)pw * bits_pp + 7) / 8;
        uint8_t *prev = NULL;
        for (int y = 0; y < ph && ok; y++) {
            uint8_t *row = cur + 1;
            ok = unfilter(row, prev, rowbytes, bpp, cur[0]);
            if (ok) {
                convert(&p, row, pw, argb);
                fn(ctx, y0 + y * dy, x0, dx, argb, pw);
            }
            prev = row;
            cur += rowbytes + 1;
        }
    }
    free(raw);
    free(argb);
    return ok;
}
