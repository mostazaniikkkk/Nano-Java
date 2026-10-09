package javax.microedition.lcdui;

/*
 * The text and the on-screen keyboard fill the screen. The soft keys run
 * the application's commands; without commands the right soft key is
 * backspace.
 */
public class TextBox extends Screen {
    private static final String[] LABELS_DEL = {null, "Del"};

    private final TextBuffer buf;
    private final Keyboard keyboard;

    public TextBox(String title, String text, int maxSize, int constraints) {
        this.title = title;
        buf = new TextBuffer(text, maxSize, constraints);
        keyboard = new Keyboard(buf, false);
    }

    public String getString() {
        return buf.getString();
    }

    public void setString(String text) {
        buf.setString(text);
        repaintDisplayable();
    }

    public int getChars(char[] data) {
        return buf.getChars(data);
    }

    public void setChars(char[] data, int offset, int length) {
        buf.setChars(data, offset, length);
        repaintDisplayable();
    }

    public void insert(String src, int position) {
        buf.insert(src, position);
        repaintDisplayable();
    }

    public void insert(char[] data, int offset, int length, int position) {
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        insert(new String(data, offset, length), position);
    }

    public void delete(int offset, int length) {
        buf.delete(offset, length);
        repaintDisplayable();
    }

    public int getMaxSize() {
        return buf.maxSize;
    }

    public int setMaxSize(int maxSize) {
        int r = buf.setMaxSize(maxSize);
        repaintDisplayable();
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
        repaintDisplayable();
    }

    public int getConstraints() {
        return buf.constraints;
    }

    public void setInitialInputMode(String characterSubset) {
    }

    /* ---- Screen hooks ---- */

    void paintContent(Graphics g, int w, int h) {
        keyboard.paint(g, w, h);
    }

    boolean wantsSoftKeys() {
        return commands.size() == 0;
    }

    String[] softLabels() {
        return commands.size() == 0 ? LABELS_DEL : super.softLabels();
    }

    boolean keyEvent(int type, int code) {
        if (code == Canvas.KEY_SOFT_RIGHT || code == Canvas.KEY_SOFT_LEFT) {
            if (code == Canvas.KEY_SOFT_RIGHT && type != nanojava.Events.KEY_UP && buf.backspace()) {
                repaintDisplayable();
            }
            return true;
        }
        if (keyboard.key(type, code) != Keyboard.NONE) {
            repaintDisplayable();
        }
        return true;
    }

    void pointerEvent(int type, int x, int y) {
        if (keyboard.pointer(type, x, y - Theme.titleHeight()) != Keyboard.NONE) {
            repaintDisplayable();
        }
    }
}
