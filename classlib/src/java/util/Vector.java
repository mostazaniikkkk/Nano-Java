package java.util;

public class Vector {
    protected Object[] elementData;
    protected int elementCount;
    protected int capacityIncrement;

    public Vector(int initialCapacity, int capacityIncrement) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        this.elementData = new Object[initialCapacity];
        this.capacityIncrement = capacityIncrement;
    }

    public Vector(int initialCapacity) {
        this(initialCapacity, 0);
    }

    public Vector() {
        this(10);
    }

    public synchronized void copyInto(Object[] anArray) {
        System.arraycopy(elementData, 0, anArray, 0, elementCount);
    }

    public synchronized void trimToSize() {
        if (elementCount < elementData.length) {
            Object[] a = new Object[elementCount];
            System.arraycopy(elementData, 0, a, 0, elementCount);
            elementData = a;
        }
    }

    public synchronized void ensureCapacity(int minCapacity) {
        if (minCapacity > elementData.length) {
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        int old = elementData.length;
        int n = capacityIncrement > 0 ? old + capacityIncrement : old * 2;
        if (n < minCapacity) {
            n = minCapacity;
        }
        Object[] a = new Object[n];
        System.arraycopy(elementData, 0, a, 0, elementCount);
        elementData = a;
    }

    public synchronized void setSize(int newSize) {
        if (newSize < 0) {
            throw new ArrayIndexOutOfBoundsException(newSize);
        }
        if (newSize > elementData.length) {
            grow(newSize);
        } else {
            for (int i = newSize; i < elementCount; i++) {
                elementData[i] = null;
            }
        }
        elementCount = newSize;
    }

    public int capacity() {
        return elementData.length;
    }

    public int size() {
        return elementCount;
    }

    public boolean isEmpty() {
        return elementCount == 0;
    }

    /* Live, index-based enumeration: tolerant of concurrent changes. */
    public Enumeration elements() {
        return new Enumeration() {
            private int index;

            public boolean hasMoreElements() {
                return index < elementCount;
            }

            public Object nextElement() {
                synchronized (Vector.this) {
                    if (index < elementCount) {
                        return elementData[index++];
                    }
                }
                throw new NoSuchElementException("Vector Enumeration");
            }
        };
    }

    public boolean contains(Object elem) {
        return indexOf(elem, 0) >= 0;
    }

    public int indexOf(Object elem) {
        return indexOf(elem, 0);
    }

    public synchronized int indexOf(Object elem, int index) {
        if (index < 0) {
            index = 0;
        }
        if (elem == null) {
            for (int i = index; i < elementCount; i++) {
                if (elementData[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = index; i < elementCount; i++) {
                if (elem.equals(elementData[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public synchronized int lastIndexOf(Object elem) {
        return lastIndexOf(elem, elementCount - 1);
    }

    public synchronized int lastIndexOf(Object elem, int index) {
        if (index >= elementCount) {
            throw new IndexOutOfBoundsException(index + " >= " + elementCount);
        }
        if (elem == null) {
            for (int i = index; i >= 0; i--) {
                if (elementData[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = index; i >= 0; i--) {
                if (elem.equals(elementData[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public synchronized Object elementAt(int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
        }
        return elementData[index];
    }

    public synchronized Object firstElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData[0];
    }

    public synchronized Object lastElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData[elementCount - 1];
    }

    public synchronized void setElementAt(Object obj, int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
        }
        elementData[index] = obj;
    }

    public synchronized void removeElementAt(int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
        } else if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int j = elementCount - index - 1;
        if (j > 0) {
            System.arraycopy(elementData, index + 1, elementData, index, j);
        }
        elementCount--;
        elementData[elementCount] = null;
    }

    public synchronized void insertElementAt(Object obj, int index) {
        if (index > elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " > " + elementCount);
        } else if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (elementCount == elementData.length) {
            grow(elementCount + 1);
        }
        System.arraycopy(elementData, index, elementData, index + 1, elementCount - index);
        elementData[index] = obj;
        elementCount++;
    }

    public synchronized void addElement(Object obj) {
        if (elementCount == elementData.length) {
            grow(elementCount + 1);
        }
        elementData[elementCount++] = obj;
    }

    public synchronized boolean removeElement(Object obj) {
        int i = indexOf(obj, 0);
        if (i >= 0) {
            removeElementAt(i);
            return true;
        }
        return false;
    }

    public synchronized void removeAllElements() {
        for (int i = 0; i < elementCount; i++) {
            elementData[i] = null;
        }
        elementCount = 0;
    }

    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        for (int i = 0; i < elementCount; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Object o = elementData[i];
            sb.append(o == this ? "(this Collection)" : String.valueOf(o));
        }
        sb.append(']');
        return sb.toString();
    }
}
