package cs.tcd.ie;

import java.net.InetSocketAddress;

/**
 *
 * @author aran
 */
public interface Sender {
    
    public void send(PacketContent packet, InetSocketAddress address);
    
    public void start();
}
