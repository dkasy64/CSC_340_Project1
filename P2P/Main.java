package P2P;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        try {
            // Read configuration
            List<Node> nodes = ConfigReader.readConfig("config.txt");

            // Initialize nodes and start listening
            for (Node node : nodes) {
                node.listen();
            }

            // Start heartbeat synchronization
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            SecureRandom secureRandom = new SecureRandom(); // Use SecureRandom instead of Random
            for (Node node : nodes) {
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        int delay = secureRandom.nextInt(10000); // Random delay between 0 and 30 seconds
                        Thread.sleep(delay);
                        node.sendHeartbeat(nodes); // Send heartbeat
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, 0, 10, TimeUnit.SECONDS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}