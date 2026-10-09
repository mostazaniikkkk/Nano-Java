package com.nokia.mid.ui;

/* No lights or vibration on the DS: all calls are accepted and ignored. */
public class DeviceControl {
    private DeviceControl() {
    }

    public static void setLights(int num, int level) {
        if (level < 0 || level > 100) {
            throw new IllegalArgumentException();
        }
    }

    public static void flashLights(long duration) {
        if (duration < 0) {
            throw new IllegalArgumentException();
        }
    }

    public static void startVibra(int freq, long duration) {
        if (freq < 0 || freq > 100 || duration < 0) {
            throw new IllegalArgumentException();
        }
    }

    public static void stopVibra() {
    }
}
