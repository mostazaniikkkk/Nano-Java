/*
 * inflate.c - a small, single-shot DEFLATE decoder (canonical Huffman
 * decoding in the style of zlib's "puff"). Output size is always known up
 * front (zip headers, PNG dimensions), so no sliding window is needed: back
 * references read straight from the output buffer.
 */
#include "inflate.h"

#include <string.h>

#define MAXBITS  15
#define MAXLCODE 286
#define MAXDCODE 30

typedef struct {
    const uint8_t *src, *src_end;
    uint8_t       *dst, *out, *dst_end;
    uint32_t       bitbuf;
    int            bitcnt;
    int            err;
} State;

typedef struct {
    uint16_t count[MAXBITS + 1];
    uint16_t symbol[288];
} Huff;

static inline int bits(State *s, int need)
{
    uint32_t val = s->bitbuf;
    while (s->bitcnt < need) {
        if (s->src == s->src_end) {
            s->err = 1;
            return 0;
        }
        val |= (uint32_t)*s->src++ << s->bitcnt;
        s->bitcnt += 8;
    }
    s->bitbuf = val >> need;
    s->bitcnt -= need;
    return (int)(val & ((1u << need) - 1));
}

static int decode(State *s, const Huff *h)
{
    int code = 0, first = 0, index = 0;
    for (int len = 1; len <= MAXBITS; len++) {
        code |= bits(s, 1);
        int count = h->count[len];
        if (code - count < first)
            return h->symbol[index + (code - first)];
        index += count;
        first += count;
        first <<= 1;
        code <<= 1;
        if (s->err)
            return -1;
    }
    return -1;
}

/* Returns <0 if over-subscribed, 0 if complete, >0 if incomplete. */
static int build(Huff *h, const uint8_t *length, int n)
{
    uint16_t offs[MAXBITS + 1];
    memset(h->count, 0, sizeof h->count);
    for (int i = 0; i < n; i++)
        h->count[length[i]]++;
    if (h->count[0] == n)
        return 0;
    int left = 1;
    for (int len = 1; len <= MAXBITS; len++) {
        left <<= 1;
        left -= h->count[len];
        if (left < 0)
            return left;
    }
    offs[1] = 0;
    for (int len = 1; len < MAXBITS; len++)
        offs[len + 1] = offs[len] + h->count[len];
    for (int i = 0; i < n; i++)
        if (length[i])
            h->symbol[offs[length[i]]++] = (uint16_t)i;
    return left;
}

static int stored(State *s)
{
    s->bitbuf = 0;
    s->bitcnt = 0;
    if (s->src_end - s->src < 4)
        return -1;
    unsigned len = s->src[0] | (s->src[1] << 8);
    unsigned nlen = s->src[2] | (s->src[3] << 8);
    s->src += 4;
    if (len != (~nlen & 0xffff))
        return -1;
    if ((unsigned)(s->src_end - s->src) < len)
        return -1;
    if ((unsigned)(s->dst_end - s->out) < len)
        return -1;
    memcpy(s->out, s->src, len);
    s->out += len;
    s->src += len;
    return 0;
}

static const uint16_t lbase[29] = {
    3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
    35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258};
static const uint8_t lext[29] = {
    0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
    3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0};
static const uint16_t dbase[30] = {
    1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129,
    193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097,
    6145, 8193, 12289, 16385, 24577};
static const uint8_t dext[30] = {
    0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
    7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13};

static int codes(State *s, const Huff *lencode, const Huff *distcode)
{
    for (;;) {
        int sym = decode(s, lencode);
        if (sym < 0 || s->err)
            return -1;
        if (sym < 256) {
            if (s->out == s->dst_end)
                return -1;
            *s->out++ = (uint8_t)sym;
        } else if (sym == 256) {
            return 0;
        } else {
            sym -= 257;
            if (sym >= 29)
                return -1;
            unsigned len = lbase[sym] + bits(s, lext[sym]);
            int dsym = decode(s, distcode);
            if (dsym < 0 || dsym >= 30 || s->err)
                return -1;
            unsigned dist = dbase[dsym] + bits(s, dext[dsym]);
            if (dist > (unsigned)(s->out - s->dst))
                return -1;
            if (len > (unsigned)(s->dst_end - s->out))
                return -1;
            const uint8_t *from = s->out - dist;
            while (len--)
                *s->out++ = *from++;
        }
    }
}

static int fixed(State *s)
{
    static Huff lencode, distcode;
    static int  ready;
    if (!ready) {
        uint8_t lengths[288];
        int     i;
        for (i = 0; i < 144; i++) lengths[i] = 8;
        for (; i < 256; i++) lengths[i] = 9;
        for (; i < 280; i++) lengths[i] = 7;
        for (; i < 288; i++) lengths[i] = 8;
        build(&lencode, lengths, 288);
        for (i = 0; i < MAXDCODE; i++) lengths[i] = 5;
        build(&distcode, lengths, MAXDCODE);
        ready = 1;
    }
    return codes(s, &lencode, &distcode);
}

static int dynamic(State *s)
{
    static const uint8_t order[19] = {
        16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
    uint8_t lengths[MAXLCODE + MAXDCODE];
    Huff    lencode, distcode;

    int nlen = bits(s, 5) + 257;
    int ndist = bits(s, 5) + 1;
    int ncode = bits(s, 4) + 4;
    if (s->err || nlen > MAXLCODE || ndist > MAXDCODE)
        return -1;

    int index;
    for (index = 0; index < ncode; index++)
        lengths[order[index]] = (uint8_t)bits(s, 3);
    for (; index < 19; index++)
        lengths[order[index]] = 0;
    if (build(&lencode, lengths, 19) != 0)
        return -1;

    index = 0;
    while (index < nlen + ndist) {
        int sym = decode(s, &lencode);
        if (sym < 0 || s->err)
            return -1;
        if (sym < 16) {
            lengths[index++] = (uint8_t)sym;
        } else {
            uint8_t len = 0;
            int     rep;
            if (sym == 16) {
                if (index == 0)
                    return -1;
                len = lengths[index - 1];
                rep = 3 + bits(s, 2);
            } else if (sym == 17) {
                rep = 3 + bits(s, 3);
            } else {
                rep = 11 + bits(s, 7);
            }
            if (index + rep > nlen + ndist)
                return -1;
            while (rep--)
                lengths[index++] = len;
        }
    }
    if (lengths[256] == 0)
        return -1;

    int err = build(&lencode, lengths, nlen);
    if (err < 0 || (err > 0 && nlen - lencode.count[0] != 1))
        return -1;
    err = build(&distcode, lengths + nlen, ndist);
    if (err < 0 || (err > 0 && ndist - distcode.count[0] != 1))
        return -1;
    return codes(s, &lencode, &distcode);
}

int32_t nj_inflate(uint8_t *dst, uint32_t dst_len,
                   const uint8_t *src, uint32_t src_len)
{
    State s;
    s.src = src;
    s.src_end = src + src_len;
    s.dst = s.out = dst;
    s.dst_end = dst + dst_len;
    s.bitbuf = 0;
    s.bitcnt = 0;
    s.err = 0;

    int last;
    do {
        last = bits(&s, 1);
        int type = bits(&s, 2);
        if (s.err)
            return -1;
        int r;
        switch (type) {
        case 0: r = stored(&s); break;
        case 1: r = fixed(&s); break;
        case 2: r = dynamic(&s); break;
        default: r = -1; break;
        }
        if (r != 0)
            return -1;
    } while (!last);
    return (int32_t)(s.out - dst);
}

int32_t nj_zlib_inflate(uint8_t *dst, uint32_t dst_len,
                        const uint8_t *src, uint32_t src_len)
{
    if (src_len < 2 || (src[0] & 0x0f) != 8 ||
        ((src[0] << 8) | src[1]) % 31 != 0 || (src[1] & 0x20))
        return -1;
    return nj_inflate(dst, dst_len, src + 2, src_len - 2);
}
