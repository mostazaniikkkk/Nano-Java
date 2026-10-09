package javax.microedition.rms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/*
 * Each record store is one nanojava.Storage blob under "<suite>/<name>",
 * rewritten after every change (MIDlets often never close their stores).
 * Blob layout, big-endian:
 *   "NJRS"  magic
 *   u8      format version (1)
 *   u8      flags (bit 0: writable by other suites)
 *   u8      authmode
 *   s32     store version
 *   s64     last modified (ms since epoch)
 *   s32     next record id
 *   s32     record count
 *   then per record, in ascending id order: s32 id, s32 length, bytes.
 */
public class RecordStore {
    public static final int AUTHMODE_PRIVATE = 0;
    public static final int AUTHMODE_ANY = 1;

    private static final int FORMAT = 1;
    private static final int HEADER_SIZE = 27;
    private static final int RECORD_OVERHEAD = 8;
    private static final int QUOTA = 1024 * 1024;

    /* Open stores by storage key. Also the lock for the static methods. */
    private static final Hashtable openStores = new Hashtable();
    private static String suiteName;

    private final String name;
    private final String key;
    private final boolean owned;
    private int refs;
    private int authmode;
    private boolean writable;
    private int version;
    private long lastModified;
    private int nextId = 1;
    private int count;
    private int[] ids = new int[8];
    private byte[][] data = new byte[8][];
    private int dataSize;
    private final Vector listeners = new Vector();

    private RecordStore(String name, String key, boolean owned) {
        this.name = name;
        this.key = key;
        this.owned = owned;
    }

    /* ---- Opening and naming ---- */

    private static String ownSuite() {
        if (suiteName == null) {
            String s = nanojava.AppProperties.get("MIDlet-Name");
            suiteName = s != null && s.length() > 0 ? s : "default";
        }
        return suiteName;
    }

    /* Escapes '/' so a suite name can never alias another suite's prefix. */
    private static String escape(String s) {
        StringBuffer b = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%') {
                b.append("%25");
            } else if (c == '/') {
                b.append("%2F");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static void checkName(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        if (name.length() < 1 || name.length() > 32) {
            throw new IllegalArgumentException("invalid record store name");
        }
    }

    private static void checkAuthmode(int authmode) {
        if (authmode != AUTHMODE_PRIVATE && authmode != AUTHMODE_ANY) {
            throw new IllegalArgumentException();
        }
    }

    private static RecordStore open(String suite, String name, boolean create, int authmode,
                                    boolean writable) throws RecordStoreException {
        checkName(name);
        String key = escape(suite) + "/" + name;
        synchronized (openStores) {
            RecordStore rs = (RecordStore) openStores.get(key);
            if (rs == null) {
                rs = new RecordStore(name, key, suite.equals(ownSuite()));
                byte[] blob = nanojava.Storage.read(key);
                if (blob != null) {
                    rs.load(blob);
                } else if (!create) {
                    throw new RecordStoreNotFoundException(name);
                } else {
                    rs.authmode = authmode;
                    rs.writable = writable;
                    rs.lastModified = System.currentTimeMillis();
                    rs.save();
                }
                openStores.put(key, rs);
            }
            rs.refs++;
            return rs;
        }
    }

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
            throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
        return open(ownSuite(), recordStoreName, createIfNecessary, AUTHMODE_PRIVATE, false);
    }

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary,
                                              int authmode, boolean writable)
            throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
        checkAuthmode(authmode);
        return open(ownSuite(), recordStoreName, createIfNecessary, authmode, writable);
    }

    public static RecordStore openRecordStore(String recordStoreName, String vendorName,
                                              String suiteName)
            throws RecordStoreException, RecordStoreNotFoundException {
        if (vendorName == null || suiteName == null) {
            throw new IllegalArgumentException();
        }
        RecordStore rs = open(suiteName, recordStoreName, false, AUTHMODE_PRIVATE, false);
        if (!rs.owned && rs.authmode != AUTHMODE_ANY) {
            rs.closeRecordStore();
            throw new SecurityException();
        }
        return rs;
    }

    public static void deleteRecordStore(String recordStoreName)
            throws RecordStoreException, RecordStoreNotFoundException {
        checkName(recordStoreName);
        String key = escape(ownSuite()) + "/" + recordStoreName;
        synchronized (openStores) {
            if (openStores.get(key) != null) {
                throw new RecordStoreException("record store is open: " + recordStoreName);
            }
            if (!nanojava.Storage.delete(key)) {
                throw new RecordStoreNotFoundException(recordStoreName);
            }
        }
    }

    public static String[] listRecordStores() {
        String prefix = escape(ownSuite()) + "/";
        String[] keys = nanojava.Storage.list();
        Vector names = new Vector();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].startsWith(prefix) && keys[i].length() > prefix.length()) {
                    names.addElement(keys[i].substring(prefix.length()));
                }
            }
        }
        if (names.size() == 0) {
            return null;
        }
        String[] result = new String[names.size()];
        names.copyInto(result);
        return result;
    }

    public void closeRecordStore() throws RecordStoreNotOpenException, RecordStoreException {
        synchronized (openStores) {
            synchronized (this) {
                checkOpen();
                if (--refs == 0) {
                    openStores.remove(key);
                    listeners.removeAllElements();
                }
            }
        }
    }

    /* ---- Persistence ---- */

    private void load(byte[] blob) throws RecordStoreException {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(blob));
            if (in.readByte() != 'N' || in.readByte() != 'J' || in.readByte() != 'R'
                    || in.readByte() != 'S' || in.readUnsignedByte() != FORMAT) {
                throw new RecordStoreException("unknown record store format: " + name);
            }
            writable = (in.readUnsignedByte() & 1) != 0;
            authmode = in.readUnsignedByte() == AUTHMODE_ANY ? AUTHMODE_ANY : AUTHMODE_PRIVATE;
            version = in.readInt();
            lastModified = in.readLong();
            nextId = in.readInt();
            int n = in.readInt();
            if (n < 0) {
                throw new IOException();
            }
            ids = new int[n + 8];
            data = new byte[n + 8][];
            for (int i = 0; i < n; i++) {
                ids[i] = in.readInt();
                byte[] d = new byte[in.readInt()];
                in.readFully(d);
                data[i] = d;
                dataSize += d.length;
            }
            count = n;
        } catch (IOException e) {
            throw new RecordStoreException("corrupt record store: " + name);
        } catch (NegativeArraySizeException e) {
            throw new RecordStoreException("corrupt record store: " + name);
        }
    }

    private void save() throws RecordStoreException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(sizeOf(0));
        DataOutputStream out = new DataOutputStream(bytes);
        try {
            out.write('N');
            out.write('J');
            out.write('R');
            out.write('S');
            out.writeByte(FORMAT);
            out.writeByte(writable ? 1 : 0);
            out.writeByte(authmode);
            out.writeInt(version);
            out.writeLong(lastModified);
            out.writeInt(nextId);
            out.writeInt(count);
            for (int i = 0; i < count; i++) {
                out.writeInt(ids[i]);
                out.writeInt(data[i].length);
                out.write(data[i], 0, data[i].length);
            }
        } catch (IOException e) {
            throw new RecordStoreException(e.toString());
        }
        if (!nanojava.Storage.write(key, bytes.toByteArray())) {
            throw new RecordStoreFullException("cannot write record store " + name);
        }
    }

    private int sizeOf(int extra) {
        return HEADER_SIZE + count * RECORD_OVERHEAD + dataSize + extra;
    }

    /* ---- Checks and lookup (callers hold the lock) ---- */

    private void checkOpen() throws RecordStoreNotOpenException {
        if (refs <= 0) {
            throw new RecordStoreNotOpenException(name);
        }
    }

    private void checkWritable() {
        if (!owned && !writable) {
            throw new SecurityException();
        }
    }

    private int indexOf(int recordId) throws InvalidRecordIDException {
        int lo = 0;
        int hi = count - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (ids[mid] < recordId) {
                lo = mid + 1;
            } else if (ids[mid] > recordId) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        throw new InvalidRecordIDException(String.valueOf(recordId));
    }

    private static byte[] copy(byte[] src, int offset, int numBytes) {
        if (src == null && numBytes > 0) {
            throw new NullPointerException();
        }
        if (numBytes < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte[] d = new byte[numBytes];
        if (numBytes > 0) {
            System.arraycopy(src, offset, d, 0, numBytes);
        }
        return d;
    }

    private void touch() {
        version++;
        lastModified = System.currentTimeMillis();
    }

    private RecordListener[] listenerSnapshot() {
        synchronized (listeners) {
            RecordListener[] l = new RecordListener[listeners.size()];
            listeners.copyInto(l);
            return l;
        }
    }

    /* ---- Records ---- */

    public int addRecord(byte[] data, int offset, int numBytes)
            throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {
        int id;
        synchronized (this) {
            checkOpen();
            checkWritable();
            byte[] d = copy(data, offset, numBytes);
            if (sizeOf(RECORD_OVERHEAD + numBytes) > QUOTA) {
                throw new RecordStoreFullException();
            }
            if (count == ids.length) {
                int[] ni = new int[count * 2];
                byte[][] nd = new byte[count * 2][];
                System.arraycopy(ids, 0, ni, 0, count);
                System.arraycopy(this.data, 0, nd, 0, count);
                ids = ni;
                this.data = nd;
            }
            int oldVersion = version;
            long oldModified = lastModified;
            id = nextId++;
            ids[count] = id;
            this.data[count] = d;
            count++;
            dataSize += numBytes;
            touch();
            try {
                save();
            } catch (RecordStoreException e) {
                count--;
                this.data[count] = null;
                dataSize -= numBytes;
                nextId--;
                version = oldVersion;
                lastModified = oldModified;
                throw e;
            }
        }
        RecordListener[] l = listenerSnapshot();
        for (int i = 0; i < l.length; i++) {
            l[i].recordAdded(this, id);
        }
        return id;
    }

    public void deleteRecord(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        synchronized (this) {
            checkOpen();
            checkWritable();
            int i = indexOf(recordId);
            int[] oldIds = ids;
            byte[][] oldData = data;
            int oldVersion = version;
            long oldModified = lastModified;
            int[] ni = new int[ids.length];
            byte[][] nd = new byte[ids.length][];
            System.arraycopy(ids, 0, ni, 0, i);
            System.arraycopy(data, 0, nd, 0, i);
            System.arraycopy(ids, i + 1, ni, i, count - i - 1);
            System.arraycopy(data, i + 1, nd, i, count - i - 1);
            int removed = data[i].length;
            ids = ni;
            data = nd;
            count--;
            dataSize -= removed;
            touch();
            try {
                save();
            } catch (RecordStoreException e) {
                ids = oldIds;
                data = oldData;
                count++;
                dataSize += removed;
                version = oldVersion;
                lastModified = oldModified;
                throw e;
            }
        }
        RecordListener[] l = listenerSnapshot();
        for (int i = 0; i < l.length; i++) {
            l[i].recordDeleted(this, recordId);
        }
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException,
            RecordStoreFullException {
        synchronized (this) {
            checkOpen();
            checkWritable();
            int i = indexOf(recordId);
            byte[] d = copy(newData, offset, numBytes);
            byte[] old = data[i];
            if (sizeOf(numBytes - old.length) > QUOTA) {
                throw new RecordStoreFullException();
            }
            int oldVersion = version;
            long oldModified = lastModified;
            data[i] = d;
            dataSize += numBytes - old.length;
            touch();
            try {
                save();
            } catch (RecordStoreException e) {
                data[i] = old;
                dataSize -= numBytes - old.length;
                version = oldVersion;
                lastModified = oldModified;
                throw e;
            }
        }
        RecordListener[] l = listenerSnapshot();
        for (int i = 0; i < l.length; i++) {
            l[i].recordChanged(this, recordId);
        }
    }

    public synchronized byte[] getRecord(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        checkOpen();
        byte[] d = data[indexOf(recordId)];
        return d.length == 0 ? null : copy(d, 0, d.length);
    }

    public synchronized int getRecord(int recordId, byte[] buffer, int offset)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        checkOpen();
        byte[] d = data[indexOf(recordId)];
        if (d.length > 0) {
            if (buffer == null) {
                throw new NullPointerException();
            }
            if (offset < 0 || offset > buffer.length - d.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            System.arraycopy(d, 0, buffer, offset, d.length);
        }
        return d.length;
    }

    public synchronized int getRecordSize(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        checkOpen();
        return data[indexOf(recordId)].length;
    }

    /* ---- Store information ---- */

    public String getName() throws RecordStoreNotOpenException {
        synchronized (this) {
            checkOpen();
        }
        return name;
    }

    public synchronized int getVersion() throws RecordStoreNotOpenException {
        checkOpen();
        return version;
    }

    public synchronized int getNumRecords() throws RecordStoreNotOpenException {
        checkOpen();
        return count;
    }

    public synchronized int getSize() throws RecordStoreNotOpenException {
        checkOpen();
        return sizeOf(0);
    }

    public synchronized int getSizeAvailable() throws RecordStoreNotOpenException {
        checkOpen();
        int free = QUOTA - sizeOf(RECORD_OVERHEAD);
        return free > 0 ? free : 0;
    }

    public synchronized long getLastModified() throws RecordStoreNotOpenException {
        checkOpen();
        return lastModified;
    }

    public synchronized int getNextRecordID()
            throws RecordStoreNotOpenException, RecordStoreException {
        checkOpen();
        return nextId;
    }

    public synchronized void setMode(int authmode, boolean writable) throws RecordStoreException {
        checkOpen();
        if (!owned) {
            throw new SecurityException();
        }
        checkAuthmode(authmode);
        int oldMode = this.authmode;
        boolean oldWritable = this.writable;
        this.authmode = authmode;
        this.writable = writable;
        try {
            save();
        } catch (RecordStoreException e) {
            this.authmode = oldMode;
            this.writable = oldWritable;
            throw e;
        }
    }

    public void addRecordListener(RecordListener listener) {
        synchronized (this) {
            if (refs <= 0 || listener == null) {
                return;
            }
        }
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.addElement(listener);
            }
        }
    }

    public void removeRecordListener(RecordListener listener) {
        listeners.removeElement(listener);
    }

    public RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator,
                                              boolean keepUpdated)
            throws RecordStoreNotOpenException {
        synchronized (this) {
            checkOpen();
        }
        return new RecordEnumerationImpl(this, filter, comparator, keepUpdated);
    }

    /* ---- For RecordEnumerationImpl ---- */

    /* Ids of the records accepted by filter, ordered by comparator (or by
     * ascending id). The callbacks run on copies, outside the lock. */
    int[] select(RecordFilter filter, final RecordComparator comparator) {
        int n;
        int[] sel;
        byte[][] recs;
        synchronized (this) {
            n = count;
            sel = new int[n];
            recs = new byte[n][];
            System.arraycopy(ids, 0, sel, 0, n);
            for (int i = 0; i < n; i++) {
                if (filter != null || comparator != null) {
                    recs[i] = copy(data[i], 0, data[i].length);
                }
            }
        }
        int m = 0;
        for (int i = 0; i < n; i++) {
            if (filter == null || filter.matches(recs[i])) {
                sel[m] = sel[i];
                recs[m] = recs[i];
                m++;
            }
        }
        if (comparator != null && m > 1) {
            sort(sel, recs, new int[m], new byte[m][], 0, m, comparator);
        }
        int[] result = new int[m];
        System.arraycopy(sel, 0, result, 0, m);
        return result;
    }

    /* Stable merge sort of ids[lo, hi) keyed by recs. */
    private static void sort(int[] ids, byte[][] recs, int[] tmpIds, byte[][] tmpRecs,
                             int lo, int hi, RecordComparator c) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        sort(ids, recs, tmpIds, tmpRecs, lo, mid, c);
        sort(ids, recs, tmpIds, tmpRecs, mid, hi, c);
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
            if (c.compare(recs[j], recs[i]) == RecordComparator.PRECEDES) {
                tmpIds[k] = ids[j];
                tmpRecs[k++] = recs[j++];
            } else {
                tmpIds[k] = ids[i];
                tmpRecs[k++] = recs[i++];
            }
        }
        while (i < mid) {
            tmpIds[k] = ids[i];
            tmpRecs[k++] = recs[i++];
        }
        while (j < hi) {
            tmpIds[k] = ids[j];
            tmpRecs[k++] = recs[j++];
        }
        System.arraycopy(tmpIds, lo, ids, lo, hi - lo);
        System.arraycopy(tmpRecs, lo, recs, lo, hi - lo);
    }
}
