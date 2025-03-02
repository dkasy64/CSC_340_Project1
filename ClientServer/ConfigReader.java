package ClientServer;

import java.io.*;
import java.util.*;

public class ConfigReader {
    public static List<Node> readConfig(String filePath, int serverPort) throws IOException {
        System.out.println("Reading configuration file: " + filePath);
        List<Node> nodes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue; // Skip empty lines and comments
            }
            String[] parts = line.split(",");
            int nodeID = Integer.parseInt(parts[0].trim());
            String ipAddress = parts[1].trim();
            int port = Integer.parseInt(parts[2].trim());

            //makes sure the Server node is not the same port number as the client nodes already exisisting :)
            if(port == serverPort) {
                System.out.println("Port ID cannot be the same as the server port, generating new one");
                port = serverPort + 1000;
                System.out.println("New port ID: " + port);
            }

            String homeDir = parts[3].trim();

            Node nodeInfo = new Node(nodeID, ipAddress, port, homeDir);
            nodes.add(nodeInfo);
        }

        reader.close();
        return nodes;
    }
}