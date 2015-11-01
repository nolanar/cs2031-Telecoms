package cs.tcd.ie;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author aran
 */
public class WindowedSender {
    
    private final Node parent;
    
    private final LinkedBlockingQueue<DatagramPacket> windowBuffer;
    private final ArrayBlockingList<ScheduledPacket> window;
    private final DelayQueue<ScheduledPacket> schedule;

    private static final int THREAD_COUNT = 2;
    private final ExecutorService pool;
    private boolean started;
    
    private final Sender sender;
    
    private final int sequenceLength;
    private final int windowLength;
    private int bufferNumber;
    private int windowStart;
    
    public WindowedSender(Node parent, int windowLength, int sequenceLength) {
        this.parent = parent;
                
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        bufferNumber = 0;
        windowStart = 0;

        windowBuffer = new LinkedBlockingQueue<>();
        window = new ArrayBlockingList<>(windowLength);
        schedule = new DelayQueue<>();
        
        sender = new Sender(parent);
        
        pool = Executors.newFixedThreadPool(THREAD_COUNT);
        started = false;
    }

    /**
     * Starts the WindowedSender
     */
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
                DatagramPacket packet = windowBuffer.take();  // Blocking
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
                sender.add(schPacket.getPacket());
                // renew the delay on the packet and feed back into schedule
                schedule.put(schPacket.repeat());
            } catch (InterruptedException e) {
                System.out.println("Terminating scheduleHandler"); //DEBUG
                return;
            }
        }    
    }    
    
    public synchronized boolean add(PacketContent packet, InetSocketAddress address) {
        packet.number = bufferNumber;
        DatagramPacket dataPacket = packet.toDatagramPacket();
        dataPacket.setSocketAddress(address);
        bufferNumber = (bufferNumber + 1) % sequenceLength;
        return windowBuffer.add(dataPacket);
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
        System.err.println("rel: " + relative); //<-- DEBUG
        if (0 < relative && relative < windowLength + 1) {
            for (int i = 0; i < relative; i++) {
                ScheduledPacket schPacket = window.remove();
                windowStart = (windowStart + 1) % sequenceLength;
                schedule.remove(schPacket);
            }
            result = true;
        } else {
            System.err.println("Not in range"); //<-- DEBUG
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
                    ScheduledPacket schPacket = window.get(i);
                    schedule.remove(schPacket);
                    schedule.add(schPacket.reset());
                }
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
 
}
