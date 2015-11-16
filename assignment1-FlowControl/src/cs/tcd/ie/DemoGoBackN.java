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
public class DemoGoBackN extends Demo{

    public static void main(String[] args) {
        
        int windowSize = 4;
        Node.debugMode = true;
        Node.dropRate = 0.20;
        
        Terminal clientTerm =  new Terminal("Client");
        Terminal serverTerm =  new Terminal("Server");
        
        Client client = new Client(clientTerm, DEMO_HOST, DEMO_SRC_PORT,
                DEMO_DST_PORT, windowSize, 2 * windowSize, true);
        Server server = new Server(serverTerm, DEMO_HOST, DEMO_SRC_PORT,
                DEMO_DST_PORT, windowSize, 2 * windowSize, true);
        
        try {
            client.start();
            server.start();
        } catch (Exception ex) {
            Logger.getLogger(DemoGoBackN.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
