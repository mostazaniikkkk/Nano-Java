/*
 * media.c - MIDI (SMF 0/1), MMAPI tone sequences and WAV (PCM, IMA ADPCM,
 * u-law, A-law) decoding.
 */
#include "media.h"

#include <stdlib.h>
#include <string.h>

/* ------------------------------------------------------------------------ */
/* Standard MIDI files                                                      */

typedef struct {
    uint32_t tick;
    uint32_t seq;         /* order of appearance, to keep sorting stable */
    uint32_t tempo;       /* tempo events: microseconds per quarter note */
    uint8_t  status, d1, d2;
} RawEvent;

typedef struct {
    RawEvent *ev;
    uint32_t  n, cap;
} RawList;

static bool raw_push(RawList *l, RawEvent e)
{
    if (l->n == l->cap) {
        uint32_t  ncap = l->cap ? l->cap * 2 : 256;
        RawEvent *nev = realloc(l->ev, sizeof(RawEvent) * ncap);
        if (!nev)
            return false;
        l->ev = nev;
        l->cap = ncap;
    }
    e.seq = l->n;
    l->ev[l->n++] = e;
    return true;
}

static uint32_t be32(const uint8_t *p)
{
    return ((uint32_t)p[0] << 24) | ((uint32_t)p[1] << 16) | ((uint32_t)p[2] << 8) | p[3];
}

static bool vlq(const uint8_t **p, const uint8_t *end, uint32_t *out)
{
    uint32_t v = 0;
    for (int i = 0; i < 4; i++) {
        if (*p >= end)
            return false;
        uint8_t b = *(*p)++;
        v = (v << 7) | (b & 0x7F);
        if (!(b & 0x80)) {
            *out = v;
            return true;
        }
    }
    return false;
}

static bool parse_track(const uint8_t *p, const uint8_t *end, RawList *out)
{
    uint32_t tick = 0;
    uint8_t  running = 0;
    while (p < end) {
        uint32_t delta;
        if (!vlq(&p, end, &delta) || p >= end)
            return true;   /* tolerate truncated tracks */
        tick += delta;
        uint8_t status = *p;
        if (status & 0x80) {
            p++;
        } else if (running) {
            status = running;
        } else {
            return true;
        }
        if (status == 0xFF) {
            if (p >= end)
                return true;
            uint8_t  type = *p++;
            uint32_t len;
            if (!vlq(&p, end, &len) || len > (uint32_t)(end - p))
                return true;
            if (type == 0x51 && len == 3) {
                RawEvent e = {tick, 0, ((uint32_t)p[0] << 16) | (p[1] << 8) | p[2], 0xFF, 0, 0};
                if (!raw_push(out, e))
                    return false;
            } else if (type == 0x2F) {
                return true;
            }
            p += len;
            continue;
        }
        if (status == 0xF0 || status == 0xF7) {
            uint32_t len;
            if (!vlq(&p, end, &len) || len > (uint32_t)(end - p))
                return true;
            p += len;
            continue;
        }
        running = status;
        int nbytes = ((status & 0xF0) == 0xC0 || (status & 0xF0) == 0xD0) ? 1 : 2;
        if (end - p < nbytes)
            return true;
        uint8_t d1 = p[0] & 0x7F, d2 = nbytes == 2 ? p[1] & 0x7F : 0;
        p += nbytes;
        uint8_t kind = status & 0xF0;
        if (kind == 0x80 || kind == 0x90 || kind == 0xB0 || kind == 0xC0 || kind == 0xE0) {
            RawEvent e = {tick, 0, 0, status, d1, d2};
            if (!raw_push(out, e))
                return false;
        }
    }
    return true;
}

static int compare_raw(const void *a, const void *b)
{
    const RawEvent *x = a, *y = b;
    if (x->tick != y->tick)
        return x->tick < y->tick ? -1 : 1;
    /* At equal times: tempo first, then note offs, then the rest. */
    int px = x->status == 0xFF ? 0 : (x->status & 0xF0) == 0x80 ? 1 : 2;
    int py = y->status == 0xFF ? 0 : (y->status & 0xF0) == 0x80 ? 1 : 2;
    if (px != py)
        return px - py;
    return x->seq < y->seq ? -1 : x->seq > y->seq;
}

static bool decode_midi(const uint8_t *data, uint32_t len, NjaClip *out)
{
    if (len < 14 || memcmp(data, "MThd", 4) != 0)
        return false;
    uint32_t hlen = be32(data + 4);
    if (hlen < 6 || 8 + hlen > len)
        return false;
    uint16_t ntracks = (uint16_t)((data[10] << 8) | data[11]);
    uint16_t division = (uint16_t)((data[12] << 8) | data[13]);
    if (division == 0)
        return false;

    RawList  raw = {NULL, 0, 0};
    uint32_t pos = 8 + hlen;
    int      found = 0;
    /* Walk the chunks, skipping anything that is not a track. */
    while (found < ntracks && pos + 8 <= len) {
        uint32_t tlen = be32(data + pos + 4);
        uint32_t avail = len - pos - 8;
        if (tlen > avail)
            tlen = avail;
        if (memcmp(data + pos, "MTrk", 4) == 0) {
            const uint8_t *start = data + pos + 8;
            if (!parse_track(start, start + tlen, &raw)) {
                free(raw.ev);
                return false;
            }
            found++;
        }
        pos += 8 + tlen;
    }
    qsort(raw.ev, raw.n, sizeof(RawEvent), compare_raw);

    /* Ticks to milliseconds through the tempo map. SMPTE divisions give
     * frames per second and ticks per frame instead. */
    NjaEvent *ev = malloc(sizeof(NjaEvent) * (raw.n ? raw.n : 1));
    if (!ev) {
        free(raw.ev);
        return false;
    }
    uint32_t n = 0;
    uint64_t base_us = 0;
    uint32_t base_tick = 0, tempo = 500000;
    bool     smpte = division & 0x8000;
    uint32_t ticks_per_sec = smpte ? (uint32_t)(256 - (division >> 8)) * (division & 0xFF) : 0;
    uint32_t last_ms = 0;
    for (uint32_t i = 0; i < raw.n; i++) {
        RawEvent *r = &raw.ev[i];
        uint64_t  us = smpte ? (uint64_t)r->tick * 1000000 / (ticks_per_sec ? ticks_per_sec : 1)
                             : base_us + (uint64_t)(r->tick - base_tick) * tempo / division;
        if (r->status == 0xFF) {
            base_us = us;
            base_tick = r->tick;
            tempo = r->tempo ? r->tempo : 500000;
            continue;
        }
        NjaEvent *e = &ev[n++];
        e->ms = (uint32_t)(us / 1000);
        e->status = r->status;
        e->d1 = r->d1;
        e->d2 = r->d2;
        e->pad = 0;
        last_ms = e->ms;
    }
    free(raw.ev);
    out->kind = CLIP_SONG;
    out->ev = ev;
    out->n = n;
    out->length_ms = last_ms + 200;   /* let the last notes ring */
    return true;
}

/* ------------------------------------------------------------------------ */
/* MMAPI tone sequences (javax.microedition.media.control.ToneControl)       */

enum {
    TONE_VERSION = -2, TONE_TEMPO = -3, TONE_RESOLUTION = -4, TONE_BLOCK_START = -5,
    TONE_BLOCK_END = -6, TONE_PLAY_BLOCK = -7, TONE_SET_VOLUME = -8, TONE_REPEAT = -9,
    TONE_SILENCE = -1
};

typedef struct {
    const int8_t *seq;
    uint32_t      len;
    int32_t       blocks[128];   /* start offset of each block, -1 if none */
    uint32_t      tempo, resolution;
    int           volume;
    uint64_t      us;            /* running time */
    NjaEvent     *ev;
    uint32_t      n, cap;
    int           depth;
} ToneState;

static void tone_emit(ToneState *s, uint8_t status, uint8_t d1, uint8_t d2)
{
    if (s->n == s->cap) {
        uint32_t  ncap = s->cap ? s->cap * 2 : 64;
        NjaEvent *nev = realloc(s->ev, sizeof(NjaEvent) * ncap);
        if (!nev)
            return;
        s->ev = nev;
        s->cap = ncap;
    }
    s->ev[s->n++] = (NjaEvent){(uint32_t)(s->us / 1000), status, d1, d2, 0};
}

static void tone_note(ToneState *s, int note, int duration)
{
    /* duration is in 1/resolution of a whole note; tempo is in beats
     * (quarter notes) per minute. */
    uint64_t us = (uint64_t)duration * 4 * 60000000 / ((uint64_t)s->tempo * s->resolution);
    if (note != TONE_SILENCE && note >= 0) {
        int vel = 127 * s->volume / 100;
        tone_emit(s, 0x90, (uint8_t)note, (uint8_t)(vel ? vel : 1));
        s->us += us;
        tone_emit(s, 0x80, (uint8_t)note, 0);
    } else {
        s->us += us;
    }
}

/* Plays bytes [pos, end) of the sequence. */
static void tone_run(ToneState *s, uint32_t pos, uint32_t end)
{
    if (++s->depth > 8)
        return;
    while (pos + 1 < end) {
        int8_t a = s->seq[pos], b = s->seq[pos + 1];
        pos += 2;
        switch (a) {
        case TONE_TEMPO:
            s->tempo = (uint32_t)(b > 0 ? b : 30) * 4;
            break;
        case TONE_RESOLUTION:
            s->resolution = (uint32_t)(b > 0 ? b : 64);
            break;
        case TONE_SET_VOLUME:
            s->volume = b < 0 ? 0 : b > 100 ? 100 : b;
            break;
        case TONE_BLOCK_START:
            /* Blocks are defined at the start and skipped when reached. */
            while (pos + 1 < end && !(s->seq[pos] == TONE_BLOCK_END && s->seq[pos + 1] == b))
                pos += 2;
            pos += 2;
            break;
        case TONE_PLAY_BLOCK:
            if (b >= 0 && s->blocks[b] >= 0) {
                uint32_t bend = (uint32_t)s->blocks[b];
                while (bend + 1 < s->len && !(s->seq[bend] == TONE_BLOCK_END && s->seq[bend + 1] == b))
                    bend += 2;
                tone_run(s, (uint32_t)s->blocks[b], bend);
            }
            break;
        case TONE_REPEAT:
            if (pos + 1 < end) {
                int8_t note = s->seq[pos], dur = s->seq[pos + 1];
                pos += 2;
                for (int i = 0; i < b; i++)
                    tone_note(s, note, dur);
            }
            break;
        case TONE_VERSION:
        case TONE_BLOCK_END:
            break;
        default:
            if (a >= TONE_SILENCE)
                tone_note(s, a, b);
            break;
        }
    }
    s->depth--;
}

static bool decode_tones(const uint8_t *data, uint32_t len, NjaClip *out)
{
    if (len < 2 || (int8_t)data[0] != TONE_VERSION)
        return false;
    ToneState *s = calloc(1, sizeof *s);
    if (!s)
        return false;
    s->seq = (const int8_t *)data;
    s->len = len;
    s->tempo = 120;
    s->resolution = 64;
    s->volume = 100;
    for (int i = 0; i < 128; i++)
        s->blocks[i] = -1;
    for (uint32_t p = 0; p + 1 < len; p += 2)
        if (s->seq[p] == TONE_BLOCK_START && s->seq[p + 1] >= 0)
            s->blocks[s->seq[p + 1]] = (int32_t)(p + 2);
    /* A square lead, like phone buzzers. */
    tone_emit(s, 0xC0, 80, 0);
    tone_run(s, 0, len);
    out->kind = CLIP_SONG;
    out->ev = s->ev;
    out->n = s->n;
    out->length_ms = (uint32_t)(s->us / 1000);
    free(s);
    return out->ev != NULL;
}

/* ------------------------------------------------------------------------ */
/* WAV                                                                      */

static uint16_t le16(const uint8_t *p) { return (uint16_t)(p[0] | (p[1] << 8)); }
static uint32_t le32(const uint8_t *p)
{
    return p[0] | (p[1] << 8) | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

static int16_t ulaw(uint8_t u)
{
    u = (uint8_t)~u;
    int t = ((u & 0x0F) << 3) + 0x84;
    t <<= (u & 0x70) >> 4;
    return (int16_t)((u & 0x80) ? (0x84 - t) : (t - 0x84));
}

static int16_t alaw(uint8_t a)
{
    a ^= 0x55;
    int t = (a & 0x0F) << 4;
    int seg = (a & 0x70) >> 4;
    if (seg == 0)
        t += 8;
    else
        t = (t + 0x108) << (seg - 1);
    return (int16_t)((a & 0x80) ? t : -t);
}

static const int16_t ima_steps[89] = {
    7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60,
    66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371,
    408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878,
    2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845,
    8630, 9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086,
    29794, 32767};
static const int8_t ima_index[16] = {-1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8};

static int16_t ima_step(int *pred, int *index, int nibble)
{
    int step = ima_steps[*index];
    int diff = step >> 3;
    if (nibble & 1) diff += step >> 2;
    if (nibble & 2) diff += step >> 1;
    if (nibble & 4) diff += step;
    *pred += (nibble & 8) ? -diff : diff;
    if (*pred > 32767) *pred = 32767;
    if (*pred < -32768) *pred = -32768;
    *index += ima_index[nibble];
    if (*index < 0) *index = 0;
    if (*index > 88) *index = 88;
    return (int16_t)*pred;
}

/* Decodes mono or stereo IMA ADPCM blocks into mono samples (left channel
 * only for stereo, which phones rarely used). */
static uint32_t decode_ima(const uint8_t *d, uint32_t len, int channels, uint32_t block,
                           int16_t *out, uint32_t max)
{
    uint32_t n = 0;
    for (uint32_t pos = 0; pos + 4u * channels <= len && n < max; pos += block) {
        uint32_t bl = block < len - pos ? block : len - pos;
        const uint8_t *b = d + pos;
        int pred = (int16_t)le16(b);
        int index = b[2] > 88 ? 88 : b[2];
        out[n++] = (int16_t)pred;
        uint32_t i = 4u * channels;
        while (i + 4u * channels <= bl && n < max) {
            /* 4 bytes (8 samples) of the first channel, then the others. */
            for (int k = 0; k < 4 && n + 1 < max; k++) {
                out[n++] = ima_step(&pred, &index, b[i + k] & 15);
                out[n++] = ima_step(&pred, &index, b[i + k] >> 4);
            }
            i += 4u * channels;
        }
    }
    return n;
}

static bool decode_wav(const uint8_t *data, uint32_t len, NjaClip *out)
{
    if (len < 12 || memcmp(data, "RIFF", 4) != 0 || memcmp(data + 8, "WAVE", 4) != 0)
        return false;
    uint16_t fmt = 0, channels = 1, bits = 16, block = 0;
    uint32_t rate = 0;
    const uint8_t *pcm = NULL;
    uint32_t pcm_len = 0;
    for (uint32_t pos = 12; pos + 8 <= len;) {
        uint32_t clen = le32(data + pos + 4);
        const uint8_t *body = data + pos + 8;
        if (clen > len - pos - 8)
            clen = len - pos - 8;
        if (memcmp(data + pos, "fmt ", 4) == 0 && clen >= 16) {
            fmt = le16(body);
            channels = le16(body + 2);
            rate = le32(body + 4);
            block = le16(body + 12);
            bits = le16(body + 14);
        } else if (memcmp(data + pos, "data", 4) == 0) {
            pcm = body;
            pcm_len = clen;
        }
        pos += 8 + clen + (clen & 1);
    }
    if (!pcm || rate < 1000 || rate > 96000 || channels < 1 || channels > 2)
        return false;

    uint32_t max;
    switch (fmt) {
    case 1:  max = pcm_len / (bits / 8 ? bits / 8 : 1) / channels; break;
    case 6:
    case 7:  max = pcm_len / channels; break;
    case 0x11: max = pcm_len * 2 / channels + 2; break;
    default: return false;
    }
    int16_t *s = malloc(sizeof(int16_t) * (max ? max : 1));
    if (!s)
        return false;
    uint32_t n = 0;
    if (fmt == 1 && bits == 8) {
        for (; n < max; n++)
            s[n] = (int16_t)((pcm[n * channels] - 128) << 8);
    } else if (fmt == 1 && bits == 16) {
        for (; n < max; n++)
            s[n] = (int16_t)le16(pcm + n * 2 * channels);
    } else if (fmt == 7) {
        for (; n < max; n++)
            s[n] = ulaw(pcm[n * channels]);
    } else if (fmt == 6) {
        for (; n < max; n++)
            s[n] = alaw(pcm[n * channels]);
    } else if (fmt == 0x11 && block) {
        n = decode_ima(pcm, pcm_len, channels, block, s, max);
    } else {
        free(s);
        return false;
    }
    out->kind = CLIP_PCM;
    out->pcm = s;
    out->samples = n;
    out->rate = rate;
    out->length_ms = (uint32_t)((uint64_t)n * 1000 / rate);
    return n > 0;
}

/* ------------------------------------------------------------------------ */

bool media_decode(const uint8_t *data, uint32_t len, const char *type, NjaClip *out)
{
    memset(out, 0, sizeof *out);
    if (len >= 4 && memcmp(data, "MThd", 4) == 0)
        return decode_midi(data, len, out);
    if (len >= 12 && memcmp(data, "RIFF", 4) == 0)
        return decode_wav(data, len, out);
    if (type && strstr(type, "tone"))
        return decode_tones(data, len, out);
    if (len >= 2 && (int8_t)data[0] == TONE_VERSION)
        return decode_tones(data, len, out);
    return false;
}

void media_free(NjaClip *clip)
{
    free(clip->ev);
    free(clip->pcm);
    memset(clip, 0, sizeof *clip);
}
