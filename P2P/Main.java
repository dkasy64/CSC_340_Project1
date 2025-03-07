package P2P;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        try {
            // Read configuration
            List<Node> nodes = ConfigReader.readConfig("config.txt");
            
            // Register nodes with each other
            for (Node node : nodes) {
                node.registerNodes(nodes);
            }
            
            // Initialize nodes and start listening
            for (Node node : nodes) {
                node.listen();
            }

            // Print file listings for each node based on their home directory
            for (Node node : nodes) {
                node.listFilesInDirectory();
            }
            
            // Start heartbeat synchronization at a fixed rate
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(nodes.size());
            for (Node node : nodes) {
                scheduler.scheduleAtFixedRate(() -> {
                    node.sendHeartbeat(nodes); // Send heartbeat every 15 seconds
                }, 0, 15, TimeUnit.SECONDS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
