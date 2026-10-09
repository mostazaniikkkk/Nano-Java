package javax.microedition.lcdui;

public final class Font {
    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_UNDERLINED = 4;

    public static final int SIZE_SMALL = 8;
    public static final int SIZE_MEDIUM = 0;
    public static final int SIZE_LARGE = 16;

    public static final int FACE_SYSTEM = 0;
    public static final int FACE_MONOSPACE = 32;
    public static final int FACE_PROPORTIONAL = 64;

    public static final int FONT_STATIC_TEXT = 0;
    public static final int FONT_INPUT_TEXT = 1;

    private static final Font[] cache = new Font[3 * 8 * 3];
    private static final Font DEFAULT = getFont(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);

    private final int face;
    private final int size;
    /* Read by the Graphics natives. */
    final int style;
    final int sizeIndex;
    private final int height;
    private final int baseline;

    private Font(int face, int style, int size) {
        this.face = face;
        this.style = style;
        this.size = size;
        sizeIndex = size == SIZE_SMALL ? 0 : size == SIZE_LARGE ? 2 : 1;
        height = height0(sizeIndex);
        baseline = baseline0(sizeIndex);
    }

    public static Font getDefaultFont() {
        return DEFAULT;
    }

    public static Font getFont(int fontSpecifier) {
        if (fontSpecifier != FONT_STATIC_TEXT && fontSpecifier != FONT_INPUT_TEXT) {
            throw new IllegalArgumentException();
        }
        return DEFAULT;
    }

    public static Font getFont(int face, int style, int size) {
        if ((face != FACE_SYSTEM && face != FACE_MONOSPACE && face != FACE_PROPORTIONAL)
                || (style & ~7) != 0
                || (size != SIZE_SMALL && size != SIZE_MEDIUM && size != SIZE_LARGE)) {
            throw new IllegalArgumentException();
        }
        int f = face == FACE_SYSTEM ? 0 : face == FACE_MONOSPACE ? 1 : 2;
        int s = size == SIZE_SMALL ? 0 : size == SIZE_LARGE ? 2 : 1;
        int key = (f * 8 + style) * 3 + s;
        synchronized (cache) {
            if (cache[key] == null) {
                cache[key] = new Font(face, style, size);
            }
            return cache[key];
        }
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    public int getFace() {
        return face;
    }

    public boolean isPlain() {
        return style == STYLE_PLAIN;
    }

    public boolean isBold() {
        return (style & STYLE_BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & STYLE_ITALIC) != 0;
    }

    public boolean isUnderlined() {
        return (style & STYLE_UNDERLINED) != 0;
    }

    public int getHeight() {
        return height;
    }

    public int getBaselinePosition() {
        return baseline;
    }

    public int charWidth(char ch) {
        return charWidth0(sizeIndex, style, ch);
    }

    public int charsWidth(char[] ch, int offset, int length) {
        if (offset < 0 || length < 0 || offset > ch.length - length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return charsWidth0(sizeIndex, style, ch, offset, length);
    }

    public int stringWidth(String str) {
        return stringWidth0(sizeIndex, style, str, 0, str.length());
    }

    public int substringWidth(String str, int offset, int len) {
        if (offset < 0 || len < 0 || offset > str.length() - len) {
            throw new StringIndexOutOfBoundsException();
        }
        return stringWidth0(sizeIndex, style, str, offset, len);
    }

    private static native int charWidth0(int size, int style, char ch);

    private static native int charsWidth0(int size, int style, char[] ch, int off, int len);

    private static native int stringWidth0(int size, int style, String s, int off, int len);

    private static native int height0(int size);

    private static native int baseline0(int size);
}
