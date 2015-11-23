

import com.eleet.dragonconsole.*;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aran
 */
public class Terminal {
    
    private DragonConsole dc;
    private LinkedBlockingQueue<String> buffer;
    
    public Terminal(String WindowTitle) {
        dc = new MyConsole();
        DragonConsoleFrame dcf = new DragonConsoleFrame(WindowTitle, dc);
        dcf.setResizable(true);
        dcf.setVisible(true);
    }

    public void println(String text) {
        dc.appendWithoutProcessing(text + "\n");
    }
    
    public void printSys(String text) {
        dc.appendSystemMessage(text + "\n");
    }
    
    /**
     * Blocking method that removes and returns next buffered string.
     */
    public String readLine() {
        String line = null;
        try {
            line = buffer.take();
        } catch (InterruptedException ex) {
            Logger.getLogger(Terminal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return line;
    }
    
    class MyCommandProcessor extends CommandProcessor {
        
        private final DragonConsole console;
        
        public MyCommandProcessor(DragonConsole console) {
            super();
            this.console = console;
            buffer = new LinkedBlockingQueue<>();
        }
        
        @Override
        public void processCommand(String input) {
            buffer.add(input);
            console.appendWithoutProcessing(input + "\n");
        }
    }
    
    class MyConsole extends DragonConsole {
        
        public MyConsole() throws IllegalArgumentException {
            super(false, false);
            this.setCommandProcessor(new MyCommandProcessor(this));
            setPrompt("&Ob" + ">>> ");
            setDefaultColor("wb");
            setSystmeColor("Xb");
        }
    }
}
