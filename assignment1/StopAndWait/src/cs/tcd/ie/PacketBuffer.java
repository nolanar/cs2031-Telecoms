/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs.tcd.ie;

import java.util.LinkedList;

/**
 *
 * @author aran
 */
public class PacketBuffer {
    
    private final int windowSize;
    private final int sequenceSize;

    private int start = 0;

    LinkedList<PacketContent> buffer;

    public PacketBuffer(int windowSize, int sequenceSize) {
        this.windowSize = windowSize;
        this.sequenceSize = sequenceSize;
    }

    public void add(PacketContent pContent) {
        pContent.packetNumber = nextNumber();
        buffer.add(pContent);
    }

    public void ack(int packetNumber) {
        int target = incr(packetNumber);
        while (start != target) {
            buffer.remove();
            start = incr(start);
        }
    }

    /**
     * 
     * @return The packet number of one after the last element in the buffer
     */
    private int nextNumber() {
        return (buffer.size() + start) % sequenceSize;
    }

    private int incr(int num) {
        return (num + 1) % sequenceSize;
    }
}
