package cs.tcd.ie;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.LinkedBlockingQueue;

public class Server extends Node {

    final ReceiverWindow window;
    
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
        sender = new Sender(this, srcAddress);
        sender.start();
        window = new ReceiverWindow(this, windowSize, sequenceLength, goBackN);
        window.start();
        receiver = new LinkedBlockingQueue<>();
        listener.go();
    }

    @Override
    public void onReceipt(DatagramPacket packet) {
        try {
            window.receive(packet);
        }
        catch(Exception e) {e.printStackTrace();}
    }

    /**
     * Action to take upon packets entering the received buffer.
     * 
     */
    @Override
    public void packetReady() {
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
     * The server passively waits for packets.
     */
    @Override
    public PacketContent getPacket() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public synchronized void start() throws Exception {
        terminal.println("Waiting for contact");
        this.wait();
    }
}
