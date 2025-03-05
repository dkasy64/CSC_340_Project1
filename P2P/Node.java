package P2P;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class Node {
    private int nodeID;
    private String ipAddress;
    private int port;
    private DatagramSocket socket;
    private Map<Integer, Long> lastHeartbeatTimes; // Tracks last heartbeat time for each node
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public Node(int nodeID, String ipAddress, int port) {
        this.nodeID = nodeID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastHeartbeatTimes = new HashMap<>(); // Ensure this is empty
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
            ProtocolPacket packet = new ProtocolPacket("1.0", 0, 0, "reserved", nodeID, System.currentTimeMillis());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(packet);
            byte[] data = baos.toByteArray();

            for (Node node : nodes) {
                if (node.getNodeID() != this.nodeID) { // Don't send to self
                    InetAddress address = InetAddress.getByName(node.getIpAddress());
                    DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, node.getPort());
                    socket.send(datagramPacket);
                    System.out.println("[SENT] Heartbeat to Node " + node.getNodeID() + " at " + timeFormat.format(new Date()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        new Thread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Get the sender's actual IP address from the packet
                    InetAddress senderAddress = packet.getAddress();
                    int senderPort = packet.getPort();
                    
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    ProtocolPacket receivedPacket = (ProtocolPacket) ois.readObject();

                    int claimedSenderID = receivedPacket.getNodeID();
                    long receivedTime = System.currentTimeMillis();

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
                        System.out.println("[OK] Received heartbeat from Node " + claimedSenderID + 
                                          " at " + timeFormat.format(new Date()) + 
                                          " (IP: " + senderAddress.getHostAddress() + ")");
                    } else {
                        System.out.println("[WARN] Received packet claiming to be from Node " + claimedSenderID + 
                                          " but source IP " + senderAddress.getHostAddress() + 
                                          " doesn't match expected IP for that node");
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
        System.out.println("Debug: Current lastHeartbeatTimes = " + lastHeartbeatTimes);
        System.out.println("Node ID\tStatus\tLast Heartbeat");
        long currentTime = System.currentTimeMillis();
        List<Integer> deadNodes = new ArrayList<>();

        for (int nodeID = 1; nodeID <= 3; nodeID++) { // Assuming 3 nodes
            if (nodeID == this.nodeID) { // A node should never mark itself as dead
                System.out.println(nodeID + "\tAlive\tSelf");
                continue;
            }

            // Only mark as alive if a heartbeat was received within the last 10 seconds
            if (lastHeartbeatTimes.containsKey(nodeID)) {
                long lastHeartbeat = lastHeartbeatTimes.get(nodeID);
                if (currentTime - lastHeartbeat < 10000) {
                    System.out.println(nodeID + "\tAlive\t" + 
                                      timeFormat.format(new Date(lastHeartbeat)) + 
                                      " (" + (currentTime - lastHeartbeat) + "ms ago)");
                } else {
                    System.out.println(nodeID + "\tDead\t" + 
                                      timeFormat.format(new Date(lastHeartbeat)) + 
                                      " (" + (currentTime - lastHeartbeat) + "ms ago)");
                    deadNodes.add(nodeID); // Mark for removal
                }
            } else {
                System.out.println(nodeID + "\tDead\tNever");
            }
        }

        // Remove stale heartbeats to avoid false positives
        for (int nodeID : deadNodes) {
            lastHeartbeatTimes.remove(nodeID);
        }

        System.out.println("-------------------");
    }
}