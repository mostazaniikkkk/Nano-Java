package javax.microedition.lcdui;

/*
 * Image (or a type icon), wrapped text and an optional indicator gauge,
 * centered. A timed alert schedules DISMISS_COMMAND when shown. Commands
 * go to the application's listener if it set one; otherwise any command
 * dismisses the alert and shows returnTo.
 */
public class Alert extends Screen {
    public static final int FOREVER = -2;
    public static final Command DISMISS_COMMAND = new Command("Done", Command.OK, 0);

    private static final int DEFAULT_TIMEOUT = 2000;
    private static final int ICON = 24;

    /* Set by Display.setCurrent(Alert, Displayable). */
    Displayable returnTo;

    private String text;
    private Image image;
    private AlertType type;
    private int timeout = DEFAULT_TIMEOUT;
    private Gauge indicator;
    private final TextWrap wrap = new TextWrap();
    private int scroll;
    private int contentTotal;
    /* Bumped on every show/hide so stale timers do nothing. */
    private int generation;

    public Alert(String title) {
        this(title, null, null, null);
    }

    public Alert(String title, String alertText, Image alertImage, AlertType alertType) {
        this.title = title;
        text = alertText;
        image = alertImage;
        type = alertType;
        super.addCommand(DISMISS_COMMAND);
    }

    public int getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int time) {
        if (time <= 0 && time != FOREVER) {
            throw new IllegalArgumentException();
        }
        timeout = time;
        if (shown) {
            startTimer();
        }
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
        repaintDisplayable();
    }

    public String getString() {
        return text;
    }

    public void setString(String str) {
        text = str;
        repaintDisplayable();
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image img) {
        image = img;
        repaintDisplayable();
    }

    public void setIndicator(Gauge indicator) {
        if (indicator != null && (indicator.interactive || indicator.owner != null
                || indicator.commands.size() > 0)) {
            throw new IllegalArgumentException();
        }
        if (this.indicator != null) {
            this.indicator.owner = null;
        }
        this.indicator = indicator;
        if (indicator != null) {
            indicator.owner = this;
        }
        repaintDisplayable();
    }

    public Gauge getIndicator() {
        return indicator;
    }

    public void addCommand(Command cmd) {
        if (cmd == null) {
            throw new NullPointerException();
        }
        if (cmd == DISMISS_COMMAND) {
            return;
        }
        commands.removeElement(DISMISS_COMMAND);
        super.addCommand(cmd);
    }

    public void removeCommand(Command cmd) {
        if (cmd == DISMISS_COMMAND) {
            return;
        }
        super.removeCommand(cmd);
        if (commands.size() == 0) {
            super.addCommand(DISMISS_COMMAND);
        }
    }

    /* ---- Screen hooks ---- */

    /* With two or more application commands the alert is modal. */
    private int effectiveTimeout() {
        return commands.size() > 1 ? FOREVER : timeout;
    }

    private void startTimer() {
        final int gen = ++generation;
        final int t = effectiveTimeout();
        if (t == FOREVER) {
            return;
        }
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(t);
                } catch (InterruptedException e) {
                    return;
                }
                Display.get().callSerially(new Runnable() {
                    public void run() {
                        if (gen == generation && shown) {
                            fireCommand(DISMISS_COMMAND);
                        }
                    }
                });
            }
        }.start();
    }

    void showNotify0() {
        scroll = 0;
        startTimer();
    }

    void hideNotify0() {
        generation++;
    }

    void fireCommand(Command c) {
        CommandListener l = listener;
        if (l != null) {
            l.commandAction(c, this);
            return;
        }
        Displayable next = returnTo;
        if (next != null && shown) {
            Display.get().show(next);
        }
    }

    void paintContent(Graphics g, int w, int h) {
        Font f = Theme.FONT;
        int fh = f.getHeight();
        int y = 8 - scroll;
        Image img = image;
        int kind = type != null ? type.kind : 0;
        if (img != null) {
            g.drawImage(img, w / 2, y, Graphics.TOP | Graphics.HCENTER);
            y += img.getHeight() + 6;
        } else if (kind != 0) {
            paintIcon(g, (w - ICON) / 2, y, kind);
            y += ICON + 6;
        }
        String t = text;
        if (t != null) {
            int lines = wrap.wrap(t, f, w - 16);
            g.setFont(f);
            g.setColor(Theme.FOREGROUND);
            wrap.paint(g, w / 2, y, fh, 0, lines, true);
            y += lines * fh + 6;
        }
        Gauge gauge = indicator;
        if (gauge != null) {
            int gh = gauge.layout(w - 16);
            g.translate(8, y);
            gauge.paintItem(g, w - 16, false);
            g.translate(-8, -y);
            y += gh;
        }
        contentTotal = y + scroll;
        if (contentTotal > h) {
            int bh = Math.max(10, h * h / contentTotal);
            int by = scroll * (h - bh) / (contentTotal - h);
            g.setColor(Theme.BORDER);
            g.fillRect(w - 3, by, 3, bh);
        }
    }

    private static void paintIcon(Graphics g, int x, int y, int kind) {
        int color;
        char c;
        switch (kind) {
        case 1: color = 0x2060C0; c = 'i'; break;
        case 2: color = 0xE08000; c = '!'; break;
        case 3: color = 0xC02020; c = 'x'; break;
        case 4: color = 0xC02080; c = '!'; break;
        default: color = 0x20A040; c = '?'; break;
        }
        g.setColor(color);
        g.fillArc(x, y, ICON, ICON, 0, 360);
        g.setColor(0xFFFFFF);
        g.setFont(Theme.BOLD);
        g.drawChar(c, x + ICON / 2, y + (ICON - Theme.BOLD.getHeight()) / 2 + 1,
                Graphics.TOP | Graphics.HCENTER);
    }

    boolean keyEvent(int type, int code) {
        if (type == nanojava.Events.KEY_UP) {
            return true;
        }
        int h = contentHeight();
        switch (Canvas.gameAction(code)) {
        case Canvas.UP:
            scroll = Math.max(0, scroll - h / 3);
            repaintDisplayable();
            return true;
        case Canvas.DOWN:
            scroll = Math.max(0, Math.min(contentTotal - h, scroll + h / 3));
            repaintDisplayable();
            return true;
        case Canvas.FIRE:
            if (type == nanojava.Events.KEY_DOWN && commands.size() == 1) {
                fireCommand((Command) commands.elementAt(0));
            }
            return true;
        default:
            return false;
        }
    }

    void pointerEvent(int type, int x, int y) {
        if (type == nanojava.Events.POINTER_UP && commands.size() == 1
                && y >= Theme.titleHeight() && y < Theme.titleHeight() + contentHeight()) {
            fireCommand((Command) commands.elementAt(0));
        }
    }
}
