package com.nokia.mid.sound;

import java.util.Timer;
import java.util.TimerTask;

/*
 * Silent implementation: play() goes to SOUND_PLAYING and back to
 * SOUND_STOPPED after the sound's duration (tones: as given; WAV: from the
 * header; anything else: one second) times the loop count.
 */
public class Sound {
    public static final int FORMAT_TONE = 1;
    public static final int FORMAT_WAV = 5;
    public static final int SOUND_PLAYING = 0;
    public static final int SOUND_STOPPED = 1;
    public static final int SOUND_UNINITIALIZED = 3;

    private static Timer timer;

    private int state = SOUND_UNINITIALIZED;
    private int gain = 255;
    private long durationMs;
    private int freq;
    private SoundListener listener;
    private TimerTask pending;

    public Sound(int freq, long duration) {
        init(freq, duration);
    }

    public Sound(byte[] data, int type) {
        init(data, type);
    }

    public static int getConcurrentSoundCount(int type) {
        return 1;
    }

    public static int[] getSupportedFormats() {
        return new int[] {FORMAT_TONE, FORMAT_WAV};
    }

    public void init(int freq, long duration) {
        if (freq < 0 || duration <= 0) {
            throw new IllegalArgumentException();
        }
        stopPending();
        this.freq = freq;
        durationMs = duration;
        setState(SOUND_STOPPED);
    }

    public void init(byte[] data, int type) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (type != FORMAT_TONE && type != FORMAT_WAV) {
            throw new IllegalArgumentException();
        }
        stopPending();
        freq = 0;
        durationMs = type == FORMAT_WAV ? wavDuration(data) : 1000;
        setState(SOUND_STOPPED);
    }

    private static long wavDuration(byte[] d) {
        int pos = 12;
        long byteRate = 0;
        while (pos + 8 <= d.length) {
            int len = le32(d, pos + 4);
            if (len < 0) {
                break;
            }
            if (d[pos] == 'f' && d[pos + 1] == 'm' && d[pos + 2] == 't' && pos + 20 <= d.length) {
                byteRate = le32(d, pos + 16) & 0xffffffffL;
            } else if (d[pos] == 'd' && d[pos + 1] == 'a' && d[pos + 2] == 't' && byteRate > 0) {
                return Math.max(1, Math.min(len, d.length - pos - 8) * 1000L / byteRate);
            }
            pos += 8 + len + (len & 1);
        }
        return 1000;
    }

    private static int le32(byte[] d, int off) {
        return (d[off] & 0xff) | (d[off + 1] & 0xff) << 8 | (d[off + 2] & 0xff) << 16
                | (d[off + 3] & 0xff) << 24;
    }

    public void play(int loop) {
        if (loop < 0) {
            throw new IllegalArgumentException();
        }
        if (state == SOUND_UNINITIALIZED) {
            return;
        }
        stopPending();
        if (freq > 0 && nanojava.Audio.available()) {
            nanojava.Audio.playTone(noteFor(freq), (int) Math.min(durationMs, Integer.MAX_VALUE),
                    gain * 100 / 255);
        }
        setState(SOUND_PLAYING);
        if (loop != 0) {
            final TimerTask task = new TimerTask() {
                public void run() {
                    synchronized (Sound.this) {
                        if (pending != this) {
                            return;
                        }
                        pending = null;
                    }
                    setState(SOUND_STOPPED);
                }
            };
            synchronized (this) {
                pending = task;
            }
            timer().schedule(task, durationMs * loop);
        }
    }

    /* Nearest MIDI note for a frequency in Hz. */
    private static int noteFor(int freq) {
        int note = 69;
        double f = 440;
        while (f * 1.0293 < freq && note < 127) {
            f *= 1.0594631;
            note++;
        }
        while (f / 1.0293 > freq && note > 0) {
            f /= 1.0594631;
            note--;
        }
        return note;
    }

    private static synchronized Timer timer() {
        if (timer == null) {
            timer = new Timer();
        }
        return timer;
    }

    private synchronized void stopPending() {
        if (pending != null) {
            pending.cancel();
            pending = null;
        }
    }

    public void stop() {
        stopPending();
        if (state == SOUND_PLAYING) {
            setState(SOUND_STOPPED);
        }
    }

    public void resume() {
        if (state == SOUND_STOPPED) {
            play(1);
        }
    }

    public void release() {
        stopPending();
        setState(SOUND_UNINITIALIZED);
    }

    public int getState() {
        return state;
    }

    public void setGain(int gain) {
        this.gain = Math.max(0, Math.min(255, gain));
    }

    public int getGain() {
        return gain;
    }

    public void setSoundListener(SoundListener listener) {
        this.listener = listener;
    }

    private void setState(int s) {
        SoundListener l;
        synchronized (this) {
            if (state == s) {
                return;
            }
            state = s;
            l = listener;
        }
        if (l != null) {
            l.soundStateChanged(this, s);
        }
    }
}
