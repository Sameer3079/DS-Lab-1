package chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * text area to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    
    JTextField textField = new JTextField(40);
    JList<String> listBox = new JList<String>();
    DefaultListModel<String> model = new DefaultListModel<>();
    JTextArea messageArea = new JTextArea(8, 40);
    HashSet<String> selectedUsers = new HashSet<String>();
    String selectedUsersString = "";
    String messageType = "";
    String name = "";
    
    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the text field so that pressing Return in the
     * listener sends the text field contents to the server.  Note
     * however that the text field is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        listBox.setVisible(true);
        listBox.setSize(500, 100);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.getContentPane().add(listBox, "West"); // Setting the listBox on the left side of the application
        frame.pack();
        
        

        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the text field by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
            	out.println(textField.getText());
                textField.setText("");
                selectedUsers.clear(); // Empty the hash set
                selectedUsersString = ""; // Clearing the selected users after the message has been sent
            }
        });
        
        listBox.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub
				// Checking to see whether the newly selected name has already been selected and whether the selected name is the name of the sending client
				if(!(selectedUsers.contains(listBox.getSelectedValue().toString()) & (name.equals(listBox.getSelectedValue().toString())))){
					System.out.println("User Selected : " + listBox.getSelectedValue().toString() + " " + selectedUsers.contains(listBox.getSelectedValue().toString()));
					selectedUsers.add(listBox.getSelectedValue().toString());
					selectedUsersString += listBox.getSelectedValue().toString();
					
					// Resetting the text field so that the string can be placed in it
					if(textField.getText() != ""){
						textField.setText("");
					}
					
					textField.setText(selectedUsersString + ">>");
					selectedUsersString += ",";
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
        	
        });
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chatter",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            String senderName;
            if (line.startsWith("SUBMITNAME")) {
            	senderName = getName();
                out.println(senderName);
                frame.setTitle("Chatter: " + senderName);
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            }
            else if (line.startsWith("USERLIST")) {
            	line = line.substring(8);
            	String[] userArray = line.split(":");
            	model.clear();
            	for (String user : userArray) {
					model.addElement(user);
				}
            	listBox.setModel(model);
            }
            else {
            	messageArea.append(line + "\n");
            }
        }
    }

    /**
     * Runs the client as an application with a closable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}