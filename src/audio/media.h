/*
 * media.h - converts the media formats J2ME games ship (MIDI, WAV, MMAPI
 * tone sequences) into what the synthesizer plays.
 */
#ifndef NJ_MEDIA_H
#define NJ_MEDIA_H

#include <stdbool.h>
#include <stdint.h>

#include "synth.h"

enum { CLIP_SONG, CLIP_PCM };

typedef struct NjaClip {
    int       kind;
    NjaEvent *ev;          /* CLIP_SONG */
    uint32_t  n;
    int16_t  *pcm;         /* CLIP_PCM: 16-bit mono */
    uint32_t  samples;
    uint32_t  rate;
    uint32_t  length_ms;
} NjaClip;

/* Decodes data (format detected from its contents, or from type for tone
 * sequences). Returns false for unsupported or broken media. */
bool media_decode(const uint8_t *data, uint32_t len, const char *type, NjaClip *out);
void media_free(NjaClip *clip);

#endif
