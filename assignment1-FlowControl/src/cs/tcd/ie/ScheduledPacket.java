
package cs.tcd.ie;

import java.net.DatagramPacket;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author aran
 */
public class ScheduledPacket implements Delayed {
        
    public static final long REPEAT_TIME = TimeUnit.SECONDS.toNanos(5);

    private long delay;
    private final DatagramPacket packet;

    /**
     * Delay time in nanoseconds.
     * 
     */
    public ScheduledPacket(DatagramPacket packet, long delay, TimeUnit unit) {
        this.delay = unit.toNanos(delay) + System.nanoTime();
        this.packet = packet;
    }

    public ScheduledPacket(DatagramPacket packet) {
        this.delay = System.nanoTime();
        this.packet = packet;
    }

    private ScheduledPacket(DatagramPacket packet, long delay) {
        this.delay = delay;
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

    public DatagramPacket getPacket() {
        return packet;
    }
    
    public synchronized ScheduledPacket repeat() {
        delay += REPEAT_TIME;
        return this;
    }

    public synchronized ScheduledPacket repeat(long time, TimeUnit unit) {
        delay += unit.toNanos(time);
        return this;
    }
    
    public synchronized ScheduledPacket reset() {
        delay = System.nanoTime();
        return this;
    }
}