/*
 * synth.c - MIDI sequencer and synthesizer; see synth.h.
 *
 * General MIDI programs are grouped into 16 families of 8, each with a
 * waveform and an attack/decay/sustain/release envelope. Drums (MIDI
 * channel 10) are noise bursts or short sine thumps. Everything uses
 * integer arithmetic: on the DS this runs on the ARM7, which has no FPU.
 */
#include "synth.h"

#include <string.h>

#include "waves.h"

enum { STAGE_OFF, STAGE_ATTACK, STAGE_DECAY, STAGE_SUSTAIN, STAGE_RELEASE };

enum { WAVE_SINE, WAVE_PIANO, WAVE_ORGAN, WAVE_SAW, WAVE_SQUARE, WAVE_STRINGS, WAVE_BRASS,
       WAVE_TRIANGLE };

#define FIRST_PCM    0
#define LAST_PCM     1
#define FIRST_VOICE  2
#define LAST_VOICE   13
#define FIRST_NOISE  14
#define LAST_NOISE   15
#define ENV_MAX      (255 << 8)

typedef struct {
    uint8_t  wave;
    uint16_t attack, decay;   /* ms */
    uint8_t  sustain;         /* 0-255 */
    uint16_t release;         /* ms */
} Timbre;

/* One per family of 8 General MIDI programs. */
static const Timbre timbres[16] = {
    {WAVE_PIANO,    2, 1600,   0, 200},   /* piano */
    {WAVE_SINE,     1,  700,   0, 200},   /* chromatic percussion */
    {WAVE_ORGAN,    5,    0, 255,  60},   /* organ */
    {WAVE_SAW,      2,  900,   0, 150},   /* guitar */
    {WAVE_TRIANGLE, 3,  700,  90,  80},   /* bass */
    {WAVE_STRINGS, 60,    0, 255, 250},   /* strings */
    {WAVE_STRINGS, 80,    0, 255, 300},   /* ensemble */
    {WAVE_BRASS,   20,  200, 200, 120},   /* brass */
    {WAVE_SQUARE,  15,    0, 220, 100},   /* reed */
    {WAVE_SINE,    30,    0, 230, 120},   /* pipe */
    {WAVE_SQUARE,   3,    0, 230,  80},   /* synth lead */
    {WAVE_SAW,    150,    0, 255, 400},   /* synth pad */
    {WAVE_SAW,     50,  800, 120, 300},   /* synth effects */
    {WAVE_SAW,      2,  700,   0, 150},   /* ethnic */
    {WAVE_SINE,     1,  300,   0, 100},   /* percussive */
    {WAVE_SQUARE,   5,  300,   0, 100},   /* sound effects */
};

typedef struct {
    uint8_t program;
    uint8_t volume;       /* CC 7 */
    uint8_t expression;   /* CC 11 */
    uint8_t pan;          /* CC 10 */
    bool    sustain;      /* CC 64 */
    int16_t bend;         /* -8192..8191 */
} MidiChannel;

typedef struct {
    bool            playing;
    bool            pcm;
    const NjaEvent *ev;
    uint32_t        n, idx;
    uint32_t        now;          /* ms into the song or clip */
    uint32_t        length;       /* PCM length in ms */
    int             volume;       /* 0-100 */
    MidiChannel     ch[16];
} Slot;

typedef struct {
    int8_t   slot;        /* -1: free */
    uint8_t  midi_ch, note, vel;
    uint8_t  stage;
    bool     held_by_pedal;
    bool     noise;
    const Timbre *timbre;
    uint16_t attack, decay, release;
    uint8_t  sustain;
    int32_t  env;         /* 0..ENV_MAX */
    uint32_t age;
} Voice;

static const SynthHw *hw;
static Slot           slots[SYNTH_SLOTS];
static Voice          voices[SYNTH_CHANNELS];
static int            master = 100;
static uint32_t       now_ms;

/* 2^(i/12) for i = 0..12, in 1.15 fixed point. */
static const uint16_t semitone[13] = {
    32768, 34716, 36781, 38968, 41285, 43740, 46341, 49097, 52016, 55109, 58386, 61858, 65535
};

/* Playback rate for a pitch in 1/64 semitones (MIDI note * 64): the rate at
 * which a SYNTH_WAVE_LEN sample cycle plays at the note's frequency. */
static uint32_t pitch_rate(int32_t p)
{
    if (p < 0)
        p = 0;
    int note = p >> 6, frac = p & 63;
    int octave = note / 12, i = note % 12;
    uint32_t r = semitone[i] + (uint32_t)(semitone[i + 1] - semitone[i]) * frac / 64;
    /* C-1 (MIDI note 0) is 8.1758 Hz: times 32 samples = 261.6256 Hz,
     * kept in 16.16 fixed point. */
    uint64_t rate = (uint64_t)17146064u * r >> 15;
    return (uint32_t)((rate << octave) >> 16);
}

static void reset_channels(Slot *s)
{
    for (int c = 0; c < 16; c++) {
        s->ch[c].program = 0;
        s->ch[c].volume = 100;
        s->ch[c].expression = 127;
        s->ch[c].pan = 64;
        s->ch[c].sustain = false;
        s->ch[c].bend = 0;
    }
}

void synth_init(const SynthHw *h)
{
    hw = h;
    memset(slots, 0, sizeof slots);
    for (int i = 0; i < SYNTH_CHANNELS; i++) {
        voices[i].slot = -1;
        voices[i].stage = STAGE_OFF;
    }
}

/* ------------------------------------------------------------------------ */
/* Voices                                                                   */

static int voice_volume(const Voice *v)
{
    const Slot        *s = &slots[v->slot];
    const MidiChannel *c = &s->ch[v->midi_ch];
    uint32_t vol = (uint32_t)(v->env >> 8);                  /* 0-255 */
    vol = vol * v->vel / 127;
    vol = vol * c->volume / 127;
    vol = vol * c->expression / 127;
    vol = vol * (uint32_t)s->volume / 100;
    vol = vol * (uint32_t)master / 100;
    return (int)(vol >> 1);                                  /* 0-127 */
}

static void voice_free(int i)
{
    Voice *v = &voices[i];
    if (v->stage != STAGE_OFF)
        hw->stop(i);
    v->stage = STAGE_OFF;
    v->slot = -1;
}

/* Picks a channel in [first, last]: a free one, else one releasing, else
 * the oldest. */
static int voice_alloc(int first, int last)
{
    int best = first;
    uint32_t best_score = 0;
    for (int i = first; i <= last; i++) {
        Voice *v = &voices[i];
        if (v->stage == STAGE_OFF || !hw->active(i))
            return i;
        uint32_t score = (now_ms - v->age) + (v->stage == STAGE_RELEASE ? 0x40000000u : 0);
        if (score > best_score) {
            best_score = score;
            best = i;
        }
    }
    voice_free(best);
    return best;
}

static uint32_t voice_rate(const Voice *v)
{
    const MidiChannel *c = &slots[v->slot].ch[v->midi_ch];
    /* Pitch bend range: 2 semitones. */
    return pitch_rate(v->note * 64 + c->bend * 128 / 8192);
}

typedef struct {
    bool     noise;
    uint8_t  pitch;      /* MIDI note for tonal drums, or noise "note" */
    uint16_t decay;      /* ms */
    uint8_t  level;      /* 0-255 */
} Drum;

static Drum drum_for(int note)
{
    switch (note) {
    case 35: case 36: return (Drum){false, 28, 180, 255};                 /* kick */
    case 37: case 38: case 39: case 40: return (Drum){true, 100, 160, 220};   /* snare, clap */
    case 41: case 43: case 45: case 47: case 48: case 50:
        return (Drum){false, (uint8_t)(note + 4), 260, 230};                 /* toms */
    case 42: case 44: return (Drum){true, 124, 50, 150};                  /* closed hi-hat */
    case 46: return (Drum){true, 124, 260, 150};                          /* open hi-hat */
    case 49: case 52: case 55: case 57: return (Drum){true, 118, 900, 170};   /* crash */
    case 51: case 53: case 59: return (Drum){true, 122, 600, 130};       /* ride */
    default: return (Drum){true, 110, 120, 170};
    }
}

static void note_on(int slot, int midi_ch, int note, int vel)
{
    Slot  *s = &slots[slot];
    Voice *v;
    int    i;
    if (midi_ch == 9) {
        Drum d = drum_for(note);
        i = d.noise ? voice_alloc(FIRST_NOISE, LAST_NOISE) : voice_alloc(FIRST_VOICE, LAST_VOICE);
        v = &voices[i];
        v->noise = d.noise;
        v->timbre = NULL;
        v->attack = 1;
        v->decay = d.decay;
        v->sustain = 0;
        v->release = 30;
        vel = vel * d.level / 255;
        note = d.pitch;
    } else {
        i = voice_alloc(FIRST_VOICE, LAST_VOICE);
        v = &voices[i];
        const Timbre *t = &timbres[s->ch[midi_ch].program >> 3];
        v->noise = false;
        v->timbre = t;
        v->attack = t->attack;
        v->decay = t->decay;
        v->sustain = t->sustain;
        v->release = t->release;
    }
    v->slot = (int8_t)slot;
    v->midi_ch = (uint8_t)midi_ch;
    v->note = (uint8_t)note;
    v->vel = (uint8_t)vel;
    v->held_by_pedal = false;
    v->age = now_ms;
    v->stage = STAGE_ATTACK;
    v->env = v->attack <= 1 ? ENV_MAX : 0;
    if (v->env == ENV_MAX)
        v->stage = STAGE_DECAY;

    int pan = s->ch[midi_ch].pan;
    if (v->noise) {
        /* Noise pitch: higher drum "notes" give brighter noise. */
        hw->noise(i, pitch_rate(note * 64) / 4, voice_volume(v), pan);
    } else {
        const int8_t *wave = waves[v->timbre ? v->timbre->wave : WAVE_SINE];
        hw->wave(i, wave, SYNTH_WAVE_LEN, voice_rate(v), voice_volume(v), pan);
    }
}

static void note_off(int slot, int midi_ch, int note)
{
    for (int i = FIRST_VOICE; i <= LAST_NOISE; i++) {
        Voice *v = &voices[i];
        if (v->slot != slot || v->midi_ch != midi_ch || v->note != note ||
            v->stage == STAGE_OFF || v->stage == STAGE_RELEASE)
            continue;
        if (slots[slot].ch[midi_ch].sustain && midi_ch != 9)
            v->held_by_pedal = true;
        else
            v->stage = STAGE_RELEASE;
    }
}

static void slot_voices_off(int slot, bool immediately)
{
    for (int i = 0; i < SYNTH_CHANNELS; i++) {
        if (voices[i].slot != slot)
            continue;
        if (immediately)
            voice_free(i);
        else if (voices[i].stage != STAGE_OFF)
            voices[i].stage = STAGE_RELEASE;
    }
}

/* Re-applies volume (and pitch) to a slot's sounding voices. */
static void refresh_slot(int slot, bool pitch)
{
    for (int i = FIRST_VOICE; i <= LAST_NOISE; i++) {
        Voice *v = &voices[i];
        if (v->slot != slot || v->stage == STAGE_OFF)
            continue;
        hw->volume(i, voice_volume(v), slots[slot].ch[v->midi_ch].pan);
        if (pitch && !v->noise)
            hw->rate(i, voice_rate(v));
    }
}

/* ------------------------------------------------------------------------ */
/* Sequencing                                                               */

static void control_change(int slot, int c, int cc, int value)
{
    MidiChannel *ch = &slots[slot].ch[c];
    switch (cc) {
    case 7:  ch->volume = (uint8_t)value; break;
    case 10: ch->pan = (uint8_t)value; break;
    case 11: ch->expression = (uint8_t)value; break;
    case 64:
        ch->sustain = value >= 64;
        if (!ch->sustain) {
            for (int i = FIRST_VOICE; i <= LAST_VOICE; i++) {
                Voice *v = &voices[i];
                if (v->slot == slot && v->midi_ch == c && v->held_by_pedal) {
                    v->held_by_pedal = false;
                    v->stage = STAGE_RELEASE;
                }
            }
        }
        return;
    case 120: case 123:   /* all sound / notes off */
        for (int i = FIRST_VOICE; i <= LAST_NOISE; i++)
            if (voices[i].slot == slot && voices[i].midi_ch == c)
                voices[i].stage = STAGE_RELEASE;
        return;
    case 121:             /* reset controllers */
        ch->volume = 100;
        ch->expression = 127;
        ch->pan = 64;
        ch->bend = 0;
        ch->sustain = false;
        break;
    default:
        return;
    }
    refresh_slot(slot, false);
}

static void dispatch(int slot, const NjaEvent *e)
{
    int c = e->status & 15;
    switch (e->status & 0xF0) {
    case 0x90:
        if (e->d2) {
            note_on(slot, c, e->d1, e->d2);
            break;
        }
        /* fall through: note on with velocity 0 is a note off */
    case 0x80:
        note_off(slot, c, e->d1);
        break;
    case 0xB0:
        control_change(slot, c, e->d1, e->d2);
        break;
    case 0xC0:
        slots[slot].ch[c].program = e->d1;
        break;
    case 0xE0:
        slots[slot].ch[c].bend = (int16_t)(((e->d2 << 7) | e->d1) - 8192);
        refresh_slot(slot, true);
        break;
    }
}

void synth_song(int slot, const NjaEvent *ev, uint32_t n, uint32_t start_ms, int volume)
{
    if (slot < 0 || slot >= SYNTH_SLOTS)
        return;
    synth_stop(slot);
    Slot *s = &slots[slot];
    reset_channels(s);
    s->pcm = false;
    s->ev = ev;
    s->n = n;
    s->idx = 0;
    s->now = start_ms;
    s->volume = volume;
    s->playing = true;
    /* Skip to the start time, keeping controller and program changes. */
    while (s->idx < n && ev[s->idx].ms < start_ms) {
        const NjaEvent *e = &ev[s->idx++];
        uint8_t kind = e->status & 0xF0;
        if (kind == 0xB0 || kind == 0xC0 || kind == 0xE0)
            dispatch(slot, e);
    }
}

void synth_pcm(int slot, const int16_t *pcm, uint32_t samples, uint32_t rate, uint32_t start,
               int volume)
{
    if (slot < 0 || slot >= SYNTH_SLOTS || rate == 0 || start >= samples)
        return;
    synth_stop(slot);
    Slot *s = &slots[slot];
    int   i = voice_alloc(FIRST_PCM, LAST_PCM);
    Voice *v = &voices[i];
    s->pcm = true;
    s->playing = true;
    s->volume = volume;
    s->now = 0;
    s->length = (uint32_t)((uint64_t)(samples - start) * 1000 / rate);
    reset_channels(s);
    v->slot = (int8_t)slot;
    v->midi_ch = 0;
    v->vel = 127;
    v->env = ENV_MAX;
    v->stage = STAGE_SUSTAIN;
    v->age = now_ms;
    hw->pcm16(i, pcm + start, samples - start, rate, voice_volume(v), 64);
}

void synth_stop(int slot)
{
    if (slot < 0 || slot >= SYNTH_SLOTS)
        return;
    slot_voices_off(slot, true);
    slots[slot].playing = false;
}

void synth_volume(int slot, int volume)
{
    if (slot < 0 || slot >= SYNTH_SLOTS)
        return;
    slots[slot].volume = volume;
    for (int i = 0; i < SYNTH_CHANNELS; i++)
        if (voices[i].slot == slot && voices[i].stage != STAGE_OFF)
            hw->volume(i, voice_volume(&voices[i]), slots[slot].ch[voices[i].midi_ch].pan);
}

void synth_master(int volume)
{
    master = volume < 0 ? 0 : volume > 100 ? 100 : volume;
    for (int s = 0; s < SYNTH_SLOTS; s++)
        if (slots[s].playing)
            synth_volume(s, slots[s].volume);
}

bool synth_playing(int slot)
{
    return slot >= 0 && slot < SYNTH_SLOTS && slots[slot].playing;
}

static void advance_envelopes(uint32_t ms)
{
    for (int i = FIRST_VOICE; i <= LAST_NOISE; i++) {
        Voice *v = &voices[i];
        if (v->stage == STAGE_OFF)
            continue;
        if (!hw->active(i)) {
            v->stage = STAGE_OFF;
            v->slot = -1;
            continue;
        }
        int32_t sustain = v->sustain << 8;
        int32_t old = v->env;
        switch (v->stage) {
        case STAGE_ATTACK:
            v->env += ENV_MAX * (int32_t)ms / (v->attack ? v->attack : 1);
            if (v->env >= ENV_MAX) {
                v->env = ENV_MAX;
                v->stage = STAGE_DECAY;
            }
            break;
        case STAGE_DECAY:
            if (v->decay == 0) {
                v->env = sustain;
            } else {
                v->env -= ENV_MAX * (int32_t)ms / v->decay;
                if (v->env < sustain)
                    v->env = sustain;
            }
            if (v->env <= sustain)
                v->stage = STAGE_SUSTAIN;
            break;
        case STAGE_RELEASE:
            v->env -= ENV_MAX * (int32_t)ms / (v->release ? v->release : 1);
            break;
        }
        if (v->env <= 0 && v->stage != STAGE_ATTACK) {
            voice_free(i);
            continue;
        }
        if (v->env != old)
            hw->volume(i, voice_volume(v), slots[v->slot].ch[v->midi_ch].pan);
    }
}

void synth_tick(uint32_t elapsed_ms)
{
    now_ms += elapsed_ms;
    for (int s = 0; s < SYNTH_SLOTS; s++) {
        Slot *sl = &slots[s];
        if (!sl->playing)
            continue;
        sl->now += elapsed_ms;
        if (sl->pcm) {
            if (sl->now >= sl->length)
                synth_stop(s);
            continue;
        }
        while (sl->idx < sl->n && sl->ev[sl->idx].ms <= sl->now)
            dispatch(s, &sl->ev[sl->idx++]);
        if (sl->idx >= sl->n) {
            /* The song is over; let the last notes ring out. */
            slot_voices_off(s, false);
            sl->playing = false;
        }
    }
    advance_envelopes(elapsed_ms);
}
