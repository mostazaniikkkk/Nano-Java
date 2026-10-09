package javax.microedition.lcdui;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/*
 * Shown as "yyyy-mm-dd hh:mm". While focused one part is highlighted:
 * left/right change it, fire moves to the next part.
 */
public class DateField extends Item {
    public static final int DATE = 1;
    public static final int TIME = 2;
    public static final int DATE_TIME = 3;

    private static final int[] DATE_FIELDS = {Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH};
    private static final int[] TIME_FIELDS = {Calendar.HOUR_OF_DAY, Calendar.MINUTE};
    private static final int[] ALL_FIELDS = {Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH,
        Calendar.HOUR_OF_DAY, Calendar.MINUTE};

    private int mode;
    private final Calendar cal;
    private boolean set;
    private int part;
    private int[] fields;
    /* Formatted text and the character range of each part. */
    private String text;
    private final int[] partStart = new int[5];
    private final int[] partEnd = new int[5];

    public DateField(String label, int mode) {
        this(label, mode, null);
    }

    public DateField(String label, int mode, TimeZone timeZone) {
        super(label);
        cal = Calendar.getInstance(timeZone != null ? timeZone : TimeZone.getDefault());
        setInputMode(mode);
    }

    public Date getDate() {
        return set ? cal.getTime() : null;
    }

    public void setDate(Date date) {
        if (date == null) {
            set = false;
        } else {
            cal.setTime(date);
            set = true;
            normalize();
        }
        format();
        repaint0();
    }

    public int getInputMode() {
        return mode;
    }

    public void setInputMode(int mode) {
        if (mode < DATE || mode > DATE_TIME) {
            throw new IllegalArgumentException();
        }
        this.mode = mode;
        fields = mode == DATE ? DATE_FIELDS : mode == TIME ? TIME_FIELDS : ALL_FIELDS;
        part = 0;
        if (set) {
            normalize();
        }
        format();
        invalidate0();
    }

    /* TIME values live on January 1, 1970. */
    private void normalize() {
        if (mode == TIME) {
            cal.set(Calendar.YEAR, 1970);
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private static void two(StringBuffer sb, int v) {
        if (v < 10) {
            sb.append('0');
        }
        sb.append(v);
    }

    private void format() {
        StringBuffer sb = new StringBuffer(16);
        for (int i = 0; i < fields.length; i++) {
            int f = fields[i];
            if (i > 0) {
                sb.append(f == Calendar.HOUR_OF_DAY ? ' ' : f == Calendar.MINUTE ? ':' : '-');
            }
            partStart[i] = sb.length();
            if (!set) {
                sb.append(f == Calendar.YEAR ? "----" : "--");
            } else if (f == Calendar.YEAR) {
                sb.append(cal.get(f));
            } else {
                two(sb, cal.get(f) + (f == Calendar.MONTH ? 1 : 0));
            }
            partEnd[i] = sb.length();
        }
        text = sb.toString();
    }

    private static int daysIn(int year, int month) {
        switch (month) {
        case 1:
            return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0 ? 29 : 28;
        case 3: case 5: case 8: case 10:
            return 30;
        default:
            return 31;
        }
    }

    private static int wrap(int v, int lo, int hi) {
        if (v < lo) {
            return hi;
        }
        return v > hi ? lo : v;
    }

    private void adjust(int delta) {
        if (!set) {
            cal.setTime(new Date());
            set = true;
            normalize();
        } else {
            int f = fields[part];
            int v = cal.get(f) + delta;
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            switch (f) {
            case Calendar.YEAR: v = wrap(v, 1900, 2099); break;
            case Calendar.MONTH: v = wrap(v, 0, 11); break;
            case Calendar.DAY_OF_MONTH: v = wrap(v, 1, daysIn(year, month)); break;
            case Calendar.HOUR_OF_DAY: v = wrap(v, 0, 23); break;
            default: v = wrap(v, 0, 59); break;
            }
            int day = cal.get(Calendar.DAY_OF_MONTH);
            cal.set(f, v);
            if (f == Calendar.YEAR || f == Calendar.MONTH) {
                int max = daysIn(f == Calendar.YEAR ? v : year, f == Calendar.MONTH ? v : month);
                if (day > max) {
                    cal.set(Calendar.DAY_OF_MONTH, max);
                }
            }
        }
        format();
        repaint0();
        notifyStateChanged();
    }

    /* ---- Item protocol ---- */

    boolean isFocusable() {
        return true;
    }

    int measureBody(int w) {
        return Theme.FONT.getHeight() + 6;
    }

    void paintBody(Graphics g, int w, int h, boolean focused) {
        Font f = Theme.FONT;
        String t = text;
        g.setColor(Theme.FIELD_BG);
        g.fillRect(0, 0, w, h);
        g.setColor(focused ? Theme.HIGHLIGHT : Theme.BORDER);
        g.drawRect(0, 0, w - 1, h - 1);
        g.setFont(f);
        g.setColor(Theme.FOREGROUND);
        g.drawString(t, 4, 3, Graphics.TOP | Graphics.LEFT);
        if (focused) {
            int s = partStart[part];
            int len = partEnd[part] - s;
            int x = 4 + f.substringWidth(t, 0, s);
            g.setColor(Theme.HIGHLIGHT);
            g.fillRect(x - 1, 2, f.substringWidth(t, s, len) + 2, h - 4);
            g.setColor(Theme.HIGHLIGHT_TEXT);
            g.drawSubstring(t, s, len, x, 3, Graphics.TOP | Graphics.LEFT);
        }
    }

    boolean itemKey(int type, int code) {
        if (type == nanojava.Events.KEY_UP) {
            return false;
        }
        int action = Canvas.gameAction(code);
        if (action == Canvas.LEFT || action == Canvas.RIGHT) {
            adjust(action == Canvas.LEFT ? -1 : 1);
            return true;
        }
        if (action == Canvas.FIRE && type == nanojava.Events.KEY_DOWN && activationCommand() == null) {
            part = (part + 1) % fields.length;
            repaint0();
            return true;
        }
        return false;
    }

    boolean itemPointer(int type, int x, int y) {
        if (type == nanojava.Events.POINTER_UP) {
            /* Tap selects the part under the pointer, or bumps it. */
            Font f = Theme.FONT;
            for (int i = 0; i < fields.length; i++) {
                int x2 = 4 + f.substringWidth(text, 0, partEnd[i]);
                if (x < x2 + 3 || i == fields.length - 1) {
                    if (i == part) {
                        adjust(1);
                    } else {
                        part = i;
                        repaint0();
                    }
                    break;
                }
            }
        }
        return true;
    }
}
