import java.nio.ByteBuffer;

public class CustomPacket {

    public static final byte TYPE_DATA = 0; // data packet
    public static final byte TYPE_METADATA = 1; // metadata packet
    public static final byte TYPE_ACK = 2; // ack packet
    public static final byte TYPE_FIN = 3; // fin packet

    public byte type; // to identify packet type
    public int sequenceNumber; // to identify packet order
    public short payloadLength; // to identify payload length
    public short checkSum; // to identify payload integrity
    public byte[] payload; // actual data

    // full constructor
    public CustomPacket(byte type, int sequenceNumber, short payloadLength, byte[] payload) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.payloadLength = payloadLength;
        this.payload = (payload != null) ? payload : new byte[0];
        this.checkSum = Utils.computeChecksum(this.payload);
    }

    public CustomPacket(byte type, int sequenceNumber, byte[] payload) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.payload = (payload != null) ? payload : new byte[0];
        this.payloadLength = (short) this.payload.length;
        this.checkSum = Utils.computeChecksum(this.payload);
    }

    // convert to bytes → [type(1)][seq(4)][len(2)][checksum(2)][payload]
    public byte[] toBytes() {
        ByteBuffer bb = ByteBuffer.allocate(1 + 4 + 2 + 2 + payload.length);
        bb.put(type);
        bb.putInt(sequenceNumber);
        bb.putShort(payloadLength);
        bb.putShort(checkSum);
        bb.put(payload);
        return bb.array();
    }

    // convert bytes → CustomPacket
    public static CustomPacket fromBytes(byte[] data) {

        if (data.length < 9)
            throw new IllegalArgumentException("Packet too short");

        ByteBuffer bb = ByteBuffer.wrap(data);

        byte type = bb.get(); // first byte = type
        int seq = bb.getInt(); // next 4 = seq
        short len = bb.getShort(); // next 2 = payload length
        short checksum = bb.getShort(); // next 2 = checksum

        int unsignedLen = len & 0xFFFF;

        if (data.length < 9 + unsignedLen)
            throw new IllegalArgumentException("Invalid payload length");

        byte[] payload = new byte[unsignedLen];
        if (unsignedLen > 0)
            bb.get(payload);

        CustomPacket packet = new CustomPacket(type, seq, payload);
        packet.checkSum = checksum; // Override calculated checksum with the one from the network

        return packet;
    }

    public boolean isValid() {
        return checkSum == Utils.computeChecksum(payload);
    }
}
