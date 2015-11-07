package cs.tcd.ie;

/**
 *
 * @author aran
 */
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.*;

/**
 * A data structure with functionality of both a fixed sized list and a queue.
 * 
 * 
 * 
 * @param <E> - Type of list elements
 */
public class ArrayBlockingList<E> {

    private final ReentrantLock lock;
    private final Condition notFull;
    private final Condition notEmpty;

    private final ArrayList<E> items;
    private final int capacity;
    private int front;
    private int back;
    private int count;

    public ArrayBlockingList(int capacity) {
        lock = new ReentrantLock();
        notFull = lock.newCondition();
        notEmpty = lock.newCondition();

        items = new ArrayList<>(capacity);
        for (int i = 0 ; i < capacity; i++) {
            items.add(null);
        }
        this.capacity = capacity;
    }

    /**
     * Inserts element at back of queue if possible. 
     * 
     * @param e - the element to add
     * @return true if element added to list
     */
    public boolean add(E e) {
        lock.lock();
        try {
            if (isFull()) {
                throw new IllegalStateException();
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Inserts the specified element at the tail of this queue.
     * 
     * It will block the calling thread until space becomes available
     * if the list is full.
     *
     * @param e - the element to add
     * @throws InterruptedException
     */
    public void put(E e) throws InterruptedException {
        lock.lock();
        try {
            while (isFull()) {
                notFull.await();
            }
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Replaces the element at the specified position in this list with the specified element.
     * 
     * Will cause queue to expand if setting the back of the queue.
     * 
     * @param index - index of the element to replace
     * @param e - element to be stored at the specified position
     * @return the element previously at the specified position
     */
    public E set(int index, E e) {
        lock.lock();
        try {
            if (inRange(index)) {
                throw new IndexOutOfBoundsException();
            }
            int position = position(index);
            E result = items.get(position);
            if (position == back) {
                enqueue(e);
            } else { 
                items.set(position, e);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Retrieves and removes the head of this queue.
     * 
     * @return the head of this list
     */
    public E remove() {
        lock.lock();
        try {
            if (isEmpty()) {
                throw new NoSuchElementException();
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes the head of this queue.
     * 
     * It will block the calling thread until an element becomes available.
     *
     * @return the head of this list
     * @throws InterruptedException
     */
    public E take() throws InterruptedException {
        lock.lock();
        try {
            while (isEmpty()) {
                notFull.await();
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Returns the element at the specified position in this list, or null
     * if that position is empty.
     * 
     * @param index - position in list
     * @return the element at the position, null if that position is empty.
     */
    public E get(int index) {
        lock.lock();
        try {
            if (inRange(index)) {
                throw new IndexOutOfBoundsException();
            }
            int position = position(index);
            return items.get(position);
        } finally {
            lock.unlock();
        }

    }
    
    /**
     * Blocks the calling thread until the queue has space.
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
    
    /**
     * Blocks the calling thread until the queue is not empty.
     * 
     * @throws InterruptedException 
     */
    public void awaitNotEmpty() throws InterruptedException {
        lock.lock();
        try {
            while (isEmpty()) {
                notEmpty.await();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Returns the number of elements in the queue.
     *
     * @return the number of elements in this queue
     */
    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }        
    }
    
    /**
     * Returns true if queue is full, false otherwise.
     * 
     * Call only when holding lock
     */
    private boolean isFull() {
        return count == capacity;
    }

    /**
     * Returns true if queue is empty, false otherwise.
     * 
     * Call only when holding lock
     */
    boolean isEmpty() {
        return count == 0;
    }
    
    /** 
     * Cyclically increments number.
     */
    private int incr(int number) {
        if (++number == capacity) {
            number = 0;
        }
        return number;
    }
    
    /**
     * Inserts element into back of queue and signals notEmpty.
     * 
     * Any elements that become contiguous to the back of the queue become 
     * part of the queue.
     * 
     * Call only when holding lock.
     */
    private void enqueue(E e) {
        items.set(back, e);
        E next;
        do {
            back = incr(back);
            count++;
            next = items.get(back);
        } while (!isFull() && next != null);
        notEmpty.signal();
    }    

    /**
     * Extracts element at front of queue and signals notFull.
     * 
     * Call only when holding lock.
     */
    private E dequeue() {
        E e = items.get(front);
        items.set(front, null);
        front = incr(front);
        count--;
        notFull.signal();
        return e;
    }
    
    /**
     * Checks if given index is in the range of the list.
     */
    private boolean inRange(int index) {
        return index < 0 || index > capacity - 1;
    }
    
    /**
     * Returns the position of index in the underlying array.
     * 
     * Index specifies the position relative to the front of the queue.
     * 
     * Call only when holding lock.
     */
    private int position(int index) {
        return (index + front) % capacity;
    }
    
}