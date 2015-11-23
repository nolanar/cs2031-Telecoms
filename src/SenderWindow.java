import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A sender packet window which corrects for packet loss.
 * 
 * @author aran
 */
public abstract class SenderWindow {
    
    // window components:
    private final ArrayBlockingList<ScheduledPacket> windowPackets;
    private final DelayQueue<ScheduledPacket> windowTimers;
    
    private final ExecutorService pool;
    
    private final int sequenceLength;
    private final int windowLength;
    private int bufferNumber;
    private int windowStart;
    
    public SenderWindow(int windowLength, int sequenceLength) {
                
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        bufferNumber = 0;
        windowStart = 0;

        windowPackets = new ArrayBlockingList<>(windowLength);
        windowTimers = new DelayQueue<>();
        
        pool = Executors.newFixedThreadPool(2);
        pool.execute(() -> inputToWindow());
        pool.execute(() -> windowWorker());
    }

    /**
     * Get a packet.
     * 
     * Implement this method to be used as packet input into window.
     * This must be a blocking method.
     * 
     * @return packet taken from input
     */
    public abstract PacketContent getPacket();
    
    /**
     * Send the specified packet.
     * 
     * Implement this method to handle output packets from the window.
     * 
     * @param packet to be sent
     */
    public abstract void sendPacket(PacketContent packet);    
    
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
                PacketContent packet = getPacket();  // Blocking
                packet.setNumber(bufferNumber);
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
                sendPacket(schPacket.getPacket());
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
