package java.util;

/* A time zone with a constant offset from UTC and no daylight saving. */
final class FixedTimeZone extends TimeZone {
    private final int rawOffset;

    FixedTimeZone(String id, int rawOffset) {
        this.rawOffset = rawOffset;
        setID(id);
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis) {
        return rawOffset;
    }

    public int getRawOffset() {
        return rawOffset;
    }

    public boolean useDaylightTime() {
        return false;
    }
}
