public class Main {

    public static void main(String[] args) throws Exception {

        int receiverPort = 6000;
        String receiverIp = "127.0.0.1";
        int windowSize = 20;

        // Start Receiver in background thread
        Receiver receiver = new Receiver(receiverPort);

        Thread receiverThread = new Thread(() -> {
            try {
                receiver.receiveData(); // No printing — binary safe
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        receiverThread.start();

        // Give receiver time to start
        Thread.sleep(1000);

        // Start Sender
        Sender sender = new Sender(receiverIp, receiverPort, windowSize);

        // -------- SELECT FILE TO SEND --------
        String filePath = "E:\\NetViz\\Java_Core\\test.jpg";

        System.out.println("\n===== SENDER STARTED FILE TRANSMISSION =====");

        sender.sendFile(filePath);
        sender.close();

        System.out.println("===== SENDER FINISHED SENDING FILE =====\n");

        // Wait for receiver to finish rebuilding and saving file
        receiverThread.join();

        // Stop receiver
        receiver.stop();

        System.out.println("All processes completed.");
    }
}
