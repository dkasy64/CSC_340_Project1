package P2P;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

public class Node {
    private int nodeID;
    private String ipAddress;
    private int port;
    private DatagramSocket socket;
    private Map<Integer, Long> lastHeartbeatTimes; // Tracks last heartbeat time for each node
    private Map<Integer, String> nodeFileListings; // Stores file listings for each node
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private String homeDirectoryPath;

    //Cosmetics to make it *pretty*
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_RESET = "\u001B[0m";

    public Node(int nodeID, String ipAddress, int port, String homeDirectoryPath) {
        this.nodeID = nodeID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.homeDirectoryPath = homeDirectoryPath;
        this.lastHeartbeatTimes = new HashMap<>();
        this.nodeFileListings = new HashMap<>();
        System.out.println("Initial state: All nodes are Dead");
        initializeSocket();
    }

    private void initializeSocket() {
        try {
            this.socket = new DatagramSocket(port);
            System.out.println("Node " + nodeID + " is listening on port " + port);

            // Add a shutdown hook to close the socket gracefully
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("Node " + nodeID + " socket closed.");
                }
            }));
        } catch (SocketException e) {
            System.err.println("Failed to bind Node " + nodeID + " to port " + port + ": " + e.getMessage());
            // Retry with a different port
            this.port = port + 1; // Increment port number
            initializeSocket(); // Retry initialization
        }
    }

    public int getNodeID() { return nodeID; }
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; }

    // Method to send a heartbeat to all other nodes
    public void sendHeartbeat(List<Node> nodes) {
        try {
            // Get current file listing
            String fileList = getFileNames();
            
            // Create a protocol packet that includes file listing
            ProtocolPacket packet = new ProtocolPacket(
                "1.0", 0, 0, "reserved", nodeID, System.currentTimeMillis(), fileList);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(packet);
            byte[] data = baos.toByteArray();

            for (Node node : nodes) {
                if (node.getNodeID() != this.nodeID) { // Don't send to self
                    InetAddress address = InetAddress.getByName(node.getIpAddress());
                    DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, node.getPort());
                    socket.send(datagramPacket);
                    //System.out.println("[SENT] Heartbeat to Node " + node.getNodeID() + " at " + timeFormat.format(new Date()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listFilesInDirectory() {
        File folder = new File(homeDirectoryPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            System.out.println("Files in " + homeDirectoryPath + " for Node " + nodeID + ":");
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    System.out.println(file.getName());
                }
            }
        } else {
            System.out.println("The directory " + homeDirectoryPath + " does not exist or is not a directory.");
        }
    }

    public void listen() {
        new Thread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[4096]; // Larger buffer to accommodate file listings
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Get the sender's actual IP address from the packet
                    InetAddress senderAddress = packet.getAddress();
                    
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    ProtocolPacket receivedPacket = (ProtocolPacket) ois.readObject();
                    
                    int claimedSenderID = receivedPacket.getNodeID();
                    long receivedTime = System.currentTimeMillis();
                    String receivedFileList = receivedPacket.getFileList();

                    // Look up the node that matches the claimed sender ID
                    boolean validSender = false;
                    for (Map.Entry<Integer, NodeInfo> entry : knownNodes.entrySet()) {
                        if (entry.getKey() == claimedSenderID) {
                            NodeInfo nodeInfo = entry.getValue();
                            // Verify that the sender's IP matches what we expect for that node ID
                            if (senderAddress.getHostAddress().equals(nodeInfo.ipAddress)) {
                                validSender = true;
                                break;
                            }
                        }
                    }

                    if (validSender) {
                        lastHeartbeatTimes.put(claimedSenderID, receivedTime);
                        // Store the file listing from this node
                        nodeFileListings.put(claimedSenderID, receivedFileList);
                        
                        //System.out.println("[OK] Received heartbeat from Node " + claimedSenderID + 
                                         //" at " + timeFormat.format(new Date()) + 
                                         //" (IP: " + senderAddress.getHostAddress() + ")");
                    } else {
                        //System.out.println("[WARN] Received packet claiming to be from Node " + claimedSenderID + 
                                         //" but source IP " + senderAddress.getHostAddress() + 
                                         //" doesn't match expected IP for that node");
                    }

                    displayStatus();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Store information about other nodes
    private Map<Integer, NodeInfo> knownNodes = new HashMap<>();
    
    // Method to register other nodes
    public void registerNodes(List<Node> nodes) {
        for (Node node : nodes) {
            if (node.getNodeID() != this.nodeID) {
                knownNodes.put(node.getNodeID(), new NodeInfo(node.getIpAddress(), node.getPort()));
            }
        }
    }

    // Helper class to store node information
    private static class NodeInfo {
        String ipAddress;
        int port;
        
        NodeInfo(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }
    }

    private void displayStatus() {
        //System.out.println("Debug: Current lastHeartbeatTimes = " + lastHeartbeatTimes);
        System.out.println(ANSI_BOLD + "Node ID\tStatus\tLast Heartbeat\tFiles in Directory" + ANSI_RESET);
        long currentTime = System.currentTimeMillis();
        List<Integer> deadNodes = new ArrayList<>();

        for (int nodeID = 1; nodeID <= 6; nodeID++) { // Assuming 3 nodes
            String status;
            String lastHeartbeatTime;
            String fileNames = "";

            // For the current node, always get the current file listing
            if (nodeID == this.nodeID) {
                fileNames = getFileNames();
                status = ANSI_GREEN + "Alive" + ANSI_RESET;
                lastHeartbeatTime = "Self";
            } else if (lastHeartbeatTimes.containsKey(nodeID)) {
                long lastHeartbeat = lastHeartbeatTimes.get(nodeID);
                if (currentTime - lastHeartbeat < 10000) {
                    status = ANSI_GREEN + "Alive" + ANSI_RESET;
                    lastHeartbeatTime = timeFormat.format(new Date(lastHeartbeat)) + 
                                       " (" + (currentTime - lastHeartbeat) + "ms ago)";
                    
                    // Show file listing only for alive nodes
                    fileNames = nodeFileListings.getOrDefault(nodeID, "No files reported");
                } else {
                    status = ANSI_RED + "Dead" + ANSI_RESET;
                    lastHeartbeatTime = timeFormat.format(new Date(lastHeartbeat)) + 
                                       " (" + (currentTime - lastHeartbeat) + "ms ago)";
                    deadNodes.add(nodeID); // Mark for removal
                    
                    // Don't show file listing for dead nodes
                    fileNames = ANSI_RED + "Node is dead" + ANSI_RESET;
                }
            } else {
                status = ANSI_RED + "Dead" + ANSI_RESET;
                lastHeartbeatTime = "Never";
                fileNames = ANSI_RED + "Node is dead" + ANSI_RESET;
            }

            System.out.println(nodeID + "\t" + status + "\t" + lastHeartbeatTime + "\t\t" + ANSI_YELLOW + fileNames + ANSI_RESET);
        }

        // Remove stale heartbeats to avoid false positives
        for (int nodeID : deadNodes) {
            lastHeartbeatTimes.remove(nodeID);
            // Also remove file listings for dead nodes
            nodeFileListings.remove(nodeID);
        }

        System.out.println("-------------------\n");
    }

    // Helper method to get file names
    private String getFileNames() {
        File folder = new File(homeDirectoryPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            return "No files or directory not found";
        }
        
        return Arrays.stream(listOfFiles)
                     .filter(File::isFile)
                     .map(File::getName)
                     .collect(Collectors.joining(", "));
    }
}