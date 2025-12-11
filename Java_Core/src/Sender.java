import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
    }

    // Main method: splits data and sends using sliding window
    public void sendData(byte[] fullData) throws Exception {

        int chunkSize = 1024;
        int totalPackets = (int) Math.ceil(fullData.length / (double) chunkSize);

        int seq = 0;

        while (seq < totalPackets || !windowManager.getUnackedSeqs().isEmpty()) {

            while (seq < totalPackets && windowManager.canSend(seq)) {

                int start = seq * chunkSize;
                int end = Math.min(start + chunkSize, fullData.length);

                byte[] payload = new byte[end - start];
                System.arraycopy(fullData, start, payload, 0, payload.length);

                sendPacket(seq, payload);
                seq++;

                System.out.println("... Simulating network delay ...");
                Thread.sleep(800); // 0.8 seconds pause so we can see the "Fly" animation
            }

            checkTimeouts();
            Thread.sleep(10);
        }

        sendFinPacket();
        running = false;
        System.out.println("All data sent successfully.");
    }

    // Sends a single packet
    private void sendPacket(int seq, byte[] payload) throws Exception {
        CustomPacket packet = new CustomPacket(seq, payload);
        byte[] packetBytes = packet.toBytes();

        DatagramPacket udpPacket = new DatagramPacket(packetBytes, packetBytes.length, receiverAddress, receiverPort);
        socket.send(udpPacket);

        windowManager.recordSent(seq, packetBytes);

        Logger.logPacketSent(seq, windowManager.getWindowStart(), windowManager.getWindowEnd());
    }

    // Sends FIN packet to notify receiver
    private void sendFinPacket() throws Exception {
        CustomPacket fin = new CustomPacket(-1, new byte[0]);
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
                    e.printStackTrace();
                }
            }
        });
        listener.start();
    }

    // Receives and processes ACK packets
    private void listenForAck() throws Exception {

        byte[] buffer = new byte[32];
        DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(udpPacket);

        byte[] receivedBytes = java.util.Arrays.copyOf(buffer, udpPacket.getLength());

        CustomPacket ackPacket = CustomPacket.fromBytes(receivedBytes);

        if (!ackPacket.isValid()) {
            System.out.println("Received corrupted ACK → ignored");
            return;
        }
        int ackSeq = ackPacket.sequenceNumber;

        if (ackSeq == -1) {
            System.out.println("Receiver ACKed FIN.");
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
