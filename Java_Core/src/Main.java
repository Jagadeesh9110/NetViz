public class Main {

    public static void main(String[] args) throws Exception {

        int receiverPort = 6000; //
        String receiverIp = "127.0.0.1";
        int windowSize = 5;

        // Start Receiver in Background Thread
        Receiver receiver = new Receiver(receiverPort);

        Thread receiverThread = new Thread(() -> {
            try {
                byte[] finalOutput = receiver.receiveData();
                System.out.println("\n======== FINAL RECEIVED DATA ========");
                System.out.println(new String(finalOutput));
                System.out.println("=====================================\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        receiverThread.start();

        // Give receiver time to start up
        Thread.sleep(1000);

        // Start Sender
        Sender sender = new Sender(receiverIp, receiverPort, windowSize);

        String message = "Hello this is a test message being sent with sliding window ARQ over UDP!"
                + " We are testing packet segmentation, retransmission, cumulative ACKs, FIN handling, "
                + "and final reconstruction.";

        byte[] dataToSend = message.getBytes();

        System.out.println("\n===== SENDER STARTED TRANSMISSION =====");
        sender.sendData(dataToSend);
        sender.close();
        System.out.println("===== SENDER FINISHED =====\n");

        // Wait for receiver thread to end
        receiverThread.join();

        // Stop receiver after finishing
        receiver.stop();

        System.out.println("All processes completed.");
    }
}
