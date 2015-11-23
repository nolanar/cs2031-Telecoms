


import java.net.DatagramPacket;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author aran
 */
public class ScheduledPacket implements Delayed {
        
    public static final long REPEAT_TIME = TimeUnit.SECONDS.toNanos(2);

    private long delay;
    private final PacketContent packet;

    /**
     * Delay time in nanoseconds.
     * 
     * @param packet
     */
    public ScheduledPacket(PacketContent packet) {
        this.delay = System.nanoTime();
        this.packet = packet;
    }

    @Override
    public int compareTo(Delayed o) {
        if (this.delay < ((ScheduledPacket) o).delay) {
            return -1;
        } else if (this.delay > ((ScheduledPacket) o).delay) {
            return 1;
        }
        return 0;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delay-System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public PacketContent getPacket() {
        return packet;
    }
    
    public synchronized ScheduledPacket repeat() {
        delay += REPEAT_TIME;
        return this;
    }
    
    public synchronized ScheduledPacket reset() {
        delay = System.nanoTime();
        return this;
    }
}