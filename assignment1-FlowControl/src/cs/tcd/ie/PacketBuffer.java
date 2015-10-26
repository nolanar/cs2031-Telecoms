package cs.tcd.ie;

/**
 *
 * @author aran
 */
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

//--> testing imports
import java.util.Scanner;
//--<

public class PacketBuffer {
    
    public static final long PACKETRATE = 250;
    
    private final LinkedBlockingQueue<PacketContent> buffer;
    private final CapacityBlockingQueue<ScheduledPacket> window;
    private final DelayQueue<ScheduledPacket> schedule;

    private final Thread feeder;
    private final Thread scheduleHandler;
    
    private final int sequenceLength;
    private int bufferNumber;
    
    public PacketBuffer(int windowSize, int sequenceLength) {
        buffer = new LinkedBlockingQueue<>();
        window = new CapacityBlockingQueue<>(windowSize);
        schedule = new DelayQueue<>();
        
        feeder = new Thread(new Feeder());
        scheduleHandler = new Thread(new ScheduleHandler());
        
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

        final Lock lock = new ReentrantLock();
        final Condition rate = lock.newCondition();
        
        @Override
        public void run() {
            while (true) {
                lock.lock();
                try {
                    window.awaitNotFull();                // Blocking
                    PacketContent packet = buffer.take(); // Blocking
                    ScheduledPacket schPacket = new ScheduledPacket(packet);
                    window.add(schPacket);
                    schedule.add(schPacket);
                    //packet rate
                    rate.await(PACKETRATE, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    System.out.println("Terminating Feeder"); //--> DEBUG
                    return;
                } finally {
                    lock.unlock();
                }
            }
        }

    }
    
    /**
     * Handles packet when associated delay expires.
     * 
     * Handles packet when associated delay expires. The packet delay is
     * extended (by REPEAT_TIME) and is fed back into schedule.
     */
    private class ScheduleHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // Wait for next packet to expire
                    ScheduledPacket schPacket = schedule.take(); // Blocking
                    // Renew the delay on the packet and feed back into schedule
                    schedule.put(schPacket.repeat());
                    // Action to take with packet
                    testAction(schPacket.getPacketContent()); // TESTING
                    //send(schPacket); //<--IMPLEMENT
                } catch (InterruptedException e) {
                    System.out.println("Terminating ScheduleHandler");
                    return;
                }
            }            
        }
    }
    
    /**
     * Code below this point is for testing purposes and should be removed.
     * 
     */
    
    
    void testAction(PacketContent packet) {
        System.out.println(packet.toString());
    }
    
    public static void main(String[] args) {
        
        PacketBuffer pb = new PacketBuffer(4, 8);
        pb.feeder.start();
        pb.scheduleHandler.start();
        
        for (int i = 0; i < 6; i++) {
            pb.add(new AckPacketContent("Test"));
        }
        
        Scanner console = new Scanner(System.in);
        
    }
    
}
