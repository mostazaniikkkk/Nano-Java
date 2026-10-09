package javax.microedition.media;

import java.util.Vector;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.control.VolumeControl;

/*
 * The Player state machine. Playback goes to nanojava.Audio when a backend
 * exists; either way the media clock is simulated here so that games see
 * STARTED/END_OF_MEDIA at plausible times. Media of unknown length "plays"
 * for one second per loop. Listener events go through one dispatcher
 * thread, in order.
 */
class PlayerImpl implements Player {
    private static final long UNKNOWN_LENGTH = 1000000;

    private final String contentType;
    private byte[] data;
    private final boolean tonePlayer;
    private int state = UNREALIZED;
    private long duration;
    private int loopCount = 1;
    private int loopsDone;
    /* Media time while not started; at startedAt while started. */
    private long mediaTime;
    private long startedAt;
    long endAt;
    private int handle;
    private final Vector listeners = new Vector();
    private final Volume volume = new Volume();
    private Tone tone;

    PlayerImpl(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
        tonePlayer = MediaInfo.TONE.equals(contentType);
        if (tonePlayer) {
            tone = new Tone();
        }
        duration = data == null && tonePlayer ? 0 : MediaInfo.duration(data, contentType);
    }

    /* ---- Controls ---- */

    final class Volume implements VolumeControl {
        private int level = 100;
        private boolean muted;

        public void setMute(boolean mute) {
            synchronized (PlayerImpl.this) {
                if (mute == muted) {
                    return;
                }
                muted = mute;
                apply();
            }
            post(PlayerListener.VOLUME_CHANGED, this);
        }

        public boolean isMuted() {
            return muted;
        }

        public int setLevel(int level) {
            if (level < 0) {
                level = 0;
            } else if (level > 100) {
                level = 100;
            }
            synchronized (PlayerImpl.this) {
                if (level == this.level) {
                    return level;
                }
                this.level = level;
                apply();
            }
            post(PlayerListener.VOLUME_CHANGED, this);
            return level;
        }

        public int getLevel() {
            return level;
        }

        void apply() {
            if (handle != 0) {
                nanojava.Audio.setVolume(handle, muted ? 0 : level);
            }
        }
    }

    final class Tone implements ToneControl {
        public void setSequence(byte[] sequence) {
            synchronized (PlayerImpl.this) {
                if (state == PREFETCHED || state == STARTED) {
                    throw new IllegalStateException();
                }
                long d = MediaInfo.tone(sequence);
                byte[] s = new byte[sequence.length];
                System.arraycopy(sequence, 0, s, 0, s.length);
                data = s;
                duration = d;
                if (handle != 0) {
                    nanojava.Audio.close(handle);
                    handle = 0;
                }
            }
        }
    }

    public Control[] getControls() {
        checkRealized();
        if (tonePlayer) {
            return new Control[] {volume, tone};
        }
        return new Control[] {volume};
    }

    public Control getControl(String controlType) {
        checkRealized();
        if (controlType == null) {
            throw new IllegalArgumentException();
        }
        String t = controlType;
        if (t.startsWith("javax.microedition.media.control.")) {
            t = t.substring("javax.microedition.media.control.".length());
        }
        if (t.equals("VolumeControl")) {
            return volume;
        }
        if (t.equals("ToneControl") && tonePlayer) {
            return tone;
        }
        return null;
    }

    /* ---- State machine ---- */

    private void checkClosed() {
        if (state == CLOSED) {
            throw new IllegalStateException("player is closed");
        }
    }

    private synchronized void checkRealized() {
        checkClosed();
        if (state == UNREALIZED) {
            throw new IllegalStateException("player is not realized");
        }
    }

    /* Simulated play length of one loop, in microseconds. */
    private long length() {
        return duration != TIME_UNKNOWN ? duration : UNKNOWN_LENGTH;
    }

    private long currentTime(long now) {
        long t = mediaTime;
        if (state == STARTED) {
            t += (now - startedAt) * 1000;
        }
        return Math.min(t, length());
    }

    public synchronized void realize() throws MediaException {
        checkClosed();
        if (state == UNREALIZED) {
            state = REALIZED;
        }
    }

    public synchronized void prefetch() throws MediaException {
        realize();
        if (state == REALIZED) {
            if (handle == 0 && data != null && nanojava.Audio.available()) {
                handle = nanojava.Audio.open(data, contentType);
                if (handle != 0) {
                    volume.apply();
                    long d = nanojava.Audio.duration(handle);
                    if (d >= 0 && d != duration) {
                        duration = d;
                        post(PlayerListener.DURATION_UPDATED, new Long(d));
                    }
                }
            }
            state = PREFETCHED;
        }
    }

    public synchronized void start() throws MediaException {
        prefetch();
        if (state == STARTED) {
            return;
        }
        if (mediaTime >= length()) {
            mediaTime = 0;
        }
        state = STARTED;
        startedAt = System.currentTimeMillis();
        if (handle != 0) {
            nanojava.Audio.start(handle, mediaTime);
        }
        post(PlayerListener.STARTED, new Long(mediaTime));
        Dispatcher.activate(this, startedAt + (length() - mediaTime + 999) / 1000);
    }

    public synchronized void stop() throws MediaException {
        checkClosed();
        if (state == STARTED) {
            halt();
            post(PlayerListener.STOPPED, new Long(mediaTime));
        }
    }

    private void halt() {
        mediaTime = currentTime(System.currentTimeMillis());
        state = PREFETCHED;
        Dispatcher.deactivate(this);
        if (handle != 0) {
            nanojava.Audio.stop(handle);
        }
    }

    public synchronized void deallocate() {
        checkClosed();
        if (state == STARTED) {
            halt();
            post(PlayerListener.STOPPED, new Long(mediaTime));
        }
        if (state == PREFETCHED) {
            if (handle != 0) {
                nanojava.Audio.close(handle);
                handle = 0;
            }
            state = REALIZED;
        }
    }

    public synchronized void close() {
        if (state == CLOSED) {
            return;
        }
        if (state == STARTED) {
            halt();
        }
        if (handle != 0) {
            nanojava.Audio.close(handle);
            handle = 0;
        }
        state = CLOSED;
        data = null;
        post(PlayerListener.CLOSED, null);
    }

    public synchronized long setMediaTime(long now) throws MediaException {
        checkRealized();
        if (now < 0) {
            now = 0;
        }
        if (duration != TIME_UNKNOWN && now > duration) {
            now = duration;
        }
        mediaTime = now;
        if (state == STARTED) {
            startedAt = System.currentTimeMillis();
            if (handle != 0) {
                nanojava.Audio.start(handle, now);
            }
            Dispatcher.activate(this, startedAt + (length() - now + 999) / 1000);
        }
        return now;
    }

    public synchronized long getMediaTime() {
        checkClosed();
        return currentTime(System.currentTimeMillis());
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized long getDuration() {
        checkClosed();
        return duration;
    }

    public synchronized String getContentType() {
        checkRealized();
        return contentType;
    }

    public synchronized void setLoopCount(int count) {
        checkClosed();
        if (state == STARTED) {
            throw new IllegalStateException();
        }
        if (count == 0 || count < -1) {
            throw new IllegalArgumentException();
        }
        loopCount = count;
        loopsDone = 0;
    }

    public void addPlayerListener(PlayerListener l) {
        synchronized (this) {
            checkClosed();
        }
        if (l != null && !listeners.contains(l)) {
            listeners.addElement(l);
        }
    }

    public void removePlayerListener(PlayerListener l) {
        synchronized (this) {
            checkClosed();
        }
        listeners.removeElement(l);
    }

    /* Called by the dispatcher once the current loop should have ended. */
    synchronized void tick(long now) {
        if (state != STARTED || now < endAt) {
            return;
        }
        long len = length();
        loopsDone++;
        if (loopCount == -1 || loopsDone < loopCount) {
            post(PlayerListener.END_OF_MEDIA, new Long(len));
            mediaTime = 0;
            startedAt = now;
            if (handle != 0) {
                nanojava.Audio.start(handle, 0);
            }
            Dispatcher.activate(this, now + (len + 999) / 1000);
        } else {
            loopsDone = 0;
            mediaTime = len;
            state = PREFETCHED;
            Dispatcher.deactivate(this);
            if (handle != 0) {
                nanojava.Audio.stop(handle);
            }
            post(PlayerListener.END_OF_MEDIA, new Long(len));
        }
    }

    private void post(String event, Object data) {
        Dispatcher.post(this, event, data);
    }

    void deliver(String event, Object data) {
        Object[] l;
        synchronized (listeners) {
            l = new Object[listeners.size()];
            listeners.copyInto(l);
        }
        for (int i = 0; i < l.length; i++) {
            try {
                ((PlayerListener) l[i]).playerUpdate(this, event, data);
            } catch (Throwable e) {
                System.out.println("PlayerListener: " + e);
            }
        }
    }

    /* ---- Event and timing thread ---- */

    static final class Dispatcher implements Runnable {
        private static final long IDLE_MS = 3000;
        private static final Vector events = new Vector();
        private static final Vector active = new Vector();
        private static Thread thread;

        static void post(PlayerImpl p, String event, Object data) {
            synchronized (events) {
                events.addElement(new Object[] {p, event, data});
                wake();
            }
        }

        static void activate(PlayerImpl p, long endAt) {
            synchronized (events) {
                p.endAt = endAt;
                if (!active.contains(p)) {
                    active.addElement(p);
                }
                wake();
            }
        }

        static void deactivate(PlayerImpl p) {
            synchronized (events) {
                active.removeElement(p);
            }
        }

        private static void wake() {
            if (thread == null) {
                thread = new Thread(new Dispatcher(), "media");
                thread.start();
            } else {
                events.notifyAll();
            }
        }

        public void run() {
            boolean idle = false;
            for (;;) {
                Object[] ev = null;
                Vector due = null;
                long now = 0;
                synchronized (events) {
                    for (;;) {
                        if (events.size() > 0) {
                            ev = (Object[]) events.elementAt(0);
                            events.removeElementAt(0);
                            break;
                        }
                        now = System.currentTimeMillis();
                        long next = Long.MAX_VALUE;
                        for (int i = 0; i < active.size(); i++) {
                            PlayerImpl p = (PlayerImpl) active.elementAt(i);
                            if (p.endAt <= now) {
                                if (due == null) {
                                    due = new Vector();
                                }
                                due.addElement(p);
                            } else if (p.endAt < next) {
                                next = p.endAt;
                            }
                        }
                        if (due != null) {
                            break;
                        }
                        if (active.size() == 0 && idle) {
                            thread = null;
                            return;
                        }
                        idle = active.size() == 0;
                        try {
                            events.wait(idle ? IDLE_MS : next - now);
                        } catch (InterruptedException e) {
                            /* Keep running. */
                        }
                    }
                }
                idle = false;
                if (ev != null) {
                    ((PlayerImpl) ev[0]).deliver((String) ev[1], ev[2]);
                }
                if (due != null) {
                    for (int i = 0; i < due.size(); i++) {
                        ((PlayerImpl) due.elementAt(i)).tick(now);
                    }
                }
            }
        }
    }
}
