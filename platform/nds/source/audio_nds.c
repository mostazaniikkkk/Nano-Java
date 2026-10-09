/*
 * audio_nds.c - pal_audio_* on the DS: commands go to the synthesizer on
 * the ARM7 through the ring in src/audio/nja_shared.h. Data handed over
 * (event lists, PCM) is flushed from the ARM9 cache first, since the ARM7
 * and the sound hardware read main RAM directly.
 */
#include <nds.h>

#include "nds_platform.h"
#include "../../../src/pal/pal.h"
#include "../../../src/audio/nja_shared.h"

static NjaShared ring __attribute__((aligned(32)));
static bool      connected;
static int       master_volume = 100;

static void connect(void)
{
    if (connected)
        return;
    pxiWaitRemote(PxiChannel_User0);
    pxiSend(PxiChannel_User0, (u32)&ring - 0x02000000);
    connected = true;
}

static uint32_t remote_read(void)
{
    DC_InvalidateRange((const void *)&ring.read, 32);
    return ring.read;
}

/* Queues a command; returns its sequence number. */
static uint32_t send(const NjaCmd *c)
{
    connect();
    while (ring.write - remote_read() >= NJA_RING)
        ;
    NjaCmd *slot = &ring.cmds[ring.write % NJA_RING];
    *slot = *c;
    DC_FlushRange(slot, sizeof *slot);
    uint32_t seq = ring.write++;
    DC_FlushRange((const void *)&ring.write, 32);
    return seq;
}

/* Waits until the ARM7 has run command seq. */
static void wait_done(uint32_t seq)
{
    while ((int32_t)(remote_read() - seq) <= 0)
        ;
}

void nds_audio_master(int volume)
{
    master_volume = volume;
    NjaCmd c = {NJA_MASTER, 0, 0, 0, 0, (uint32_t)volume, 0, 0};
    send(&c);
}

bool pal_audio_available(void)
{
    return true;
}

void pal_audio_song(int slot, const NjaEvent *ev, uint32_t n, uint32_t start_ms, int volume)
{
    DC_FlushRange(ev, sizeof(NjaEvent) * n);
    NjaCmd c = {NJA_SONG, (uint32_t)slot, (uint32_t)ev, n, start_ms, (uint32_t)volume, 0, 0};
    send(&c);
}

void pal_audio_pcm(int slot, const int16_t *pcm, uint32_t samples, uint32_t rate,
                   uint32_t start, int volume)
{
    DC_FlushRange(pcm, sizeof(int16_t) * samples);
    NjaCmd c = {NJA_PCM, (uint32_t)slot, (uint32_t)pcm, samples, start, (uint32_t)volume, rate, 0};
    send(&c);
}

void pal_audio_stop(int slot)
{
    NjaCmd c = {NJA_STOP, (uint32_t)slot, 0, 0, 0, 0, 0, 0};
    /* The caller may free the slot's data next: wait for the ARM7. */
    wait_done(send(&c));
}

void pal_audio_volume(int slot, int volume)
{
    NjaCmd c = {NJA_VOLUME, (uint32_t)slot, 0, 0, 0, (uint32_t)volume, 0, 0};
    send(&c);
}
