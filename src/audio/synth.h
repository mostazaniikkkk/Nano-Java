/*
 * synth.h - MIDI sequencer and synthesizer driving DS-style sound channels.
 *
 * The synthesizer does not produce samples itself: it plays notes on 16
 * hardware channels (looping single-cycle waveforms, PSG noise, PCM) and
 * updates their volume and pitch as envelopes and MIDI controllers change.
 * On the DS it runs on the ARM7 against the real sound hardware; the host
 * build emulates the channels in software (platform/host/audio_host.c).
 *
 * Channel use: 0-1 PCM clips, 2-13 melodic voices, 14-15 noise (drums).
 */
#ifndef NJ_SYNTH_H
#define NJ_SYNTH_H

#include <stdbool.h>
#include <stdint.h>

#define SYNTH_SLOTS    6     /* players that can sound at the same time */
#define SYNTH_CHANNELS 16
#define SYNTH_WAVE_LEN 32    /* samples per single-cycle waveform */

/* A MIDI channel message at an absolute time. Meta events and tempo are
 * resolved when the file is converted (src/audio/midi.c). */
typedef struct NjaEvent {
    uint32_t ms;
    uint8_t  status, d1, d2, pad;
} NjaEvent;

/* The sound hardware, as seen by the synthesizer. vol and pan are 0-127,
 * rates are in samples per second. */
typedef struct SynthHw {
    void (*wave)(int ch, const int8_t *data, int len, uint32_t rate, int vol, int pan);
    void (*noise)(int ch, uint32_t rate, int vol, int pan);
    void (*pcm16)(int ch, const int16_t *data, uint32_t samples, uint32_t rate, int vol, int pan);
    void (*volume)(int ch, int vol, int pan);
    void (*rate)(int ch, uint32_t rate);
    void (*stop)(int ch);
    bool (*active)(int ch);
} SynthHw;

void synth_init(const SynthHw *hw);

/* Starts a sequence on a slot, from start_ms into it. volume is 0-100. */
void synth_song(int slot, const NjaEvent *ev, uint32_t n, uint32_t start_ms, int volume);
/* Starts PCM (16-bit mono) on a slot, from sample start. */
void synth_pcm(int slot, const int16_t *pcm, uint32_t samples, uint32_t rate, uint32_t start,
               int volume);
void synth_stop(int slot);
void synth_volume(int slot, int volume);
void synth_master(int volume);

/* Advances sequences and envelopes; call every few milliseconds. */
void synth_tick(uint32_t elapsed_ms);

bool synth_playing(int slot);

#endif
