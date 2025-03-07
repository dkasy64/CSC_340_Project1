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
        //System.out.println("Sending: " + message + " to " + clientIP + ":" + clientPort);
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


  //this method broadcasts the status of all of the clients to all of the clients, so
    //they all know what's what, and also prints out the status of each node to da console
    private void broadcastStatus() {
        while (true) {
            try {
                Thread.sleep(TIMEOUT); //updates must match the timeout
                long currentTime = System.currentTimeMillis();
                StringBuilder statusMessage = new StringBuilder("STATUS_UPDATE " + counter++);
                StringBuilder statusMessageForNodes = new StringBuilder("SERVER_STATUS_REPLY "+ counter);
                for (Map.Entry<Integer, String> entry : clientStatus.entrySet()) {
                    int clientID = entry.getKey();
                    long lastHeartBeat = clientLastHeartbeat.get(clientID);
                    if (currentTime - lastHeartBeat <= TIMEOUT) {
                        statusMessage.append("\n").append("Node").append(clientID).append(" : Avaliable");
                    } else {
                        clientDirectoryListing.remove(clientID);
                        statusMessage.append("\n").append("Node").append(clientID).append(" : Unavaliable");
                    }

                }

                if(clientDirectoryListing.isEmpty()) {
                    statusMessage.append("\nDIRECTORY LISTING FROM AVAILABLE NODES: ");
                    statusMessage.append("\nNo nodes available");
                    statusMessageForNodes.append("\nDIRECTORY LISTING FROM AVAILABLE NODES: ");
                    statusMessageForNodes.append("\nNo nodes available");
                } else {
                    statusMessage.append("\nDIRECTORY LISTING FROM AVAILABLE NODES: ");
                    statusMessageForNodes.append("\nDIRECTORY LISTING FROM AVAILABLE NODES: ");
                }

                for (Map.Entry<Integer, String> entry : clientDirectoryListing.entrySet()) {
                    int clientID = entry.getKey();
                    String directoryListing = clientDirectoryListing.get(clientID);
                    statusMessage.append("\n").append("Node").append(clientID).append(" : ").append(directoryListing);
                    statusMessageForNodes.append("\n").append("Node").append(clientID).append(" : ").append(directoryListing);
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


    private void receivePackets() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            System.out.println("Server started at: " + port);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                //System.out.println("Received: " + message);
                String[] parts = message.split("\\|");
                String messageType = parts[0];
                int nodeId = Integer.parseInt(parts[1]);

                if (messageType.equals("HEARTBEAT")) {
                    clientLastHeartbeat.put(nodeId, System.currentTimeMillis());
                    clientStatus.put(nodeId, "Available");
                } else if (messageType.equals("DIRECTORY_LISTING")) {
                    try{
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

    public static void main(String[] args) throws IOException {
        int port = 5001;
        //For my own testing purposes
        String configFilePath = null;
         if (args.length != 2) {
            //You need client config file path to run the server
            System.out.println("Usage: java ClientServer.Server <port> <clientConfigFilePath>");
            return;
         } else {
            port =  Integer.parseInt(args[0]);
            configFilePath = args[1];
         }

        // Create the server
        Server server = new Server(port);

        // Read the configuration file
        List<Node> nodes = ConfigReader.readConfig(configFilePath);
        
        // Add client nodes to the server
        for (Node node : nodes) {
            server.addClientNode(node);
        }

        // Start the server
        server.start();
    }
}
