/*
 * main.c - Nano Java's ARM7 program: calico's standard services (input,
 * touch, RTC, power, SD card, sound power) plus the synthesizer, which
 * plays the VM's music and sound effects on the DS sound channels.
 *
 * The ARM9 hands over the address of a command ring (src/audio/
 * nja_shared.h) through PXI; from then on a thread runs the synthesizer at
 * 250 Hz and executes queued commands on each tick.
 */
#include <calico.h>
#include <nds.h>

#include "../../../../src/audio/nja_shared.h"
#include "../../../../src/audio/synth.h"

#define TICK_HZ 250
#define TICK_MS (1000 / TICK_HZ)

/* ------------------------------------------------------------------------ */
/* Sound channels                                                           */

static void hw_wave(int ch, const int8_t *data, int len, uint32_t rate, int vol, int pan)
{
    soundChStop(ch);
    soundChPreparePcm(ch, vol, SoundVolDiv_1, pan, soundTimerFromHz(rate), SoundMode_Repeat,
                      SoundFmt_Pcm8, data, 0, len / 4);
    soundChStart(ch);
}

static void hw_noise(int ch, uint32_t rate, int vol, int pan)
{
    soundChStop(ch);
    soundChPreparePsg(ch, vol, SoundVolDiv_1, pan, soundTimerFromHz(rate), SoundDuty_50);
    soundChStart(ch);
}

static void hw_pcm16(int ch, const int16_t *data, uint32_t samples, uint32_t rate, int vol,
                     int pan)
{
    soundChStop(ch);
    soundChPreparePcm(ch, vol, SoundVolDiv_1, pan, soundTimerFromHz(rate), SoundMode_OneShot,
                      SoundFmt_Pcm16, data, 0, samples / 2);
    soundChStart(ch);
}

static void hw_volume(int ch, int vol, int pan)
{
    soundChSetVolume(ch, vol, SoundVolDiv_1);
    soundChSetPan(ch, pan);
}

static void hw_rate(int ch, uint32_t rate)
{
    soundChSetTimer(ch, soundTimerFromHz(rate));
}

static void hw_stop(int ch)
{
    soundChStop(ch);
}

static bool hw_active(int ch)
{
    return soundChIsActive(ch);
}

static const SynthHw ds_hw = {
    hw_wave, hw_noise, hw_pcm16, hw_volume, hw_rate, hw_stop, hw_active
};

/* ------------------------------------------------------------------------ */
/* Command ring                                                             */

static Mailbox  ring_mailbox;
static u32      ring_mailbox_slots[2];
static Thread   synth_thread;
static u8       synth_stack[2048] __attribute__((aligned(8)));

static void run_command(const NjaCmd *c)
{
    switch (c->op) {
    case NJA_SONG:
        synth_song((int)c->slot, (const NjaEvent *)c->ptr, c->n, c->start, (int)c->volume);
        break;
    case NJA_PCM:
        synth_pcm((int)c->slot, (const int16_t *)c->ptr, c->n, c->rate, c->start, (int)c->volume);
        break;
    case NJA_STOP:
        synth_stop((int)c->slot);
        break;
    case NJA_VOLUME:
        synth_volume((int)c->slot, (int)c->volume);
        break;
    case NJA_MASTER:
        synth_master((int)c->volume);
        break;
    }
}

static int synth_main(void *arg)
{
    (void)arg;
    /* The ARM9 sends the ring's offset in main RAM. */
    NjaShared *ring = (NjaShared *)(0x02000000 + mailboxRecv(&ring_mailbox));
    synth_init(&ds_hw);

    TickTask task;
    threadTimerStart(&task, TICK_HZ);
    uint32_t read = ring->read;
    for (;;) {
        threadTimerWait(&task);
        while (read != ring->write) {
            run_command(&ring->cmds[read % NJA_RING]);
            read++;
            ring->read = read;
        }
        synth_tick(TICK_MS);
    }
    return 0;
}

int main(void)
{
    envReadNvramSettings();
    keypadStartExtServer();
    lcdSetIrqMask(DISPSTAT_IE_ALL, DISPSTAT_IE_VBLANK);
    irqEnable(IRQ_VBLANK);
    rtcInit();
    rtcSyncTime();
    pmInit();
    blkInit();
    touchInit();
    touchStartServer(80, MAIN_THREAD_PRIO);
    /* Powers the sound hardware on; the synthesizer drives the channels
     * directly. */
    soundStartServer(MAIN_THREAD_PRIO - 0x10);

    mailboxPrepare(&ring_mailbox, ring_mailbox_slots, 2);
    pxiSetMailbox(PxiChannel_User0, &ring_mailbox);
    threadPrepare(&synth_thread, synth_main, NULL, &synth_stack[sizeof synth_stack],
                  MAIN_THREAD_PRIO - 0x20);
    threadStart(&synth_thread);

    while (pmMainLoop())
        threadWaitForVBlank();
    return 0;
}
