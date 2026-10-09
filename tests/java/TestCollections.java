import java.io.*;
import java.util.*;

// java.util and java.io, plus enough allocation to exercise the collector.
public class TestCollections {
    static class Node {
        Node next;
        int[] payload;

        Node(Node next, int n) {
            this.next = next;
            payload = new int[n];
            payload[0] = n;
        }
    }

    public static void main(String[] args) throws IOException {
        Vector v = new Vector();
        for (int i = 0; i < 10; i++) {
            v.addElement(new Integer(i * i));
        }
        v.removeElementAt(3);
        v.insertElementAt("x", 0);
        System.out.println(v + " " + v.size() + " " + v.indexOf(new Integer(16)));

        Hashtable h = new Hashtable();
        for (int i = 0; i < 100; i++) {
            h.put("k" + i, new Integer(i));
        }
        h.remove("k50");
        int sum = 0;
        for (Enumeration e = h.elements(); e.hasMoreElements();) {
            sum += ((Integer) e.nextElement()).intValue();
        }
        System.out.println(h.size() + " " + sum + " " + h.get("k7") + " " + h.containsKey("k50"));

        Stack st = new Stack();
        st.push("a");
        st.push("b");
        System.out.println("" + st.pop() + st.peek() + st.empty());

        Random r = new Random(42);
        System.out.println(r.nextInt() + " " + r.nextInt(100) + " " + r.nextLong() + " " + r.nextDouble());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(0xCAFEBABE);
        dos.writeUTF("héllo");
        dos.writeLong(-2L);
        dos.writeDouble(Math.PI);
        dos.writeBoolean(true);
        dos.close();
        byte[] bytes = bos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        System.out.println(bytes.length + " " + Integer.toHexString(dis.readInt()) + " " + (int) dis.readUTF().charAt(1)
                + " " + dis.readLong() + " " + dis.readDouble() + " " + dis.readBoolean());
        try {
            dis.readByte();
        } catch (EOFException e) {
            System.out.println("EOF");
        }

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(new Date(1000000000000L));
        System.out.println(c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-"
                + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE));

        // Allocate far more than the heap holds; only the last list stays live.
        Node keep = null;
        for (int round = 0; round < 1000; round++) {
            Node list = null;
            for (int i = 0; i < 200; i++) {
                list = new Node(list, 1 + (i % 50));
            }
            keep = list;
        }
        int n = 0;
        for (Node p = keep; p != null; p = p.next) {
            n += p.payload[0];
        }
        System.out.println("nodes " + n);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 2000; i++) {
            sb.append(i % 10);
        }
        System.out.println(sb.length() + " " + sb.toString().hashCode());
    }
}
