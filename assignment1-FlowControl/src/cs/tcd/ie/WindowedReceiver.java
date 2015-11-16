package cs.tcd.ie;

import java.net.DatagramPacket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author aran
 */
public class WindowedReceiver implements Receiver {
    private final Node parent;
    
    private boolean goBackN;
    
    private final ArrayBlockingList<PacketContent> window;
    private final LinkedBlockingQueue<PacketContent> buffer;
    
    private final ExecutorService executor;
    private boolean started;
    
    private final int sequenceLength;
    private final int windowLength;
    private int windowStart;
    private int expectedNumber;
    
    public WindowedReceiver(Node parent, int windowLength, int sequenceLength, boolean goBackN) {
        this.parent = parent;
                
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        windowStart = 0;
        expectedNumber = 0;
        
        this.goBackN = goBackN;
        
        window = new ArrayBlockingList<>(windowLength);
        buffer = new LinkedBlockingQueue<>();
        
        executor = Executors.newSingleThreadExecutor();
        started = false;
    }
    
    @Override
    public synchronized boolean receive(DatagramPacket packet) {

        PacketContent content = PacketContent.fromDatagramPacket(packet);
        int packetNumber = content.getPacketNumber();

        int numberPos = posInWindow(packetNumber);
        int expectedPos = posInWindow(expectedNumber);
        if (numberPos < expectedPos) {
            window.set(numberPos, content);
        } else if (numberPos == expectedPos) {
            window.set(numberPos, content);
            expectedNumber = nextNumber(expectedNumber);            
        } else if (numberPos < windowLength) {
            // NAK any missed packets
            while (expectedNumber != packetNumber) {
                PacketContent nak;
                if (goBackN) {
                    nak = new NakBackNContent(expectedNumber);
                } else {
                    nak = new NakSelectContent(expectedNumber);
                }
                parent.bufferPacket(nak);
                expectedNumber = nextNumber(expectedNumber);
            }
            window.set(numberPos, content);
            expectedNumber = nextNumber(expectedNumber);
        } else {
            // ACK packet that should be received next
            PacketContent ack = new AckPacketContent(windowStart);
            parent.bufferPacket(ack);
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
    
    @Override
    public void start() {
        if (!started) {
            started = true;
            executor.execute(() -> feeder());
        }
    }
    
    @Override
    public boolean isEmpty() {
        return buffer.isEmpty();
    }
    
    @Override
    public PacketContent remove() {
        return buffer.remove();
    }    
    
    public int nextNumber(int number) {
        return (number + 1) % sequenceLength;
    }
    
    private void feeder() {
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
            buffer.put(content);
        }
        parent.packetReady(this);
        PacketContent ack = new AckPacketContent(windowStart);
        parent.bufferPacket(ack);
    }  

}
