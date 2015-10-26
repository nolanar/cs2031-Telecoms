package cs.tcd.ie;

/**
 *
 * @author aran
 */
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketBuffer {
    
    private LinkedBlockingQueue<PacketContent> buffer;
    private CapacityBlockingQueue<ScheduledPacket> window;
    private DelayQueue<ScheduledPacket> schedule;

    private final int sequenceLength;
    private int bufferNumber;
    
    public PacketBuffer(int windowSize, int sequenceLength) {
        buffer = new LinkedBlockingQueue<>();
        window = new CapacityBlockingQueue<>(windowSize);
        schedule = new DelayQueue<>();
        
        this.sequenceLength = sequenceLength;
        bufferNumber = 0;
    }
    
    public boolean add(PacketContent packet) {
        packet.number = bufferNumber;
        bufferNumber = (bufferNumber + 1) % sequenceLength;
        return buffer.add(packet);
    }
    
    /**
     * Feeds packets from buffer to window.
     * 
     * Packets are fed from buffer to window when buffer is not empty and
     * window is not full. Any packets added to window are also added to
     * schedule.
     */
    private class Feeder implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    window.awaitNotFull();                // Blocking
                    PacketContent packet = buffer.take(); // Blocking
                    ScheduledPacket schPacket = new ScheduledPacket(packet);
                    window.add(schPacket);
                    schedule.add(schPacket);

                } catch (InterruptedException e) {
                    System.out.println("Terminating Feeder"); //--> DEBUG
                    return;
                }
            }
        }

    }
    
    private class ScheduleHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    ScheduledPacket schPacket = schedule.take(); // Blocking
                    schedule.put(schPacket.repeat());
                    //send(schPacket);                           //<--IMPLEMENT
                } catch (InterruptedException e) {
                    System.out.println("Terminating ScheduleHandler");
                    return;
                }
            }            
        }
    }
    
}
