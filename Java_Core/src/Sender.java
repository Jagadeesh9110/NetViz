import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;

public class Sender {

    private DatagramSocket socket;
    private InetAddress receiverAddress;
    private int receiverPort;

    private WindowManager windowManager;

    private long timeoutMs = 1000; // timeout in ms

    private boolean running = true;

    public Sender(String receiverIp, int receiverPort, int windowSize) throws Exception {
        this.socket = new DatagramSocket();
        this.receiverAddress = InetAddress.getByName(receiverIp);
        this.receiverPort = receiverPort;

        this.windowManager = new WindowManager(windowSize, 0);

        startAckListener();

        // Start dynamic window size listener
        Thread controlThread = new Thread(new WindowControlListener(windowManager));
        controlThread.setDaemon(true);
        controlThread.start();
    }

    // Public helper to send file from disk (reads bytes and calls
    // sendMetadata+sendData)
    public void sendFile(String filePath) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get(filePath));
        String fileName = Paths.get(filePath).getFileName().toString();
        sendFileBytes(fileName, fileData);
    }

    // Send metadata packet (seq=0) contains filename, filesize, and firstChunk
    public void sendFileBytes(String fileName, byte[] fileData) throws Exception {
        int chunkSize = 1024;
        int totalChunks = (int) Math.ceil(fileData.length / (double) chunkSize);

        // Prepare first chunk
        int firstChunkLen = Math.min(chunkSize, fileData.length);
        byte[] firstChunk = new byte[firstChunkLen];
        System.arraycopy(fileData, 0, firstChunk, 0, firstChunkLen);

        // Send metadata (seq=0) containing filename, filesize, and firstChunk
        sendMetadata(fileName, fileData.length, firstChunk);

        // Now send remaining chunks as seq = 1 .. totalChunks-1 (because chunk 0
        // embedded)
        int seq = 1;
        while (seq < totalChunks + 0 || !windowManager.getUnackedSeqs().isEmpty()) {
            while (seq < totalChunks && windowManager.canSend(seq)) {
                int chunkIndex = seq; // because chunk 0 was in metadata
                int start = chunkIndex * chunkSize;
                int end = Math.min(start + chunkSize, fileData.length);

                byte[] payload = new byte[end - start];
                System.arraycopy(fileData, start, payload, 0, payload.length);

                sendPacket(seq, payload);
                seq++;

                // Demo delay so UI animations are visible
                Thread.sleep(30);
            }
            checkTimeouts();
            Thread.sleep(10);
        }

        sendFinPacket();
        running = false;
        System.out.println("All file data sent successfully.");
    }

    // Builds and sends metadata packet (seq=0, TYPE_METADATA)
    private void sendMetadata(String fileName, long fileSize, byte[] firstChunk) throws Exception {
        byte[] fileNameBytes = fileName.getBytes("UTF-8");

        // Layout: int nameLen (4) | nameBytes | long fileSize (8) | firstChunk bytes
        int metaLen = 4 + fileNameBytes.length + 8 + (firstChunk != null ? firstChunk.length : 0);
        ByteBuffer buffer = ByteBuffer.allocate(metaLen);
        buffer.putInt(fileNameBytes.length);
        buffer.put(fileNameBytes);
        buffer.putLong(fileSize);
        if (firstChunk != null)
            buffer.put(firstChunk);

        byte[] metadataPayload = buffer.array();

        CustomPacket packet = new CustomPacket(CustomPacket.TYPE_METADATA, 0, metadataPayload);
        byte[] packetBytes = packet.toBytes();

        DatagramPacket udp = new DatagramPacket(packetBytes, packetBytes.length, receiverAddress, receiverPort);
        socket.send(udp);

        windowManager.recordSent(0, packetBytes);
        Logger.logPacketSent(0, windowManager.getWindowStart(), windowManager.getWindowEnd());
    }

    // // Main method: splits data and sends using sliding window
    // public void sendData(byte[] fullData) throws Exception {

    // int chunkSize = 1024;
    // int totalPackets = (int) Math.ceil(fullData.length / (double) chunkSize);

    // int seq = 0;

    // while (seq < totalPackets || !windowManager.getUnackedSeqs().isEmpty()) {

    // while (seq < totalPackets && windowManager.canSend(seq)) {

    // int start = seq * chunkSize;
    // int end = Math.min(start + chunkSize, fullData.length);

    // byte[] payload = new byte[end - start];
    // System.arraycopy(fullData, start, payload, 0, payload.length);

    // sendPacket(seq, payload);
    // seq++;

    // System.out.println("... Simulating network delay ...");
    // Thread.sleep(800); // 0.8 seconds pause so we can see the "Fly" animation
    // }

    // checkTimeouts();
    // Thread.sleep(10);
    // }

    // sendFinPacket();
    // running = false;
    // System.out.println("All data sent successfully.");
    // }

    // Send a regular data packet (seq >= 1)
    private void sendPacket(int seq, byte[] payload) throws Exception {
        CustomPacket packet = new CustomPacket(CustomPacket.TYPE_DATA, seq, payload);
        byte[] packetBytes = packet.toBytes();

        DatagramPacket udpPacket = new DatagramPacket(packetBytes, packetBytes.length, receiverAddress, receiverPort);
        socket.send(udpPacket);

        windowManager.recordSent(seq, packetBytes);
        Logger.logPacketSent(seq, windowManager.getWindowStart(), windowManager.getWindowEnd());
    }

    // Sends FIN packet to notify receiver
    private void sendFinPacket() throws Exception {
        CustomPacket fin = new CustomPacket(CustomPacket.TYPE_FIN, -1, new byte[0]);
        byte[] finBytes = fin.toBytes();

        DatagramPacket udpPacket = new DatagramPacket(finBytes, finBytes.length, receiverAddress, receiverPort);
        socket.send(udpPacket);

        System.out.println("FIN packet sent to receiver.");
    }

    // Starts ACK listener thread
    private void startAckListener() {
        Thread listener = new Thread(() -> {
            while (running) {
                try {
                    listenForAck();
                } catch (Exception e) {
                    // socket might be closed on shutdown
                    if (running)
                        e.printStackTrace();
                }
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    // Receives and processes ACK packets
    private void listenForAck() throws Exception {
        byte[] buffer = new byte[64];
        DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(udpPacket);

        byte[] receivedBytes = java.util.Arrays.copyOf(buffer, udpPacket.getLength());

        CustomPacket ackPacket;
        try {
            ackPacket = CustomPacket.fromBytes(receivedBytes);
        } catch (IllegalArgumentException ex) {
            System.out.println("Malformed ACK packet received -> ignored: " + ex.getMessage());
            return;
        }

        if (ackPacket.type != CustomPacket.TYPE_ACK) {
            // ignore non-ACKs on this socket (FIN ack may be TYPE_ACK with seq -1 depending
            // on design)
            // but we support TYPE_FIN being acked with TYPE_ACK if needed.
        }

        if (!ackPacket.isValid()) {
            System.out.println("Received corrupted ACK → ignored");
            return;
        }

        int ackSeq = ackPacket.sequenceNumber;

        if (ackSeq == -1) {
            System.out.println("Receiver ACKed FIN.");
            Logger.logAckReceived(-1);
            return;
        }

        windowManager.recordAck(ackSeq);
        Logger.logAckReceived(ackSeq);
    }

    // Checks for timeouts and retransmits if needed
    private void checkTimeouts() throws Exception {
        List<Integer> pending = windowManager.getUnackedSeqs();
        long now = System.currentTimeMillis();

        for (int seq : pending) {
            Long lastSend = windowManager.getLastSendTime(seq);

            if (lastSend != null && now - lastSend >= timeoutMs) {

                // Retrieve packet safely
                Object packetObj = windowManager.getPacket(seq);

                // SAFETY CHECK — if packet was ACKed just now, ignore timeout
                if (packetObj != null) {
                    Logger.logTimeout(seq);

                    byte[] packetBytes = (byte[]) packetObj;

                    DatagramPacket udpPacket = new DatagramPacket(
                            packetBytes, packetBytes.length,
                            receiverAddress, receiverPort);
                    socket.send(udpPacket);

                    windowManager.updateSendTimestamp(seq);
                    Logger.logRetransmission(seq);
                }
            }
        }
    }

    public void close() {
        running = false;
        socket.close();
    }
}
