/*
NAME: REDDAMMAGARI SREE SOUMYA
ID: 1001646494
NET-ID: sxr6494 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.sql.Timestamp;



public class MessagingServer extends JFrame implements ActionListener, Runnable {
    
    private Map<String, User> users = new HashMap<>();
    private Map<String, Queue<String>> messages = new HashMap<>();

    
    private JPanel usersPanel = new JPanel();

    // Set the UI of the server
    public MessagingServer() {
        setTitle("Messaging Server");
        setSize(300, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
      // Set the UI for disconnection
        /*used JPanel https://www.geeksforgeeks.org/java-swing-jpanel-examples/ link for setting up panel properties*/
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Checked users are online."));
        
        JButton disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this);
        topPanel.add(disconnectButton);
        
        add(BorderLayout.NORTH, topPanel);

        // Set UI for users
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setBackground(Color.WHITE);
        usersPanel.setBorder(BorderFactory.createEtchedBorder());
        
        add(BorderLayout.CENTER, usersPanel);

        // Load all messages from file
        /*Reading and Writing objects into file:
        	https://www.spigotmc.org/threads/save-load-hashmap-in-dat-file.132177/?__cf_chl_jschl_tk__=e437e361783caa93a8a772de9fe4111d0c6d90fc-1585621687-0-AbssqmZb7YmrC7T-y2wXj4VQBVr69HzAhYaUcTPb777lHRm-uxQsSxZbrV09GqjVyu5v8BIQLX20TlKsfL3UJjC0aPhUB0iSr66ZIJCSo5qd54TeUcHS3milwyZhKqe426efPXy5F9n6mTgsWj_ejoK_-OoFm2sXNqxpvZnDKLxDRHSapANDXvQTp3iwxHb5tEVAYqhE-iKaF3NqmXZC3oN0ABKxhpFe83RxtEVddVLVENa4Er7ilu-ZzKH0fYsSkRZrXYU8g8ja0GeGvIS1_TFKMa_UfgkUnNZd8TnBYWIIbvGX7z6cIKg9eRb_Ie5CrTmmKYRp5cCRIN_7XK7PCgM*/

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("messages.dat"));
            messages = (Map<String, Queue<String>>) ois.readObject();

            // Load the usernames to the list
            for (String username : messages.keySet()) {
                User user = new User(createReadOnlyCheckBox(username), null, null);
                
                users.put(username, user);
                usersPanel.add(user.checkBox);
            }
            
            ois.close();
        } catch (Exception e) {
        }

        // Server now starts
        new Thread(this).start();
    }

    // Write all messages to file
    /*Reading and Writing objects into file:
    	https://www.spigotmc.org/threads/save-load-hashmap-in-dat-file.132177/?__cf_chl_jschl_tk__=e437e361783caa93a8a772de9fe4111d0c6d90fc-1585621687-0-AbssqmZb7YmrC7T-y2wXj4VQBVr69HzAhYaUcTPb777lHRm-uxQsSxZbrV09GqjVyu5v8BIQLX20TlKsfL3UJjC0aPhUB0iSr66ZIJCSo5qd54TeUcHS3milwyZhKqe426efPXy5F9n6mTgsWj_ejoK_-OoFm2sXNqxpvZnDKLxDRHSapANDXvQTp3iwxHb5tEVAYqhE-iKaF3NqmXZC3oN0ABKxhpFe83RxtEVddVLVENa4Er7ilu-ZzKH0fYsSkRZrXYU8g8ja0GeGvIS1_TFKMa_UfgkUnNZd8TnBYWIIbvGX7z6cIKg9eRb_Ie5CrTmmKYRp5cCRIN_7XK7PCgM*/

    private synchronized void saveMessages() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("messages.dat"));
            oos.writeObject(messages);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Wait for client connections
    @Override
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(8080);
            
            while (true) {
                // Wait for a new client to connect
                Socket s = ss.accept();
                
                ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                
                Message message = (Message) ois.readObject();

                // At this point we should expect the client to welcome with a CONNECT message, otherwise
                // just reject the client
                if (!message.getMessage().equals("CONNECT")) {
                    oos.writeObject(new Message("INVALID", "Unknown welcome message."));
                    s.close();
                    continue;
                }

                /* Check the validity of the username
                setting the state of checkbox
                http://www.java2s.com/Code/JavaAPI/javax.swing/JCheckBoxisSelected.htm*/
                String username = (String) message.getData();
                User user;
                
                if (users.containsKey(username)) {
                    user = users.get(username);
                    //if checkbox is marked then username is online
                    if (user.checkBox.isSelected()) {
                        oos.writeObject(new Message("INVALID", "Username is online."));
                        continue;
                    }
                    
                    user.inputStream = ois;
                    user.outputStream = oos;
                }
                //if username is not in the list create a new user by adding readonly checkbox for the user,input and output streams
                else {
                    user = new User(createReadOnlyCheckBox(username), ois, oos);
                    usersPanel.add(user.checkBox);
                }
                
                if (!messages.containsKey(username)) {
                    messages.put(username, new LinkedList<>());
                }

                // Mark the user online
                user.checkBox.setSelected(true);
                usersPanel.updateUI();
                
                users.put(username, user);
                /*for (String aUsername : users.keySet()) {
                    usernames.add(aUsername);
                }*/
                String x="";
                for ( String key : users.keySet() ) {
                     //System.out.println( key );
                     x=x+","+key;
                     }
                // Create another thread that handles specifically this user
                oos.writeObject(new Message("VALID", "Welcome " + username + "!\n"+" List of usernames\n"+x));
                new Thread(new UserThread(username)).start();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
    //used to handle sending messages to multiple clients input is list of usernames and 
    //message that needs to be sent to client for handling 
    private void broadcastMessage(Set<String> usernames, Message message) {
        for (String username : usernames) {
            if (!users.containsKey(username)) {
                // Don't send to a non-existing user
                continue;
            }

            User user = users.get(username);

            if (!user.checkBox.isSelected()) {
                // Don't send to an offline user
                continue;
            }

            try {
                user.outputStream.writeObject(message);
                //System.out.println(message.getData().toString());
               // user.mesq.add(message); 
               // System.out.println("HI");

               // System.out.println(user.mesq.peek().getData().toString());
               // user.outputStream.writeObject(user.mesq.peek());
                //user.mesq.remove();
            } catch (Exception e) {
            }
        }
    }

    // Handle button actions, there's only one button at the moment, that is to disconnect the server
    @Override
    public void actionPerformed(ActionEvent e) {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to stop the server?") == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // Start the server
    public static void main(String[] args) {
        new MessagingServer().setVisible(true);
    }

    // Tracks user info
    private class User {
        
        public JCheckBox checkBox;
        public ObjectInputStream inputStream;
        public ObjectOutputStream outputStream;

        // Create a new user
        public User(JCheckBox checkBox, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
            this.checkBox = checkBox;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }
    }

    // A thread to accept and respond to a user's request
    private class UserThread implements Runnable {
        
        public String username;

        // Create a thread
        public UserThread(String username) {
            this.username = username;
        }

        // Run and wait for requests from user
        //code used for timestamp: //https://tecadmin.net/get-current-timestamp-in-java/
        @Override
        public void run() {
            User user = users.get(username);
            
            try {
                ObjectInputStream ois = user.inputStream;
                ObjectOutputStream oos = user.outputStream;
                
                while (true) {
                    // Wait for a message to broadcast to other users
                    Message message = (Message) ois.readObject();

                    // Interpret the request of the user
                    if (message.getMessage().equals("DELIVER MESSAGE")) {
                        // Deliver the message to all users but to the queue
                        Set<String> receipients = (Set<String>) message.getData();
                        Date date= new Date();
                    	long time = date.getTime();
                        Timestamp ts = new Timestamp(time);
                        
                        for (String username : receipients) {
                            if (!messages.containsKey(username)) {
                                messages.put(username, new LinkedList<>());
                            }
                            
                            messages.get(username).add(message.getExtraData().toString()+" || "+" timestamp: "+ts);
                        }

                        // Save the messages to a file so that when the server crashes we can still load it
                        saveMessages();
                    } else if (message.getMessage().equals("DISCONNECT")) {
                        break;
                    }
                    //Send a set of active users to client 
                    else if (message.getMessage().equals("USERS")) {
                        // Respond all users
                       Set<String> usernames = new HashSet<>();
                        
                        /*for (String aUsername : users.keySet()) {
                            usernames.add(aUsername);
                        }*/
                    	for (String aUsername : users.keySet()) {
                            if (users.get(aUsername).checkBox.isSelected()) {
                                usernames.add(aUsername);
                            }
                        }
                        broadcastMessage(users.keySet(), new Message("USERS", usernames));

                        
                        //oos.writeObject(new Message("USERS", usernames));
                    } else if (message.getMessage().equals("USERS1")) {
                        // Respond all users

                        
                        oos.writeObject(new Message("USERS1", null));
                    }else if (message.getMessage().equals("MESSAGES")) {
                        // Get all the messages of the user and clear up the queue and send them to client one by one for displaying
                        if (messages.get(username).isEmpty()) {
                            oos.writeObject(new Message("MESSAGE DELIVERY", "There are no messages queued."));
                        } else {
                            while (!messages.get(username).isEmpty()) {
                                oos.writeObject(new Message("MESSAGE DELIVERY", messages.get(username).remove()));
                            }
                        }
                        
                        saveMessages();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Disconnect this user, tell everyone this user got disconnected
            user.checkBox.setSelected(false);
            usersPanel.updateUI();
            broadcastMessage(users.keySet(), new Message("DISCONNECTED USER", username));

        }
    }

    // Create a read-only checkbox
    private JCheckBox createReadOnlyCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);

        // Remove mouse events
        for (MouseListener mouseListener : (MouseListener[]) checkBox.getListeners(MouseListener.class)) {
            checkBox.removeMouseListener(mouseListener);
        }

        // Remove key events
        //https://stackoverflow.com/questions/4472530/disabling-space-bar-triggering-click-for-jbutton
        InputMap inputMap = checkBox.getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "none");
        inputMap.put(KeyStroke.getKeyStroke("released SPACE"), "none");
        
        return checkBox;
    }
}
