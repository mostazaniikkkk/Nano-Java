package javax.microedition.io;

public interface CommConnection extends StreamConnection {
    int getBaudRate();

    int setBaudRate(int baudrate);
}
