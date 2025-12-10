public class Utils {

    // Convert sequence number to 4 bytes.
    public static byte[] intToBytes(int value) {
        byte[] arr = new byte[4];
        arr[0] = (byte) (value >> 24);
        arr[1] = (byte) (value >> 16);
        arr[2] = (byte) (value >> 8);
        arr[3] = (byte) (value);

        return arr;

    }

    // Convert 4 bytes to sequence number.
    public static int bytesToInt(byte[] bytes) {
        int value = 0;
        value |= (bytes[0] & 0xFF) << 24;
        value |= (bytes[1] & 0xFF) << 16;
        value |= (bytes[2] & 0xFF) << 8;
        value |= (bytes[3] & 0xFF);

        return value;
    }

    // Convert checksum to 2 bytes.
    public static byte[] shortToBytes(short value) {
        byte[] arr = new byte[2];
        arr[0] = (byte) (value >> 8);
        arr[1] = (byte) (value);

        return arr;
    }

    // Convert 2 bytes to checksum.
    public static short bytesToShort(byte[] bytes) {
        int value = 0;
        value |= (bytes[0] & 0xFF) << 8;
        value |= (bytes[1] & 0xFF);

        return (short) value;
    }

    // Combine two byte arrays (used for building final packet in CustomPacket)
    public static byte[] combine(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);

        return result;
    }

    // we will compute checkSum here
    public static short computeChecksum(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += (b & 0xFF);
        }
        sum = sum % 65535;

        return (short) sum;
    }
}
