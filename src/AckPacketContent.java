

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class for packet content that represents acknowledgments
 * 
 */
public class AckPacketContent extends PacketContent {


    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    AckPacketContent(int number) {
        type= ACKPACKET;
        setNumber(number);
    }

    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    AckPacketContent(int number, String info) {
        type= ACKPACKET;
        setNumber(number);
    }

    /**
     * Constructs an object out of a datagram packet.
     * @param oin
     */
    protected AckPacketContent(ObjectInputStream oin) {
        try {
            type= ACKPACKET;
            setNumber(oin.readInt());
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
        return "ACK" + getNumber();
    }
}