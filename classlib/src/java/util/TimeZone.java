package java.util;

/*
 * CLDC time zones. Only fixed-offset zones exist: "GMT", "UTC" and custom
 * ids of the form "GMT+hh:mm" / "GMT-hhmm". The default zone is GMT.
 */
public abstract class TimeZone {
    private static TimeZone defaultZone;

    private String id;

    public TimeZone() {
    }

    public abstract int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis);

    public abstract int getRawOffset();

    public abstract boolean useDaylightTime();

    public String getID() {
        return id;
    }

    void setID(String id) {
        this.id = id;
    }

    public static synchronized TimeZone getTimeZone(String ID) {
        if (ID == null) {
            throw new NullPointerException();
        }
        if (ID.equals("GMT") || ID.equals("UTC")) {
            return new FixedTimeZone(ID, 0);
        }
        TimeZone tz = parseCustom(ID);
        return tz != null ? tz : new FixedTimeZone("GMT", 0);
    }

    /* Parses "GMT+h", "GMT+hh", "GMT+hhmm" and "GMT+hh:mm" (or "-"). */
    private static TimeZone parseCustom(String id) {
        if (!id.startsWith("GMT") || id.length() < 5) {
            return null;
        }
        char sign = id.charAt(3);
        if (sign != '+' && sign != '-') {
            return null;
        }
        String s = id.substring(4);
        int hours;
        int minutes = 0;
        try {
            int colon = s.indexOf(':');
            if (colon >= 0) {
                if (colon == 0 || colon > 2 || s.length() - colon != 3) {
                    return null;
                }
                hours = parseDigits(s.substring(0, colon));
                minutes = parseDigits(s.substring(colon + 1));
            } else if (s.length() <= 2) {
                hours = parseDigits(s);
            } else if (s.length() == 4) {
                hours = parseDigits(s.substring(0, 2));
                minutes = parseDigits(s.substring(2));
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (hours > 23 || minutes > 59) {
            return null;
        }
        StringBuffer sb = new StringBuffer("GMT");
        sb.append(sign);
        if (hours < 10) {
            sb.append('0');
        }
        sb.append(hours).append(':');
        if (minutes < 10) {
            sb.append('0');
        }
        sb.append(minutes);
        int offset = (hours * 60 + minutes) * 60000;
        return new FixedTimeZone(sb.toString(), sign == '-' ? -offset : offset);
    }

    private static int parseDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                throw new NumberFormatException(s);
            }
        }
        return Integer.parseInt(s);
    }

    public static synchronized TimeZone getDefault() {
        if (defaultZone == null) {
            defaultZone = new FixedTimeZone("GMT", 0);
        }
        return defaultZone;
    }

    public static String[] getAvailableIDs() {
        String[] ids = new String[2];
        ids[0] = "GMT";
        ids[1] = "UTC";
        return ids;
    }
}
