package cs.tcd.ie;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class for packet content that represents acknowledgments
 * 
 */
public class NakSelectContent extends PacketContent {

    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    NakSelectContent(int number) {
        type= NAK_SELECT;
        this.number = number;
    }    
    
    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    NakSelectContent(int number, String info) {
        type= NAK_SELECT;
        this.number = number;
    }

    /**
     * Constructs an object out of a datagram packet.
     * @param oin
     */
    protected NakSelectContent(ObjectInputStream oin) {
        try {
            type= NAK_SELECT;
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
        return "NAK" + number + " -- Selective";
    }
}