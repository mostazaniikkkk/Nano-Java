package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface VolumeControl extends Control {
    void setMute(boolean mute);

    boolean isMuted();

    int setLevel(int level);

    int getLevel();
}
