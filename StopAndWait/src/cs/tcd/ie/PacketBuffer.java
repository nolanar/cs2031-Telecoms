package cs.tcd.ie;

/**
 *
 * @author aran
 */
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketBuffer {
    
    private LinkedBlockingQueue<PacketContent> buffer;
    private Window window;
    
    private class Window {
        
        CapacityBlockingQueue<ScheduledPacket> window;
        DelayQueue<ScheduledPacket> scheduler;
            
    }
}
