/*
 * nja_shared.h - the command ring between the DS's ARM9 (the VM) and ARM7
 * (the synthesizer). It lives in main RAM: the ARM9 writes commands and
 * flushes them from its data cache; the ARM7, which has no cache, polls the
 * ring on every synthesizer tick. Each index sits in its own cache line.
 */
#ifndef NJ_NJA_SHARED_H
#define NJ_NJA_SHARED_H

#include <stdint.h>

#define NJA_RING 32

enum { NJA_SONG = 1, NJA_PCM, NJA_STOP, NJA_VOLUME, NJA_MASTER };

typedef struct NjaCmd {
    uint32_t op, slot;
    uint32_t ptr;        /* events or PCM samples */
    uint32_t n;          /* event or sample count */
    uint32_t start;      /* ms (songs) or sample (PCM) */
    uint32_t volume;
    uint32_t rate;       /* PCM sample rate */
    uint32_t pad;
} NjaCmd;                /* 32 bytes: one cache line */

typedef struct NjaShared {
    volatile uint32_t write;     /* written by the ARM9 */
    uint32_t          pad0[7];
    volatile uint32_t read;      /* written by the ARM7 */
    uint32_t          pad1[7];
    NjaCmd            cmds[NJA_RING];
} NjaShared;

#endif
