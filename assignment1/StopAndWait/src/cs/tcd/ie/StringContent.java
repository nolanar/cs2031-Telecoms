package cs.tcd.ie;

import java.net.DatagramPacket;

public class StringContent implements PacketContent {

    public static final char NULL_CHAR = '\u0000';
    
    private String string;
	
    public StringContent(DatagramPacket packet) {
        byte[] data;

        data= packet.getData();
        string = new String(data);
        string = string.substring(0, string.indexOf(NULL_CHAR));
    }

    public StringContent(String string) {
        this.string = string;
    }

    public String toString() {
        return string;
    }

    public DatagramPacket toDatagramPacket() {
        DatagramPacket packet= null;
        try {
                byte[] data= string.getBytes();
                packet= new DatagramPacket(data, data.length);
        }
        catch(Exception e) {e.printStackTrace();}
        return packet;
    }
}
