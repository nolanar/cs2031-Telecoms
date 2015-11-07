/**
 * 
 */
package cs.tcd.ie;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.FileInputStream;
import java.net.SocketAddress;

import tcdIO.*;

/**
 *
 * Client class
 * 
 * An instance accepts user input 
 *
 */
public class Client extends Node {
    Terminal terminal;
    WindowedSender sender;
    
    /**
     * Constructor
     * 	 
     * Attempts to create socket at given port and create an InetSocketAddress for the destinations
     */
    Client(Terminal terminal, String dstHost, int dstPort, int srcPort) {
        try {
            this.terminal = terminal;
            
            SocketAddress dstAddress = new InetSocketAddress(dstHost, dstPort);
            socket = new DatagramSocket(srcPort);
            sender = new WindowedSender(this, dstAddress, 4, 8);
            sender.start();
            listener.go();
        }
        catch(java.lang.Exception e) {e.printStackTrace();}
    }

    @Override
    public void onReceipt(DatagramPacket packet) {
        PacketContent content= PacketContent.fromDatagramPacket(packet);
System.out.println("Received Packet: " + content); // TESTING
        switch(content.getType()) {
        case PacketContent.ACK_FRAME:
            sender.ack(content.number);
            break;
        case PacketContent.NAKPACKET:
            sender.nak(content.number, false);
            break;
        }
    }

    @Override
    public void packetReady(Receiver receiver) {
    }
     
    @Override
    public void bufferPacket(PacketContent content) {
        sender.send(content);
    }
    
    /**
     * Sender Method
     * 
     */
    public void start() throws Exception {
        
        boolean running = true;
        while (running) {            
            readString();
        }
    }
    
    void readString() {
        String message = terminal.readString();//"message: ");
        StringContent content = new StringContent(message);
//        terminal.println("Sending message");        
        sender.send(content);
    }
    
    void readFromFile() throws Exception {
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
            sender.send(fcontent);
            terminal.println("Packet sent");        
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
