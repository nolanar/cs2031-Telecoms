package cs.tcd.ie;

import java.net.DatagramSocket;

/**
 *
 * @author aran
 */
public class WindowedReceiver implements Receiver{
    Node parent;
    
    ArrayBlockingList<DatagramSocket> window;
    
    private final int sequenceLength;
    private final int windowLength;
    private int windowStart;
    private int expectedNumber;
    
    public WindowedReceiver(Node parent, int windowLength, int sequenceLength) {
        this.parent = parent;
                
        this.sequenceLength = sequenceLength;
        this.windowLength = windowLength;
        windowStart = 0;   
        expectedNumber = 0;
    }
    
    @Override
    public void receive() {
        
    }

    @Override
    public void start() {

    }
}
