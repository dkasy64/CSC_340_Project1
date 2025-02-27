package P2P;

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
}
