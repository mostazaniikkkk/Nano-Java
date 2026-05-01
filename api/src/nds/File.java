package nds;

public class File {

    public static native boolean exists(String filename);
    public static native int size(String filename);
    public static native int load(String filename, byte[] buffer, int maxlen);
    public static native int save(String filename, byte[] buffer);

    public static native int loadToVRAM(String filename, int address);




}
