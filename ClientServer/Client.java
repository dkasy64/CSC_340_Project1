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

    //receives status updates from the server and prints them
     public void recieveStatusUpdates(){
         try (DatagramSocket recieveSocket = new DatagramSocket(node.getPort())) {
             byte[] buffer = new byte[1024];
             while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                recieveSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println(message);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

    //main method to run the client, and reads from the config file
    public static void main(String[] args) throws Exception {
        Client client = null;
        String clientConfigFilePath = null;
        String nodeId;
        if (args.length != 2) {
            //use your client config file path here
            //example: java ClientServer/Client 3 /Users/nataliespiska/CSC_340_Project1/ClientServer/ClientConfig.txt  
            System.out.println("Usage: java ClientServer.Client <nodeID> <clientConfigFilePath>");
            return;
        } else {
            clientConfigFilePath = args[1];
            nodeId = args[0];
        }

        //Read the configuration file
        List<Node> nodes = ConfigReader.readConfig(clientConfigFilePath);

        for (Node node : nodes) {
            if(node.getNodeID() == Integer.parseInt(nodeId)){
                client = new Client(node, null, 0);
                break;
            }
        }

        DatagramSocket socket = new DatagramSocket();
        System.out.println("Using default hardcoded server config file path:" + serverConfigFilePath);
        List<Node> serverNodes = ConfigReader.readConfig(serverConfigFilePath);

        if (serverNodes.size() != 1) {
            System.out.println("There should be only one node in the configuration file.");
            return;
        }

        String serverIPAddress = null;
        int serverPort = 0;

        //gets the server IP address and port
        for (Node serverNode : serverNodes) {
            serverIPAddress = serverNode.getIpAddress();
            serverPort = serverNode.getPort();
        }

        //Print the server IP address and port, and start the client
        System.out.println("Server IP Address: " + serverIPAddress + " Server Port: " + serverPort);
        InetAddress serverAddress = InetAddress.getByName(serverIPAddress);    
        client.start();

        //Send heartbeat and directory listing to the server
        while (true) {
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