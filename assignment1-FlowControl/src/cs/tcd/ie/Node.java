package cs.tcd.ie;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Node {
    static final int DEFAULT_SRC_PORT = 50000;
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";	
    static final String DEFAULT_SRC_NODE = "localhost";	
   
    static final int PACKETSIZE = 65536;
    
    DatagramSocket socket;
    Listener listener;
    CountDownLatch latch;
    
    Node() {
        latch= new CountDownLatch(1);
        listener= new Listener();
        listener.setDaemon(true);
        listener.start();
    }

     /**
      * Action to take upon receiving datagram packet.
      * 
      * @param packet - Stores received packet
      */
    public abstract void onReceipt(DatagramPacket packet);

    /**
     * Action to take upon packets entering the received buffer.
     * 
     * @param receiver
     */
    public abstract void packetReady(Receiver receiver);
    
    /**
     * Puts the packet in the send buffer to await being sent.
     * 
     * @param content
     */
    public abstract void bufferPacket(PacketContent content);
    
    /**
     * Sends the packet immediately.
     * 
     * 0.75 chance the packet will actually send.
     * 
     * @param packet
     */
    public void sendPacket(DatagramPacket packet) {
        if (new Random().nextDouble() < 0.75) { 
            // random packet drop
            try {
            socket.send(packet);
            } catch (IOException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("Dropped packet: " 
                    + PacketContent.fromDatagramPacket(packet).getPacketNumber());
        }
    }  
    
    /**
     * Listener thread.
     * 
     * Listens for incoming packets on a datagram socket and informs 
     * registered receivers about incoming packets.
     */
    class Listener extends Thread {

        /*
         *  Telling the listener that the socket has been initialized 
         */
        public void go() {
            latch.countDown();
        }

        /*
         * Listen for incoming packets and inform receivers
         */
        @Override
        public void run() {
            try {
                latch.await();
                // Endless loop: attempt to receive packet, notify receivers, etc
                while(true) {
                    DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);
                    socket.receive(packet);
                    System.out.println("\nNode got: " + PacketContent.fromDatagramPacket(packet).getPacketNumber());
                    onReceipt(packet);
                }
            } catch (Exception e) {if (!(e instanceof SocketException)) e.printStackTrace();}
        }
    }
}
