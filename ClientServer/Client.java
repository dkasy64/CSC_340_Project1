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
        String nodeId;
        if (args.length != 1) {
            System.out.println("Usage: java ClientServer.Client <nodeID>");
            return;
        } else {
            nodeId = args[0];
        }

        // Read the configuration file
        List<Node> nodes = ConfigReader.readConfig(null);

        //pick node representing this Client ID
        for (Node node : nodes) {
           // if(node.getNodeID() == Integer.parseInt(args[0])){
                if(node.getNodeID() == Integer.parseInt(nodeId)){
                    client = new Client(node, null, 0);
                    break;
            }
        }

            DatagramSocket socket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName("127.0.0.1");
            int serverPort = 5001;
            
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