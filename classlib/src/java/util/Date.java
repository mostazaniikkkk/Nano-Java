package java.util;

public class Date {
    private static final String[] DAYS = {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };
    private static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private long millis;

    public Date() {
        this(System.currentTimeMillis());
    }

    public Date(long date) {
        millis = date;
    }

    public long getTime() {
        return millis;
    }

    public void setTime(long time) {
        millis = time;
    }

    public boolean equals(Object obj) {
        return obj instanceof Date && ((Date) obj).millis == millis;
    }

    public int hashCode() {
        return (int) millis ^ (int) (millis >> 32);
    }

    /* Formats as "EEE MMM dd HH:mm:ss zzz yyyy" in the default time zone. */
    public String toString() {
        TimeZone tz = TimeZone.getDefault();
        Calendar c = Calendar.getInstance(tz);
        c.setTime(this);
        StringBuffer sb = new StringBuffer(28);
        sb.append(DAYS[c.get(Calendar.DAY_OF_WEEK) - 1]).append(' ');
        sb.append(MONTHS[c.get(Calendar.MONTH)]).append(' ');
        pad2(sb, c.get(Calendar.DAY_OF_MONTH));
        sb.append(' ');
        pad2(sb, c.get(Calendar.HOUR_OF_DAY));
        sb.append(':');
        pad2(sb, c.get(Calendar.MINUTE));
        sb.append(':');
        pad2(sb, c.get(Calendar.SECOND));
        sb.append(' ').append(tz.getID()).append(' ');
        sb.append(c.get(Calendar.YEAR));
        return sb.toString();
    }

    private static void pad2(StringBuffer sb, int v) {
        if (v < 10) {
            sb.append('0');
        }
        sb.append(v);
    }
}
