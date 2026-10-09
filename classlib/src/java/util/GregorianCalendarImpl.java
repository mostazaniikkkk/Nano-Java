package java.util;

/*
 * The calendar returned by Calendar.getInstance(): proleptic Gregorian,
 * lenient (out-of-range field values roll over into larger fields).
 */
final class GregorianCalendarImpl extends Calendar {
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    GregorianCalendarImpl() {
    }

    protected void computeFields() {
        TimeZone tz = getTimeZone();
        int offset = tz.getRawOffset();
        fillFields(time + offset);
        if (tz.useDaylightTime()) {
            int full = tz.getOffset(1, fields[YEAR], fields[MONTH], fields[DATE],
                    fields[DAY_OF_WEEK], millisInDay());
            if (full != offset) {
                fillFields(time + full);
            }
        }
    }

    private int millisInDay() {
        return ((fields[HOUR_OF_DAY] * 60 + fields[MINUTE]) * 60 + fields[SECOND]) * 1000
                + fields[MILLISECOND];
    }

    private void fillFields(long local) {
        long days = floorDiv(local, DAY_MS);
        int ms = (int) (local - days * DAY_MS);

        // Civil-from-days (Howard Hinnant's algorithm).
        long z = days + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        int day = (int) (doy - (153 * mp + 2) / 5 + 1);
        int month = (int) (mp < 10 ? mp + 3 : mp - 9);
        int year = (int) (yoe + era * 400 + (month <= 2 ? 1 : 0));

        fields[YEAR] = year;
        fields[MONTH] = month - 1;
        fields[DATE] = day;
        fields[DAY_OF_WEEK] = (int) floorMod(days + 4, 7) + 1;
        int hourOfDay = ms / 3600000;
        fields[HOUR_OF_DAY] = hourOfDay;
        fields[AM_PM] = hourOfDay < 12 ? AM : PM;
        fields[HOUR] = hourOfDay % 12;
        fields[MINUTE] = (ms / 60000) % 60;
        fields[SECOND] = (ms / 1000) % 60;
        fields[MILLISECOND] = ms % 1000;
    }

    protected void computeTime() {
        long year = fields[YEAR];
        long month = fields[MONTH];
        year += floorDiv(month, 12);
        int m = (int) floorMod(month, 12) + 1;

        // Days-from-civil (Howard Hinnant's algorithm), day of month added linearly.
        long y = m <= 2 ? year - 1 : year;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m > 2 ? m - 3 : m + 9) + 2) / 5;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long days = era * 146097 + doe - 719468 + (fields[DATE] - 1);

        // A DAY_OF_WEEK set after the date moves to that day of the same week.
        int dateStamp = Math.max(stamp[DATE], Math.max(stamp[MONTH], stamp[YEAR]));
        if (stamp[DAY_OF_WEEK] > dateStamp) {
            int current = (int) floorMod(days + 4, 7) + 1;
            days += fields[DAY_OF_WEEK] - current;
        }

        long hourOfDay;
        if (Math.max(stamp[HOUR], stamp[AM_PM]) > stamp[HOUR_OF_DAY]) {
            hourOfDay = fields[AM_PM] * 12L + fields[HOUR];
        } else {
            hourOfDay = fields[HOUR_OF_DAY];
        }
        long local = days * DAY_MS
                + ((hourOfDay * 60 + fields[MINUTE]) * 60 + fields[SECOND]) * 1000
                + fields[MILLISECOND];

        TimeZone tz = getTimeZone();
        int offset = tz.getRawOffset();
        if (tz.useDaylightTime()) {
            fillFields(local);
            offset = tz.getOffset(1, fields[YEAR], fields[MONTH], fields[DATE],
                    fields[DAY_OF_WEEK], millisInDay());
        }
        time = local - offset;
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        return (a % b != 0 && ((a < 0) != (b < 0))) ? q - 1 : q;
    }

    private static long floorMod(long a, long b) {
        return a - floorDiv(a, b) * b;
    }
}
