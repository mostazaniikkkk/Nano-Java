package audiotest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.midlet.MIDlet;

/*
 * Plays a known MIDI note (A4 = 440 Hz on a sine-like "pipe" program) for
 * one second, then a 1 kHz WAV tone for half a second, then a tone via
 * Manager.playTone. Run with --audio-out to check the result.
 */
public class AudioTest extends MIDlet implements PlayerListener {
    private int ended;

    /* Format 0 MIDI: program 73 (flute, "pipe" family), A4 for 1 s at 120 bpm
     * (480 ticks per quarter note: 960 ticks = 1 s). */
    private static byte[] midi() {
        int[] track = {
            0x00, 0xC0, 73,
            0x00, 0x90, 69, 100,
            0x87, 0x40, 0x80, 69, 0,      // delta 960
            0x00, 0xFF, 0x2F, 0x00
        };
        byte[] b = new byte[14 + 8 + track.length];
        int[] header = {'M', 'T', 'h', 'd', 0, 0, 0, 6, 0, 0, 0, 1, 0x01, 0xE0,
                        'M', 'T', 'r', 'k', 0, 0, 0, track.length};
        for (int i = 0; i < header.length; i++) {
            b[i] = (byte) header[i];
        }
        for (int i = 0; i < track.length; i++) {
            b[header.length + i] = (byte) track[i];
        }
        return b;
    }

    /* 16-bit mono WAV, 8000 Hz, 1 kHz sine for 0.5 s. */
    private static byte[] wav() {
        int rate = 8000, n = rate / 2;
        byte[] b = new byte[44 + n * 2];
        String riff = "RIFF....WAVEfmt ";
        for (int i = 0; i < riff.length(); i++) {
            b[i] = (byte) riff.charAt(i);
        }
        put32(b, 4, 36 + n * 2);
        put32(b, 16, 16);
        put16(b, 20, 1);
        put16(b, 22, 1);
        put32(b, 24, rate);
        put32(b, 28, rate * 2);
        put16(b, 32, 2);
        put16(b, 34, 16);
        b[36] = 'd'; b[37] = 'a'; b[38] = 't'; b[39] = 'a';
        put32(b, 40, n * 2);
        for (int i = 0; i < n; i++) {
            int s = (int) (Math.sin(2 * Math.PI * 1000 * i / rate) * 20000);
            put16(b, 44 + i * 2, s);
        }
        return b;
    }

    private static void put16(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >> 8);
    }

    private static void put32(byte[] b, int o, int v) {
        put16(b, o, v);
        put16(b, o + 2, v >> 16);
    }

    public void playerUpdate(Player p, String event, Object data) {
        if (event.equals(PlayerListener.END_OF_MEDIA)) {
            synchronized (this) {
                ended++;
                notifyAll();
            }
        }
    }

    private synchronized void waitEnd(int count) throws InterruptedException {
        while (ended < count) {
            wait();
        }
    }

    protected void startApp() {
        Display.getDisplay(this).setCurrent(new Canvas() {
            protected void paint(Graphics g) {
                g.setColor(0);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        });
        new Thread() {
            public void run() {
                try {
                    InputStream in = new ByteArrayInputStream(midi());
                    Player p = Manager.createPlayer(in, "audio/midi");
                    p.addPlayerListener(AudioTest.this);
                    p.start();
                    System.out.println("midi duration " + p.getDuration() / 1000 + " ms");
                    waitEnd(1);
                    p.close();
                    Thread.sleep(300);

                    Player w = Manager.createPlayer(new ByteArrayInputStream(wav()), "audio/x-wav");
                    w.addPlayerListener(AudioTest.this);
                    w.start();
                    System.out.println("wav duration " + w.getDuration() / 1000 + " ms");
                    waitEnd(2);
                    w.close();
                    Thread.sleep(300);

                    Manager.playTone(81, 400, 100);   // A5 = 880 Hz
                    Thread.sleep(600);
                    System.out.println("done");
                } catch (Exception e) {
                    System.out.println("error " + e);
                }
            }
        }.start();
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }
}
