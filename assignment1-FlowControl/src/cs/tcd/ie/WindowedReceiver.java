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
    
    private final ArrayBlockingList<PacketContent> window;
    private final LinkedBlockingQueue<PacketContent> buffer;
    
    private final ExecutorService executor;
    private boolean started;
    
    private final int sequenceLength;
    private final int windowLength;
    private int windowStart;
    private int expectedNumber;
    
    public WindowedReceiver(Node parent, int windowLength, int sequenceLength) {
        this.parent = parent;
                
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        windowStart = 0;
        expectedNumber = 0;
        
        window = new ArrayBlockingList<>(windowLength);
        buffer = new LinkedBlockingQueue<>();
        
        executor = Executors.newSingleThreadExecutor();
        started = false;
    }
    
    @Override
    public synchronized boolean receive(DatagramPacket packet) {
        PacketContent content = PacketContent.fromDatagramPacket(packet);
        int packetNumber = content.getPacketNumber();
        int relative = position(expectedNumber);
        boolean received = false;
        if (0 <= relative && relative < windowLength) {
            while (expectedNumber != packetNumber) {
                PacketContent nak = new NakPacketContent(expectedNumber);
                parent.sender.send(nak);
                expectedNumber = nextNumber(expectedNumber);
            }
            window.set(relative, content);
            received = true;
        }
        return received;
    }

    @Override
    public void start() {
        if (!started) {
            started = true;
            executor.execute(() -> feeder());
        }
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
        for (int i = 0; i < size; i++) {
            PacketContent content = window.remove();
            windowStart = nextNumber(windowStart);
            buffer.put(content);
        }
        int ackNumber = (windowStart + size) % sequenceLength;
        PacketContent ack = new AckPacketContent(ackNumber, "ACK" + ackNumber);
        parent.sender.send(ack);
        
    }
    
    private int position(int number) {
        return (number - windowStart + sequenceLength) % sequenceLength;
    }    
}
