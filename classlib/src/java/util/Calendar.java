package java.util;

public abstract class Calendar {
    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DATE = 5;
    public static final int DAY_OF_MONTH = 5;
    public static final int DAY_OF_WEEK = 7;
    public static final int AM_PM = 9;
    public static final int HOUR = 10;
    public static final int HOUR_OF_DAY = 11;
    public static final int MINUTE = 12;
    public static final int SECOND = 13;
    public static final int MILLISECOND = 14;

    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    public static final int THURSDAY = 5;
    public static final int FRIDAY = 6;
    public static final int SATURDAY = 7;

    public static final int JANUARY = 0;
    public static final int FEBRUARY = 1;
    public static final int MARCH = 2;
    public static final int APRIL = 3;
    public static final int MAY = 4;
    public static final int JUNE = 5;
    public static final int JULY = 6;
    public static final int AUGUST = 7;
    public static final int SEPTEMBER = 8;
    public static final int OCTOBER = 9;
    public static final int NOVEMBER = 10;
    public static final int DECEMBER = 11;

    public static final int AM = 0;
    public static final int PM = 1;

    private static final int FIELD_COUNT = 15;

    protected int[] fields = new int[FIELD_COUNT];
    protected boolean[] isSet = new boolean[FIELD_COUNT];
    protected long time;

    /* Order in which fields were set; lets computeTime pick HOUR vs HOUR_OF_DAY. */
    int[] stamp = new int[FIELD_COUNT];
    private int nextStamp = 1;

    /* When false, the fields are authoritative and time must be recomputed. */
    private boolean isTimeSet;
    private TimeZone zone;

    protected Calendar() {
        zone = TimeZone.getDefault();
    }

    public final Date getTime() {
        return new Date(getTimeInMillis());
    }

    public final void setTime(Date date) {
        setTimeInMillis(date.getTime());
    }

    public static synchronized Calendar getInstance() {
        return getInstance(TimeZone.getDefault());
    }

    public static synchronized Calendar getInstance(TimeZone zone) {
        Calendar c = new GregorianCalendarImpl();
        c.zone = zone;
        c.setTimeInMillis(System.currentTimeMillis());
        return c;
    }

    protected long getTimeInMillis() {
        if (!isTimeSet) {
            computeTime();
            isTimeSet = true;
            computeFields();
            setAllFlags();
        }
        return time;
    }

    protected void setTimeInMillis(long millis) {
        time = millis;
        isTimeSet = true;
        computeFields();
        setAllFlags();
    }

    private void setAllFlags() {
        for (int i = 0; i < FIELD_COUNT; i++) {
            isSet[i] = true;
            stamp[i] = 0;
        }
        nextStamp = 1;
    }

    public final int get(int field) {
        getTimeInMillis();
        return fields[field];
    }

    public final void set(int field, int value) {
        fields[field] = value;
        isSet[field] = true;
        stamp[field] = nextStamp++;
        isTimeSet = false;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Calendar)) {
            return false;
        }
        Calendar that = (Calendar) obj;
        return getTimeInMillis() == that.getTimeInMillis()
                && zone.getRawOffset() == that.zone.getRawOffset()
                && String.valueOf(zone.getID()).equals(String.valueOf(that.zone.getID()));
    }

    public int hashCode() {
        long t = getTimeInMillis();
        return (int) t ^ (int) (t >> 32);
    }

    public boolean before(Object when) {
        return when instanceof Calendar && getTimeInMillis() < ((Calendar) when).getTimeInMillis();
    }

    public boolean after(Object when) {
        return when instanceof Calendar && getTimeInMillis() > ((Calendar) when).getTimeInMillis();
    }

    public void setTimeZone(TimeZone value) {
        // Keep the instant; recompute the local fields for the new zone.
        long t = getTimeInMillis();
        zone = value;
        setTimeInMillis(t);
    }

    public TimeZone getTimeZone() {
        return zone;
    }

    protected abstract void computeFields();

    protected abstract void computeTime();
}
