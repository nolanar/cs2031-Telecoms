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
    private final int windowLength;
    private int bufferNumber;
    private int windowStart;
    
    public PacketBuffer(int windowLength, int sequenceLength) {
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        bufferNumber = 0;
        windowStart = 0;

        buffer = new LinkedBlockingQueue<>();
        window = new CapacityBlockingQueue<>(windowLength);
        schedule = new DelayQueue<>();
        
        feeder = new Thread(new Feeder());
        scheduleHandler = new Thread(new ScheduleHandler()); 
    }
    
    public boolean add(PacketContent packet) {
        packet.number = bufferNumber;
        bufferNumber = (bufferNumber + 1) % sequenceLength;
        return buffer.add(packet);
    }
    
    public boolean acknowledge(int number) {
        boolean result = false;
        
        int relative = (number - windowStart + sequenceLength) % sequenceLength;
        System.err.println("rel: " + relative);
        if (relative <= windowLength) {
            for (int i = 0; i < relative; i++) {
                ScheduledPacket schPacket = window.remove();
                windowStart = (windowStart + 1) % sequenceLength;
                schedule.remove(schPacket);
            }
        } else {
            System.err.println("Not in range");
        }
        
        return result;
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
                    rate.await(PACKETRATE, TimeUnit.MILLISECONDS); //packet rate
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
        System.out.print((packet.number == windowStart ? "\n" : "\t") + packet.toString());
    }
    
    public static void main(String[] args) {
        
        PacketBuffer pb = new PacketBuffer(4, 8);
        pb.feeder.start();
        pb.scheduleHandler.start();
        
        for (int i = 0; i < 100; i++) {
            pb.add(new AckPacketContent("Test"));
        }
        
        Scanner console = new Scanner(System.in);
        
        while (true) {
            String input = console.nextLine();
            Scanner line = new Scanner(input);
            if (line.hasNextInt()) {
                int packet = line.nextInt();
                pb.acknowledge(packet);
            } else {
                String command = line.next();
                switch (command) {
                    case "ACK":
                        int number = line.nextInt();
                        pb.acknowledge(number);
                        break;
                    default :
                        System.out.println("Command not found");
                        break;
                }
            }
        }
        
    }
    
}
