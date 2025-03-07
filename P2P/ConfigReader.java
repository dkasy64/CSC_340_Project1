package P2P;

import java.io.*;
import java.util.*;

public class ConfigReader {
    public static List<Node> readConfig(String filePath) throws IOException {
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
            String homeDirectoryPath = parts[3].trim();

            Node node = new Node(nodeID, ipAddress, port, homeDirectoryPath);
            nodes.add(node);
        }

        reader.close();
        return nodes;
    }
}
