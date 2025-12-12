import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.FileOutputStream;

import java.util.HashMap;
import java.util.Map;

public class Receiver {

    private DatagramSocket socket; // udp receiver socket
    private InetAddress senderAddress; // sender ip
    private int senderPort; // sender port

    private String outputFileName; // filename from metadata
    private long expectedFileSize = -1; // size of file
    private long receivedBytesTotal = 0; // total bytes received

    private Map<Integer, byte[]> receivedBuffer = new HashMap<>(); // to store chunks
    private int expectedSeq = 0; // sliding expected seq

    private boolean running = true; // loop flag

    public Receiver(int listenPort) throws Exception {
        this.socket = new DatagramSocket(listenPort);
        System.out.println("Receiver listening on port " + listenPort);
    }

    // main receive loop
    public byte[] receiveData() throws Exception {

        while (running)
            receivePacket();

        byte[] full = rebuildData(); // join chunks

        if (outputFileName != null)
            saveToFile(full, outputFileName);

        return full;
    }

    private void saveToFile(byte[] data, String filename) {
        try {
            // Save inside Node_Bridge/received/
            File folder = new File("../../Node_Bridge/received");

            if (!folder.exists())
                folder.mkdirs();

            File file = new File(folder, filename);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }

            System.out.println("Saved file to: " + file.getAbsolutePath());

            // Notify Node.js that file is ready (for preview UI)
            Logger.logFileComplete(filename, data.length);

        } catch (Exception e) {
            System.out.println("File save error: " + e.getMessage());
        }
    }

    // receive + process single packet
    private void receivePacket() throws Exception {

        byte[] buf = new byte[65536];
        DatagramPacket udp = new DatagramPacket(buf, buf.length);
        socket.receive(udp);

        senderAddress = udp.getAddress();
        senderPort = udp.getPort();

        byte[] raw = java.util.Arrays.copyOf(buf, udp.getLength());

        CustomPacket packet = CustomPacket.fromBytes(raw);

        if (!packet.isValid())
            return;

        // metadata packet
        if (packet.type == CustomPacket.TYPE_METADATA && packet.sequenceNumber == 0) {

            ByteBuffer bb = ByteBuffer.wrap(packet.payload);

            int nameLen = bb.getInt();
            byte[] nameBytes = new byte[nameLen];
            bb.get(nameBytes);

            outputFileName = new String(nameBytes, StandardCharsets.UTF_8);
            expectedFileSize = bb.getLong();

            int totalPayload = packet.payloadLength & 0xFFFF;
            int remaining = totalPayload - (4 + nameLen + 8);

            byte[] firstChunk = new byte[Math.max(0, remaining)];
            if (remaining > 0)
                bb.get(firstChunk);

            receivedBuffer.put(0, firstChunk);

            receivedBytesTotal = firstChunk.length;

            Logger.logPacketReceived(0);

            while (receivedBuffer.containsKey(expectedSeq))
                expectedSeq++;

            sendAck(expectedSeq - 1);
            return;
        }

        // fin packet
        if (packet.type == CustomPacket.TYPE_FIN && packet.sequenceNumber == -1) {
            sendAck(-1);
            running = false;
            return;
        }

        // data packet
        if (packet.type == CustomPacket.TYPE_DATA) {

            int seq = packet.sequenceNumber;

            if (!receivedBuffer.containsKey(seq)) {
                receivedBuffer.put(seq, packet.payload);
                receivedBytesTotal += packet.payload.length; // count new bytes only
            } else {
                receivedBuffer.put(seq, packet.payload); // overwrite if needed
            }

            // ------ Progress Update ------
            if (expectedFileSize > 0) {
                Logger.logProgress(receivedBytesTotal, expectedFileSize);
            }

            Logger.logPacketReceived(seq);

            while (receivedBuffer.containsKey(expectedSeq)) {
                expectedSeq++;
            }

            sendAck(expectedSeq - 1);
        }

    }

    // send ack packet
    private void sendAck(int seq) throws Exception {
        CustomPacket ack = new CustomPacket(CustomPacket.TYPE_ACK, seq, new byte[0]);
        byte[] bytes = ack.toBytes();

        DatagramPacket udp = new DatagramPacket(bytes, bytes.length, senderAddress, senderPort);
        socket.send(udp);

        Logger.logAckSent(seq);
    }

    // join chunks into full file
    private byte[] rebuildData() {
        int total = receivedBuffer.values().stream().mapToInt(a -> a.length).sum();
        byte[] full = new byte[total];

        int index = 0;
        for (int seq = 0; seq < expectedSeq; seq++) {
            byte[] part = receivedBuffer.get(seq);
            if (part != null) {
                System.arraycopy(part, 0, full, index, part.length);
                index += part.length;
            }
        }

        return full;
    }

    public void stop() {
        running = false;
        socket.close();
    }
}
