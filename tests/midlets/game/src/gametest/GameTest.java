package gametest;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;
import javax.microedition.media.*;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.*;
import com.nokia.mid.sound.Sound;
import com.nokia.mid.sound.SoundListener;
import com.nokia.mid.ui.DirectGraphics;
import com.nokia.mid.ui.DirectUtils;
import com.nokia.mid.ui.FullCanvas;

/*
 * Exercises lcdui.game, rms, media, io and the Nokia UI API. Every check
 * prints "ok <name>" or "FAIL <name> ..."; the last line is a summary.
 * Only the "rms run" line changes between runs (it shows persistence).
 */
public class GameTest extends MIDlet implements Runnable {
    private static int failures;
    private static int checks;

    private Image frames;
    private Image tiles;
    private Scene scene;

    static void check(String name, boolean ok) {
        checks++;
        if (ok) {
            System.out.println("ok " + name);
        } else {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    static void check(String name, long got, long expected) {
        checks++;
        if (got == expected) {
            System.out.println("ok " + name + " = " + got);
        } else {
            failures++;
            System.out.println("FAIL " + name + ": got " + got + ", expected " + expected);
        }
    }

    static void check(String name, String got, String expected) {
        checks++;
        if (got == null ? expected == null : got.equals(expected)) {
            System.out.println("ok " + name + " = " + got);
        } else {
            failures++;
            System.out.println("FAIL " + name + ": got " + got + ", expected " + expected);
        }
    }

    protected void startApp() {
        if (scene == null) {
            try {
                frames = Image.createImage("/frames.png");
                tiles = Image.createImage("/tiles.png");
            } catch (IOException e) {
                System.out.println("FAIL images: " + e);
                return;
            }
            scene = new Scene();
            Display.getDisplay(this).setCurrent(scene);
            new Thread(this).start();
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }

    public void run() {
        try {
            testRms();
            testSprite();
            testCollisions();
            testTiledLayer();
            testIo();
            testNokia();
            scene.draw();
            testMedia();
        } catch (Throwable e) {
            failures++;
            System.out.println("FAIL exception " + e);
        }
        System.out.println("done: " + checks + " checks, " + failures + " failures");
    }

    /* ---- RMS ---- */

    static String str(byte[] b) {
        return b == null ? "null" : new String(b);
    }

    static int listened;

    private void testRms() throws Exception {
        RecordStore runs = RecordStore.openRecordStore("runs", true);
        int n = 1;
        if (runs.getNumRecords() == 0) {
            runs.addRecord(new byte[] {1}, 0, 1);
        } else {
            byte[] b = runs.getRecord(1);
            n = b[0] + 1;
            b[0] = (byte) n;
            runs.setRecord(1, b, 0, 1);
        }
        System.out.println("rms run " + n);
        /* Left open on purpose: write-through must persist it anyway. */

        try {
            RecordStore.deleteRecordStore("test");
        } catch (RecordStoreNotFoundException e) {
            /* First run. */
        }
        RecordStore rs = RecordStore.openRecordStore("test", true);
        RecordStore rs2 = RecordStore.openRecordStore("test", false);
        check("rms same instance", rs == rs2);
        rs.addRecordListener(new RecordListener() {
            public void recordAdded(RecordStore s, int id) {
                listened += 100;
            }

            public void recordChanged(RecordStore s, int id) {
                listened += 10;
            }

            public void recordDeleted(RecordStore s, int id) {
                listened += 1;
            }
        });
        check("rms empty", rs.getNumRecords(), 0);
        check("rms first id", rs.getNextRecordID(), 1);
        check("rms add c", rs.addRecord("c".getBytes(), 0, 1), 1);
        check("rms add a", rs.addRecord("xay".getBytes(), 1, 1), 2);
        check("rms add b", rs.addRecord("b".getBytes(), 0, 1), 3);
        check("rms add empty", rs.addRecord(null, 0, 0), 4);
        rs.setRecord(2, "aa".getBytes(), 0, 2);
        rs.deleteRecord(3);
        check("rms listener", listened, 411);
        check("rms next id", rs.getNextRecordID(), 5);
        check("rms count", rs.getNumRecords(), 3);
        check("rms version", rs.getVersion(), 6);
        check("rms get 2", str(rs.getRecord(2)), "aa");
        check("rms get empty", str(rs.getRecord(4)), "null");
        check("rms size 2", rs.getRecordSize(2), 2);
        byte[] buf = new byte[4];
        check("rms get into", rs.getRecord(1, buf, 3), 1);
        check("rms get into data", buf[3], 'c');
        try {
            rs.getRecord(3);
            check("rms deleted id", false);
        } catch (InvalidRecordIDException e) {
            check("rms deleted id", true);
        }
        try {
            rs.getRecord(2, new byte[1], 0);
            check("rms small buffer", false);
        } catch (ArrayIndexOutOfBoundsException e) {
            check("rms small buffer", true);
        }
        check("rms size", rs.getSize() > 0 && rs.getSizeAvailable() > 0);

        RecordComparator byText = new RecordComparator() {
            public int compare(byte[] a, byte[] b) {
                int c = str(a).compareTo(str(b));
                return c < 0 ? PRECEDES : c > 0 ? FOLLOWS : EQUIVALENT;
            }
        };
        RecordFilter nonEmpty = new RecordFilter() {
            public boolean matches(byte[] r) {
                return r.length > 0;
            }
        };
        RecordEnumeration e = rs.enumerateRecords(nonEmpty, byText, false);
        check("rms enum count", e.numRecords(), 2);
        StringBuffer order = new StringBuffer();
        while (e.hasNextElement()) {
            order.append(str(e.nextRecord())).append(' ');
        }
        check("rms enum sorted", order.toString(), "aa c ");
        check("rms enum previous", e.previousRecordId(), 2);
        e.reset();
        check("rms enum reset", e.nextRecordId(), 2);
        e.destroy();
        try {
            e.numRecords();
            check("rms enum destroyed", false);
        } catch (IllegalStateException ex) {
            check("rms enum destroyed", true);
        }

        RecordEnumeration all = rs.enumerateRecords(null, null, true);
        check("rms enum all", all.numRecords(), 3);
        check("rms enum first", all.nextRecordId(), 1);
        rs.addRecord("z".getBytes(), 0, 1);
        check("rms enum updated", all.numRecords(), 4);
        rs.deleteRecord(2);
        check("rms enum after delete", all.nextRecordId(), 4);
        check("rms enum last", all.nextRecordId(), 5);
        check("rms enum end", !all.hasNextElement());
        all.destroy();

        try {
            RecordStore.deleteRecordStore("test");
            check("rms delete open", false);
        } catch (RecordStoreException ex) {
            check("rms delete open", true);
        }
        rs.closeRecordStore();
        check("rms still open", rs.getNumRecords(), 3);
        rs.closeRecordStore();
        try {
            rs.getNumRecords();
            check("rms closed", false);
        } catch (RecordStoreNotOpenException ex) {
            check("rms closed", true);
        }

        rs = RecordStore.openRecordStore("test", false);
        check("rms reopened new", rs != rs2);
        check("rms reopen count", rs.getNumRecords(), 3);
        check("rms reopen next id", rs.getNextRecordID(), 6);
        check("rms reopen version", rs.getVersion(), 8);
        check("rms reopen data", str(rs.getRecord(1)) + str(rs.getRecord(5)), "cz");
        rs.closeRecordStore();

        try {
            RecordStore.openRecordStore("missing", false);
            check("rms not found", false);
        } catch (RecordStoreNotFoundException ex) {
            check("rms not found", true);
        }
        try {
            RecordStore.openRecordStore("123456789012345678901234567890123", true);
            check("rms long name", false);
        } catch (IllegalArgumentException ex) {
            check("rms long name", true);
        }
        try {
            RecordStore.openRecordStore("runs", "Other", "OtherSuite");
            check("rms other suite", false);
        } catch (RecordStoreNotFoundException ex) {
            check("rms other suite", true);
        }
        RecordStore own = RecordStore.openRecordStore("runs", "Nano Java", "GameTest");
        check("rms own suite lookup", own == runs);
        own.closeRecordStore();

        String[] names = RecordStore.listRecordStores();
        boolean hasRuns = false;
        boolean hasTest = false;
        for (int i = 0; names != null && i < names.length; i++) {
            hasRuns |= names[i].equals("runs");
            hasTest |= names[i].equals("test");
        }
        check("rms list", hasRuns && hasTest);
    }

    /* ---- Sprite ---- */

    private void testSprite() {
        Sprite s = new Sprite(frames, 16, 16);
        check("sprite frames", s.getRawFrameCount(), 4);
        check("sprite size", s.getWidth() * 100 + s.getHeight(), 1616);
        for (int i = 0; i < 5; i++) {
            s.nextFrame();
        }
        check("sprite nextFrame", s.getFrame(), 1);
        s.prevFrame();
        s.prevFrame();
        check("sprite prevFrame", s.getFrame(), 3);
        s.setFrameSequence(new int[] {3, 1, 2});
        check("sprite sequence", s.getFrameSequenceLength() * 10 + s.getFrame(), 30);
        s.prevFrame();
        check("sprite sequence wrap", s.getFrame(), 2);
        s.setFrameSequence(null);
        check("sprite default sequence", s.getFrameSequenceLength(), 4);
        try {
            s.setFrame(4);
            check("sprite bad frame", false);
        } catch (IndexOutOfBoundsException e) {
            check("sprite bad frame", true);
        }
        try {
            new Sprite(frames, 15, 16);
            check("sprite bad frame size", false);
        } catch (IllegalArgumentException e) {
            check("sprite bad frame size", true);
        }
        try {
            s.setFrameSequence(new int[] {0, 4});
            check("sprite bad sequence", false);
        } catch (ArrayIndexOutOfBoundsException e) {
            check("sprite bad sequence", true);
        }

        s.defineReferencePixel(4, 2);
        s.setRefPixelPosition(100, 50);
        check("sprite ref position", s.getX() * 1000 + s.getY(), 96048);
        int[] expected = {
            /* x, y after setTransform with the reference pixel at (100, 50) */
            96, 48,   /* NONE */
            96, 37,   /* MIRROR_ROT180 */
            89, 48,   /* MIRROR */
            89, 37,   /* ROT180 */
            98, 46,   /* MIRROR_ROT270 */
            87, 46,   /* ROT90 */
            98, 39,   /* ROT270 */
            87, 39,   /* MIRROR_ROT90 */
        };
        for (int t = 0; t < 8; t++) {
            s.setTransform(t);
            check("sprite transform " + t + " pos", s.getX() * 1000 + s.getY(),
                    expected[t * 2] * 1000 + expected[t * 2 + 1]);
            check("sprite transform " + t + " ref", s.getRefPixelX() * 1000 + s.getRefPixelY(),
                    100050);
        }
        check("sprite rotated size", s.getWidth() * 100 + s.getHeight(), 1616);

        /* setImage with a larger frame keeps the reference pixel in place. */
        Sprite big = new Sprite(frames, 16, 16);
        big.defineReferencePixel(8, 8);
        big.setRefPixelPosition(50, 50);
        big.setTransform(Sprite.TRANS_ROT90);
        big.setFrame(2);
        big.setImage(frames, 32, 16);
        check("sprite setImage frames", big.getRawFrameCount() * 10 + big.getFrame(), 20);
        check("sprite setImage ref", big.getRefPixelX() * 1000 + big.getRefPixelY(), 50050);
        check("sprite setImage size", big.getWidth() * 100 + big.getHeight(), 1632);

        Sprite copy = new Sprite(big);
        check("sprite copy", copy.getX() * 1000 + copy.getY(), big.getX() * 1000 + big.getY());
    }

    private void testCollisions() {
        Sprite a = new Sprite(frames, 16, 16);
        Image block = Image.createImage(4, 4);
        check("collide image rect", a.collidesWith(block, 12, 12, false));
        check("collide image transparent", !a.collidesWith(block, 12, 12, true));
        check("collide image opaque", a.collidesWith(block, 3, 10, true));
        a.setTransform(Sprite.TRANS_MIRROR);
        a.setPosition(0, 0);
        check("collide mirror opaque", a.collidesWith(block, 12, 12, true));
        check("collide mirror transparent", !a.collidesWith(block, 3, 10, true));
        a.setTransform(Sprite.TRANS_ROT90);
        a.setPosition(0, 0);
        check("collide rot90 opaque", a.collidesWith(block, 12, 12, true));
        check("collide rot90 transparent", !a.collidesWith(block, 0, 8, true));
        a.setTransform(Sprite.TRANS_NONE);
        a.setPosition(0, 0);

        Sprite b = new Sprite(a);
        check("collide same place", a.collidesWith(b, true));
        b.setPosition(12, 12);
        check("collide sprite rect", a.collidesWith(b, false));
        check("collide sprite pixels", !a.collidesWith(b, true));
        b.setPosition(-6, 4);
        check("collide sprite pixels overlap", a.collidesWith(b, true));
        a.defineCollisionRectangle(12, 0, 4, 16);
        check("collide collision rect", !a.collidesWith(b, false));
        a.defineCollisionRectangle(0, 0, 16, 16);
        b.setVisible(false);
        check("collide invisible", !a.collidesWith(b, false));

        TiledLayer t = new TiledLayer(4, 4, tiles, 16, 16);
        t.setCell(2, 2, 1);
        a.setPosition(0, 0);
        check("collide tiles empty", !a.collidesWith(t, false));
        a.setPosition(20, 20);
        check("collide tiles rect", a.collidesWith(t, false));
        check("collide tiles transparent", !a.collidesWith(t, true));
        /* F stem at local (3..5, 2..13) -> (33..35, 32..43), inside the tile. */
        a.setPosition(30, 30);
        check("collide tiles pixels", a.collidesWith(t, true));
        int anim = t.createAnimatedTile(0);
        t.setCell(2, 2, anim);
        check("collide animated empty", !a.collidesWith(t, false));
        t.setAnimatedTile(anim, 2);
        check("collide animated tile", a.collidesWith(t, true));
    }

    /* ---- TiledLayer and LayerManager ---- */

    private void testTiledLayer() {
        TiledLayer t = new TiledLayer(10, 6, tiles, 16, 16);
        check("tiled size", t.getWidth() * 1000 + t.getHeight(), 160096);
        t.fillCells(0, 0, 10, 6, 1);
        t.setCell(2, 1, 2);
        int anim = t.createAnimatedTile(3);
        check("tiled animated index", anim, -1);
        t.setCell(3, 1, anim);
        check("tiled getCell", t.getCell(3, 1), -1);
        check("tiled animated", t.getAnimatedTile(anim), 3);
        t.setAnimatedTile(anim, 4);
        check("tiled animated set", t.getAnimatedTile(anim), 4);
        try {
            t.setCell(10, 0, 1);
            check("tiled bad cell", false);
        } catch (IndexOutOfBoundsException e) {
            check("tiled bad cell", true);
        }
        try {
            t.setCell(0, 0, 5);
            check("tiled bad tile", false);
        } catch (IndexOutOfBoundsException e) {
            check("tiled bad tile", true);
        }
        try {
            t.setCell(0, 0, -2);
            check("tiled bad animated", false);
        } catch (IndexOutOfBoundsException e) {
            check("tiled bad animated", true);
        }

        LayerManager lm = new LayerManager();
        Sprite s = new Sprite(frames, 16, 16);
        lm.append(s);
        lm.append(t);
        check("lm size", lm.getSize(), 2);
        check("lm order", lm.getLayerAt(0) == s && lm.getLayerAt(1) == t);
        lm.insert(t, 0);
        check("lm insert existing", lm.getLayerAt(0) == t && lm.getSize() == 2);
        try {
            lm.insert(t, 2);
            check("lm insert bad index", false);
        } catch (IndexOutOfBoundsException e) {
            check("lm insert bad index", true);
        }
        lm.remove(t);
        check("lm remove", lm.getSize() == 1 && lm.getLayerAt(0) == s);
    }

    /* ---- io ---- */

    private void testIo() throws IOException {
        InputStream in = Connector.openInputStream("resource:///frames.png");
        int n = 0;
        while (in.read() >= 0) {
            n++;
        }
        in.close();
        check("io resource length", n, 154);
        DataInputStream d = Connector.openDataInputStream("/tiles.png");
        check("io png magic", d.readInt(), 0x89504e47);
        d.close();
        try {
            Connector.open("http://example.com/");
            check("io http", false);
        } catch (ConnectionNotFoundException e) {
            check("io http", true);
        }
        try {
            Connector.open("resource:///missing");
            check("io missing", false);
        } catch (ConnectionNotFoundException e) {
            check("io missing", true);
        }
    }

    /* ---- Nokia ---- */

    static class Full extends FullCanvas {
        protected void paint(Graphics g) {
        }
    }

    static int soundEvents;

    private void testNokia() throws InterruptedException {
        Full f = new Full();
        try {
            f.addCommand(new Command("x", Command.OK, 1));
            check("nokia fullcanvas command", false);
        } catch (IllegalStateException e) {
            check("nokia fullcanvas command", true);
        }
        check("nokia softkey", FullCanvas.KEY_SOFTKEY1, -6);
        Image img = DirectUtils.createImage(8, 8, 0xff00ff00);
        int[] px = new int[1];
        img.getRGB(px, 0, 1, 4, 4, 1, 1);
        check("nokia createImage", Integer.toHexString(px[0] & 0xffffff), "ff00");

        Sound snd = new Sound(440, 100);
        snd.setSoundListener(new SoundListener() {
            public void soundStateChanged(Sound s, int event) {
                soundEvents = soundEvents * 10 + event + 1;
            }
        });
        snd.play(2);
        check("nokia sound playing", snd.getState(), Sound.SOUND_PLAYING);
        Thread.sleep(400);
        check("nokia sound stopped", snd.getState(), Sound.SOUND_STOPPED);
        check("nokia sound events", soundEvents, 12);
        snd.release();
    }

    /* ---- media ---- */

    static class Log implements PlayerListener {
        final StringBuffer events = new StringBuffer();
        int ends;
        boolean closed;

        public synchronized void playerUpdate(Player p, String event, Object data) {
            events.append(event).append(' ');
            if (event.equals(END_OF_MEDIA)) {
                ends++;
            }
            if (event.equals(CLOSED)) {
                closed = true;
            }
            notifyAll();
        }

        synchronized void waitEnds(int n) throws InterruptedException {
            long until = System.currentTimeMillis() + 4000;
            while (ends < n && System.currentTimeMillis() < until) {
                wait(100);
            }
        }

        synchronized void waitClosed() throws InterruptedException {
            long until = System.currentTimeMillis() + 2000;
            while (!closed && System.currentTimeMillis() < until) {
                wait(100);
            }
        }

        synchronized String text() {
            return events.toString().trim();
        }
    }

    static byte[] wav(int byteRate, int dataLen) {
        byte[] w = new byte[44 + dataLen];
        String hdr = "RIFF....WAVEfmt ";
        for (int i = 0; i < 16; i++) {
            w[i] = (byte) hdr.charAt(i);
        }
        le(w, 4, 36 + dataLen);
        le(w, 16, 16);
        w[20] = 1;
        w[22] = 1;
        le(w, 24, byteRate);
        le(w, 28, byteRate);
        w[32] = 1;
        w[34] = 8;
        w[36] = 'd';
        w[37] = 'a';
        w[38] = 't';
        w[39] = 'a';
        le(w, 40, dataLen);
        return w;
    }

    static void le(byte[] b, int off, int v) {
        b[off] = (byte) v;
        b[off + 1] = (byte) (v >> 8);
        b[off + 2] = (byte) (v >> 16);
        b[off + 3] = (byte) (v >> 24);
    }

    private void testMedia() throws Exception {
        byte[] midi = {
            'M', 'T', 'h', 'd', 0, 0, 0, 6, 0, 0, 0, 1, 0, 96,
            'M', 'T', 'r', 'k', 0, 0, 0, 19,
            0, (byte) 0xff, 0x51, 3, 0x07, (byte) 0xa1, 0x20,
            0, (byte) 0x90, 60, 64,
            96, (byte) 0x80, 60, 0,
            0, (byte) 0xff, 0x2f, 0
        };
        Player mp = Manager.createPlayer(new ByteArrayInputStream(midi), null);
        mp.realize();
        check("media midi type", mp.getContentType(), "audio/midi");
        check("media midi duration", mp.getDuration(), 500000);
        mp.close();

        /* WAV, 0.5 s, played twice. */
        Player p = Manager.createPlayer(new ByteArrayInputStream(wav(8000, 4000)), "audio/x-wav");
        Log log = new Log();
        p.addPlayerListener(log);
        check("media state unrealized", p.getState(), Player.UNREALIZED);
        check("media wav duration", p.getDuration(), 500000);
        try {
            p.getContentType();
            check("media unrealized content type", false);
        } catch (IllegalStateException e) {
            check("media unrealized content type", true);
        }
        p.realize();
        check("media state realized", p.getState(), Player.REALIZED);
        VolumeControl vc = (VolumeControl) p.getControl("VolumeControl");
        check("media volume control", vc != null);
        check("media volume clamp", vc.setLevel(150), 100);
        check("media volume level", vc.setLevel(40), 40);
        vc.setMute(true);
        check("media mute", vc.isMuted() && vc.getLevel() == 40);
        check("media no tone control", p.getControl("ToneControl") == null);
        p.prefetch();
        check("media state prefetched", p.getState(), Player.PREFETCHED);
        p.setLoopCount(2);

        /* Unknown media: one simulated second, stopped and resumed. */
        Player u = Manager.createPlayer(new ByteArrayInputStream(new byte[] {1, 2, 3}), "audio/amr");
        Log ulog = new Log();
        u.addPlayerListener(ulog);
        check("media unknown duration", u.getDuration(), Player.TIME_UNKNOWN);

        long t0 = System.currentTimeMillis();
        p.start();
        u.start();
        check("media state started", p.getState(), Player.STARTED);
        try {
            p.setLoopCount(3);
            check("media loop count while started", false);
        } catch (IllegalStateException e) {
            check("media loop count while started", true);
        }
        Thread.sleep(300);
        u.stop();
        long mt = u.getMediaTime();
        check("media stopped time", mt >= 200000 && mt < 600000);
        check("media stopped state", u.getState(), Player.PREFETCHED);
        u.start();
        log.waitEnds(2);
        long elapsed = System.currentTimeMillis() - t0;
        check("media loops took ~1s", elapsed >= 950 && elapsed < 1600);
        check("media end state", p.getState(), Player.PREFETCHED);
        check("media end time", p.getMediaTime(), 500000);
        ulog.waitEnds(1);
        elapsed = System.currentTimeMillis() - t0;
        check("media unknown ~1s", elapsed >= 950 && elapsed < 1600);
        p.close();
        u.deallocate();
        check("media deallocated", u.getState(), Player.REALIZED);
        u.close();
        log.waitClosed();
        ulog.waitClosed();
        check("media events", log.text(),
                "volumeChanged volumeChanged started endOfMedia endOfMedia closed");
        check("media unknown events", ulog.text(), "started stopped started endOfMedia closed");
        check("media closed", p.getState(), Player.CLOSED);
        try {
            p.start();
            check("media start closed", false);
        } catch (IllegalStateException e) {
            check("media start closed", true);
        }

        Player tp = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
        tp.realize();
        ToneControl tc = (ToneControl) tp.getControl("javax.microedition.media.control.ToneControl");
        byte[] seq = {
            ToneControl.VERSION, 1, ToneControl.TEMPO, 30,
            ToneControl.BLOCK_START, 0, ToneControl.C4, 8, 62, 8, ToneControl.BLOCK_END, 0,
            ToneControl.PLAY_BLOCK, 0, ToneControl.PLAY_BLOCK, 0,
            ToneControl.SILENCE, 16, ToneControl.REPEAT, 2, ToneControl.C4, 4
        };
        tc.setSequence(seq);
        check("media tone duration", tp.getDuration(), 1750000);
        try {
            tc.setSequence(new byte[] {ToneControl.VERSION, 1, ToneControl.PLAY_BLOCK, 3});
            check("media bad tone", false);
        } catch (IllegalArgumentException e) {
            check("media bad tone", true);
        }
        tp.close();
        Manager.playTone(ToneControl.C4, 100, 50);
        try {
            Manager.playTone(128, 100, 50);
            check("media bad note", false);
        } catch (IllegalArgumentException e) {
            check("media bad note", true);
        }
        try {
            Manager.createPlayer("http://example.com/a.mid");
            check("media http", false);
        } catch (MediaException e) {
            check("media http", true);
        }
    }

    /* ---- Rendering ---- */

    class Scene extends GameCanvas {
        Scene() {
            super(true);
            setFullScreenMode(true);
        }

        void draw() {
            Graphics g = getGraphics();
            g.setColor(0);
            g.fillRect(0, 0, getWidth(), getHeight());

            /* World of 10x6 tiles seen through a 128x64 window at (16, 8). */
            TiledLayer t = new TiledLayer(10, 6, tiles, 16, 16);
            t.fillCells(0, 0, 10, 6, 1);
            t.fillCells(2, 1, 3, 2, 2);
            int anim = t.createAnimatedTile(3);
            t.setCell(6, 2, anim);
            t.setAnimatedTile(anim, 4);
            t.setCell(1, 3, 0);
            Sprite hero = new Sprite(frames, 16, 16);
            hero.setFrame(3);
            hero.setPosition(40, 24);
            LayerManager lm = new LayerManager();
            lm.append(hero);
            lm.append(t);
            lm.setViewWindow(16, 8, 128, 64);
            g.setClip(4, 4, 200, 100);
            g.translate(2, 3);
            lm.paint(g, 6, 5);
            check("lm restores clip", g.getClipX() * 1000000 + g.getClipY() * 10000
                    + g.getClipWidth() * 100 + g.getClipHeight(), 2010000 + 200 * 100 + 100);
            check("lm restores translation", g.getTranslateX() * 10 + g.getTranslateY(), 23);
            g.translate(-2, -3);
            g.setClip(0, 0, getWidth(), getHeight());

            DirectGraphics dg = DirectUtils.getDirectGraphics(g);
            /* Screen (8, 8) shows world (16, 8): tile 1, whose left column is white. */
            check("lm view pixel", pixel(dg, 8, 8), 0xffffff);
            check("lm view pixel 2", pixel(dg, 9, 9), 0xa52929);
            check("lm outside view", pixel(dg, 7, 7), 0);
            check("lm outside view right", pixel(dg, 8 + 128, 20), 0);
            /* Sprite at world (40, 24) -> screen (32, 24); F stem at (+3, +5). */
            check("lm sprite pixel", pixel(dg, 35, 29), 0xffff00);

            /* The 8 transforms, checked pixel by pixel against the spec. */
            Sprite s = new Sprite(frames, 16, 16);
            int bad = 0;
            for (int tr = 0; tr < 8; tr++) {
                int x = 150 + (tr % 4) * 24;
                int y = 8 + (tr / 4) * 24;
                s.setTransform(tr);
                s.setPosition(x, y);
                s.paint(g);
                bad += mismatches(dg, x, y, tr);
            }
            check("sprite transforms drawn", bad, 0);

            /* DirectGraphics.drawImage with manipulations, same checks. */
            Image f = Image.createImage(frames, 0, 0, 16, 16, 0);
            int[] manip = {
                0, DirectGraphics.FLIP_VERTICAL, DirectGraphics.FLIP_HORIZONTAL,
                DirectGraphics.ROTATE_180, DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_90,
                DirectGraphics.ROTATE_270, DirectGraphics.ROTATE_90,
                DirectGraphics.FLIP_VERTICAL | DirectGraphics.ROTATE_90
            };
            /* Expected Sprite transform for each manipulation above, by index. */
            int[] asTrans = {0, 1, 2, 3, 7, 5, 6, 4};
            bad = 0;
            for (int i = 0; i < 8; i++) {
                int x = 8 + i * 24;
                dg.drawImage(f, x, 80, Graphics.TOP | Graphics.LEFT, manip[i]);
                bad += mismatches(dg, x, 80, asTrans[i]);
            }
            check("nokia drawImage manipulations", bad, 0);

            /* drawPixels: 2x1 red/green blocks, flipped and rotated. */
            int[] rg = {0xffff0000, 0xff00ff00};
            dg.drawPixels(rg, false, 0, 2, 8, 110, 2, 1, 0, DirectGraphics.TYPE_INT_888_RGB);
            dg.drawPixels(rg, false, 0, 2, 12, 110, 2, 1, DirectGraphics.FLIP_HORIZONTAL,
                    DirectGraphics.TYPE_INT_888_RGB);
            dg.drawPixels(rg, false, 0, 2, 16, 110, 2, 1, DirectGraphics.ROTATE_90,
                    DirectGraphics.TYPE_INT_888_RGB);
            check("nokia drawPixels int", pixel(dg, 8, 110), 0xff0000);
            check("nokia drawPixels flip", pixel(dg, 12, 110), 0x00ff00);
            check("nokia drawPixels rot90 top", pixel(dg, 16, 110), 0x00ff00);
            check("nokia drawPixels rot90 bottom", pixel(dg, 16, 111), 0xff0000);

            /* 4444 ARGB shorts: an 8x8 F with transparent background. */
            short[] sp = new short[64];
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    boolean on = x == 1 || (y == 0 && x < 7) || (y == 3 && x < 5);
                    sp[y * 8 + x] = (short) (on ? 0xf0ff : 0x0f00);
                }
            }
            int[] mans = {0, DirectGraphics.FLIP_HORIZONTAL, DirectGraphics.FLIP_VERTICAL,
                DirectGraphics.ROTATE_90, DirectGraphics.ROTATE_180, DirectGraphics.ROTATE_270};
            for (int i = 0; i < mans.length; i++) {
                dg.drawPixels(sp, true, 0, 8, 30 + i * 12, 110, 8, 8, mans[i],
                        DirectGraphics.TYPE_USHORT_4444_ARGB);
            }
            check("nokia 4444 opaque", pixel(dg, 31, 112), 0x00ffff);
            check("nokia 4444 transparent", pixel(dg, 36, 116), 0);
            check("nokia 4444 hflip", pixel(dg, 42 + 6, 112), 0x00ffff);
            check("nokia 4444 vflip", pixel(dg, 54 + 1, 110 + 7), 0x00ffff);

            /* 1-bit gray with a mask: a checkerboard, black squares masked out. */
            byte[] bits = new byte[8];
            byte[] mask = new byte[8];
            for (int i = 0; i < 8; i++) {
                bits[i] = (byte) (i % 2 == 0 ? 0xaa : 0x55);
                mask[i] = (byte) 0xff;
            }
            mask[0] = 0;
            g.setColor(0x404040);
            g.fillRect(110, 108, 12, 12);
            dg.drawPixels(bits, mask, 0, 8, 112, 110, 8, 8, 0, DirectGraphics.TYPE_BYTE_1_GRAY);
            check("nokia 1bit black", pixel(dg, 113, 111), 0);
            check("nokia 1bit white", pixel(dg, 112, 111), 0xffffff);
            check("nokia 1bit masked", pixel(dg, 113, 110), 0x424242);

            /* getPixels in 565 round trip. */
            short[] back = new short[2];
            dg.getPixels(back, 0, 2, 12, 110, 2, 1, DirectGraphics.TYPE_USHORT_565_RGB);
            check("nokia getPixels 565", (back[0] & 0xffff) * 100000L + (back[1] & 0xffff),
                    0x07e0 * 100000L + 0xf800);

            dg.fillPolygon(new int[] {140, 170, 150, 130}, 0, new int[] {108, 115, 135, 120}, 0, 4,
                    0xff8000ff);
            dg.drawTriangle(180, 110, 210, 110, 195, 135, 0xffffffff);
            dg.fillTriangle(215, 110, 245, 110, 230, 135, 0xff00ffff);
            dg.fillTriangle(215, 110, 245, 110, 230, 135, 0x00ff0000);
            check("nokia polygon inside", pixel(dg, 150, 118), 0x8400ff);
            check("nokia transparent triangle", pixel(dg, 230, 115), 0x00ffff);
            g.setColor(0xffffff);
            g.drawString("Nano Java game/rms/media/nokia", 4, 150, Graphics.TOP | Graphics.LEFT);
            flushGraphics();
        }

        /* Compares the 16x16 area at (x, y) with frame 0 of frames.png
         * drawn with Sprite transform tr; returns the mismatch count. */
        int mismatches(DirectGraphics dg, int x, int y, int tr) {
            int[] got = new int[256];
            dg.getPixels(got, 0, 16, x, y, 16, 16, DirectGraphics.TYPE_INT_888_RGB);
            int bad = 0;
            for (int py = 0; py < 16; py++) {
                for (int px = 0; px < 16; px++) {
                    boolean on = (px >= 3 && px <= 5 && py >= 2 && py <= 13)
                            || (px >= 3 && px <= 12 && py >= 2 && py <= 4)
                            || (px >= 3 && px <= 9 && py >= 7 && py <= 9);
                    int dx, dy;
                    switch (tr) {
                    case Sprite.TRANS_NONE: dx = px; dy = py; break;
                    case Sprite.TRANS_ROT90: dx = 15 - py; dy = px; break;
                    case Sprite.TRANS_ROT180: dx = 15 - px; dy = 15 - py; break;
                    case Sprite.TRANS_ROT270: dx = py; dy = 15 - px; break;
                    case Sprite.TRANS_MIRROR: dx = 15 - px; dy = py; break;
                    case Sprite.TRANS_MIRROR_ROT90: dx = 15 - py; dy = 15 - px; break;
                    case Sprite.TRANS_MIRROR_ROT180: dx = px; dy = 15 - py; break;
                    default: dx = py; dy = px; break;
                    }
                    int want = on ? 0xff0000 : 0;
                    if ((got[dy * 16 + dx] & 0xffffff) != want) {
                        bad++;
                    }
                }
            }
            return bad;
        }

        int pixel(DirectGraphics dg, int x, int y) {
            int[] p = new int[1];
            dg.getPixels(p, 0, 1, x, y, 1, 1, DirectGraphics.TYPE_INT_888_RGB);
            return p[0];
        }
    }
}
