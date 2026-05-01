/*
 * nds_png.c - Self-contained PNG decoder for the NDS KVM port.
 *
 * Supports 8-bit: palette (with/without tRNS), RGB, RGBA, grayscale.
 * Output: NDS RGB555 (0x8000|R5<<10|G5<<5|B5) into imageData.pixelBuf,
 *         raw alpha bytes into imageData.alphaBuf.
 *
 * imgType set so Java caller receives imgType+1:
 *   0 -> grayscale  (Java 1)
 *   1 -> RGB        (Java 2)
 *   2 -> RGBA       (Java 3)
 *   3 -> palette    (Java 4, TYPE_TRANSP, bit-15 transparency in pixelBuf)
 */

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "gxj_image.h"

/* ------------------------------------------------------------------ */
/* Bit reader (LSB-first, as DEFLATE requires)                         */
/* ------------------------------------------------------------------ */
typedef struct {
    const unsigned char *buf;
    int pos, size;
    unsigned int bits;
    int nbits;
} BR;

static void br_init(BR *b, const unsigned char *p, int n) {
    b->buf = p; b->pos = 0; b->size = n;
    b->bits = 0; b->nbits = 0;
}

static void br_fill(BR *b) {
    while (b->nbits <= 24 && b->pos < b->size)
        b->bits |= (unsigned int)b->buf[b->pos++] << b->nbits, b->nbits += 8;
}

static unsigned int br_read(BR *b, int n) {
    unsigned int v;
    if (b->nbits < n) br_fill(b);
    v = b->bits & ((1u << n) - 1);
    b->bits >>= n; b->nbits -= n;
    return v;
}

static void br_align(BR *b) {
    int r = b->nbits & 7;
    if (r) { b->bits >>= r; b->nbits -= r; }
}

/* ------------------------------------------------------------------ */
/* Canonical Huffman decoder                                           */
/* ------------------------------------------------------------------ */
typedef struct {
    int cnt[16];  /* count[1..15] */
    int sym[320]; /* symbols in canonical order */
} HT;

static void huff_build(HT *h, const unsigned char *lens, int n) {
    int i, offs[17];
    memset(h->cnt, 0, sizeof(h->cnt));
    for (i = 0; i < n; i++) if (lens[i]) h->cnt[(int)lens[i]]++;
    offs[1] = 0;
    for (i = 2; i <= 15; i++) offs[i] = offs[i-1] + h->cnt[i-1];
    for (i = 0; i < n; i++) if (lens[i]) h->sym[offs[(int)lens[i]]++] = i;
}

static int huff_sym(BR *b, const HT *h) {
    int len, code = 0, first = 0, idx = 0;
    for (len = 1; len <= 15; len++) {
        code = (code << 1) | (int)br_read(b, 1);
        int c = h->cnt[len];
        if (code - first < c) return h->sym[idx + (code - first)];
        idx += c;
        first = (first + c) << 1;
    }
    return -1;
}

/* ------------------------------------------------------------------ */
/* DEFLATE tables                                                       */
/* ------------------------------------------------------------------ */
static const unsigned char s_len_extra[29] = {
    0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0
};
static const unsigned short s_len_base[29] = {
    3,4,5,6,7,8,9,10,11,13,15,17,19,23,27,31,
    35,43,51,59,67,83,99,115,131,163,195,227,258
};
static const unsigned char s_dist_extra[30] = {
    0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13
};
static const unsigned int s_dist_base[30] = {
    1,2,3,4,5,7,9,13,17,25,33,49,65,97,129,193,
    257,385,513,769,1025,1537,2049,3073,4097,6145,
    8193,12289,16385,24577
};
static const unsigned char s_cl_order[19] = {
    16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15
};

/* ------------------------------------------------------------------ */
/* Output buffer (fixed, pre-allocated)                                */
/* ------------------------------------------------------------------ */
typedef struct { unsigned char *data; int sz, cap; } OB;

static int ob_put(OB *o, unsigned char c) {
    if (o->sz >= o->cap) return -1;
    o->data[o->sz++] = c;
    return 0;
}

static int ob_copy(OB *o, int dist, int len) {
    if (dist <= 0 || o->sz - dist < 0) return -1;
    if (o->sz + len > o->cap) return -1;
    while (len--) {
        o->data[o->sz] = o->data[o->sz - dist];
        o->sz++;
    }
    return 0;
}

/* ------------------------------------------------------------------ */
/* DEFLATE decompressor                                                 */
/* ------------------------------------------------------------------ */

/* Static to avoid large stack frames on ARM9 */
static HT s_lit, s_dist, s_cl;

static void build_fixed_trees(void) {
    unsigned char lens[288];
    int i;
    for (i =   0; i < 144; i++) lens[i] = 8;
    for (i = 144; i < 256; i++) lens[i] = 9;
    for (i = 256; i < 280; i++) lens[i] = 7;
    for (i = 280; i < 288; i++) lens[i] = 8;
    huff_build(&s_lit, lens, 288);
    for (i = 0; i < 32; i++) lens[i] = 5;
    huff_build(&s_dist, lens, 32);
}

static int inflate_stored(BR *b, OB *o) {
    unsigned int len, nlen;
    br_align(b);
    len  = br_read(b, 16);
    nlen = br_read(b, 16);
    (void)nlen;
    if (o->sz + (int)len > o->cap) return -1;
    while (len--) {
        if (b->nbits >= 8) {
            o->data[o->sz++] = (unsigned char)(b->bits & 0xFF);
            b->bits >>= 8; b->nbits -= 8;
        } else {
            if (b->pos >= b->size) return -1;
            o->data[o->sz++] = b->buf[b->pos++];
        }
    }
    return 0;
}

static int inflate_huf(BR *b, OB *o) {
    for (;;) {
        int sym = huff_sym(b, &s_lit);
        if (sym < 0) return -1;
        if (sym < 256) {
            if (ob_put(o, (unsigned char)sym)) return -1;
        } else if (sym == 256) {
            break;
        } else {
            int li = sym - 257;
            if (li >= 29) return -1;
            int length = (int)s_len_base[li] + (int)br_read(b, s_len_extra[li]);
            int ds = huff_sym(b, &s_dist);
            if (ds < 0 || ds >= 30) return -1;
            int dist = (int)s_dist_base[ds] + (int)br_read(b, s_dist_extra[ds]);
            if (ob_copy(o, dist, length)) return -1;
        }
    }
    return 0;
}

static int inflate(const unsigned char *in, int insz, OB *o) {
    BR b;
    int bfinal, btype;

    if (insz < 2) return -1;
    br_init(&b, in + 2, insz - 2); /* skip zlib CMF/FLG */

    do {
        bfinal = (int)br_read(&b, 1);
        btype  = (int)br_read(&b, 2);

        if (btype == 0) {
            if (inflate_stored(&b, o)) return -1;
        } else if (btype == 1) {
            build_fixed_trees();
            if (inflate_huf(&b, o)) return -1;
        } else if (btype == 2) {
            unsigned char code_lens[320];
            unsigned char cl_lens[19];
            int i, hlit, hdist, hclen, total, idx;

            hlit  = (int)br_read(&b, 5) + 257;
            hdist = (int)br_read(&b, 5) + 1;
            hclen = (int)br_read(&b, 4) + 4;

            if (hlit > 286 || hdist > 30) return -1;

            memset(cl_lens, 0, sizeof(cl_lens));
            for (i = 0; i < hclen; i++)
                cl_lens[s_cl_order[i]] = (unsigned char)br_read(&b, 3);
            huff_build(&s_cl, cl_lens, 19);

            total = hlit + hdist;
            memset(code_lens, 0, total);
            idx = 0;
            while (idx < total) {
                int sym = huff_sym(&b, &s_cl);
                if (sym < 0) return -1;
                if (sym < 16) {
                    code_lens[idx++] = (unsigned char)sym;
                } else if (sym == 16) {
                    unsigned char last;
                    int rep;
                    if (idx == 0) return -1;
                    last = code_lens[idx - 1];
                    rep  = (int)br_read(&b, 2) + 3;
                    while (rep-- && idx < total) code_lens[idx++] = last;
                } else if (sym == 17) {
                    int rep = (int)br_read(&b, 3) + 3;
                    while (rep-- && idx < total) code_lens[idx++] = 0;
                } else {
                    int rep = (int)br_read(&b, 7) + 11;
                    while (rep-- && idx < total) code_lens[idx++] = 0;
                }
            }

            huff_build(&s_lit,  code_lens,        hlit);
            huff_build(&s_dist, code_lens + hlit,  hdist);
            if (inflate_huf(&b, o)) return -1;
        } else {
            return -1;
        }
    } while (!bfinal);

    return 0;
}

/* ------------------------------------------------------------------ */
/* PNG helpers                                                          */
/* ------------------------------------------------------------------ */
static unsigned int u32be(const unsigned char *p) {
    return ((unsigned int)p[0] << 24) | ((unsigned int)p[1] << 16) |
           ((unsigned int)p[2] <<  8) |  (unsigned int)p[3];
}

static unsigned char paeth_pred(int a, int b, int c) {
    int p  = a + b - c;
    int pa = abs(p - a), pb = abs(p - b), pc = abs(p - c);
    if (pa <= pb && pa <= pc) return (unsigned char)a;
    if (pb <= pc)             return (unsigned char)b;
    return (unsigned char)c;
}

#define PNG_IHDR 0x49484452u
#define PNG_PLTE 0x504C5445u
#define PNG_IDAT 0x49444154u
#define PNG_IEND 0x49454E44u
#define PNG_tRNS 0x74524E53u

/* ------------------------------------------------------------------ */
/* decode_png_image                                                     */
/* ------------------------------------------------------------------ */
int decode_png_image(imageSrcData *s, imageDstData *d) {
    const unsigned char *src = (const unsigned char *)s->src->data;
    int srclen               = s->src->size;
    imageData *img           = d->dst;

    img->width = img->height = img->imgType = 0;

    if (srclen < 8 ||
        src[0] != 0x89 || src[1] != 0x50 ||
        src[2] != 0x4E || src[3] != 0x47) {
        printf("PNG: bad signature\n");
        return 0;
    }

    int pos = 8;
    int width = 0, height = 0, color_type = 0, samples = 0;

    unsigned short plte[256];
    unsigned char  trns[256];
    int plte_count = 0, has_trns = 0;
    memset(trns, 0xFF, 256);

    unsigned char *idat = NULL;
    int idat_sz = 0, idat_cap = 0;

    /* --- parse chunks --- */
    while (pos + 12 <= srclen) {
        unsigned int clen  = u32be(src + pos);
        unsigned int ctype = u32be(src + pos + 4);
        const unsigned char *cd = src + pos + 8;

        if ((int)clen < 0 || pos + 12 + (int)clen > srclen) break;
        pos += 12 + (int)clen;

        if (ctype == PNG_IHDR) {
            if (clen < 13) goto fail;
            width      = (int)u32be(cd);
            height     = (int)u32be(cd + 4);
            if (cd[8] != 8) { printf("PNG: bit depth %d unsupported\n", cd[8]); goto fail; }
            color_type = cd[9];
            switch (color_type) {
                case 0: samples = 1; break;
                case 2: samples = 3; break;
                case 3: samples = 1; break;
                case 4: samples = 2; break;
                case 6: samples = 4; break;
                default: printf("PNG: color type %d unsupported\n", color_type); goto fail;
            }
        } else if (ctype == PNG_PLTE) {
            plte_count = (int)clen / 3;
            if (plte_count > 256) plte_count = 256;
            for (int i = 0; i < plte_count; i++) {
                int r = cd[i*3  ] >> 3;
                int g = cd[i*3+1] >> 3;
                int b = cd[i*3+2] >> 3;
                plte[i] = (unsigned short)(0x8000 | (b << 10) | (g << 5) | r);
            }
        } else if (ctype == PNG_tRNS) {
            has_trns = 1;
            if (color_type == 3) {
                int cnt = (int)clen;
                if (cnt > 256) cnt = 256;
                for (int i = 0; i < cnt; i++) trns[i] = cd[i];
            }
        } else if (ctype == PNG_IDAT) {
            if (idat_sz + (int)clen > idat_cap) {
                int nc = idat_cap ? idat_cap * 2 : 65536;
                while (nc < idat_sz + (int)clen) nc *= 2;
                unsigned char *nb = (unsigned char *)realloc(idat, nc);
                if (!nb) goto fail;
                idat = nb; idat_cap = nc;
            }
            memcpy(idat + idat_sz, cd, clen);
            idat_sz += (int)clen;
        } else if (ctype == PNG_IEND) {
            break;
        }
    }

    if (!width || !height || !idat_sz || !samples) {
        printf("PNG: missing data w=%d h=%d idat=%d\n", width, height, idat_sz);
        goto fail;
    }

    /* --- inflate --- */
    {
        int expected = height * (1 + width * samples);
        OB raw;
        raw.data = (unsigned char *)malloc(expected);
        if (!raw.data) goto fail;
        raw.sz = 0; raw.cap = expected;

        if (inflate(idat, idat_sz, &raw) != 0) {
            printf("PNG: inflate failed (got %d of %d)\n", raw.sz, expected);
            free(raw.data);
            goto fail;
        }
        if (raw.sz < expected) {
            printf("PNG: short inflate %d < %d\n", raw.sz, expected);
            free(raw.data);
            goto fail;
        }
        free(idat); idat = NULL;

        /* --- un-filter and convert --- */
        int stride = 1 + width * samples;
        int bpp    = samples;
        unsigned char *prev = (unsigned char *)calloc(width * samples, 1);
        if (!prev) { free(raw.data); goto fail; }

        unsigned short *pix = img->pixelBuf;
        unsigned char  *alp = img->alphaBuf;
        int ok = 1;

        for (int y = 0; y < height && ok; y++) {
            unsigned char *row  = raw.data + y * stride;
            int ftype            = row[0];
            unsigned char *curr  = row + 1;
            int rowsz            = width * samples;

            switch (ftype) {
            case 0: break;
            case 1:
                for (int x = bpp; x < rowsz; x++)
                    curr[x] = (unsigned char)(curr[x] + curr[x - bpp]);
                break;
            case 2:
                for (int x = 0; x < rowsz; x++)
                    curr[x] = (unsigned char)(curr[x] + prev[x]);
                break;
            case 3:
                for (int x = 0; x < rowsz; x++) {
                    int a = (x >= bpp) ? (unsigned char)curr[x - bpp] : 0;
                    curr[x] = (unsigned char)(curr[x] + ((a + (unsigned char)prev[x]) >> 1));
                }
                break;
            case 4:
                for (int x = 0; x < rowsz; x++) {
                    int a = (x >= bpp) ? (unsigned char)curr[x - bpp] : 0;
                    int b = (unsigned char)prev[x];
                    int c = (x >= bpp) ? (unsigned char)prev[x - bpp] : 0;
                    curr[x] = (unsigned char)(curr[x] + paeth_pred(a, b, c));
                }
                break;
            default:
                printf("PNG: unknown filter %d row %d\n", ftype, y);
                ok = 0; break;
            }

            if (!ok) break;

            for (int x = 0; x < width; x++) {
                unsigned char *px = curr + x * samples;
                int oi = y * width + x;
                switch (color_type) {
                case 0: /* grayscale */
                    if (alp) alp[oi] = px[0];
                    break;
                case 2: /* RGB — NDS BGR555: B<<10 G<<5 R */
                    if (pix) pix[oi] = (unsigned short)(0x8000 |
                        ((px[2] >> 3) << 10) | ((px[1] >> 3) << 5) | (px[0] >> 3));
                    break;
                case 3: /* palette */
                    if (pix) {
                        int pi = px[0];
                        if (has_trns && trns[pi] < 128)
                            pix[oi] = 0;
                        else
                            pix[oi] = (pi < plte_count) ? plte[pi] : (unsigned short)0x8000;
                    }
                    break;
                case 4: /* grayscale+alpha */
                    if (alp) alp[oi] = px[0];
                    break;
                case 6: /* RGBA — NDS BGR555: B<<10 G<<5 R */
                    if (pix) {
                        unsigned char a = px[3];
                        pix[oi] = (unsigned short)((a >= 128 ? 0x8000 : 0) |
                            ((px[2] >> 3) << 10) | ((px[1] >> 3) << 5) | (px[0] >> 3));
                    }
                    if (alp) alp[oi] = px[3];
                    break;
                }
            }

            memcpy(prev, curr, rowsz);
        }

        free(prev);
        free(raw.data);

        if (!ok) goto fail;
    }

    img->width  = width;
    img->height = height;
    switch (color_type) {
    case 0: case 4: img->imgType = 0; break;
    case 2:         img->imgType = 1; break;
    case 6:         img->imgType = 2; break;
    case 3:         img->imgType = 3; break;
    }

    printf("PNG: OK %dx%d ct=%d\n", width, height, color_type);
    free(idat);
    return 1;

fail:
    free(idat);
    return 0;
}
