/*
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package java.lang;

/**
 *
 * The Byte class is the standard wrapper for byte values.
 *
 * @author  Nakul Saraiya
 * @version 12/17/01 (CLDC 1.1)
 * @since   JDK1.1, CLDC 1.0
 */
public final class Byte {

    /**
     * The minimum value a Byte can have.
     */
    public static final byte MIN_VALUE = -128;

    /**
     * The maximum value a Byte can have.
     */
    public static final byte MAX_VALUE = 127;

    /**
     * Assuming the specified String represents a byte, returns
     * that byte's value. Throws an exception if the String cannot
     * be parsed as a byte.  The radix is assumed to be 10.
     *
     * @param s       the String containing the byte
     * @return        the parsed value of the byte
     * @exception     NumberFormatException If the string does not
     *                contain a parsable byte.
     */
    public static byte parseByte(String s) throws NumberFormatException {
        return parseByte(s, 10);
    }

    /**
     * Assuming the specified String represents a byte, returns
     * that byte's value. Throws an exception if the String cannot
     * be parsed as a byte.
     *
     * @param s       the String containing the byte
     * @param radix   the radix to be used
     * @return        the parsed value of the byte
     * @exception     NumberFormatException If the String does not
     *                contain a parsable byte.
     */
    public static byte parseByte(String s, int radix)
        throws NumberFormatException {
        int i = Integer.parseInt(s, radix);
        if (i < MIN_VALUE || i > MAX_VALUE)
            throw new NumberFormatException();
        return (byte)i;
    }

    /**
     * The value of the Byte.
     */
    private byte value;

    /**
     * Constructs a Byte object initialized to the specified byte value.
     *
     * @param value     the initial value of the Byte
     */
    public Byte(byte value) {
        this.value = value;
    }

    /**
     * Returns the value of this Byte as a byte.
     *
     * @return the value of this Byte as a byte.
     */
    public byte byteValue() {
        return value;
    }

    /**
     * Returns a String object representing this Byte's value.
     */
    public String toString() {
      return String.valueOf((int)value);
    }

    /**
     * Returns a hashcode for this Byte.
     */
    public int hashCode() {
        return (int)value;
    }

    /**
     * Compares this object to the specified object.
     *
     * @param obj       the object to compare with
     * @return          true if the objects are the same; false otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Byte) {
            return value == ((Byte)obj).byteValue();
        }
        return false;
    }

}

