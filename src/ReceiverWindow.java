package cs.tcd.ie;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A receiver packet window which corrects for packet loss.
 * 
 * @author aran
 */
public abstract class ReceiverWindow {
    
    private boolean goBackN;
    
    private final ArrayBlockingList<PacketContent> window;
    
    private final ExecutorService executor;
    
    private final int sequenceLength;
    private final int windowLength;
    private int windowStart;
    private int expectedNumber;
    
    public ReceiverWindow(int windowLength, int sequenceLength, boolean goBackN) {
                
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        windowStart = 0;
        expectedNumber = 0;
        
        this.goBackN = goBackN;
        
        window = new ArrayBlockingList<>(windowLength);
        
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> windowToBuffer());
    }

    /**
     * Send the specified packet.
     * 
     * Implement this method to handle packets that need to be sent.
     * 
     * @param packet to be sent
     */    
    public abstract void sendPacket(PacketContent packet);
    
    /**
     * Output the specified packet.
     * 
     * Implement this method to handle packets that are ready for use.
     * 
     * @param packet output packet
     */
    public abstract void outputPacket(PacketContent packet);
    
    /**
     * Action to take upon receiving a packet.
     * 
     * @param packet received packet
     */
    public synchronized boolean receive(PacketContent packet) {
        
        boolean result;
        if (goBackN) { 
            result = receiveGoBackN(packet);
        } else {
            result = receiveSelectiveRepeat(packet);            
        }
        return result;
    }
    
    private boolean receiveGoBackN(PacketContent packet) {
        int packetNumber = packet.getPacketNumber();
        boolean gotExpected = packetNumber == expectedNumber;
        if (gotExpected) {
            window.set(0, packet);
            expectedNumber = nextNumber(expectedNumber);            
        } else {
            sendPacket(new NakBackNContent(expectedNumber));
            sendPacket(new AckPacketContent(expectedNumber));
        }
        return gotExpected;
    }
    
    private boolean receiveSelectiveRepeat(PacketContent packet) {
        int packetNumber = packet.getPacketNumber();
        int numberPos = posInWindow(packetNumber);
        int expectedPos = posInWindow(expectedNumber);
        if (numberPos < expectedPos) {
            window.set(numberPos, packet);
        } else if (numberPos == expectedPos) {
            window.set(numberPos, packet);
            expectedNumber = nextNumber(expectedNumber);            
        } else if (numberPos < windowLength) {
            // NAK any missed packets
            while (expectedNumber != packetNumber) {
                sendPacket(new NakSelectContent(expectedNumber));
                expectedNumber = nextNumber(expectedNumber);
            }
            window.set(numberPos, packet);
            expectedNumber = nextNumber(expectedNumber);
        } else {
            // ACK packet that should be received next
            PacketContent ack = new AckPacketContent(windowStart);
            sendPacket(ack);
        }
        return numberPos < windowLength;
    }
    
    private int cyclicShift(int number, int shift, int modulo) {
        int n = (number + shift) % modulo;
        return n < 0 ? n + modulo : n;
    }

    private int posInWindow(int number) {
        return cyclicShift(number, -windowStart, sequenceLength);
    }      
    
    
    public int nextNumber(int number) {
        return (number + 1) % sequenceLength;
    }
    
    private void windowToBuffer() {
        while (true) {
            try {
                window.awaitNotEmpty();
                ackAll();
            } catch (InterruptedException ex) {
                System.out.println("Terminating receiver feeder");
                return;
            }
        }
    }
    
    private synchronized void ackAll() throws InterruptedException {
        int size = window.size();
        // Move from window into buffer
        for (int i = 0; i < size; i++) {
            PacketContent content = window.remove();
            windowStart = nextNumber(windowStart);
            outputPacket(content);
        }
        PacketContent ack = new AckPacketContent(windowStart);
        sendPacket(ack);
    }  

}
