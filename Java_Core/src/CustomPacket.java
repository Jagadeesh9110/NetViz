public class CustomPacket {
    int sequenceNumber; // to identify packet order
    short payloadLength; // to identify payload length
    short checkSum; // to identify payload integrity
    byte[] payload; // actual data

    public CustomPacket(int sequenceNumber, short payloadLength, byte[] payload) {
        this.sequenceNumber = sequenceNumber;
        this.payloadLength = payloadLength;
        this.payload = payload;
        this.checkSum = Utils.computeChecksum(payload);
    }

    public CustomPacket(int sequenceNumber, byte[] payload) {
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
        this.payloadLength = (short) payload.length;
        this.checkSum = Utils.computeChecksum(payload);
    }

    // This converts the packet into a raw byte array that UDP can send.
    public byte[] toBytes() {
        // Convert fields into bytes
        byte[] seqBytes = Utils.intToBytes(sequenceNumber);
        byte[] lenBytes = Utils.shortToBytes(payloadLength);
        byte[] checksumBytes = Utils.shortToBytes(checkSum);

        // Combine header fields in order
        byte[] header = Utils.combine(seqBytes, lenBytes);
        header = Utils.combine(header, checksumBytes);

        // Combine header + payload
        byte[] finalPacket = Utils.combine(header, payload);

        return finalPacket;

    }

    public static CustomPacket fromBytes(byte[] data) {
        // Extract sequence number (first 4 bytes)
        byte[] seqBytes = new byte[4];
        System.arraycopy(data, 0, seqBytes, 0, 4);
        int sequenceNumber = Utils.bytesToInt(seqBytes);

        // Extract payload length (next 2 bytes)
        byte[] lenBytes = new byte[2];
        System.arraycopy(data, 4, lenBytes, 0, 2);
        short payloadLength = Utils.bytesToShort(lenBytes);

        // Extract checksum (next 2 bytes)
        byte[] checksumBytes = new byte[2];
        System.arraycopy(data, 6, checksumBytes, 0, 2);
        short checkSum = Utils.bytesToShort(checksumBytes);

        // Extract payload (remaining bytes)
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, 8, payload, 0, payload.length);

        // Create packet
        CustomPacket packet = new CustomPacket(sequenceNumber, payload);
        // Override checksum from network
        packet.checkSum = checkSum;

        return packet;
    }

    public boolean isValid() {
        return checkSum == Utils.computeChecksum(payload);
    }
}
