package nanojava;

/**
 * The platform audio backend used by javax.microedition.media and the Nokia
 * Sound API. Media is decoded natively (MIDI, WAV, tone sequences) and
 * played by the platform's synthesizer; when available() is false, or a
 * clip cannot be decoded (open returns 0), players fall back to silent,
 * timed playback.
 */
public final class Audio {
    private Audio() {
    }

    /** Whether the platform can play sound. */
    public static native boolean available();

    /** Plays a MIDI note (0..127) for durationMs at volume 0..100. */
    public static native void playTone(int note, int durationMs, int volume);

    /** Prepares encoded media for playback. Returns a handle, or 0 if the
     *  data cannot be played. */
    public static native int open(byte[] data, String contentType);

    /** Duration in microseconds, or -1 if unknown. */
    public static native long duration(int handle);

    /** Starts playing from the given media time in microseconds. */
    public static native void start(int handle, long mediaTime);

    public static native void stop(int handle);

    /** Volume 0..100 (0 while muted). */
    public static native void setVolume(int handle, int level);

    public static native void close(int handle);
}
