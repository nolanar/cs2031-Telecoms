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
            this.terminal= terminal;
            dstAddress= new InetSocketAddress(dstHost, dstPort);
            socket= new DatagramSocket(srcPort);
            listener.go();
        }
        catch(java.lang.Exception e) {e.printStackTrace();}
    }


    /**
     * Assume that incoming packets contain a String and print the string.
     */
    public synchronized void onReceipt(DatagramPacket packet) {
        PacketContent content= PacketContent.fromDatagramPacket(packet);

        terminal.println(content.toString());
        this.notify();
    }


    /**
     * Sender Method
     * 
     */
    public synchronized void start() throws Exception {
        String fname;
        File file= null;
        FileInputStream fin= null;

        FileInfoContent fcontent;

        int size;
        byte[] buffer= null;
        DatagramPacket packet= null;

        fname= terminal.readString("Name of file: ");

        file= new File(fname);				// Reserve buffer for length of file and read file
        buffer= new byte[(int) file.length()];
        fin= new FileInputStream(file);
        size= fin.read(buffer);
        if (size==-1) {
            fin.close();
            throw new Exception("Problem with File Access");
        }
        terminal.println("File size: " + buffer.length);

        fcontent= new FileInfoContent(fname, size);

        terminal.println("Sending packet w/ name & length"); // Send packet with file name and length
        packet= fcontent.toDatagramPacket();
        packet.setSocketAddress(dstAddress);
        socket.send(packet);
        terminal.println("Packet sent");
        this.wait();
        fin.close();
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
