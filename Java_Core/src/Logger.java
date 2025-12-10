public class Logger {

    public static void logPacketSent(int seq, int windowStart, int windowEnd) {

        String json = "{"
                + "\"event\": \"PACKET_SENT\", "
                + "\"seq\": " + seq + ", "
                + "\"windowStart\": " + windowStart + ", "
                + "\"windowEnd\": " + windowEnd + ", "
                + "\"timestamp\": " + System.currentTimeMillis()
                + "}";

        sendToNode(json);
    }

    public static void logPacketReceived(int seq) {
        String json = "{"
                + "\"event\": \"PACKET_RECEIVED\", "
                + "\"seq\": " + seq + ", "
                + "\"timestamp\": " + System.currentTimeMillis()
                + "}";

        sendToNode(json);
    }

    public static void logAckSent(int ack) {
        String json = "{"
                + "\"event\": \"ACK_SENT\", "
                + "\"ack\": " + ack + ", "
                + "\"timestamp\": " + System.currentTimeMillis()
                + "}";

        sendToNode(json);
    }

    public static void logAckReceived(int ack) {
        String json = "{"
                + "\"event\": \"ACK_RECEIVED\", "
                + "\"ack\": " + ack + ", "
                + "\"timestamp\": " + System.currentTimeMillis()
                + "}";

        sendToNode(json);
    }

    public static void logTimeout(int seq) {
        String json = "{"
                + "\"event\": \"TIMEOUT\", "
                + "\"seq\": " + seq + ", "
                + "\"timestamp\": " + System.currentTimeMillis()
                + "}";

        sendToNode(json);
    }

    public static void logRetransmission(int seq) {
        String json = "{"
                + "\"event\": \"RETRANSMIT\", "
                + "\"seq\": " + seq + ", "
                + "\"timestamp\": " + System.currentTimeMillis()
                + "}";

        sendToNode(json);
    }

    public static void logWindowMove(int oldStart, int newStart, int newEnd) {
        String json = "{"
                + "\"event\": \"WINDOW_MOVED\", "
                + "\"oldStart\": " + oldStart + ", "
                + "\"newStart\": " + newStart + ", "
                + "\"newEnd\": " + newEnd + ", "
                + "\"timestamp\": " + System.currentTimeMillis()
                + "}";

        sendToNode(json);
    }

    private static void sendToNode(String json) {
        System.out.println(json);
    }
}
