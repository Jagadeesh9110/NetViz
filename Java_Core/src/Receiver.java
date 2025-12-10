import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Receiver {

    private DatagramSocket socket;
    private InetAddress senderAddress;
    private int senderPort;

    // Stores received packets temporarily (out-of-order allowed)
    private Map<Integer, byte[]> receivedBuffer = new HashMap<>();

    private int expectedSeq = 0; // sliding expected sequence number

    private boolean running = true;

    public Receiver(int listenPort) throws Exception {
        this.socket = new DatagramSocket(listenPort);
        System.out.println("Receiver listening on port " + listenPort);
    }

    // Main method: continuously receives packets + sends ACKs.
    public byte[] receiveData() throws Exception {

        while (running) {
            receivePacket();
        }

        System.out.println("Receiver stopped. Rebuilding data...");
        return rebuildData();
    }

    // Receives a single UDP packet, parses it, validates checksum, and sends ACK.
    private void receivePacket() throws Exception {

        byte[] buffer = new byte[2048];
        DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(udpPacket);

        senderAddress = udpPacket.getAddress();
        senderPort = udpPacket.getPort();

        byte[] receivedBytes = java.util.Arrays.copyOf(buffer, udpPacket.getLength());

        CustomPacket packet = CustomPacket.fromBytes(receivedBytes);

        if (!packet.isValid()) {
            System.out.println("Corrupted packet received → dropped");
            return;
        }

        // FIN packet received
        if (packet.sequenceNumber == -1 && packet.payloadLength == 0) {
            System.out.println("FIN packet received → transmission completed.");
            sendAck(-1);
            running = false;
            return;
        }

        int seq = packet.sequenceNumber;
        Logger.logPacketReceived(seq);

        // Store payload (even out of order)
        receivedBuffer.put(seq, packet.payload);

        // Slide expectedSeq if next packets are already present
        while (receivedBuffer.containsKey(expectedSeq)) {
            expectedSeq++;
        }

        // Send cumulative ACK
        sendAck(expectedSeq - 1);
    }

    // Send ACK back to sender.
    private void sendAck(int ackSeq) throws Exception {

        CustomPacket ack = new CustomPacket(ackSeq, new byte[0]); // empty payload

        byte[] ackBytes = ack.toBytes();

        DatagramPacket udpPacket = new DatagramPacket(
                ackBytes,
                ackBytes.length,
                senderAddress,
                senderPort);

        socket.send(udpPacket);

        Logger.logAckSent(ackSeq);
    }

    // Combines all received packet payloads into one final byte array.
    private byte[] rebuildData() {

        int totalBytes = receivedBuffer.values().stream().mapToInt(a -> a.length).sum();
        byte[] fullData = new byte[totalBytes];

        int index = 0;

        for (int seq = 0; seq < expectedSeq; seq++) {
            byte[] part = receivedBuffer.get(seq);
            if (part != null) {
                System.arraycopy(part, 0, fullData, index, part.length);
                index += part.length;
            }
        }
        return fullData;
    }

    public void stop() {
        running = false;
        socket.close();
    }
}
