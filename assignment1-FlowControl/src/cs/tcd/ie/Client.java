package cs.tcd.ie;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.FileInputStream;
import java.net.SocketAddress;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Client class.
 * 
 * An instance accepts user input 
 */
public class Client extends Node {
    SenderWindow window;
    
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
        window = new SenderWindow(this, windowSize, sequenceLength);
        sender = new Sender(this, dstAddress);
        receiver = new LinkedBlockingQueue<>();
        window.start();
        sender.start();
        listener.go();
    }

    @Override
    public void onReceipt(DatagramPacket packet) {
        PacketContent content= PacketContent.fromDatagramPacket(packet);
        switch(content.getType()) {
        case PacketContent.ACKPACKET:
            window.ack(content.number);
            break;
        case PacketContent.NAK_SELECT:
            window.nak(content.number, false);
            break;
        case PacketContent.NAK_BACK_N:
            window.nak(content.number, true);
            break;
        }
    }

    /**
     * Client is never expected to receive information packets.
     */
    @Override
    public void packetReady() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void bufferPacket(PacketContent content) {
        sender.add(content);
    }

    @Override
    public PacketContent getPacket() {
        return readString();
    }
    
    PacketContent readString() {
        String message = terminal.readLine();
        return new StringContent(message);
    }
    
    PacketContent readFromFile() throws Exception {
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
        return fcontent;
    }
}
