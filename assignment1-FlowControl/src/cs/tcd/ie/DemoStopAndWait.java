/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs.tcd.ie;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aran
 */
public class DemoStopAndWait {

    public static final int DEMO_CLIENT_PORT = 50000;
    public static final int DEMO_SERVER_PORT = 50001;
    public static final String DEMO_HOST = "localhost";    
    
    public static void main(String[] args) {
        
        Node.debugMode = true;
        Node.dropRate = 0.20;
        
        Terminal clientTerm =  new Terminal("Client");
        Terminal serverTerm =  new Terminal("Server");
        
        Client client = new Client(clientTerm, DEMO_HOST, DEMO_CLIENT_PORT,
                DEMO_SERVER_PORT, 1, 2, false);
        Server server = new Server(serverTerm, DEMO_HOST, DEMO_CLIENT_PORT,
                DEMO_SERVER_PORT, 1, 2, false);
        
        try {
            client.start();
            server.start();
        } catch (Exception ex) {
            Logger.getLogger(DemoStopAndWait.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
