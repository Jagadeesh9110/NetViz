import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class WindowManager {

    private final int windowSize; // maximum packets allowed in flight
    private int windowStart; // first un-ACKed sequence
    private int nextSeqToSend; // next seq available for transmission

    // Maps to store packets and their send timestamps
    private final Map<Integer, Object> unackedPackets;
    private final Map<Integer, Long> sendTimestamps;

    public WindowManager(int windowSize, int initialSeqStart) {
        this.windowSize = windowSize;
        this.windowStart = initialSeqStart;
        this.nextSeqToSend = initialSeqStart;

        this.unackedPackets = new HashMap<>();
        this.sendTimestamps = new HashMap<>();
    }

    /**
     * Check if sender is allowed to send a packet with sequence number seq.
     * Allowed iff seq <= windowStart + windowSize - 1.
     */
    public synchronized boolean canSend(int seq) {
        int windowEnd = windowStart + windowSize - 1;
        return seq <= windowEnd;
    }

    // Called immediately after sender transmits a packet.
    public synchronized void recordSent(int seq, Object packetObj) {
        unackedPackets.put(seq, packetObj);
        sendTimestamps.put(seq, System.currentTimeMillis());

        if (seq >= nextSeqToSend) {
            nextSeqToSend = seq + 1;
        }
    }

    /**
     * Called when ACK for a given sequence number is received.
     * Slides the window forward while packets are acknowledged in order.
     */
    public synchronized void recordAck(int ackSeq) {

        // Remove acked packet
        unackedPackets.remove(ackSeq);
        sendTimestamps.remove(ackSeq);

        int oldStart = windowStart;
        // Slide window forward over continuous ACKs
        while (!unackedPackets.containsKey(windowStart) && windowStart < nextSeqToSend) {
            windowStart++;
        }

        // Log only if window actually moved
        if (oldStart != windowStart) {
            Logger.logWindowMove(oldStart, windowStart, getWindowEnd());
        }
    }

    /**
     * Return all unacked sequence numbers.
     * Sender uses this to detect timeouts.
     */
    public synchronized List<Integer> getUnackedSeqs() {
        return new ArrayList<>(unackedPackets.keySet());
    }

    /**
     * Retrieve stored packet for retransmission.
     */
    public synchronized Object getPacket(int seq) {
        return unackedPackets.get(seq);
    }

    /**
     * Get last time a packet was sent.
     */
    public synchronized Long getLastSendTime(int seq) {
        return sendTimestamps.get(seq);
    }

    /**
     * Update timestamp after retransmission.
     */
    public synchronized void updateSendTimestamp(int seq) {
        sendTimestamps.put(seq, System.currentTimeMillis());
    }

    public synchronized int getWindowStart() {
        return windowStart;
    }

    public synchronized int getWindowEnd() {
        return windowStart + windowSize - 1;
    }

    public synchronized int getNextSeqToSend() {
        return nextSeqToSend;
    }

}
