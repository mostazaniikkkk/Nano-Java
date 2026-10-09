/*
 * audio_host.c - audio for the host build: the synthesizer drives a
 * software model of the DS's 16 sound channels, mixed at 32768 Hz. With
 * --audio-out the mix is written to a WAV file (useful to check the
 * synthesizer without a DS).
 */
#include "host.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MIX_RATE 32768
#define TICK_MS  4

enum { CH_OFF, CH_WAVE, CH_NOISE, CH_PCM };

typedef struct {
    int            kind;
    const int8_t  *wave;
    const int16_t *pcm;
    uint32_t       len;       /* samples */
    uint64_t       pos;       /* 32.32 fixed point sample position */
    uint64_t       step;
    int            vol, pan;
    uint16_t       lfsr;
    int            noise_out;
} Channel;

static Channel  channels[SYNTH_CHANNELS];
static bool     ready;
static int64_t  last_ms;
static FILE    *out;
static uint32_t out_frames;

static uint64_t step_for(uint32_t rate)
{
    return ((uint64_t)rate << 32) / MIX_RATE;
}

static void hw_wave(int ch, const int8_t *data, int len, uint32_t rate, int vol, int pan)
{
    Channel *c = &channels[ch];
    c->kind = CH_WAVE;
    c->wave = data;
    c->len = (uint32_t)len;
    c->pos = 0;
    c->step = step_for(rate);
    c->vol = vol;
    c->pan = pan;
}

static void hw_noise(int ch, uint32_t rate, int vol, int pan)
{
    Channel *c = &channels[ch];
    c->kind = CH_NOISE;
    c->pos = 0;
    c->step = step_for(rate);
    c->vol = vol;
    c->pan = pan;
    c->lfsr = 0x7FFF;
}

static void hw_pcm16(int ch, const int16_t *data, uint32_t samples, uint32_t rate, int vol, int pan)
{
    Channel *c = &channels[ch];
    c->kind = CH_PCM;
    c->pcm = data;
    c->len = samples;
    c->pos = 0;
    c->step = step_for(rate);
    c->vol = vol;
    c->pan = pan;
}

static void hw_volume(int ch, int vol, int pan)
{
    channels[ch].vol = vol;
    channels[ch].pan = pan;
}

static void hw_rate(int ch, uint32_t rate)
{
    channels[ch].step = step_for(rate);
}

static void hw_stop(int ch)
{
    channels[ch].kind = CH_OFF;
}

static bool hw_active(int ch)
{
    return channels[ch].kind != CH_OFF;
}

static const SynthHw host_hw = {
    hw_wave, hw_noise, hw_pcm16, hw_volume, hw_rate, hw_stop, hw_active
};

static int next_sample(Channel *c)
{
    int s = 0;
    switch (c->kind) {
    case CH_WAVE:
        s = c->wave[(c->pos >> 32) % c->len] * 256;
        break;
    case CH_PCM:
        if ((c->pos >> 32) >= c->len) {
            c->kind = CH_OFF;
            return 0;
        }
        s = c->pcm[c->pos >> 32];
        break;
    case CH_NOISE: {
        /* The DS's 15-bit LFSR noise, clocked at the channel rate. */
        uint64_t before = c->pos >> 32;
        uint64_t after = (c->pos + c->step) >> 32;
        for (uint64_t i = before; i < after; i++) {
            int bit = c->lfsr & 1;
            c->lfsr >>= 1;
            if (bit)
                c->lfsr ^= 0x6000;
            c->noise_out = bit ? -0x7FFF : 0x7FFF;
        }
        s = c->noise_out;
        break;
    }
    default:
        return 0;
    }
    c->pos += c->step;
    return s;
}

static void mix(uint32_t frames)
{
    for (uint32_t f = 0; f < frames; f++) {
        int32_t l = 0, r = 0;
        for (int i = 0; i < SYNTH_CHANNELS; i++) {
            Channel *c = &channels[i];
            if (c->kind == CH_OFF)
                continue;
            int32_t s = next_sample(c) * c->vol / 127;
            l += s * (127 - c->pan) / 127;
            r += s * c->pan / 127;
        }
        /* Same headroom as the DS mixer: 16 full channels would clip. */
        l /= 4;
        r /= 4;
        int16_t frame[2] = {
            (int16_t)(l > 32767 ? 32767 : l < -32768 ? -32768 : l),
            (int16_t)(r > 32767 ? 32767 : r < -32768 ? -32768 : r),
        };
        if (out) {
            fwrite(frame, sizeof frame, 1, out);
            out_frames++;
        }
    }
}

static void ensure_ready(void)
{
    if (!ready) {
        synth_init(&host_hw);
        last_ms = pal_time_ms();
        ready = true;
    }
}

void host_audio_update(void)
{
    ensure_ready();
    int64_t now = pal_time_ms();
    while (now - last_ms >= TICK_MS) {
        synth_tick(TICK_MS);
        mix(MIX_RATE * TICK_MS / 1000);
        last_ms += TICK_MS;
    }
}

static void write_header(void)
{
    uint32_t data = out_frames * 4;
    uint8_t  h[44];
    memcpy(h, "RIFF", 4);
    uint32_t v = 36 + data;
    memcpy(h + 4, &v, 4);
    memcpy(h + 8, "WAVEfmt ", 8);
    v = 16;
    memcpy(h + 16, &v, 4);
    uint16_t fmt[2] = {1, 2};
    memcpy(h + 20, fmt, 4);
    v = MIX_RATE;
    memcpy(h + 24, &v, 4);
    v = MIX_RATE * 4;
    memcpy(h + 28, &v, 4);
    uint16_t align[2] = {4, 16};
    memcpy(h + 32, align, 4);
    memcpy(h + 36, "data", 4);
    memcpy(h + 40, &data, 4);
    fseek(out, 0, SEEK_SET);
    fwrite(h, 1, sizeof h, out);
}

bool host_audio_open(const char *path)
{
    out = fopen(path, "wb");
    if (!out)
        return false;
    write_header();
    return true;
}

void host_audio_close(void)
{
    if (out) {
        write_header();
        fclose(out);
        out = NULL;
    }
}

/* ------------------------------------------------------------------------ */
/* pal.h                                                                    */

bool pal_audio_available(void)
{
    return true;
}

void pal_audio_song(int slot, const NjaEvent *ev, uint32_t n, uint32_t start_ms, int volume)
{
    ensure_ready();
    synth_song(slot, ev, n, start_ms, volume);
}

void pal_audio_pcm(int slot, const int16_t *pcm, uint32_t samples, uint32_t rate,
                   uint32_t start, int volume)
{
    ensure_ready();
    synth_pcm(slot, pcm, samples, rate, start, volume);
}

void pal_audio_stop(int slot)
{
    ensure_ready();
    synth_stop(slot);
}

void pal_audio_volume(int slot, int volume)
{
    ensure_ready();
    synth_volume(slot, volume);
}
