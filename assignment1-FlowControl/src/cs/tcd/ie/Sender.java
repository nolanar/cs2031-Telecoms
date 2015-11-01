package cs.tcd.ie;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 *
 * @author aran
 */
public class Sender {
    
    private final Node parent;

    private final LinkedBlockingQueue<DatagramPacket> sendBuffer;
    private final ExecutorService executor;
    private boolean started;
    
    public Sender(Node parent) {
        this.parent = parent;
        sendBuffer = new LinkedBlockingQueue<>();
        executor = Executors.newSingleThreadExecutor();
        started = false;
    }
    
    public void start() {
        if (!started) {
            started = true;
            executor.execute(() -> sender());
        }
    }
    
    public void add(DatagramPacket packet) {
            sendBuffer.add(packet);        
    }

    public void add(PacketContent packet, InetSocketAddress address) {
            DatagramPacket dataPacket = packet.toDatagramPacket();
            dataPacket.setSocketAddress(address);
            sendBuffer.add(dataPacket);
    }
    
    private void sender() {
        ReentrantLock lock = new ReentrantLock();
        Condition sendTime = lock.newCondition();
        while (true) {
            lock.lock();
            try {
                DatagramPacket packet = sendBuffer.take();
                System.err.println("Sending...");
                sendTime.awaitUntil(nextSendTime());
                parent.socket.send(packet);
            } catch (InterruptedException ex) {
                System.out.println("Terminating sender");
                return;
            } catch (IOException ex) {
            } finally {
                lock.unlock();
            }
        }
    }
    
    private Date nextSendTime() {
        return new Date(System.currentTimeMillis() + 2500); // returns current time.
    }
}
