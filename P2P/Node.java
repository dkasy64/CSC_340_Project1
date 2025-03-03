package P2P;

import java.io.*;
import java.net.*;
import java.util.*;

public class Node {
    private int nodeID;
    private String ipAddress;
    private int port;
    private DatagramSocket socket;
    private Map<Integer, Long> lastHeartbeatTimes; // Tracks last heartbeat time for each node

    public Node(int nodeID, String ipAddress, int port) {
        this.nodeID = nodeID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastHeartbeatTimes = new HashMap<>();
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
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to listen for incoming messages
    public void listen() {
        new Thread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    ProtocolPacket receivedPacket = (ProtocolPacket) ois.readObject();

                    // Update last heartbeat time
                    lastHeartbeatTimes.put(receivedPacket.getNodeID(), System.currentTimeMillis());
                    System.out.println("Received heartbeat from Node " + receivedPacket.getNodeID() + " at " + System.currentTimeMillis());

                    // Display status
                    displayStatus();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Method to display the status of all nodes
    private void displayStatus() {
        System.out.println("Node ID\tStatus");
        for (Map.Entry<Integer, Long> entry : lastHeartbeatTimes.entrySet()) {
            int nodeID = entry.getKey();
            long lastHeartbeat = entry.getValue();
            String status = (System.currentTimeMillis() - lastHeartbeat < 10000) ? "Alive" : "Dead"; // 10-second timeout
            System.out.println(nodeID + "\t" + status);
        }
        System.out.println("-------------------");
    }
}