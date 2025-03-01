package ClientServer;

import P2P.ConfigReader;
import P2P.Node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
    private int port;
    private Map<Integer, Node> clientNodes;
    private Map<Integer, String> clientStatus;
    private Map<Integer, Long> clientLastHeartbeat;
    private static final long TIMEOUT = 30000;
    private int counter;

    //making all the maps and setting the port
    public Server(int port) {
        this.port = port;
        this.clientNodes = new HashMap<>();
        this.clientStatus = new HashMap<>();
        this.clientLastHeartbeat = new HashMap<>();
        this.counter = 1;
    }

    //adding client nodes, and starting them off with an unknown status, that then changes
    //and updates as the server receives updates from the clients
    public void addClientNode(Node node) {
        clientNodes.put(node.getNodeID(), node);
        clientStatus.put(node.getNodeID(), "unknown");
        clientLastHeartbeat.put(node.getNodeID(), System.currentTimeMillis());
    }

    //sends a message to a client using UDP, which is used in the broadcast message method
    //to then send a status message to all of the clients, so they all know who's alive or not
    public void sendUpdate(String message, String clientIP, int clientPort) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress clientAddress = InetAddress.getByName(clientIP);
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
        socket.send(packet);
        socket.close();
    }

    //creates datagram socket to server's port, and listens for UDP packets
    //and when the packet is recieved, it extracts the message from the packet and then
    //calls handleUpdate so that the server can keep track of the status of all of the clients
    public void recieveUpdates() throws Exception {
        DatagramSocket socket = new DatagramSocket(port);
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true) {
            socket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength());
            handleUpdate(message);
        }
    }

    //this basically puts the updates of the clients into the clientStatus map so that
    //the server can keep track of the status of all of the clients :)
    private void handleUpdate(String message) {
        String[] parts = message.split(",");
        int clientID = Integer.parseInt(parts[0]);
        String availability = parts[1];
        clientStatus.put(clientID, availability);
        clientLastHeartbeat.put(clientID, System.currentTimeMillis());
    }

    //this method broadcasts the status of all of the clients to all of the clients, so
    //they all know what's what, and also prints out the status of each node to da console
    private void broadcastStatus() {
        while (true) {
            try {
                Thread.sleep(5000); //5 second updates
                long currentTime = System.currentTimeMillis();
                StringBuilder statusMessage = new StringBuilder("STATUS_UPDATE " + counter++);
                for (Map.Entry<Integer, String> entry : clientStatus.entrySet()) {
                    int clientID = entry.getKey();
                    long lastHeartBeat = clientLastHeartbeat.get(clientID);
                    if (currentTime - lastHeartBeat <= TIMEOUT) {
                        statusMessage.append("\n").append("Node").append(clientID).append(" : Avaliable");
                    } else {
                        statusMessage.append("\n").append("Node").append(clientID).append(" : Unavaliable");
                    }
                }
                String message = statusMessage.toString();
                System.out.println(message);
                for (Map.Entry<Integer, String> entry : clientStatus.entrySet()) {
                    Node clientNode = clientNodes.get(entry.getKey());
                    if (clientNode != null) {
                        sendUpdate(message, clientNode.getIpAddress(), clientNode.getPort());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //starts two threads, one for recieving updates from clients, and one for broadcasting status
    //to the clients so they know who's alive and who's not
    public void start() {
        new Thread(() -> {
            try {
                recieveUpdates();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(this::broadcastStatus).start();
    }

    // WORK IN PROGRESS, most of this is wrong hehe but I'm working on it :)
    //  public static void main(String[] args) throws IOException {
    //     if (args.length != 2) {
    //         System.out.println("Usage: java ClientServer.Server <port> <configFilePath>");
    //         return;
    //     }
    //     int port = Integer.parseInt(args[0]);
    //     String configFilePath = args[1];

    //     // Read the configuration file
    //     List<Node> nodes = ConfigReader.readConfig(configFilePath);

    //     // Create the server
    //     Server server = new Server(port);

    //     // Add client nodes to the server
    //     for (Node node : nodes) {
    //         server.addClientNode(node);
    //     }

    //     // Start the server
    //     server.start();
    // }
}
