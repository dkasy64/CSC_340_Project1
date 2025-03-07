package ClientServer;

import java.io.*;
import java.util.*;

public class ConfigReader {
    private static String CONFIG_FILE = "/Users/nataliespiska/CSC_340_Project1/ClientServer/ClientServerConfig.txt";
    public static List<Node> readConfig(String filePath) throws IOException {
    
        if (filePath == null) {
                filePath = CONFIG_FILE;
        }

        List<Node> nodes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue; // Skip empty lines and comments
            }
            String[] parts = line.split(",");
            int nodeID = Integer.parseInt(parts[0].trim());
            String ipAddress = parts[1].trim();;
            int port =  Integer.parseInt(parts[2].trim());;
            String homeDir = parts[3].trim();

            Node nodeInfo = new Node(nodeID, ipAddress, port, homeDir);
            nodes.add(nodeInfo);
        }

        reader.close();
        return nodes;
    }
}