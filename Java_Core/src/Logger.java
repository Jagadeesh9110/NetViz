import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Logger {

    private static DatagramSocket socket;
    private static InetAddress nodeAddress;
    private static int nodePort = 5000; // Node.js will listen here

    static {
        try {
            socket = new DatagramSocket();
            nodeAddress = InetAddress.getByName("localhost");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logPacketSent(int seq, int windowStart, int windowEnd) {
        String json = String.format(
                "{\"event\":\"PACKET_SENT\",\"seq\":%d,\"windowStart\":%d,\"windowEnd\":%d,\"timestamp\":%d}",
                seq, windowStart, windowEnd, System.currentTimeMillis());
        sendToNode(json);
    }

    public static void logPacketReceived(int seq) {
        String json = String.format("{\"event\":\"PACKET_RECEIVED\",\"seq\":%d,\"timestamp\":%d}",
                seq, System.currentTimeMillis());
        sendToNode(json);
    }

    public static void logAckSent(int ack) {
        String json = String.format("{\"event\":\"ACK_SENT\",\"ack\":%d,\"timestamp\":%d}",
                ack, System.currentTimeMillis());
        sendToNode(json);
    }

    public static void logAckReceived(int ack) {
        String json = String.format("{\"event\":\"ACK_RECEIVED\",\"ack\":%d,\"timestamp\":%d}",
                ack, System.currentTimeMillis());
        sendToNode(json);
    }

    public static void logTimeout(int seq) {
        String json = String.format("{\"event\":\"TIMEOUT\",\"seq\":%d,\"timestamp\":%d}",
                seq, System.currentTimeMillis());
        sendToNode(json);
    }

    public static void logRetransmission(int seq) {
        String json = String.format("{\"event\":\"RETRANSMIT\",\"seq\":%d,\"timestamp\":%d}",
                seq, System.currentTimeMillis());
        sendToNode(json);
    }

    public static void logWindowMove(int oldStart, int newStart, int newEnd) {
        String json = String.format(
                "{\"event\":\"WINDOW_MOVED\",\"oldStart\":%d,\"newStart\":%d,\"newEnd\":%d,\"timestamp\":%d}",
                oldStart, newStart, newEnd, System.currentTimeMillis());
        sendToNode(json);
    }

    // Sends the JSON string as a UDP packet to Node.js
    private static void sendToNode(String json) {
        try {
            byte[] data = json.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, nodeAddress, nodePort);
            socket.send(packet);

            // Optional: Print to console too so you can still debug
            // System.out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}