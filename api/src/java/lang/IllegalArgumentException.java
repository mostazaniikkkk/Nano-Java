
package java.lang;

public
class IllegalArgumentException extends RuntimeException {
    /**
     * Constructs an <code>IllegalArgumentException</code> with no 
     * detail message. 
     */
    public IllegalArgumentException() {
        super();
    }

    /**
     * Constructs an <code>IllegalArgumentException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public IllegalArgumentException(String s) {
        super(s);
    }
}

