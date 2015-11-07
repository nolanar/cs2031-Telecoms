package cs.tcd.ie;

/**
 *
 * @author aran
 */
public interface Sender {
    
    public void send(PacketContent packet);
    
    public void start();
}
