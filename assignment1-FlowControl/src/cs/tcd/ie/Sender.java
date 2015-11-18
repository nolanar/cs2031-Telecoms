package cs.tcd.ie;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A buffered packet sender which does not correct for packet loss.
 * 
 * @author aran
 */
public class Sender {
    private final Node parent;
    private final SocketAddress dstAddress;

    private final LinkedBlockingQueue<DatagramPacket> sendBuffer;
    private final ExecutorService executor;
    private boolean started;
    
    public Sender(Node parent, SocketAddress dstAddress) {
        this.parent = parent;
        this.dstAddress = dstAddress;
        sendBuffer = new LinkedBlockingQueue<>();
        started = false;
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Begin the sender worker thread.
     * 
     * Tries to take a packet from the buffer to pass to the parent node to
     * be sent.
     */
    public void start() {
        if (!started) {
            started = true;
            executor.execute(() -> sender());
        }
    }

    /**
     * Add packet to buffer to be sent.
     * 
     * @param packet 
     */
    public void add(PacketContent packet) {
        
        DatagramPacket dataPacket = packet.toDatagramPacket();
        dataPacket.setSocketAddress(dstAddress);
        sendBuffer.add(dataPacket);
    }
    
    /**
     * Sender worker thread runs this method upon creation.
     * 
     * Tries to take a packet from the buffer to pass to the parent node to
     * be sent.
     */
    private void sender() {
        ReentrantLock lock = new ReentrantLock();
        Condition sendTime = lock.newCondition();
        while (true) {
            lock.lock();
            try {
                DatagramPacket packet = sendBuffer.take();
                sendTime.awaitUntil(nextSendTime());
                parent.sendPacket(packet);
            } catch (InterruptedException ex) {
                System.out.println("Terminating sender");
                return;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Returns the time at which the sender should pass the packet to it's parent.
     * 
     * Can be used to synchronise sending packets, etc. 
     */
    private Date nextSendTime() {
        return new Date(System.currentTimeMillis()); // returns current time.
    }
}
