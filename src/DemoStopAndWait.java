

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
        
        new Client(clientTerm, DEMO_HOST, DEMO_CLIENT_PORT, DEMO_SERVER_PORT, 1, 2, false);
        new Server(serverTerm, DEMO_HOST, DEMO_CLIENT_PORT, DEMO_SERVER_PORT, 1, 2, false);
    }
}
