package cs.tcd.ie;

import java.net.SocketAddress;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author aran
 */
public class WindowedSender implements Sender {

    private final LinkedBlockingQueue<PacketContent> windowBuffer;
    private final ArrayBlockingList<ScheduledPacket> window;
    private final DelayQueue<ScheduledPacket> schedule;

    private static final int THREAD_COUNT = 2;
    private final ExecutorService pool;
    private boolean started;
    
    private final BufferedSender sender;
    
    private final int sequenceLength;
    private final int windowLength;
    private int bufferNumber;
    private int windowStart;
    
    public WindowedSender(Node parent, SocketAddress dstAddress,
            int windowLength, int sequenceLength) {
        
        sender = new BufferedSender(parent, dstAddress);
                
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        bufferNumber = 0;
        windowStart = 0;

        windowBuffer = new LinkedBlockingQueue<>();
        window = new ArrayBlockingList<>(windowLength);
        schedule = new DelayQueue<>();
        
        pool = Executors.newFixedThreadPool(THREAD_COUNT);
        started = false;
    }

    /**
     * Starts the WindowedSender
     */
    @Override
    public void start() {
        if (!started) {
            started = true;
            pool.execute(() -> feeder());
            pool.execute(() -> scheduleHandler());
            sender.start();
        }
    }

    
    /**
     * Feeds packets from buffer to window.
     * 
     * Packets are fed from buffer to window when buffer is not empty and
     * window is not full. Any packets added to window are also added to
     * schedule.
     */
    private void feeder() {
        while (true) {
            try {
                window.awaitNotFull();                // Blocking
                PacketContent packet = windowBuffer.take();  // Blocking
                ScheduledPacket schPacket = new ScheduledPacket(packet);
                window.add(schPacket);
                schedule.add(schPacket);
            } catch (InterruptedException e) {
                System.out.println("Terminating feeder"); //--> DEBUG
                return;
            }
        }
    }

    
    /**
     * Handles packet when associated delay expires.
     * 
     * Handles packet when associated delay expires. The packet delay is
     * extended (by REPEAT_TIME) and is fed back into schedule.
     */
    private void scheduleHandler() {
        while (true) {
            try {
                // Wait for next packet to expire
                ScheduledPacket schPacket = schedule.take(); // Blocking
                sender.send(schPacket.getPacket());
                // renew the delay on the packet and feed back into schedule
                schedule.put(schPacket.repeat());
            } catch (InterruptedException e) {
                System.out.println("Terminating scheduleHandler"); //DEBUG
                return;
            }
        }    
    }    
    
    @Override
    public synchronized void send(PacketContent packet) {
        packet.number = bufferNumber;
        bufferNumber = nextNumber(bufferNumber);
        windowBuffer.add(packet);
    }
    
    /**
     * Action to be taken upon the receipt of an ACK
     * 
     * @param number - the packet number of the received ACK
     * @return true if packet number of ACK is in range, false otherwise
     */
    public synchronized boolean ack(int number) {
        boolean result;
        int relative = (number - windowStart + sequenceLength) % sequenceLength;
        if (0 < relative && relative < windowLength + 1) {
            for (int i = 0; i < relative; i++) {
                ScheduledPacket schPacket = window.remove();
                windowStart = nextNumber(windowStart);
                schedule.remove(schPacket);
            }
            result = true;
        } else {
            result = false;
        }
        return result;
    }
    
    /**
     * Action to be taken upon the receipt of a NAK
     * 
     * @param number - the packet number of the received NAK
     * @param goBackN - if true, resend all packets starting from the one NAK'ed
     * @return true if packet number of NAK is in range, false otherwise
     */
    public synchronized boolean nak(int number, boolean goBackN) {
        boolean result;
        int relative = (number - windowStart + sequenceLength) % sequenceLength;
        if (relative < windowLength) {
            if (goBackN) {
                // For 'go back n', resend all packets starting from the one NAK'ed
                for (int i = relative; i < window.size(); i++) {
                    System.out.println("Getting: " + i);
                    ScheduledPacket schPacket = window.get(i);
                    schedule.remove(schPacket);
                    schedule.add(schPacket.reset());
                }
                System.out.println("Got all!");
            } else {
                // For 'selective repeat', resent the packet
                ScheduledPacket schPacket = window.get(relative);
                schedule.remove(schPacket);
                schedule.add(schPacket.reset());
            }
            
            result = true;
        } else {
            result = false;
        }
        return result;
    }
    
    public int nextNumber(int number) {
        return (number + 1) % sequenceLength;
    }
}
