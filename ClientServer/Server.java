package ClientServer;

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
    private Map<Integer, String> clientDirectoryListing;
    private Map<Integer, Long> clientLastHeartbeat;
    private static final long TIMEOUT = 5000; //30 seconds
    private int counter;

    //Cosmetics to make it *pretty*
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_RESET = "\u001B[0m";


    //making all the maps and setting the port
    public Server(int port) {
        this.port = port;
        this.clientNodes = new HashMap<>();
        this.clientStatus = new HashMap<>();
        this.clientDirectoryListing = new HashMap<>();
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
        if (clientPort == 0) {
            System.out.println("Invalid client port: " + clientPort);
            return;
        }
        DatagramSocket socket = new DatagramSocket();
        InetAddress clientAddress = InetAddress.getByName(clientIP);
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
        socket.send(packet);
        socket.close();
    }


    //This method is used to broadcast the status of the server to all the clients, as well as
    //to update the status of the clients as the server receives updates from the clients
    private void broadcastStatus() {
        while (true) {
            try {
                Thread.sleep(TIMEOUT); //updates must match the timeout
                long currentTime = System.currentTimeMillis();
                StringBuilder statusMessage = new StringBuilder(ANSI_BOLD + "\nSTATUS_UPDATE " + counter++ + ANSI_RESET);
                StringBuilder statusMessageForNodes = new StringBuilder("SERVER_STATUS_REPLY "+ counter);
                for (Map.Entry<Integer, String> entry : clientStatus.entrySet()) {
                    int clientID = entry.getKey();
                    long lastHeartBeat = clientLastHeartbeat.get(clientID);
                    if (currentTime - lastHeartBeat <= TIMEOUT) {
                        statusMessage.append("\n").append("Node").append(clientID).append(": ").append(ANSI_GREEN).append("Avaliable").append(ANSI_RESET);
                    } else {
                        clientDirectoryListing.remove(clientID);
                        statusMessage.append("\n").append("Node").append(clientID).append(": ").append(ANSI_RED).append("Unavaliable").append(ANSI_RESET);
                    }

                }

                //this basically sends the directory listing of the clients to the server
                if(clientDirectoryListing.isEmpty()) {
                    statusMessage.append(ANSI_BOLD).append("\nDIRECTORY LISTING FROM AVAILABLE NODES: ").append(ANSI_RESET);
                    statusMessage.append(ANSI_RED).append("\nNo nodes available").append(ANSI_RESET);
                    statusMessageForNodes.append(ANSI_BOLD).append("\n\nDIRECTORY LISTING FROM AVAILABLE NODES: ").append(ANSI_RESET);
                    statusMessageForNodes.append(ANSI_RED).append("\nNo nodes available").append(ANSI_RESET);
                } else {
                    statusMessage.append(ANSI_BOLD).append("\nDIRECTORY LISTING FROM AVAILABLE NODES: ").append(ANSI_RESET);
                    statusMessageForNodes.append(ANSI_BOLD).append("\n\nDIRECTORY LISTING FROM AVAILABLE NODES: ").append(ANSI_RESET);
                }

                for (Map.Entry<Integer, String> entry : clientDirectoryListing.entrySet()) {
                    int clientID = entry.getKey();
                    String directoryListing = clientDirectoryListing.get(clientID);
                    statusMessage.append(ANSI_YELLOW).append("\n").append("Node").append(clientID).append(" : ").append(directoryListing).append(ANSI_RESET);
                    statusMessageForNodes.append(ANSI_YELLOW).append("\n").append("Node").append(clientID).append(" : ").append(directoryListing).append(ANSI_RESET);
                }

                String message = statusMessage.toString();
                String messageForNodes = statusMessageForNodes.toString();
                System.out.println(message);

                //only send update to nodes which are available
                for (Map.Entry<Integer, String> entry : clientDirectoryListing.entrySet()) {
                    Node clientNode = clientNodes.get(entry.getKey());
                    if (clientNode != null) {
                        sendUpdate(messageForNodes, clientNode.getIpAddress(), clientNode.getPort());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        new Thread(this::broadcastStatus).start();
        new Thread(this::receivePackets).start();
    }

    //This method is used to receive packets from the clients, and then update the server
    //along with the file directory listing of the clients
    private void receivePackets() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            System.out.println("Server started at: " + port);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String[] parts = message.split("\\|");
                String messageType = parts[0];
                int nodeId = Integer.parseInt(parts[1]);

                if (messageType.equals("HEARTBEAT")) {
                    clientLastHeartbeat.put(nodeId, System.currentTimeMillis());
                    clientStatus.put(nodeId, "Available");
                } else if (messageType.equals("DIRECTORY_LISTING")) {
                    try {
                        String directoryListing = parts[2];
                        clientDirectoryListing.put(nodeId, directoryListing);    
                    } catch (Exception e) {
                        clientDirectoryListing.put(nodeId, "No files found or directory does not exist.");
                    }
                } else if (messageType.equals("STATUS_REQUEST")) {
                    String response = "STATUS_RESPONSE|" + nodeId + "|OK";
                    byte[] responseData = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //main method to run the server
    public static void main(String[] args) throws IOException {
        int port = 5001;
        String configFilePath = null;

        if (args.length != 2) {
            //You need client config file path to run the server
            //example: java ClientServer/Server 5001 /Users/nataliespiska/CSC_340_Project1/ClientServer/ClientConfig.txt
            System.out.println("Usage: java ClientServer.Server <port> <clientConfigFilePath>");
            return;
        } else {
            port =  Integer.parseInt(args[0]);
            configFilePath = args[1];
        }

        //create a new server
        Server server = new Server(port);
        //read the configuration file
        List<Node> nodes = ConfigReader.readConfig(configFilePath);

        //add client nodes to the server
        for (Node node : nodes) {
            server.addClientNode(node);
        }

        // Start the server
        server.start();
    }
}
