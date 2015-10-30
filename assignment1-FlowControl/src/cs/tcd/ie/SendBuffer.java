package cs.tcd.ie;

/**
 * TODO:
 * - Create a 'Sender' runnable
 * - Use Executor (Executors.newFixedThreadPool()) to manage threads
 * - Make classes that implement Runnable into Runnable objects
 * - Make window/schedule changes atomic (synchronize)
 * - Investigate callable
 */

/**
 *
 * @author aran
 */
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;

//--> testing imports
import java.util.Scanner;
//--<

public class SendBuffer {
    
    public static final long PACKETRATE = 250;
    
    private final LinkedBlockingQueue<PacketContent> buffer;
    private final ArrayBlockingList<ScheduledPacket> window;
    private final DelayQueue<ScheduledPacket> schedule;

    private final Feeder feeder;
    private final ScheduleHandler scheduleHandler;
    
    private final int sequenceLength;
    private final int windowLength;
    private int bufferNumber;
    private int windowStart;
    
    public SendBuffer(int windowLength, int sequenceLength) {
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        bufferNumber = 0;
        windowStart = 0;

        buffer = new LinkedBlockingQueue<>();
        window = new ArrayBlockingList<>(windowLength);
        schedule = new DelayQueue<>();
        
        feeder = new Feeder();
        scheduleHandler = new ScheduleHandler(); 
    }
    
    public boolean add(PacketContent packet) {
        packet.number = bufferNumber;
        bufferNumber = (bufferNumber + 1) % sequenceLength;
        return buffer.add(packet);
    }
    
    /**
     * Action to be taken upon the receipt of an ACK
     * 
     * @param number - the packet number of the received ACK
     * @return true if packet number of ACK is in range, false otherwise
     */
    public boolean ack(int number) {
        boolean result;
        int relative = (number - windowStart + sequenceLength) % sequenceLength;
        System.err.println("rel: " + relative); //<-- DEBUG
        if (0 < relative && relative < windowLength + 1) {
            
            //--> Synchronize on window/schedule
            for (int i = 0; i < relative; i++) {
                ScheduledPacket schPacket = window.remove();
                windowStart = (windowStart + 1) % sequenceLength;
                schedule.remove(schPacket);
            }
            //--<
            
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
    public boolean nak(int number, boolean goBackN) {
        boolean result;
        int relative = (number - windowStart + sequenceLength) % sequenceLength;
        if (relative < windowLength) {
            
            //--> Synchronize on window/schedule
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
            //--<
            
            result = true;
        } else {
            result = false;
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

        Thread thread;
        
        @Override
        public void run() {
            while (true) {
                try {
                    
                    //--> Synchronize on window/schedule ???
                    window.awaitNotFull();                // Blocking
                    PacketContent packet = buffer.take(); // Blocking
                    ScheduledPacket schPacket = new ScheduledPacket(packet);
                    window.add(schPacket);
                    schedule.add(schPacket);
                    //--<
                    
                } catch (InterruptedException e) {
                    System.out.println("Terminating Feeder"); //--> DEBUG
                    return;
                }
            }
        }
        
        public void start() {
            thread = new Thread(this);
            thread.start();
        }
        
        public void terminate() {
            thread.interrupt();
        }
    }
    
    /**
     * Handles packet when associated delay expires.
     * 
     * Handles packet when associated delay expires. The packet delay is
     * extended (by REPEAT_TIME) and is fed back into schedule.
     */
    private class ScheduleHandler implements Runnable {

        private Thread thread;
        
        @Override
        public void run() {
            while (true) {
                try {
                    
                //--> Synchronize on window/schedule ???
                    // Wait for next packet to expire
                    ScheduledPacket schPacket = schedule.take(); // Blocking
                    // Renew the delay on the packet and feed back into schedule
                    schedule.put(schPacket.repeat());
                    // Action to take with packet
                    testAction(schPacket.getPacketContent()); // TESTING
                    //addToSenderQueue(schPacket); //<--IMPLEMENT
                //--<
                    
                } catch (InterruptedException e) {
                    System.out.println("Terminating ScheduleHandler");
                    return;
                }
            }            
        }
        
        public void start() {
            thread = new Thread(this);
            thread.start();
        }
        
        public void terminate() {
            thread.interrupt();
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
        
        SendBuffer sb = new SendBuffer(4, 8);
        sb.feeder.start();
        sb.scheduleHandler.start();
        
        for (int i = 0; i < 100; i++) {
            sb.add(new AckPacketContent("Test"));
        }
        
        Scanner console = new Scanner(System.in);
        
        boolean running = true;
        while (running) {
            String input = console.nextLine();
            Scanner line = new Scanner(input);
            if (line.hasNextInt()) {
                int packet = line.nextInt();
                sb.nak(packet, false);
            } else {
                String command = line.next();
                switch (command) {
                    case "ACK":
                        int number = line.nextInt();
                        sb.ack(number);
                        break;
                    case "quit":
                        running = false;
                        break;
                    default :
                        System.out.println("Command not found");
                        break;
                }
            }
        }
        sb.feeder.terminate();
        sb.scheduleHandler.terminate();
    }
    
}
