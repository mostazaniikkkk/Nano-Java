package javax.microedition.io;

import java.io.IOException;

public interface DatagramConnection extends Connection {
    int getMaximumLength() throws IOException;

    int getNominalLength() throws IOException;

    void send(Datagram dgram) throws IOException;

    void receive(Datagram dgram) throws IOException;

    Datagram newDatagram(int size) throws IOException;

    Datagram newDatagram(int size, String addr) throws IOException;

    Datagram newDatagram(byte[] buf, int size) throws IOException;

    Datagram newDatagram(byte[] buf, int size, String addr) throws IOException;
}
