package P2P;

import java.io.Serializable;

public class ProtocolPacket implements Serializable {
    private String version;
    private int length;
    private int flags; // 0: Heartbeat
    private String reserved;
    private int nodeID;
    private long timestamp;

    public ProtocolPacket(String version, int length, int flags, String reserved, int nodeID, long timestamp) {
        this.version = version;
        this.length = length;
        this.flags = flags;
        this.reserved = reserved;
        this.nodeID = nodeID;
        this.timestamp = timestamp;
    }

    // Getters
    public String getVersion() { return version; }
    public int getLength() { return length; }
    public int getFlags() { return flags; }
    public String getReserved() { return reserved; }
    public int getNodeID() { return nodeID; }
    public long getTimestamp() { return timestamp; }
}