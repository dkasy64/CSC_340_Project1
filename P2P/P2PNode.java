package P2P;

import java.io.IOException;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;

public class P2PNode {
    private final int port; // Port this node is running on
    private final List<InetSocketAddress> peerAddresses; // List of peer addresses
    private DatagramSocket socket; // UDP socket for communication
    private SecureRandom random; // SecureRandom for generating random intervals

    public P2PNode(int port, List<InetSocketAddress> peerAddresses) {
        this.port = port;
        this.peerAddresses = peerAddresses;
        this.random = new SecureRandom(); // Initialize SecureRandom
        try {
            this.socket = new DatagramSocket(port); // Bind socket to the specified port
        } catch (SocketException e) {
            System.err.println("Error creating socket on port " + port + ": " + e.getMessage());
            System.exit(1); // Exit if socket creation fails
        }
    }

    public void start() {
        // Start sending heartbeats in a separate thread
        new Thread(this::sendHeartbeats).start();

        // Start receiving heartbeats in a separate thread
        new Thread(this::receiveHeartbeats).start();
    }

    private void sendHeartbeats() {
        try {
            while (true) {
                for (InetSocketAddress peer : peerAddresses) {
                    // Create a heartbeat message
                    String message = "HEARTBEAT from " + port;
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, peer);

                    // Send the heartbeat to the peer
                    socket.send(packet);
                    System.out.println("Sent: " + message + " to " + peer.getPort());
                }

                // Sleep for a random interval between 0 and 30 seconds
                int sleepTime = random.nextInt(30000); // SecureRandom for randomness
                Thread.sleep(sleepTime);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error in sendHeartbeats: " + e.getMessage());
        }
    }

    private void receiveHeartbeats() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                // Wait to receive a heartbeat
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + message + " from " + packet.getPort());
            }
        } catch (IOException e) {
            System.err.println("Error in receiveHeartbeats: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java P2P.P2PNode <port>");
            System.exit(1);
        }

        // Read the configuration file
        String configFilePath = "config.txt"; // Path to the config file
        System.out.println("Reading config from: " + configFilePath);
        List<Node> nodes = ConfigReader.readConfig(configFilePath);

        // Get the current node's port from command-line arguments
        int myPort = Integer.parseInt(args[0]);

        // Create a list of peer addresses (excluding the current node)
        List<InetSocketAddress> peerAddresses = new ArrayList<>();
        for (Node node : nodes) {
            if (node.getPort() != myPort) { // Exclude the current node
                peerAddresses.add(new InetSocketAddress(node.getIpAddress(), node.getPort()));
            }
        }

        System.out.println("Peer addresses: " + peerAddresses);

        // Start the P2P node
        P2PNode node = new P2PNode(myPort, peerAddresses);
        node.start();
    }
}