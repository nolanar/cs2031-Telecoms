package cs.tcd.ie;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.FileInputStream;
import java.net.SocketAddress;

/**
 * Client class.
 * 
 * An instance accepts user input 
 */
public class Client extends Node {
    WindowedSender sender;
    
    /**
     * Constructor
     * 	 
     * Attempts to create socket at given port and create an InetSocketAddress for the destinations
     */
    Client(Terminal terminal, String dstHost, int port, int dstPort,
            int windowSize, int sequenceLength, boolean goBackN) {
        try {
            socket = new DatagramSocket(port);
        } catch(java.lang.Exception e) {e.printStackTrace();}
        this.terminal = terminal;
        SocketAddress dstAddress = new InetSocketAddress(dstHost, dstPort);
        sender = new WindowedSender(this, dstAddress, windowSize, sequenceLength);
        sender.start();
        listener.go();
    }

    @Override
    public void onReceipt(DatagramPacket packet) {
        PacketContent content= PacketContent.fromDatagramPacket(packet);
        switch(content.getType()) {
        case PacketContent.ACKPACKET:
            sender.ack(content.number);
            break;
        case PacketContent.NAK_SELECT:
            sender.nak(content.number, false);
            break;
        case PacketContent.NAK_BACK_N:
            sender.nak(content.number, true);
            break;
        }
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
        String message = terminal.readLine();
        StringContent content = new StringContent(message);
        sender.send(content);
    }
    
    void readFromFile() throws Exception {
        String fname= terminal.readLine();

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
        terminal.printSys("File size: " + buffer.length);            

        FileInfoContent fcontent = new FileInfoContent(fname, size);

        //packetBuffer.add(fcontent);       

        // Send packet with file name and length
        terminal.printSys("Sending packet w/ name & length");
        sender.send(fcontent);
        terminal.printSys("Packet sent");        
    }
}
