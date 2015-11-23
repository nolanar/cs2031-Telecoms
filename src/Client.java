package cs.tcd.ie;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client class.
 * 
 * An instance accepts user input 
 */
public class Client extends Node {
    private final SenderWindow window;
    
    /**
     * Constructor
     * 	 
     * Attempts to create socket at given port and create an InetSocketAddress for the destinations
     */
    Client(Terminal terminal, String dstHost, int port, int dstPort,
            int windowSize, int sequenceLength, boolean goBackN) {
            
        this.terminal = terminal;
        
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        SocketAddress dstAddress = new InetSocketAddress(dstHost, dstPort);
        sender = new Sender(this, dstAddress);
        receiver = new LinkedBlockingQueue<>();
        window = new SenderWindow(windowSize, sequenceLength) {

            @Override
            public PacketContent getPacket() {
                return readStringContent();
            }

            @Override
            public void sendPacket(PacketContent packet) {
                sender.add(packet);
            }
            
        };
        
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
     * Read string from terminal and return resulting packet.
     * 
     * @return 
     */
    PacketContent readStringContent() {
        String message = terminal.readLine();
        return new StringContent(message);
    }
}
