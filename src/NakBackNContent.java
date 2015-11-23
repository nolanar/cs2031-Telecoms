

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class for packet content that represents acknowledgments
 * 
 */
public class NakBackNContent extends PacketContent {

    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    NakBackNContent(int number) {
        type= NAK_BACK_N;
        setNumber(number);
    }    
    
    /**
     * Constructor that takes in information about a file.
     * @param filename Initial filename.
     * @param size Size of filename.
     */
    NakBackNContent(int number, String info) {
        type= NAK_BACK_N;
        setNumber(number);
    }

    /**
     * Constructs an object out of a datagram packet.
     * @param oin
     */
    protected NakBackNContent(ObjectInputStream oin) {
        try {
            type= NAK_BACK_N;
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
            oout.writeInt(getNumber());
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
        return "NAK" + getNumber() + " -- Go Back N";
    }
}