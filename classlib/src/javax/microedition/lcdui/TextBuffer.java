package javax.microedition.lcdui;

/* Contents, constraints and caret shared by TextField and TextBox. */
final class TextBuffer {
    private final StringBuffer buf = new StringBuffer();
    int maxSize;
    int constraints;
    int caret;
    private String cached;
    private String masked;

    TextBuffer(String text, int maxSize, int constraints) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException();
        }
        checkConstraints(constraints);
        this.maxSize = maxSize;
        this.constraints = constraints;
        setString(text);
    }

    private static void checkConstraints(int c) {
        int kind = c & TextField.CONSTRAINT_MASK;
        if (kind < TextField.ANY || kind > TextField.DECIMAL) {
            throw new IllegalArgumentException();
        }
    }

    int size() {
        return buf.length();
    }

    boolean editable() {
        return (constraints & TextField.UNEDITABLE) == 0;
    }

    String getString() {
        String s = cached;
        if (s == null) {
            s = buf.toString();
            cached = s;
        }
        return s;
    }

    /* What is shown on screen: asterisks for PASSWORD fields. */
    String displayString() {
        if ((constraints & TextField.PASSWORD) == 0) {
            return getString();
        }
        String m = masked;
        if (m == null || m.length() != buf.length()) {
            char[] c = new char[buf.length()];
            for (int i = 0; i < c.length; i++) {
                c[i] = '*';
            }
            m = new String(c);
            masked = m;
        }
        return m;
    }

    private void changed() {
        cached = null;
        if (caret > buf.length()) {
            caret = buf.length();
        }
    }

    void setString(String s) {
        if (s == null) {
            s = "";
        }
        if (s.length() > maxSize || !valid(s, constraints)) {
            throw new IllegalArgumentException();
        }
        setUnchecked(s);
    }

    /* Restores a previously valid value (editor cancel). */
    void setUnchecked(String s) {
        buf.setLength(0);
        buf.append(s);
        caret = buf.length();
        changed();
    }

    int getChars(char[] data) {
        int n = buf.length();
        if (data.length < n) {
            throw new ArrayIndexOutOfBoundsException();
        }
        buf.getChars(0, n, data, 0);
        return n;
    }

    void setChars(char[] data, int offset, int length) {
        if (data == null) {
            setString(null);
            return;
        }
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        setString(new String(data, offset, length));
    }

    void insert(String src, int position) {
        if (src == null) {
            throw new NullPointerException();
        }
        int n = buf.length();
        if (position < 0) {
            position = 0;
        } else if (position > n) {
            position = n;
        }
        if (n + src.length() > maxSize) {
            throw new IllegalArgumentException();
        }
        String s = getString();
        String next = s.substring(0, position) + src + s.substring(position);
        if (!valid(next, constraints)) {
            throw new IllegalArgumentException();
        }
        buf.insert(position, src);
        if (caret >= position) {
            caret += src.length();
        }
        changed();
    }

    void delete(int offset, int length) {
        int n = buf.length();
        if (offset < 0 || length < 0 || offset > n - length) {
            throw new StringIndexOutOfBoundsException();
        }
        String s = getString();
        String next = s.substring(0, offset) + s.substring(offset + length);
        if (!valid(next, constraints)) {
            throw new IllegalArgumentException();
        }
        buf.delete(offset, offset + length);
        if (caret > offset + length) {
            caret -= length;
        } else if (caret > offset) {
            caret = offset;
        }
        changed();
    }

    int setMaxSize(int max) {
        if (max <= 0) {
            throw new IllegalArgumentException();
        }
        if (buf.length() > max) {
            String s = getString().substring(0, max);
            if (!valid(s, constraints)) {
                throw new IllegalArgumentException();
            }
            buf.setLength(max);
            changed();
        }
        maxSize = max;
        return max;
    }

    void setConstraints(int c) {
        checkConstraints(c);
        constraints = c;
        if (!valid(getString(), c)) {
            setUnchecked("");
        }
        masked = null;
    }

    /* User input: inserts c at the caret if the result is acceptable. */
    boolean typeChar(char c) {
        if (!editable() || buf.length() >= maxSize) {
            return false;
        }
        int at = Math.min(caret, buf.length());
        buf.insert(at, c);
        cached = null;
        if (!valid(getString(), constraints)) {
            buf.deleteCharAt(at);
            cached = null;
            return false;
        }
        caret = at + 1;
        return true;
    }

    /* User input: deletes the character before the caret. */
    boolean backspace() {
        if (!editable() || caret <= 0 || buf.length() == 0) {
            return false;
        }
        int at = Math.min(caret, buf.length()) - 1;
        buf.deleteCharAt(at);
        caret = at;
        changed();
        return true;
    }

    /* Whether s is acceptable under the constraint type. A lone "-" is
     * allowed for NUMERIC/DECIMAL so it can be typed. */
    static boolean valid(String s, int constraints) {
        int kind = constraints & TextField.CONSTRAINT_MASK;
        int n = s.length();
        switch (kind) {
        case TextField.NUMERIC:
        case TextField.DECIMAL:
            boolean dot = false;
            for (int i = 0; i < n; i++) {
                char c = s.charAt(i);
                if (c == '-' && i == 0) {
                    continue;
                }
                if (c == '.' && kind == TextField.DECIMAL && !dot) {
                    dot = true;
                    continue;
                }
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            return true;
        case TextField.PHONENUMBER:
            for (int i = 0; i < n; i++) {
                char c = s.charAt(i);
                if ((c < '0' || c > '9') && "+*#pw() -".indexOf(c) < 0) {
                    return false;
                }
            }
            return true;
        default:
            return true;
        }
    }
}
