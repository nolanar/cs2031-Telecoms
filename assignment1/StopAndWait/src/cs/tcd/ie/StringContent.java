package cs.tcd.ie;

import java.net.DatagramPacket;

public class StringContent implements PacketContent {

    private String string;
	
    public StringContent(DatagramPacket packet) {
        
        string = new String(packet.getData(), 0, packet.getLength());
    }

    public StringContent(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
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
