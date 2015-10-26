package cs.tcd.ie;

/**
 *
 * @author aran
 */
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.locks.*;

/**
 *
 * @param <E> Type of queue elements
 */
public class CapacityBlockingQueue<E> {

    private final Lock lock;
    private final Condition notFull;

    private final Queue<E> content;
    private final int capacity;

    public CapacityBlockingQueue(int capacity) {
        lock = new ReentrantLock();
        notFull = lock.newCondition();

        content = new LinkedList<>(); // implemented with LinkedList
        this.capacity = capacity;
    }

    public boolean add(E e) {
        return content.add(e);
    }

    public E take() {
        lock.lock();
        try {
            E e = content.remove();
            if (!isFull()) {
                notFull.signalAll();
            }
            return e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Blocks the calling thread until capacity is no longer exceeded.
     * 
     * @throws InterruptedException 
     */
    public void awaitNotFull() throws InterruptedException {
        lock.lock();
        try {
            while (isFull()) {
                notFull.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isFull() {
        return content.size() >= capacity;
    }
}