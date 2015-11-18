package cs.tcd.ie;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Server extends Node {

    final BufferedSender sender;
    final WindowedReceiver receiver;
    
    /**
     * 
     */
    Server(Terminal terminal, String srcHost, int srcPort, int port,
            int windowSize, int sequenceLength, boolean goBackN) {
        try {
            socket= new DatagramSocket(port);
        }
        catch(java.lang.Exception e) {e.printStackTrace();}
        this.terminal= terminal;
        SocketAddress srcAddress = new InetSocketAddress(srcHost, srcPort);
        sender = new BufferedSender(this, srcAddress);
        sender.start();
        receiver = new WindowedReceiver(this, windowSize, sequenceLength, goBackN);
        receiver.start();
        listener.go();
    }

    @Override
    public void onReceipt(DatagramPacket packet) {
        try {
            receiver.receive(packet);
        }
        catch(Exception e) {e.printStackTrace();}
    }

    /**
     * Action to take upon packets entering the received buffer.
     * 
     * @param receiver
     */
    public void packetReady(WindowedReceiver receiver) {
        while (!receiver.isEmpty()) {
            PacketContent content = receiver.remove();
            
            switch (content.getType()) { 
            case PacketContent.FILEINFO:
                terminal.println("File name: " + ((FileInfoContent)content).getFileName());
                terminal.println("File size: " + ((FileInfoContent)content).getFileSize());
                break;
            case PacketContent.STRINGPACKET:
                terminal.println(content.toString());
                break;
            }
        }
    }
    
    /**
     * Puts the packet in the send buffer to await being sent.
     * 
     * @param content
     */    
    public void bufferPacket(PacketContent content) {
        sender.add(content);
    }
    
    public synchronized void start() throws Exception {
        terminal.println("Waiting for contact");
        this.wait();
    }

}
