package java.lang;

public final class Character {
    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;
    public static final char MIN_VALUE = '\u0000';
    public static final char MAX_VALUE = '￿';

    private final char value;

    public Character(char value) {
        this.value = value;
    }

    public char charValue() {
        return value;
    }

    public int hashCode() {
        return value;
    }

    public boolean equals(Object obj) {
        return obj instanceof Character && ((Character) obj).value == value;
    }

    public String toString() {
        return String.valueOf(value);
    }

    /* Case mapping covers ASCII and Latin-1, which is what CLDC requires. */
    public static boolean isLowerCase(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'ß' && ch <= 'ÿ' && ch != '÷');
    }

    public static boolean isUpperCase(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'À' && ch <= 'Þ' && ch != '×');
    }

    public static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static char toLowerCase(char ch) {
        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'À' && ch <= 'Þ' && ch != '×')) {
            return (char) (ch + 32);
        }
        return ch;
    }

    public static char toUpperCase(char ch) {
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'à' && ch <= 'þ' && ch != '÷')) {
            return (char) (ch - 32);
        }
        return ch;
    }

    public static int digit(char ch, int radix) {
        if (radix < MIN_RADIX || radix > MAX_RADIX) {
            return -1;
        }
        int d;
        if (ch >= '0' && ch <= '9') {
            d = ch - '0';
        } else if (ch >= 'a' && ch <= 'z') {
            d = ch - 'a' + 10;
        } else if (ch >= 'A' && ch <= 'Z') {
            d = ch - 'A' + 10;
        } else {
            return -1;
        }
        return d < radix ? d : -1;
    }
}
