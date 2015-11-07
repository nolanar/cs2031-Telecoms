package cs.tcd.ie;

import java.net.DatagramPacket;
import java.net.SocketAddress;
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
public class BufferedSender implements Sender {
    private final Node parent;
    private final SocketAddress dstAddress;

    private final LinkedBlockingQueue<DatagramPacket> sendBuffer;
    private final ExecutorService executor;
    private boolean started;
    
    public BufferedSender(Node parent, SocketAddress dstAddress) {
        this.parent = parent;
        this.dstAddress = dstAddress;
        sendBuffer = new LinkedBlockingQueue<>();
        started = false;
        executor = Executors.newSingleThreadExecutor();
    }
    
    @Override
    public void start() {
        if (!started) {
            started = true;
            executor.execute(() -> sender());
        }
    }
    
    public void send(DatagramPacket packet) {
            sendBuffer.add(packet);        
    }

    @Override
    public void send(PacketContent packet) {
            DatagramPacket dataPacket = packet.toDatagramPacket();
            dataPacket.setSocketAddress(dstAddress);
            sendBuffer.add(dataPacket); 
    }
    
    public boolean isEmpty() {
        return sendBuffer.isEmpty();
    }
    
    public DatagramPacket remove() {
        return sendBuffer.remove();
    }
    
    private void sender() {
        ReentrantLock lock = new ReentrantLock();
        Condition sendTime = lock.newCondition();
        while (true) {
            lock.lock();
            try {
                DatagramPacket packet = sendBuffer.take();
//                sendTime.awaitUntil(nextSendTime());
System.out.println("Sending: " + PacketContent.fromDatagramPacket(packet).getPacketNumber()
+ ", " + PacketContent.fromDatagramPacket(packet).toString());
                if (parent != null) //TESTING
                    parent.sendPacket(packet);
            } catch (InterruptedException ex) {
                System.out.println("Terminating sender");
                return;
            } finally {
                lock.unlock();
            }
        }
    }
    
    private Date nextSendTime() {
        return new Date(System.currentTimeMillis()); // returns current time.
    }
}
