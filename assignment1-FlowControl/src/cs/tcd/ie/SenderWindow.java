package cs.tcd.ie;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A buffered packet sender which corrects for packet loss.
 * 
 * @author aran
 */
public class SenderWindow {
    private final Node parent;
    
    // window components:
    private final ArrayBlockingList<ScheduledPacket> windowPackets;
    private final DelayQueue<ScheduledPacket> windowTimers;
    
    private static final int THREAD_COUNT = 2;
    private final ExecutorService pool;
    private boolean started;
    
    private final int sequenceLength;
    private final int windowLength;
    private int bufferNumber;
    private int windowStart;
    
    public SenderWindow(Client parent, int windowLength, int sequenceLength) {
        this.parent = parent;
                
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        bufferNumber = 0;
        windowStart = 0;

        windowPackets = new ArrayBlockingList<>(windowLength);
        windowTimers = new DelayQueue<>();
        
        pool = Executors.newFixedThreadPool(THREAD_COUNT);
        started = false;
    }

    /**
     * Starts the SenderWindow
     */
    public void start() {
        if (!started) {
            started = true;
            pool.execute(() -> inputToWindow());
            pool.execute(() -> windowWorker());
        }
    }

    
    /**
     * Get packet from the input put it into the window.
     * 
     * Packets are fed from buffer to window when buffer is not empty and
     * window is not full. Any packets added to window are also added to
     * schedule.
     */
    private void inputToWindow() {
        while (true) {
            try {
                windowPackets.awaitNotFull();                // Blocking
                PacketContent packet = parent.getPacket();  // Blocking
                packet.number = bufferNumber;
                bufferNumber = nextNumber(bufferNumber);
                ScheduledPacket schPacket = new ScheduledPacket(packet);
                //Add packet to both of the window components:
                windowPackets.add(schPacket);
                windowTimers.add(schPacket);
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
    private void windowWorker() {
        while (true) {
            try {
                // Wait for next packet to expire
                ScheduledPacket schPacket = windowTimers.take(); // Blocking
                parent.bufferPacket(schPacket.getPacket());
                // renew the delay on the packet and feed back into schedule
                windowTimers.put(schPacket.repeat());
            } catch (InterruptedException e) {
                System.out.println("Terminating scheduleHandler"); //DEBUG
                return;
            }
        }    
    }    
    
    /**
     * Action to be taken upon the receipt of an ACK
     * 
     * @param number - the packet number of the received ACK
     * @return true if packet number of ACK is in range, false otherwise
     */
    public synchronized boolean ack(int number) {
        boolean result;
        int relative = relativeNumber(number);
        if (0 < relative && relative < windowLength + 1) {
            for (int i = 0; i < relative; i++) {
                ScheduledPacket schPacket = windowPackets.remove();
                windowStart = nextNumber(windowStart);
                windowTimers.remove(schPacket);
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
        int relative = relativeNumber(number);
        if (relative < windowLength) {
            // For 'go back n', resend all packets starting from the one NAK'ed
            if (goBackN) {
                for (int i = relative; i < windowPackets.size(); i++) {
                    ScheduledPacket schPacket = windowPackets.get(i);
                    windowTimers.remove(schPacket);
                    windowTimers.add(schPacket.reset());
                }
            }
            // For 'selective repeat', resent the packet
            else {
                ScheduledPacket schPacket = windowPackets.get(relative);
                windowTimers.remove(schPacket);
                windowTimers.add(schPacket.reset());
            }
            
            result = true;
        } else {
            result = false;
        }
        return result;
    }
    
    /**
     * Returns the following number in the sequence.
     */
    public int nextNumber(int number) {
        return (number + 1) % sequenceLength;
    }
    
    /**
     * Returns the relative position to the start of the window of number.
     */
    public int relativeNumber(int number) {
        return (number - windowStart + sequenceLength) % sequenceLength;
    }
}
