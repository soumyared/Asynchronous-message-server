/*
NAME: REDDAMMAGARI SREE SOUMYA
ID: 1001646494
NET-ID: sxr6494 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.util.Date;
import java.sql.Timestamp;

public class MessagingClient extends JFrame implements ActionListener {

    private JTextField serverAddressTextField = new JTextField(10);
    private JTextField usernameTextField = new JTextField(10);
    private JTextField messageTextField = new JTextField(20);
    private JButton connectDisconnectButton = new JButton("Connect");
    private JButton sendMessageButton = new JButton("Send");
    private JButton sendMessageOptionButton = new JButton("Send a Message");
    private JButton checkMessagesOptionButton = new JButton("Check my Messages");
    private JPanel usersPanel = new JPanel();
    private JList messagesList = new JList(new DefaultListModel());
    private JPanel messagesPanel = new JPanel(new BorderLayout());


    private Map<String, JCheckBox> usersCheckBox = new HashMap<>();

    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    // Setup the layout of the client
    public MessagingClient() {
        super("Messaging Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

       // UI for connecting to server
        /*used JPanel https://www.geeksforgeeks.org/java-swing-jpanel-examples/ link for seting up panel properties*/
        JPanel connectionPanel = new JPanel(new FlowLayout());
        add(BorderLayout.NORTH, connectionPanel);

        connectionPanel.add(new JLabel("Server Address:"));
        connectionPanel.add(serverAddressTextField);
        connectionPanel.add(new JLabel("Username:"));
        connectionPanel.add(usernameTextField);
        connectionPanel.add(connectDisconnectButton);
        connectionPanel.add(sendMessageOptionButton);
        connectionPanel.add(checkMessagesOptionButton);

        connectDisconnectButton.addActionListener(this);

        sendMessageOptionButton.setEnabled(false);
        checkMessagesOptionButton.setEnabled(false);

        sendMessageOptionButton.addActionListener(this);
        checkMessagesOptionButton.addActionListener(this);

        // UI for displaying online users
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setBackground(Color.WHITE);
        usersPanel.setPreferredSize(new Dimension(150, 0));
        usersPanel.setBorder(BorderFactory.createEtchedBorder());
        add(BorderLayout.WEST, usersPanel);

        // UI for displaying messages
        messagesPanel.add(BorderLayout.CENTER, new JScrollPane(messagesList));

        JPanel sendMessagePanel = new JPanel(new FlowLayout());
        sendMessagePanel.add(new JLabel("Message (Checked users will receive the message): "));
        sendMessagePanel.add(messageTextField);
        sendMessagePanel.add(sendMessageButton);
        sendMessageButton.addActionListener(this);
        messagesPanel.add(BorderLayout.SOUTH, sendMessagePanel);

        add(BorderLayout.CENTER, messagesPanel);
        sendMessageButton.setEnabled(false);
    }

    // Initiate a connection to server
    private void connectToServer() {
    	//Get the text in the server address text field and username text field
        String serverAddress = serverAddressTextField.getText().trim();
        String username = usernameTextField.getText().trim();
        //if address is empty redo
        if (serverAddress.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A server address is required.");
            return;
        }
        //if username is empty redo
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A username is required.");
            return;
        }

        try {
            Socket socket = new Socket(serverAddress, 8080);

            // Send a welcome message to the server
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());
            //Send a connect message to the user along with username to check if the username is allowed to use
            oos.writeObject(new Message("CONNECT", username));

            Message message = (Message) ois.readObject();
            JOptionPane.showMessageDialog(this, message.getData());
            // Indicates username is already online by server
            if (message.getMessage().equals("INVALID")) {
                return;
            }
            //request for active users
            oos.writeObject(new Message("USERS"));

            // Start a new thread if username is valid
            new Thread(new MessageThread()).start();
            
            usernameTextField.setEditable(false);
            serverAddressTextField.setEditable(false);
            connectDisconnectButton.setText("Disconnect");

            sendMessageOptionButton.setEnabled(true);
            checkMessagesOptionButton.setEnabled(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection invalid.");
        }
    }

    // Disconnect from server
    private void disconnectFromServer() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to disconnect?") != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            oos.writeObject(new Message("DISCONNECT"));
        } catch (Exception e) {
        }

        System.exit(0);
    }

    // Send message to selected users
    // code used for time stamp https://tecadmin.net/get-current-timestamp-in-java/
    private void sendMessage() {
        String message = messageTextField.getText().trim();
        /*Date date= new Date();
    	long time = date.getTime();
        Timestamp ts = new Timestamp(time);

        if (message.isEmpty()) {
            return;
        }*/
        //add senders name to message content
        message = usernameTextField.getText() + ": "+message;

        // Get only receipients selected
        Set<String> usernames = new HashSet<>();

        for (String username : usersCheckBox.keySet()) {
            if (usersCheckBox.get(username).isSelected()) {
                usernames.add(username);
            }
        }

        try {
            // Send it to server
            oos.writeObject(new Message("DELIVER MESSAGE", usernames, message));
            messageTextField.setText("");
            sendMessageButton.setEnabled(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Connection to server was lost.");
            System.exit(0);
        }
    }

    // Send a message to the server to get all users
    private void initSendMessage() {
        try {
            oos.writeObject(new Message("USERS1"));

            // The thread will wait for the response
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Connection to server was lost.");
            System.exit(0);
        }
    }
    
    // Send a message to server to get all messages
    private void checkMessages() {
        try {
            ((DefaultListModel) messagesList.getModel()).removeAllElements();
            oos.writeObject(new Message("MESSAGES"));
            
            // The thread will wait for the response
        } catch(Exception e) {
            JOptionPane.showMessageDialog(null, "Connection to server was lost.");
            System.exit(0);
        }
    }

    // Handle button events
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Connect")) {
            connectToServer();
        } else if (e.getActionCommand().equals("Disconnect")) {
            disconnectFromServer();
        } else if (e.getActionCommand().equals("Send")) {
            sendMessage();
        } else if (e.getActionCommand().equals("Send a Message")) {
            initSendMessage();
        } else if (e.getActionCommand().equals("Check my Messages")) {
            checkMessages();
        }
    }

    // Start the program
    public static void main(String[] args) {
        new MessagingClient().setVisible(true);
    }

    // Waits for server messages
    private class MessageThread implements Runnable {

        // Retrieve the server messages
        @Override
        public void run() {
            try {
                while (true) {
                	/*setting the state of checkbox
                    http://www.java2s.com/Code/JavaAPI/javax.swing/JCheckBoxisSelected.htm*/
                    Message message = (Message) ois.readObject();
                     //update userspanel with active users
                    if (message.getMessage().equals("USERS")) {
                    	usersPanel.removeAll();

                        // We got users we have to add
                        for (String username : (Set<String>) message.getData()) {
                        //if username is same as the usernametextfield then don't add it to userspanel
                            if (username.equals(usernameTextField.getText())) {
                                continue;
                            }

                            JCheckBox checkBox = new JCheckBox(username);
                            //set checkbox to true
                            checkBox.setSelected(true);
                            //add it to userspanel
                            usersPanel.add(checkBox);
                            //update the map
                            usersCheckBox.put(username, checkBox);
                        }

                        sendMessageButton.setEnabled(true);
                        usersPanel.updateUI();
                    } 
                    //enable send button only when send message is clicked
                    else if (message.getMessage().equals("USERS1")) {
                    	sendMessageButton.setEnabled(true);
                        usersPanel.updateUI();
                        }
                    //remove user from the userspanel if a user disconnects
                    else if (message.getMessage().equals("DISCONNECTED USER")) {
                        // There's a disconnected user, we remove it
                        String username = (String) message.getData();
                        //update map by removing user and remove it from userspanel
                        if (usersCheckBox.containsKey(username)) {
                            usersPanel.remove(usersCheckBox.get(username));
                            usersPanel.updateUI();
                          //this//
                            usersCheckBox.remove(username);
                        }}
                    //Display messages onto the messages panel
                    else if (message.getMessage().equals("MESSAGE DELIVERY")) {
                        // New message arrived
                        ((DefaultListModel) messagesList.getModel()).addElement(message.getData().toString());


                    }
                    //else if (message.getMessage().equals("MESSAGE DELIVERY1")) {
                        // New message arrived
                        //((DefaultListModel) messagesList.getModel()).removeAllElements();
                       // ((DefaultListModel) messagesList.getModel()).addElement(message.getData().toString());

                    //}
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Connection to server was lost.");
                System.exit(0);
            }
        }
    }
}
