import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class WindowControlListener implements Runnable {

    private final WindowManager windowManager;
    private final Sender sender;
    private boolean running = true;

    public WindowControlListener(WindowManager manager, Sender sender) {
        this.windowManager = manager;
        this.sender = sender;
    }

    @Override
    public void run() {

        try (DatagramSocket socket = new DatagramSocket(5001)) {
            System.out.println("WindowControlListener running on UDP port 5001...");

            byte[] buffer = new byte[256];

            while (running) {

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                System.out.println("Received control message: " + message);

                // MANUAL PARSE (no org.json)
                if (message.contains("\"event\":\"SET_WINDOW\"")) {

                    try {
                        int sizeIndex = message.indexOf("\"size\":") + 7;
                        String number = "";

                        while (sizeIndex < message.length()
                                && Character.isDigit(message.charAt(sizeIndex))) {
                            number += message.charAt(sizeIndex);
                            sizeIndex++;
                        }

                        int newSize = Integer.parseInt(number);

                        System.out.println("UI → Java Window Size Updated to: " + newSize);
                        windowManager.setWindowSize(newSize);

                    } catch (Exception e) {
                        System.out.println("Failed to parse window size from: " + message);
                    }
                }

                if (message.contains("\"event\":\"SET_LOSS\"")) {
                    try {
                        int idx = message.indexOf("\"chance\":") + 9;
                        String num = "";

                        while (idx < message.length() && Character.isDigit(message.charAt(idx))) {
                            num += message.charAt(idx);
                            idx++;
                        }

                        int chance = Integer.parseInt(num);
                        System.out.println("UI → Java Packet Loss Updated to: " + chance + "%");

                        sender.setLossChance(chance);

                    } catch (Exception e) {
                        System.out.println("Failed to parse loss chance from: " + message);
                    }
                }

            }

        } catch (Exception e) {
            System.out.println("WindowControlListener error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }
}
