/*
 * Nano Java - platform abstraction layer.
 *
 * Everything the portable runtime needs from the machine it runs on. Each
 * platform (platform/host, platform/nds) implements these functions.
 */
#ifndef NJ_PAL_H
#define NJ_PAL_H

#include <stdint.h>
#include <stdbool.h>

#include "../audio/synth.h"

/* Time */
int64_t pal_time_ms(void);           /* monotonic milliseconds */
int64_t pal_epoch_ms(void);          /* wall clock, ms since 1970-01-01 UTC */

/* Console output (System.out / System.err / VM log) */
void pal_console_write(const char *s, int len);

/* Input events, already translated to MIDP key codes by the platform. */
enum {
    EV_NONE, EV_KEY_DOWN, EV_KEY_UP, EV_POINTER_DOWN, EV_POINTER_UP,
    EV_POINTER_DRAG, EV_QUIT
};

typedef struct NjEvent {
    int type;
    int a, b;          /* key code, or pointer x/y in canvas coordinates */
} NjEvent;

bool pal_poll_event(NjEvent *ev);

/* Block until the given monotonic time or until input may be available.
 * until_ms < 0 means "no deadline". */
void pal_wait(int64_t until_ms);

/* Display. Pixels are 15-bit RGB in the DS bitmap layout: bits 0-4 red,
 * 5-9 green, 10-14 blue, bit 15 set. */
void pal_screen_size(int *w, int *h);
void pal_present(const uint16_t *px, int w, int h);

/* Labels of the commands on the soft keys (NULL = none), for platforms
 * that show the soft keys outside the canvas. */
void pal_soft_labels(const char *left, const char *right);

/* Audio: SYNTH_SLOTS independent players. The data passed in stays valid
 * until pal_audio_stop() returns for that slot. Volumes are 0-100. */
bool pal_audio_available(void);
void pal_audio_song(int slot, const NjaEvent *ev, uint32_t n, uint32_t start_ms, int volume);
void pal_audio_pcm(int slot, const int16_t *pcm, uint32_t samples, uint32_t rate,
                   uint32_t start, int volume);
void pal_audio_stop(int slot);
void pal_audio_volume(int slot, int volume);

/* Directory where record stores and settings are kept (no trailing '/'),
 * or NULL when there is no writable storage. */
const char *pal_data_dir(void);

/* Reports an unrecoverable error to the user and stops. */
void pal_fatal(const char *msg)
#if defined(__GNUC__)
    __attribute__((noreturn))
#endif
    ;

#endif
