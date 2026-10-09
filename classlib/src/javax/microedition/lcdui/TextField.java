package javax.microedition.lcdui;

/*
 * Shows its text in a box. Fire (or a tap) opens the on-screen keyboard
 * editor (TextEditor); printable key codes are also typed directly while
 * the field is focused.
 */
public class TextField extends Item {
    public static final int ANY = 0;
    public static final int EMAILADDR = 1;
    public static final int NUMERIC = 2;
    public static final int PHONENUMBER = 3;
    public static final int URL = 4;
    public static final int DECIMAL = 5;
    public static final int PASSWORD = 0x10000;
    public static final int UNEDITABLE = 0x20000;
    public static final int SENSITIVE = 0x40000;
    public static final int NON_PREDICTIVE = 0x80000;
    public static final int INITIAL_CAPS_WORD = 0x100000;
    public static final int INITIAL_CAPS_SENTENCE = 0x200000;
    public static final int CONSTRAINT_MASK = 0xFFFF;

    final TextBuffer buf;
    private final TextWrap wrap = new TextWrap();

    public TextField(String label, String text, int maxSize, int constraints) {
        super(label);
        buf = new TextBuffer(text, maxSize, constraints);
    }

    public String getString() {
        return buf.getString();
    }

    public void setString(String text) {
        buf.setString(text);
        invalidate0();
    }

    public int getChars(char[] data) {
        return buf.getChars(data);
    }

    public void setChars(char[] data, int offset, int length) {
        buf.setChars(data, offset, length);
        invalidate0();
    }

    public void insert(String src, int position) {
        buf.insert(src, position);
        invalidate0();
    }

    public void insert(char[] data, int offset, int length, int position) {
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        insert(new String(data, offset, length), position);
    }

    public void delete(int offset, int length) {
        buf.delete(offset, length);
        invalidate0();
    }

    public int getMaxSize() {
        return buf.maxSize;
    }

    public int setMaxSize(int maxSize) {
        int r = buf.setMaxSize(maxSize);
        invalidate0();
        return r;
    }

    public int size() {
        return buf.size();
    }

    public int getCaretPosition() {
        return buf.caret;
    }

    public void setConstraints(int constraints) {
        buf.setConstraints(constraints);
        invalidate0();
    }

    public int getConstraints() {
        return buf.constraints;
    }

    public void setInitialInputMode(String characterSubset) {
    }

    /* ---- Item protocol ---- */

    boolean isFocusable() {
        return true;
    }

    int measureBody(int w) {
        Font f = Theme.FONT;
        return wrap.wrap(buf.displayString(), f, w - 8) * f.getHeight() + 6;
    }

    void paintBody(Graphics g, int w, int h, boolean focused) {
        Font f = Theme.FONT;
        int fh = f.getHeight();
        boolean editable = buf.editable();
        g.setColor(editable ? Theme.FIELD_BG : Theme.BUTTON_BG);
        g.fillRect(0, 0, w, h);
        g.setColor(focused ? Theme.HIGHLIGHT : Theme.BORDER);
        g.drawRect(0, 0, w - 1, h - 1);
        wrap.wrap(buf.displayString(), f, w - 8);
        g.setFont(f);
        g.setColor(Theme.FOREGROUND);
        wrap.paint(g, 4, 3, fh);
        if (focused && editable) {
            int line = wrap.lineOf(buf.caret);
            g.setColor(Theme.HIGHLIGHT);
            g.fillRect(4 + wrap.xOf(buf.caret), 3 + line * fh, 1, fh);
        }
    }

    private void edit() {
        if (!buf.editable()) {
            return;
        }
        Display d = Display.get();
        Displayable back = d.getCurrent();
        if (back == null) {
            back = owner;
        }
        d.show(new TextEditor(this, back));
    }

    /* Called by TextEditor when editing ends. */
    void edited(boolean changed) {
        invalidate0();
        if (changed) {
            notifyStateChanged();
        }
    }

    boolean itemKey(int type, int code) {
        if (type == nanojava.Events.KEY_UP) {
            return false;
        }
        if (code == Canvas.KEY_FIRE) {
            if (type == nanojava.Events.KEY_DOWN && buf.editable()) {
                edit();
                return true;
            }
            return false;
        }
        boolean typed;
        if (code == 8) {
            typed = buf.backspace();
        } else if (code >= 32 && code <= 0xFFFF) {
            typed = buf.typeChar((char) code);
        } else {
            return false;
        }
        if (typed) {
            invalidate0();
            notifyStateChanged();
        }
        return typed;
    }

    boolean itemPointer(int type, int x, int y) {
        if (type == nanojava.Events.POINTER_UP && buf.editable()) {
            edit();
        }
        return buf.editable();
    }
}
