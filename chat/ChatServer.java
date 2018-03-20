package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A multi threaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    // A hash map to replace both hash sets
    // reason: To find the writer for each client separately
    private static HashMap<String, PrintWriter> hashmap = new HashMap<String, PrintWriter>();
    /**
     * The application main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }
    
    
    // 
    private static void sendUserList() { // Sends the list of connected clients to all clients
    	ArrayList<String> userList = new ArrayList<>(hashmap.keySet());
        String userListStr = String.join(":", userList); // Joining the array to create a string
        
        for(String name : hashmap.keySet()) {
        	hashmap.get(name).println("USERLIST" + userListStr); // sending the string to all the clients
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String messageType = "";

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (hashmap.keySet()) {
                        if (!hashmap.keySet().contains(name)) {
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                hashmap.put(name, out); // adding the Name(key) and the PrintWriter(value) to the Hash Map
                out.println("NAMEACCEPTED");
                
                sendUserList();

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    // Identifying messageType
                    String message[] = input.split(">>");
                    String receipients[] = message[0].split(",");
                    if(input.contains(">>")) {
                    	if(receipients.length>1)
                    		messageType = "MULTICAST";
                    	else
                    		messageType = "UNICAST";
                    }
                    else
                    	messageType = "BROADCAST";
                    // Sending the data according to the messageType
                    if (messageType == "BROADCAST") {
                    	for (PrintWriter writer : hashmap.values()) {
                            writer.println("MESSAGE " + name + ": " + input);
                        }
                    }
                    else if(messageType == "UNICAST"){
                    	String receipientName = input.substring(0, input.indexOf(">>")); // Getting the name of the recipient
                        if(hashmap.containsKey(receipientName)) { // Checks to see if the recipient exists in the Hash Map
                			PrintWriter PW = hashmap.get(receipientName); // Getting the PrintWriter for the recipient
                			input = input.substring(receipientName.length()+2); // Getting the message excluding the >> characters
                			PW.println("MESSAGE " + name + " >> " + receipientName + "(UNICAST): " + input); // Sending the message to the client (recipient)
                			out.println(name + ">> " + receipientName + "(UNICAST): " + input); // Sending the message to the client (sender)
                        }
                    }
                    else if(messageType == "MULTICAST") {
                    	for (int x=0; x<receipients.length; x++) { // Iterating through the selected clients
                    		if(hashmap.containsKey(receipients[x])) { // Send the message if the recipient name is valid
                    			hashmap.get(receipients[x]).println("MESSAGE " + name + ": " + message[1]);
                    		}
                    	}
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null && out != null) {
                    hashmap.remove(name,out); // Remove the user and the print writer from the hash map
                    sendUserList(); // Send the updated hash map details to the clients
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}