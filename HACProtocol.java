/**
 * Team name: Vegas 
 * @auther Natalie Spiska, Dawit Kasy,Tuana Turhan 
 * @date 02/23/2025
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
// import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class HACProtocol {
    //protocol version 
    public static final byte PROTOCOL_VERSION=0x01;

    //message types
    public static final byte MSG_TYPE_HEARTBEAT =0x01; // is used to indicate that a node is still active.
    public static final byte MSG_TYPE_UPDATE =0x02; //is used to report file changes on the node.
    public static final byte MSG_TYPE_NODE_STATUS =0x03; //node's IP address, status, etc.
    public static final byte MSG_TYPE_SERVER_ELECTION =0x04; //is used in the process of identifying a new server.

    public static class PacketHeader{
        byte version ; //prtocol version
        byte messageType; //type of message (hearbeat)
        short length; // length of data 
        int nodeId; //sender node Id
        long timestamp; //packet timestamp
        byte flags; //various flags 

        public static final int HEADER_SIZE = 12; //sets the total size of the header to 12 bytes.

        //flags definitions
        public static final byte FLAG_URGENT= 0x01; //it means urgent message.
        public static final byte FLAG_RESPONSE_NEEDED= 0x02; // message waiting for a response.
        public static final byte FLAG_SERVER_MODE= 0x03; //it is the node marker running in server mode.
    }
    public static class FileListningData{
        String directoryPath; //indicates in which directory the files are listed
        List<FileInfo> files; //a list containing file information

         public static class FileInfo{
            String fileName; // name of the file
            long lastModified; //last modified date (in timestamp)
            long size; //file size 
         }
         
    }
    public static class NodeStatusData {
        int nodeId; //identification of the node
        String ipAddress; //IP adresi.
        int port; // the port number for which node is running 
        long lastHeartbeat; //time for the last heartbeat message
        boolean isAlive; //whether the node is alive or not
        FileListingData fileListing; // file information that the node has
    }

    //Packet building methods(generates a heartbeat message.)
    public static byte[] buildHeartbeatPacket(int nodeId,boolean isServer){
        ByteArrayOutputStream baos =new ByteArrayOutputStream();
        DataOutputStream dos =new DataOutputStream(baos);
        try{
            // Header
            dos.writeByte(PROTOCOL_VERSION); // write the protocol version
            dos.writeByte(MSG_TYPE_HEARTBEAT); // type of message 
            dos.writeShort(8); // Data length
            dos.writeInt(nodeId); // write the node ID
            dos.writeLong(System.currentTimeMillis()); // write time stamp
            dos.writeByte(isServer ? PacketHeader.FLAG_SERVER_MODE : 0); //write flags

            // Data
            dos.writeInt(nodeId);
            dos.writeInt(isServer ? 1 : 0); //node ID and whether it is a server or not.
            return baos.toByteArray(); //the packet is returned as a byte array.

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }
    // Packet parsing methods
    //reads the header of an incoming packet and parses it into a PacketHeader object.
    public static PacketHeader parseHeader(byte[] data) {
        PacketHeader header = new PacketHeader();
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            header.version = dis.readByte();
            header.messageType = dis.readByte();
            header.length = dis.readShort();
            header.nodeId = dis.readInt();
            header.timestamp = dis.readLong();
            header.flags = dis.readByte();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return header;
    }
}
