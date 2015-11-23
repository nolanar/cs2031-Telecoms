

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Node {

    Sender sender;
    LinkedBlockingQueue<PacketContent> receiver;
    
    Terminal terminal;

    public static boolean debugMode = true;
    public static double dropRate = 0.15;
    
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
     * Sends the packet immediately.
     * 
     * 'dropRate' chance the packet will not send.
     * 
     * @param packet
     */
    public void sendPacket(DatagramPacket packet) {
        boolean drop = new Random().nextDouble() < dropRate;
        if (!drop) { 
            try {
                socket.send(packet);
            } catch (IOException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (debugMode) {
            PacketContent content = PacketContent.fromDatagramPacket(packet);
            terminal.printSys("Sending " + content.getNumber() + ": " + content);
            if (drop) {
                terminal.printSys(" ... Dropped!");
            } else {
                terminal.printSys(" ... Sent!");
            }
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
                    if (debugMode) {
                        PacketContent content = PacketContent.fromDatagramPacket(packet);
                        terminal.printSys("Recieved " + content.getNumber() + ": " + content);
                    }
                    onReceipt(packet);
                }
            } catch (Exception e) {if (!(e instanceof SocketException)) e.printStackTrace();}
        }
    }
}
