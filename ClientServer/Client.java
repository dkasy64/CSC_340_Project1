package ClientServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
    private Node node;
    private InetAddress serverAddress;
    private int serverPort;
    private SecureRandom random;
    //put your path here to the server config file
    private static String serverConfigFilePath = "/Users/nataliespiska/CSC_340_Project1/ClientServer/ServerConfig.txt";

    public Client(Node node, String serverIP, int serverPort) throws Exception {
        this.node = node;
        this.serverAddress = InetAddress.getByName(serverIP);
        this.serverPort = serverPort;
        this.random = new SecureRandom();
    }

    //starts a thread to send updates
    public void start() {
        new Thread(this::recieveStatusUpdates).start();
    }

    public Node getNode() {
        return node;
    }

     public void recieveStatusUpdates(){
         try (DatagramSocket recieveSocket = new DatagramSocket(node.getPort())) {
             byte[] buffer = new byte[1024];
             while (true) {
                 DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                 recieveSocket.receive(packet);
                 String message = new String(packet.getData(), 0, packet.getLength());
                 System.out.println("Received: " + message);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

    public static void main(String[] args) throws Exception {
        Client client = null;
        String clientConfigFilePath = null;
        String nodeId;
        if (args.length != 2) {
            //use your client config file path here
            System.out.println("Usage: java ClientServer.Client <nodeID> <clientConfigFilePath>");
            return;
        } else {
            clientConfigFilePath = args[1];
            nodeId = args[0];
        }

        // Read the configuration file
        List<Node> nodes = ConfigReader.readConfig(clientConfigFilePath);

        //pick node representing this Client ID
        for (Node node : nodes) {
           // if(node.getNodeID() == Integer.parseInt(args[0])){
                if(node.getNodeID() == Integer.parseInt(nodeId)){
                    client = new Client(node, null, 0);
                    break;
            }
        }

            DatagramSocket socket = new DatagramSocket();
            //CHANGE IP ADDRESS HERE!

            System.out.println("Using default hardcoded server config file path:" + serverConfigFilePath);
            List<Node> serverNodes = ConfigReader.readConfig(serverConfigFilePath);

            if (serverNodes.size() != 1) {
                System.out.println("There should be only one node in the configuration file.");
                return;
            }

            String serverIPAddress = null;
            int serverPort = 0;

            // Add client nodes to the server
            for (Node serverNode : serverNodes) {
                serverIPAddress = serverNode.getIpAddress();
                serverPort = serverNode.getPort();
            }

            System.out.println("Server IP Address: " + serverIPAddress + " Server Port: " + serverPort);
            InetAddress serverAddress = InetAddress.getByName(serverIPAddress);
            
           client.start();

            while (true) {
                        //IF SWITCHING BACK TO ONLY ONE ARG, nodeID needs to be nodeId (lowercase d)
                        String message = "HEARTBEAT|" + nodeId + "|";
                        byte[] data = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                        socket.send(packet);
                        message = "DIRECTORY_LISTING|" + nodeId + "|" + client.getNode().getFileNames();
                        data = message.getBytes();
                        packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                        socket.send(packet);
                       
                Thread.sleep(5000);
            }  
    }
}