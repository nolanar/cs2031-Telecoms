package cs.tcd.ie;

import static cs.tcd.ie.PacketContent.ACK_FRAME;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class for packet content that represents acknowledgments
 * 
 */
public class StringContent extends PacketContent {

    String message;

    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    StringContent(String message) {
        type= STRINGPACKET;
        this.message = message;
    }

    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    StringContent(int number, String message) {
        this.number = number;
        type= STRINGPACKET;
        this.message = message;
    }

    /**
     * Constructs an object out of a datagram packet.
     * @param packet Packet that contains information about a file.
     */
    protected StringContent(ObjectInputStream oin) {
        try {
            type= STRINGPACKET;
            number = oin.readInt();
            message= oin.readUTF();
        } 
        catch(Exception e) {e.printStackTrace();}
    }

    /**
     * Writes the content into an ObjectOutputStream
     *
     */
    protected void toObjectOutputStream(ObjectOutputStream oout) {
        try {
            oout.writeUTF(message);
        }
        catch(Exception e) {e.printStackTrace();}
    }



    /**
     * Returns the content of the packet as String.
     * 
     * @return Returns the content of the packet as String.
     */
    public String toString() {
        return message;
    }
}