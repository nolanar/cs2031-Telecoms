/**
 * 
 */
package cs.tcd.ie;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.FileInputStream;

import tcdIO.*;

/**
 *
 * Client class
 * 
 * An instance accepts user input 
 *
 */
public class Client extends Node {
    static final int DEFAULT_SRC_PORT = 50000;
    static final int DEFAULT_DST_PORT = 50001;
    static final String DEFAULT_DST_NODE = "localhost";	
    
    Terminal terminal;
    InetSocketAddress dstAddress;
    
    /**
     * Constructor
     * 	 
     * Attempts to create socket at given port and create an InetSocketAddress for the destinations
     */
    Client(Terminal terminal, String dstHost, int dstPort, int srcPort) {
        try {
            this.terminal = terminal;
            dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            sender = new WindowedSender(this, 4, 8);
            sender.start();
            listener.go();
        }
        catch(java.lang.Exception e) {e.printStackTrace();}
    }

    /**
     * Assume that incoming packets contain a String and print the string.
     */
    public synchronized void onReceipt(DatagramPacket packet) {
        PacketContent content= PacketContent.fromDatagramPacket(packet);

        WindowedSender windowedSender = (WindowedSender)this.sender;
        
        switch(content.getType()) {
        case PacketContent.ACKPACKET:
            windowedSender.ack(content.number);
            break;
        case PacketContent.NAKPACKET:
            windowedSender.nak(content.number, false);
            break;
        }
        
        terminal.println(content.toString());
        this.notify();
    }


    /**
     * Sender Method
     * 
     */
    public synchronized void start() throws Exception {
        
        boolean running = true;
        while (running) {
            String fname= terminal.readString("Name of file to send: ");

            // Reserve buffer for length of file and read file
            File file= new File(fname);
            byte[] buffer= new byte[(int) file.length()];
            int size;
            try (FileInputStream fin= new FileInputStream(file)) {
                size= fin.read(buffer);
            }
            if (size==-1) {
                throw new Exception("Problem with File Access");
            }
            terminal.println("File size: " + buffer.length);            
            
            FileInfoContent fcontent = new FileInfoContent(fname, size);
            
            //packetBuffer.add(fcontent);       
            
            // Send packet with file name and length
            terminal.println("Sending packet w/ name & length");
            sender.send(fcontent, dstAddress);
            terminal.println("Packet sent");
            
            // Wait until acknowledgement returned
            terminal.println("Please wait until server is ready to recieve again.");
            this.wait();
        }
    }
    
     /**
     * Test method
     * 
     * Sends a packet to a given address
     */
    public static void main(String[] args) {
        try {					
            Terminal terminal= new Terminal("Client");		
            (new Client(terminal, DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT)).start();
            terminal.println("Program completed");
        } catch(java.lang.Exception e) {e.printStackTrace();}
    }
}
