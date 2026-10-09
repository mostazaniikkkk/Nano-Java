package javax.microedition.media;

import javax.microedition.media.control.ToneControl;

/* Content sniffing and duration estimates (in microseconds) for media data. */
final class MediaInfo {
    static final String TONE = "audio/x-tone-seq";
    static final String MIDI = "audio/midi";
    static final String WAV = "audio/x-wav";
    static final String AMR = "audio/amr";
    static final String MP3 = "audio/mpeg";

    private final byte[] d;
    private int p;

    private MediaInfo(byte[] d) {
        this.d = d;
    }

    static String sniff(byte[] d) {
        if (tag(d, 0, "MThd")) {
            return MIDI;
        }
        if (tag(d, 0, "RIFF")) {
            return WAV;
        }
        if (tag(d, 0, "#!AMR")) {
            return AMR;
        }
        if (tag(d, 0, "ID3") || (d.length > 1 && (d[0] & 0xff) == 0xff && (d[1] & 0xe0) == 0xe0)) {
            return MP3;
        }
        if (d.length > 1 && d[0] == ToneControl.VERSION && d[1] == 1) {
            return TONE;
        }
        return "application/octet-stream";
    }

    static String typeForName(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".mid") || n.endsWith(".midi") || n.endsWith(".kar")) {
            return MIDI;
        }
        if (n.endsWith(".wav")) {
            return WAV;
        }
        if (n.endsWith(".amr")) {
            return AMR;
        }
        if (n.endsWith(".mp3")) {
            return MP3;
        }
        if (n.endsWith(".jts")) {
            return TONE;
        }
        return null;
    }

    /* Duration of the data, or Player.TIME_UNKNOWN. */
    static long duration(byte[] data, String type) {
        if (data == null) {
            return Player.TIME_UNKNOWN;
        }
        try {
            if (tag(data, 0, "MThd")) {
                return new MediaInfo(data).midi();
            }
            if (tag(data, 0, "RIFF")) {
                return new MediaInfo(data).wav();
            }
            if (TONE.equals(type)) {
                return tone(data);
            }
        } catch (RuntimeException e) {
            /* Malformed data: duration stays unknown. */
        }
        return Player.TIME_UNKNOWN;
    }

    private static boolean tag(byte[] d, int off, String t) {
        if (d.length < off + t.length()) {
            return false;
        }
        for (int i = 0; i < t.length(); i++) {
            if (d[off + i] != t.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private int be(int off, int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            v = (v << 8) | (d[off + i] & 0xff);
        }
        return v;
    }

    private int le32(int off) {
        return (d[off] & 0xff) | (d[off + 1] & 0xff) << 8 | (d[off + 2] & 0xff) << 16
                | (d[off + 3] & 0xff) << 24;
    }

    private long wav() {
        if (!tag(d, 8, "WAVE")) {
            return Player.TIME_UNKNOWN;
        }
        int pos = 12;
        long byteRate = 0;
        while (pos + 8 <= d.length) {
            int len = le32(pos + 4);
            if (len < 0) {
                break;
            }
            if (tag(d, pos, "fmt ") && pos + 20 <= d.length) {
                byteRate = le32(pos + 16) & 0xffffffffL;
            } else if (tag(d, pos, "data")) {
                long size = Math.min(len, d.length - pos - 8);
                return byteRate > 0 ? size * 1000000L / byteRate : Player.TIME_UNKNOWN;
            }
            pos += 8 + len + (len & 1);
        }
        return Player.TIME_UNKNOWN;
    }

    private long varlen() {
        long v = 0;
        int b;
        do {
            b = d[p++] & 0xff;
            v = (v << 7) | (b & 0x7f);
        } while ((b & 0x80) != 0);
        return v;
    }

    private long midi() {
        int division = be(12, 2);
        int tracks = be(10, 2);
        int pos = 8 + be(4, 4);
        long[] tempoTicks = new long[8];
        int[] tempos = new int[8];
        int nTempos = 0;
        long endTick = 0;
        for (int t = 0; t < tracks && pos + 8 <= d.length; t++) {
            int len = be(pos + 4, 4);
            if (len < 0) {
                break;
            }
            if (tag(d, pos, "MTrk")) {
                p = pos + 8;
                int end = (int) Math.min((long) p + len, d.length);
                long tick = 0;
                int status = 0;
                while (p < end) {
                    tick += varlen();
                    int c = d[p] & 0xff;
                    if (c == 0xff) {
                        int type = d[p + 1] & 0xff;
                        p += 2;
                        int l = (int) varlen();
                        if (type == 0x51 && l == 3) {
                            if (nTempos == tempos.length) {
                                long[] nt = new long[nTempos * 2];
                                int[] nv = new int[nTempos * 2];
                                System.arraycopy(tempoTicks, 0, nt, 0, nTempos);
                                System.arraycopy(tempos, 0, nv, 0, nTempos);
                                tempoTicks = nt;
                                tempos = nv;
                            }
                            tempoTicks[nTempos] = tick;
                            tempos[nTempos++] = be(p, 3);
                        }
                        p += l;
                        if (type == 0x2f) {
                            break;
                        }
                    } else if (c == 0xf0 || c == 0xf7) {
                        p++;
                        p += (int) varlen();
                    } else {
                        if ((c & 0x80) != 0) {
                            status = c;
                            p++;
                        }
                        int hi = status & 0xf0;
                        p += hi == 0xc0 || hi == 0xd0 ? 1 : 2;
                    }
                }
                if (tick > endTick) {
                    endTick = tick;
                }
            }
            pos += 8 + len;
        }
        if ((division & 0x8000) != 0) {
            int fps = -(byte) (division >> 8);
            int ticksPerFrame = division & 0xff;
            return fps * ticksPerFrame > 0 ? endTick * 1000000L / (fps * ticksPerFrame)
                    : Player.TIME_UNKNOWN;
        }
        if (division == 0) {
            return Player.TIME_UNKNOWN;
        }
        /* Tempo changes may come from any track: order them by tick. */
        for (int i = 1; i < nTempos; i++) {
            for (int j = i; j > 0 && tempoTicks[j - 1] > tempoTicks[j]; j--) {
                long tt = tempoTicks[j];
                tempoTicks[j] = tempoTicks[j - 1];
                tempoTicks[j - 1] = tt;
                int tv = tempos[j];
                tempos[j] = tempos[j - 1];
                tempos[j - 1] = tv;
            }
        }
        long us = 0;
        long last = 0;
        int tempo = 500000;
        for (int i = 0; i < nTempos && tempoTicks[i] < endTick; i++) {
            us += (tempoTicks[i] - last) * tempo / division;
            last = tempoTicks[i];
            tempo = tempos[i];
        }
        return us + (endTick - last) * tempo / division;
    }

    /* Validates a tone sequence and returns its duration; throws
     * IllegalArgumentException if it is malformed. */
    static long tone(byte[] s) {
        if (s == null || s.length < 2 || s.length % 2 != 0 || s[0] != ToneControl.VERSION
                || s[1] != 1) {
            throw new IllegalArgumentException("invalid tone sequence");
        }
        int i = 2;
        int tempo = 30;
        int resolution = 64;
        if (i < s.length && s[i] == ToneControl.TEMPO) {
            tempo = s[i + 1];
            i += 2;
        }
        if (i < s.length && s[i] == ToneControl.RESOLUTION) {
            resolution = s[i + 1];
            i += 2;
        }
        if (tempo < 1 || resolution < 1) {
            throw new IllegalArgumentException("invalid tone sequence");
        }
        int[] blocks = new int[128];
        for (int b = 0; b < blocks.length; b++) {
            blocks[b] = -1;
        }
        long units = toneUnits(s, i, -1, blocks, 0);
        return units * 60000000L / (tempo * resolution);
    }

    private static long toneUnits(byte[] s, int i, int block, int[] blocks, int depth) {
        long units = 0;
        while (i < s.length) {
            int c = s[i];
            int v = s[i + 1];
            if (c == ToneControl.BLOCK_START) {
                if (v < 0 || block >= 0) {
                    throw new IllegalArgumentException("invalid tone block");
                }
                blocks[v] = i + 2;
                i += 2;
                while (i < s.length && !(s[i] == ToneControl.BLOCK_END && s[i + 1] == v)) {
                    i += 2;
                }
                if (i >= s.length) {
                    throw new IllegalArgumentException("unterminated tone block");
                }
            } else if (c == ToneControl.BLOCK_END) {
                if (v != block) {
                    throw new IllegalArgumentException("invalid tone block end");
                }
                return units;
            } else if (c == ToneControl.PLAY_BLOCK) {
                if (v < 0 || blocks[v] < 0 || depth > 8) {
                    throw new IllegalArgumentException("undefined tone block");
                }
                units += toneUnits(s, blocks[v], v, blocks, depth + 1);
            } else if (c == ToneControl.REPEAT) {
                i += 2;
                if (i >= s.length || v < 2 || s[i + 1] <= 0) {
                    throw new IllegalArgumentException("invalid tone repeat");
                }
                units += v * s[i + 1];
            } else if (c >= ToneControl.SILENCE) {
                if (v <= 0) {
                    throw new IllegalArgumentException("invalid tone duration");
                }
                units += v;
            } else if (c != ToneControl.SET_VOLUME) {
                throw new IllegalArgumentException("invalid tone event");
            }
            i += 2;
        }
        if (block >= 0) {
            throw new IllegalArgumentException("unterminated tone block");
        }
        return units;
    }
}
