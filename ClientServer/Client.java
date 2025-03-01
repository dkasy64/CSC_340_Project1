package ClientServer;

//I LITERALLY DONT UNDERSTAND WHY THIS FILE DOESN'T
//IMPORT ITS LITERALLY FINE JHGJASHGUIHRWJGNSFKJGHJSR
//import ClientServer.Node;
import P2P.Node;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;

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
        new Thread(this::sendUpdates).start();
    }

    //sends a message to the server using UDP, which is used in the broadcast message method
    private void sendUpdates() {
        try {
            DatagramSocket socket = new DatagramSocket();
            while (true) {
                String message = node.getNodeID() + ", available";
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
                socket.send(packet);
                //System.out.println("Sent: " + message + " to server");
                Thread.sleep(random.nextInt(30000));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}