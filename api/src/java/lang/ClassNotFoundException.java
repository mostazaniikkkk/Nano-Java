/*
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */

package java.lang;

/**
 * Thrown when an application tries to load in a class through its
 * string name using the <code>forName</code> method in class <code>Class</code>
 * but no definition for the class with the specified name could be found. 
 *
 * @author  unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @see     java.lang.Class#forName(java.lang.String)
 * @since   JDK1.0, CLDC 1.0
 */

public
class ClassNotFoundException extends Exception {
    /**
     * Constructs a <code>ClassNotFoundException</code> with no detail message.
     */
    public ClassNotFoundException() {
        super();
    }

    /**
     * Constructs a <code>ClassNotFoundException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public ClassNotFoundException(String s) {
        super(s);
    }

}

