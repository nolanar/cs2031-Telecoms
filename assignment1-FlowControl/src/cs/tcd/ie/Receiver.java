package cs.tcd.ie;

import java.net.DatagramPacket;

/**
 *
 * @author aran
 */
public interface Receiver {
    
    public boolean receive(DatagramPacket dataPacket);
    
    public void start();
}
