package P2P;

import java.io.IOException;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;

public class P2PNode {
    private final int port;
    private final List<InetSocketAddress> peerAddresses;
    private DatagramSocket socket;
    private SecureRandom random;

    public P2PNode(int port, List<InetSocketAddress> peerAddresses) {
        this.port = port;
        this.peerAddresses = peerAddresses;
        this.random = new SecureRandom();
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        // Start sending heartbeats
        new Thread(this::sendHeartbeats).start();

        // Start receiving heartbeats
        new Thread(this::receiveHeartbeats).start();
    }

    private void sendHeartbeats() {
        try {
            while (true) {
                for (InetSocketAddress peer : peerAddresses) {
                    String message = "HEARTBEAT from " + port;
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, peer);
                    socket.send(packet);
                    System.out.println("Sent: " + message + " to " + peer.getPort());
                }
                // Sleep for a random interval between 0 and 30 seconds
                Thread.sleep(random.nextInt(30000));
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void receiveHeartbeats() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (true) {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + message + " from " + packet.getPort());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Read the configuration file
        String configFilePath = "config.txt"; // Path to your config file
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

        // Start the P2P node
        P2PNode node = new P2PNode(myPort, peerAddresses);
        node.start();
    }
}