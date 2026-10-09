package java.util;

public class Stack extends Vector {
    public Stack() {
    }

    public Object push(Object item) {
        addElement(item);
        return item;
    }

    public synchronized Object pop() {
        Object obj = peek();
        removeElementAt(elementCount - 1);
        return obj;
    }

    public synchronized Object peek() {
        if (elementCount == 0) {
            throw new EmptyStackException();
        }
        return elementData[elementCount - 1];
    }

    public boolean empty() {
        return elementCount == 0;
    }

    /* 1-based distance from the top of the stack, or -1. */
    public synchronized int search(Object o) {
        int i = lastIndexOf(o);
        return i >= 0 ? elementCount - i : -1;
    }
}
