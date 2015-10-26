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
        lock.lock();
        try {
            return content.add(e);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Retrieves and removes the head of this queue.
     * 
     * @return the head of this queue
     */
    public E remove() {
        lock.lock();
        try {
            E e = content.poll();
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

    private boolean isFull() {
        lock.lock();
        try {
            return content.size() >= capacity;
        } finally {
            lock.unlock();
        }
    }
}