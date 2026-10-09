package javax.microedition.lcdui;

/*
 * Full-screen editor for a TextField: the on-screen keyboard over the
 * field's buffer. Left soft key = done, right soft key = backspace (or
 * cancel, restoring the original text, when the text is empty).
 */
final class TextEditor extends Screen {
    private static final String[] LABELS_DEL = {"Done", "Del"};
    private static final String[] LABELS_CANCEL = {"Done", "Cancel"};

    private final TextField field;
    private final Displayable back;
    private final String original;
    private final Keyboard keyboard;

    TextEditor(TextField field, Displayable back) {
        this.field = field;
        this.back = back;
        title = field.label != null ? field.label : back != null ? back.title : null;
        original = field.buf.getString();
        field.buf.caret = field.buf.size();
        keyboard = new Keyboard(field.buf, true);
    }

    boolean wantsSoftKeys() {
        return true;
    }

    String[] softLabels() {
        return field.buf.size() > 0 ? LABELS_DEL : LABELS_CANCEL;
    }

    void paintContent(Graphics g, int w, int h) {
        keyboard.paint(g, w, h);
    }

    private void finish(boolean ok) {
        if (!ok) {
            field.buf.setUnchecked(original);
        }
        if (back != null) {
            Display.get().show(back);
        }
        field.edited(ok && !original.equals(field.buf.getString()));
    }

    boolean keyEvent(int type, int code) {
        if (code == Canvas.KEY_SOFT_LEFT) {
            if (type == nanojava.Events.KEY_DOWN) {
                finish(true);
            }
            return true;
        }
        if (code == Canvas.KEY_SOFT_RIGHT) {
            if (type != nanojava.Events.KEY_UP) {
                if (field.buf.size() > 0) {
                    field.buf.backspace();
                    repaintDisplayable();
                } else if (type == nanojava.Events.KEY_DOWN) {
                    finish(false);
                }
            }
            return true;
        }
        int r = keyboard.key(type, code);
        if (r == Keyboard.DONE) {
            finish(true);
        } else if (r != Keyboard.NONE) {
            repaintDisplayable();
        }
        return true;
    }

    void pointerEvent(int type, int x, int y) {
        int r = keyboard.pointer(type, x, y - Theme.titleHeight());
        if (r == Keyboard.DONE) {
            finish(true);
        } else if (r != Keyboard.NONE) {
            repaintDisplayable();
        }
    }
}
