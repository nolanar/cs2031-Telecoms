package cs.tcd.ie;

import java.net.SocketAddress;

/**
 *
 * @author aran
 */
public interface Sender {
    
    public void send(PacketContent packet);
    
    public void start();
}
