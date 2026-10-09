package javax.microedition.rms;

/*
 * A snapshot of record ids. index is the position of the record returned
 * last, or -1 before the first call (next starts at the first record,
 * previous at the last one).
 */
class RecordEnumerationImpl implements RecordEnumeration, RecordListener {
    private final RecordStore store;
    private final RecordFilter filter;
    private final RecordComparator comparator;
    private boolean keepUpdated;
    private boolean destroyed;
    private int[] ids;
    private int index = -1;

    RecordEnumerationImpl(RecordStore store, RecordFilter filter, RecordComparator comparator,
                          boolean keepUpdated) {
        this.store = store;
        this.filter = filter;
        this.comparator = comparator;
        ids = store.select(filter, comparator);
        keepUpdated(keepUpdated);
    }

    private void checkDestroyed() {
        if (destroyed) {
            throw new IllegalStateException();
        }
    }

    public synchronized int numRecords() {
        checkDestroyed();
        return ids.length;
    }

    public synchronized int nextRecordId() throws InvalidRecordIDException {
        checkDestroyed();
        if (index >= ids.length - 1) {
            throw new InvalidRecordIDException();
        }
        return ids[++index];
    }

    public synchronized int previousRecordId() throws InvalidRecordIDException {
        checkDestroyed();
        if (ids.length == 0 || index == 0) {
            throw new InvalidRecordIDException();
        }
        index = index < 0 ? ids.length - 1 : index - 1;
        return ids[index];
    }

    public byte[] nextRecord() throws InvalidRecordIDException, RecordStoreNotOpenException,
            RecordStoreException {
        return store.getRecord(nextRecordId());
    }

    public byte[] previousRecord() throws InvalidRecordIDException, RecordStoreNotOpenException,
            RecordStoreException {
        return store.getRecord(previousRecordId());
    }

    public synchronized boolean hasNextElement() {
        checkDestroyed();
        return index < ids.length - 1;
    }

    public synchronized boolean hasPreviousElement() {
        checkDestroyed();
        return ids.length > 0 && index != 0;
    }

    public synchronized void reset() {
        checkDestroyed();
        index = -1;
    }

    public void rebuild() {
        checkDestroyed();
        int[] fresh = store.select(filter, comparator);
        synchronized (this) {
            int current = index >= 0 && index < ids.length ? ids[index] : 0;
            int at = -1;
            if (current != 0) {
                for (int i = 0; i < fresh.length; i++) {
                    if (fresh[i] == current) {
                        at = i;
                        break;
                    }
                }
                if (at < 0) {
                    at = Math.min(index, fresh.length) - 1;
                }
            }
            ids = fresh;
            index = at;
        }
    }

    public void keepUpdated(boolean keepUpdated) {
        checkDestroyed();
        if (keepUpdated == this.keepUpdated) {
            return;
        }
        this.keepUpdated = keepUpdated;
        if (keepUpdated) {
            store.addRecordListener(this);
            rebuild();
        } else {
            store.removeRecordListener(this);
        }
    }

    public boolean isKeptUpdated() {
        checkDestroyed();
        return keepUpdated;
    }

    public void destroy() {
        checkDestroyed();
        store.removeRecordListener(this);
        destroyed = true;
    }

    /* ---- RecordListener, active while kept updated ---- */

    public void recordAdded(RecordStore recordStore, int recordId) {
        rebuild();
    }

    public void recordChanged(RecordStore recordStore, int recordId) {
        rebuild();
    }

    public synchronized void recordDeleted(RecordStore recordStore, int recordId) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == recordId) {
                int[] n = new int[ids.length - 1];
                System.arraycopy(ids, 0, n, 0, i);
                System.arraycopy(ids, i + 1, n, i, n.length - i);
                ids = n;
                /* The next record keeps its turn. */
                if (index >= i) {
                    index--;
                }
                return;
            }
        }
    }
}
