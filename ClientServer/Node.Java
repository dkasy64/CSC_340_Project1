package ClientServer;

import java.io.File;

public class Node {
    private int nodeID;
    private String ipAddress;
    private int port;
    private String homeDir;

    public Node(int nodeID, String ipAddress, int port, String homeDir) {
        this.nodeID = nodeID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.homeDir = homeDir;
    }

    public int getNodeID() {
        return nodeID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public String getFileNames() {
        File directory = new File(homeDir);
        String returnString = "";
        String[] files = directory.list();
        if (files != null) {
            returnString = String.join(", ", files);
        } else {
            returnString = "No files found or directory does not exist";
        }
        return returnString;
    }
}
