package cs.tcd.ie;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class for packet content that represents acknowledgments
 * 
 */
public class NakPacketContent extends PacketContent {

    String info;

    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    NakPacketContent(String info) {

        type= NAKPACKET;
        this.info = info;
    }

    /**
     * Constructs an object out of a datagram packet.
     * @param packet Packet that contains information about a file.
     */
    protected NakPacketContent(ObjectInputStream oin) {
        try {
            type= NAKPACKET;
            info= oin.readUTF();
        } 
        catch(Exception e) {e.printStackTrace();}
    }

    /**
     * Writes the content into an ObjectOutputStream
     *
     */
    protected void toObjectOutputStream(ObjectOutputStream oout) {
        try {
            oout.writeUTF(info);
        }
        catch(Exception e) {e.printStackTrace();}
    }



    /**
     * Returns the content of the packet as String.
     * 
     * @return Returns the content of the packet as String.
     */
    public String toString() {
        return "NAK:" + info;
    }

    /**
     * Returns the info contained in the packet.
     * 
     * @return Returns the info contained in the packet.
     */
    public String getPacketInfo() {
        return info;
    }
}