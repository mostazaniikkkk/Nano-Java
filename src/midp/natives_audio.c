/*
 * natives_audio.c - nanojava.Audio: decoded clips and the playback slots
 * they use. Java (javax.microedition.media) keeps track of timing, looping
 * and events; this side only starts, stops and mixes.
 */
#include "midp.h"
#include "../vm/vm.h"
#include "../audio/media.h"

#include <stdlib.h>

#define MAX_CLIPS 48

typedef struct {
    bool    used;
    NjaClip clip;
    int     slot;       /* playback slot while started, else -1 */
    int     volume;
} Clip;

static Clip     clips[MAX_CLIPS + 1];    /* handles are 1..MAX_CLIPS */
static int      slot_owner[SYNTH_SLOTS]; /* handle using each slot, 0 if none */
static uint32_t slot_started[SYNTH_SLOTS];
static uint32_t start_counter;

/* MMAPI's Manager.playTone needs no clip: each slot has room for a note. */
static NjaEvent tone_events[SYNTH_SLOTS][3];

static Clip *clip_of(int h)
{
    return h >= 1 && h <= MAX_CLIPS && clips[h].used ? &clips[h] : NULL;
}

static void release_slot(int slot)
{
    pal_audio_stop(slot);
    int h = slot_owner[slot];
    if (h > 0 && clips[h].slot == slot)
        clips[h].slot = -1;
    slot_owner[slot] = 0;
}

/* A free slot, or the one started longest ago. */
static int grab_slot(void)
{
    int best = 0;
    for (int s = 0; s < SYNTH_SLOTS; s++) {
        if (slot_owner[s] == 0)
            return s;
        if (slot_started[s] < slot_started[best])
            best = s;
    }
    release_slot(best);
    return best;
}

/* ------------------------------------------------------------------------ */

static int Audio_available(Thread *t, Slot *args, Slot *ret)
{
    ret->i = pal_audio_available();
    return NATIVE_OK;
}

/* static int open(byte[] data, String contentType) */
static int Audio_open(Thread *t, Slot *args, Slot *ret)
{
    Object *data = args[0].ref;
    char    type[64] = "";
    if (!data)
        return NATIVE_OK;
    if (args[1].ref)
        str_to_utf8(args[1].ref, type, sizeof type);
    int h = 1;
    while (h <= MAX_CLIPS && clips[h].used)
        h++;
    if (h > MAX_CLIPS)
        return NATIVE_OK;
    Clip *c = &clips[h];
    bool ok = media_decode(ARRAY_DATA(data, uint8_t), (uint32_t)ARRAY_LEN(data), type, &c->clip);
    if (vm.opt.trace_audio)
        nj_log("[audio open %s, %d bytes: %s]", type, (int)ARRAY_LEN(data),
               ok ? (c->clip.kind == CLIP_SONG ? "song" : "pcm") : "unsupported");
    if (!ok)
        return NATIVE_OK;
    c->used = true;
    c->slot = -1;
    c->volume = 100;
    ret->i = h;
    return NATIVE_OK;
}

/* static long duration(int handle): microseconds */
static int Audio_duration(Thread *t, Slot *args, Slot *ret)
{
    Clip *c = clip_of(args[0].i);
    slot_set_long(ret, c ? (jlong)c->clip.length_ms * 1000 : -1);
    return NATIVE_OK;
}

/* static void start(int handle, long mediaTime) */
static int Audio_start(Thread *t, Slot *args, Slot *ret)
{
    int   h = args[0].i;
    Clip *c = clip_of(h);
    if (!c)
        return NATIVE_OK;
    jlong    us = slot_get_long(&args[1]);
    uint32_t ms = us > 0 ? (uint32_t)(us / 1000) : 0;
    int      slot = c->slot >= 0 ? c->slot : grab_slot();
    if (vm.opt.trace_audio)
        nj_log("[audio start %d at %u ms on slot %d]", h, (unsigned)ms, slot);
    c->slot = slot;
    slot_owner[slot] = h;
    slot_started[slot] = ++start_counter;
    if (c->clip.kind == CLIP_SONG) {
        pal_audio_song(slot, c->clip.ev, c->clip.n, ms, c->volume);
    } else {
        uint32_t start = (uint32_t)((uint64_t)ms * c->clip.rate / 1000);
        pal_audio_pcm(slot, c->clip.pcm, c->clip.samples, c->clip.rate, start, c->volume);
    }
    return NATIVE_OK;
}

static int Audio_stop(Thread *t, Slot *args, Slot *ret)
{
    Clip *c = clip_of(args[0].i);
    if (c && c->slot >= 0)
        release_slot(c->slot);
    return NATIVE_OK;
}

/* static void setVolume(int handle, int level) */
static int Audio_setVolume(Thread *t, Slot *args, Slot *ret)
{
    Clip *c = clip_of(args[0].i);
    if (!c)
        return NATIVE_OK;
    int v = args[1].i;
    c->volume = v < 0 ? 0 : v > 100 ? 100 : v;
    if (c->slot >= 0)
        pal_audio_volume(c->slot, c->volume);
    return NATIVE_OK;
}

static int Audio_close(Thread *t, Slot *args, Slot *ret)
{
    Clip *c = clip_of(args[0].i);
    if (!c)
        return NATIVE_OK;
    if (c->slot >= 0)
        release_slot(c->slot);
    media_free(&c->clip);
    c->used = false;
    return NATIVE_OK;
}

/* static void playTone(int note, int durationMs, int volume) */
static int Audio_playTone(Thread *t, Slot *args, Slot *ret)
{
    int note = args[0].i, ms = args[1].i, vol = args[2].i;
    if (note < 0 || note > 127 || ms <= 0)
        return NATIVE_OK;
    int slot = grab_slot();
    slot_owner[slot] = -1;   /* a tone, not a clip */
    slot_started[slot] = ++start_counter;
    NjaEvent *e = tone_events[slot];
    e[0] = (NjaEvent){0, 0xC0, 80, 0, 0};
    e[1] = (NjaEvent){0, 0x90, (uint8_t)note, 127, 0};
    e[2] = (NjaEvent){(uint32_t)ms, 0x80, (uint8_t)note, 0, 0};
    pal_audio_song(slot, e, 3, 0, vol < 0 ? 0 : vol > 100 ? 100 : vol);
    return NATIVE_OK;
}

static const NativeEntry audio_natives[] = {
    {"nanojava/Audio", "available", NULL, Audio_available},
    {"nanojava/Audio", "open", NULL, Audio_open},
    {"nanojava/Audio", "duration", NULL, Audio_duration},
    {"nanojava/Audio", "start", NULL, Audio_start},
    {"nanojava/Audio", "stop", NULL, Audio_stop},
    {"nanojava/Audio", "setVolume", NULL, Audio_setVolume},
    {"nanojava/Audio", "close", NULL, Audio_close},
    {"nanojava/Audio", "playTone", NULL, Audio_playTone},
    {NULL, NULL, NULL, NULL}
};

void midp_audio_init(void)
{
    native_register(audio_natives);
}

/* Stops everything and frees all clips (the VM is going away). */
void midp_audio_shutdown(void)
{
    for (int s = 0; s < SYNTH_SLOTS; s++)
        if (slot_owner[s])
            release_slot(s);
    for (int h = 1; h <= MAX_CLIPS; h++) {
        if (clips[h].used) {
            media_free(&clips[h].clip);
            clips[h].used = false;
        }
    }
}
