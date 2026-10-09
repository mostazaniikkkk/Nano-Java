package java.util;

public class Hashtable {
    private static final class Entry {
        final int hash;
        final Object key;
        Object value;
        Entry next;

        Entry(int hash, Object key, Object value, Entry next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    /* Enumerates a snapshot, so later changes to the table are harmless. */
    private static final class Snapshot implements Enumeration {
        private final Object[] items;
        private int index;

        Snapshot(Object[] items) {
            this.items = items;
        }

        public boolean hasMoreElements() {
            return index < items.length;
        }

        public Object nextElement() {
            if (index >= items.length) {
                throw new NoSuchElementException("Hashtable Enumerator");
            }
            Object o = items[index];
            items[index++] = null;
            return o;
        }
    }

    private Entry[] table;
    private int count;
    private int threshold;

    public Hashtable(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        if (initialCapacity == 0) {
            initialCapacity = 1;
        }
        table = new Entry[initialCapacity];
        threshold = (int) (initialCapacity * 3L / 4);
    }

    public Hashtable() {
        this(11);
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    private synchronized Enumeration snapshot(boolean keys) {
        Object[] items = new Object[count];
        int n = 0;
        for (int i = table.length - 1; i >= 0; i--) {
            for (Entry e = table[i]; e != null; e = e.next) {
                items[n++] = keys ? e.key : e.value;
            }
        }
        return new Snapshot(items);
    }

    public Enumeration keys() {
        return snapshot(true);
    }

    public Enumeration elements() {
        return snapshot(false);
    }

    public synchronized boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        for (int i = table.length - 1; i >= 0; i--) {
            for (Entry e = table[i]; e != null; e = e.next) {
                if (e.value.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized boolean containsKey(Object key) {
        return find(key) != null;
    }

    private Entry find(Object key) {
        int hash = key.hashCode();
        int index = (hash & 0x7fffffff) % table.length;
        for (Entry e = table[index]; e != null; e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                return e;
            }
        }
        return null;
    }

    public synchronized Object get(Object key) {
        Entry e = find(key);
        return e != null ? e.value : null;
    }

    protected void rehash() {
        Entry[] old = table;
        int newCapacity = old.length * 2 + 1;
        Entry[] nt = new Entry[newCapacity];
        for (int i = old.length - 1; i >= 0; i--) {
            Entry e = old[i];
            while (e != null) {
                Entry next = e.next;
                int index = (e.hash & 0x7fffffff) % newCapacity;
                e.next = nt[index];
                nt[index] = e;
                e = next;
            }
        }
        table = nt;
        threshold = (int) (newCapacity * 3L / 4);
    }

    public synchronized Object put(Object key, Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        Entry e = find(key);
        if (e != null) {
            Object old = e.value;
            e.value = value;
            return old;
        }
        if (count >= threshold) {
            rehash();
        }
        int hash = key.hashCode();
        int index = (hash & 0x7fffffff) % table.length;
        table[index] = new Entry(hash, key, value, table[index]);
        count++;
        return null;
    }

    public synchronized Object remove(Object key) {
        int hash = key.hashCode();
        int index = (hash & 0x7fffffff) % table.length;
        Entry prev = null;
        for (Entry e = table[index]; e != null; prev = e, e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    table[index] = e.next;
                }
                count--;
                return e.value;
            }
        }
        return null;
    }

    public synchronized void clear() {
        for (int i = table.length - 1; i >= 0; i--) {
            table[i] = null;
        }
        count = 0;
    }

    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append('{');
        boolean first = true;
        for (int i = table.length - 1; i >= 0; i--) {
            for (Entry e = table[i]; e != null; e = e.next) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(e.key == this ? "(this Map)" : String.valueOf(e.key));
                sb.append('=');
                sb.append(e.value == this ? "(this Map)" : String.valueOf(e.value));
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
