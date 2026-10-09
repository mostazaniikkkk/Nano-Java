package javax.microedition.media;

import java.io.IOException;
import java.io.InputStream;

public final class Manager {
    public static final String TONE_DEVICE_LOCATOR = "device://tone";
    public static final String MIDI_DEVICE_LOCATOR = "device://midi";

    private static final String[] CONTENT_TYPES = {
        MediaInfo.TONE, MediaInfo.MIDI, "audio/sp-midi", MediaInfo.WAV, MediaInfo.AMR, MediaInfo.MP3
    };
    private static final String[] DEVICE_TYPES = {MediaInfo.TONE, MediaInfo.MIDI};

    private Manager() {
    }

    public static String[] getSupportedContentTypes(String protocol) {
        if (protocol == null) {
            return copy(CONTENT_TYPES);
        }
        if (protocol.equals("device")) {
            return copy(DEVICE_TYPES);
        }
        return new String[0];
    }

    public static String[] getSupportedProtocols(String contentType) {
        if (contentType == null || contentType.equals(MediaInfo.TONE)
                || contentType.equals(MediaInfo.MIDI)) {
            return new String[] {"device"};
        }
        return new String[0];
    }

    private static String[] copy(String[] a) {
        String[] c = new String[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    public static Player createPlayer(String locator) throws IOException, MediaException {
        if (locator == null) {
            throw new IllegalArgumentException();
        }
        if (locator.equals(TONE_DEVICE_LOCATOR)) {
            return new PlayerImpl(null, MediaInfo.TONE);
        }
        if (locator.equals(MIDI_DEVICE_LOCATOR)) {
            return new PlayerImpl(null, MediaInfo.MIDI);
        }
        /* Not a standard protocol, but some handsets play jar resources by
         * plain path or resource:// locator and games rely on it. */
        String path = locator;
        if (path.startsWith("resource://")) {
            path = path.substring("resource://".length());
        } else if (path.startsWith("resource:")) {
            path = path.substring("resource:".length());
        } else if (path.indexOf("://") >= 0) {
            throw new MediaException("unsupported locator: " + locator);
        }
        byte[] data = nanojava.Resources.read(path.startsWith("/") ? path.substring(1) : path);
        if (data == null) {
            throw new MediaException("cannot open " + locator);
        }
        String type = MediaInfo.typeForName(path);
        return new PlayerImpl(data, type != null ? type : MediaInfo.sniff(data));
    }

    public static Player createPlayer(InputStream stream, String type)
            throws IOException, MediaException {
        if (stream == null) {
            throw new IllegalArgumentException();
        }
        byte[] buf = new byte[4096];
        int n = 0;
        for (;;) {
            if (n == buf.length) {
                byte[] nb = new byte[buf.length * 2];
                System.arraycopy(buf, 0, nb, 0, n);
                buf = nb;
            }
            int r = stream.read(buf, n, buf.length - n);
            if (r < 0) {
                break;
            }
            n += r;
        }
        byte[] data = new byte[n];
        System.arraycopy(buf, 0, data, 0, n);
        if (type == null) {
            type = MediaInfo.sniff(data);
        } else if (type.equals("audio/mid") || type.equals("audio/x-midi")) {
            type = MediaInfo.MIDI;
        } else if (type.equals("audio/wav")) {
            type = MediaInfo.WAV;
        } else if (type.equals("audio/mp3")) {
            type = MediaInfo.MP3;
        }
        return new PlayerImpl(data, type);
    }

    public static void playTone(int note, int duration, int volume) throws MediaException {
        if (note < 0 || note > 127 || duration <= 0) {
            throw new IllegalArgumentException();
        }
        if (volume < 0) {
            volume = 0;
        } else if (volume > 100) {
            volume = 100;
        }
        if (nanojava.Audio.available()) {
            nanojava.Audio.playTone(note, duration, volume);
        }
    }
}
