package cs.tcd.ie;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class for packet content that represents acknowledgments
 * 
 */
public class NakPacketContent extends PacketContent {

    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    NakPacketContent(int number) {
        type= NAKPACKET;
        this.number = number;
    }    
    
    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    NakPacketContent(int number, String info) {
        type= NAKPACKET;
        this.number = number;
    }

    /**
     * Constructs an object out of a datagram packet.
     * @param oin
     */
    protected NakPacketContent(ObjectInputStream oin) {
        try {
            type= NAKPACKET;
            number = oin.readInt();
        } 
        catch(Exception e) {e.printStackTrace();}
    }

    /**
     * Writes the content into an ObjectOutputStream
     *
     * @param oout
     */
    @Override
    protected void toObjectOutputStream(ObjectOutputStream oout) {
        try {
            oout.writeInt(number);
        }
        catch(Exception e) {e.printStackTrace();}
    }



    /**
     * Returns the content of the packet as String.
     * 
     * @return Returns the content of the packet as String.
     */
    @Override
    public String toString() {
        return "NAK" + number;
    }
}