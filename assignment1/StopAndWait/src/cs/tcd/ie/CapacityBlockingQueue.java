/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs.tcd.ie;

import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.locks.*;

/**
 *
 * @author aran
 * @param <E> Type of queue elements
 */
public class CapacityBlockingQueue<E> {

    final Lock lock;
    final Condition notFull;

    final Queue<E> buffer;
    final int capacity;

    public CapacityBlockingQueue(int capacity) {
        lock = new ReentrantLock();
        notFull = lock.newCondition();

        buffer = new LinkedList<>(); // implemented with LinkedList
        this.capacity = capacity;
    }

    public boolean add(E e) {
        return buffer.add(e);
    }

    public E take() {
        lock.lock();
        try {
            E e = buffer.remove();
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
        return buffer.size() >= capacity;
    }
}
