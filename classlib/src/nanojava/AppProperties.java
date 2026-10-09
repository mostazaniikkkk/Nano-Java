package nanojava;

import java.util.Hashtable;

/**
 * Application properties from the JAD (if the platform supplied one) and
 * the jar manifest. JAD values take precedence, as on real devices.
 */
public final class AppProperties {
    private static Hashtable props;

    private AppProperties() {
    }

    public static synchronized String get(String key) {
        if (props == null) {
            props = new Hashtable();
            byte[] manifest = Resources.read("META-INF/MANIFEST.MF");
            if (manifest != null) {
                parse(manifest, true);
            }
            byte[] jad = jad();
            if (jad != null) {
                parse(jad, false);
            }
        }
        return (String) props.get(key);
    }

    /* Returns the JAD file contents, or null. */
    private static native byte[] jad();

    /* Parses "Key: value" lines; manifest lines may continue on the next
     * line when it starts with a space. JAD files are often UTF-8. */
    private static void parse(byte[] data, boolean manifest) {
        String text;
        try {
            text = new String(data, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            text = new String(data);
        }
        String key = null;
        StringBuffer value = null;
        int pos = 0;
        int n = text.length();
        while (pos <= n) {
            int end = text.indexOf('\n', pos);
            if (end < 0) {
                end = n;
            }
            String line = text.substring(pos, end);
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            pos = end + 1;
            if (manifest && line.startsWith(" ") && key != null) {
                value.append(line.substring(1));
                continue;
            }
            if (key != null) {
                props.put(key, value.toString().trim());
                key = null;
            }
            int colon = line.indexOf(':');
            if (colon > 0) {
                key = line.substring(0, colon).trim();
                value = new StringBuffer(line.substring(colon + 1));
            }
        }
        if (key != null) {
            props.put(key, value.toString().trim());
        }
    }
}
