package ClientServer;

import P2P.Node;
import ClientServer.Server;
import ClientServer.Client;

public class Test {
    public static void main(String[] args) throws Exception {
        // Create server node
        Node serverNode = new Node(1, "127.0.0.1", 9876, "/server/home");
        Server server = new Server(serverNode.getPort());

        // Create client nodes
        Node clientNode1 = new Node(2, "127.0.0.1", 9877, "/client1/home");
        Node clientNode2 = new Node(3, "127.0.0.1", 9878, "/client2/home");
        Node clientNode3 = new Node(4, "127.0.0.1", 9879, "/client3/home");
        Node clientNode4 = new Node(5, "127.0.0.1", 9880, "/client4/home");
        Node clientNode5 = new Node(6, "127.0.0.1", 9881, "/client5/home");
    
        // Add client nodes to the server
        server.addClientNode(clientNode1);
        server.addClientNode(clientNode2);
        server.addClientNode(clientNode3);
        server.addClientNode(clientNode4);
        server.addClientNode(clientNode5);

        // Start the server to receive updates and broadcast status
        server.start();

        // Start the clients to send updates
        Client client1 = new Client(clientNode1, serverNode.getIpAddress(), serverNode.getPort());
        Client client2 = new Client(clientNode2, serverNode.getIpAddress(), serverNode.getPort());
        Client client3 = new Client(clientNode3, serverNode.getIpAddress(), serverNode.getPort());
        Client client4 = new Client(clientNode4, serverNode.getIpAddress(), serverNode.getPort());
        Client client5 = new Client(clientNode5, serverNode.getIpAddress(), serverNode.getPort());

        client1.start();
        client2.start();
        client3.start();
        client4.start();
        client5.start();
    }
}
