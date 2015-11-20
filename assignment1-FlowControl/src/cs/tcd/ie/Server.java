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
        window = new ReceiverWindow(windowSize, sequenceLength, goBackN) {

            @Override
            public void sendPacket(PacketContent packet) {
                sender.add(packet);
            }

            @Override
            public void outputPacket(PacketContent packet) {
                printPacketContent(packet);
            }
            
        };
        
        receiver = new LinkedBlockingQueue<>();
        listener.go();
    }

    @Override
    public void onReceipt(DatagramPacket packet) {
        try {
            PacketContent content = PacketContent.fromDatagramPacket(packet);
            window.receive(content);
        }
        catch(Exception e) {e.printStackTrace();}
    }
    
    private void printPacketContent(PacketContent packet) {

        switch (packet.getType()) { 
            case PacketContent.FILEINFO:
                terminal.println("File name: " + ((FileInfoContent)packet).getFileName());
                terminal.println("File size: " + ((FileInfoContent)packet).getFileSize());
                break;
            case PacketContent.STRINGPACKET:
                terminal.println(packet.toString());
                break;
        }
        
    }
    
    public synchronized void start() throws Exception {
        terminal.println("Waiting for contact");
        this.wait();
    }
}
