// Version 0.3 of PennQBBot by Saajid Moyen, 11/22/2012

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class TGUI extends WindowAdapter{
        
        private final static String CONNECT_TEXT = "Connect";
        private final static String DISCONNECT_TEXT = "Disconnect";
        private final static String CONNECTING_TEXT = "Connecting...";
        private static final int PING_TIME = 10;  // number of seconds between checking the bot's connection
        private static final int SECOND = 1000;  // defines 1 second = 1000 ms
        private static final int SETTINGS_NICK = 30; // location of the nickname in settings
        private static final int SETTINGS_CHAN = 31; // location of the channel in settings
        private static final int SETTINGS_PASS = 32; // location of the password in settings
        private static final int SETTINGS_SERVER = 33; // location of the server name in settings
        private static final int NUM_TEXTBOXES = 4; // number of text boxes in the window
        private static final String SETTINGS_FILE = "settings.txt"; // location of the settings file
        private static final String SETTINGS_SENTINEL = "~"; // character which stops the loading of the settings file
        
        private JFrame mainContainer;
        private JPanel mainWindow;
        private JLabel nickLabel, passLabel, chanLabel, serverLabel;
        private JTextField nickText, passText, chanText, serverText;
        private JButton connectButton, exitButton;

        private boolean isConnected;
        
        public static void main(String[] args) {
                new TGUI();
        }

        public TGUI() {
                isConnected = false;

                mainContainer = new JFrame("Quiz Bowl Bot 2.0.8");
                mainContainer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                mainContainer.setSize(320, 200);
                
                mainWindow = new JPanel(new GridLayout(5,2,10,10));
                mainContainer.add(mainWindow);
                mainContainer.addWindowListener(this);
                
                // Add the components
                nickLabel = new JLabel("Bot's nickname: ");
                mainWindow.add(nickLabel);
                nickText = new JTextField();
                mainWindow.add(nickText);
                
                passLabel = new JLabel("Bot's password (optional): ");
                mainWindow.add(passLabel);
                passText = new JPasswordField();
                mainWindow.add(passText);
                
                chanLabel = new JLabel("Bot's channel: ");
                mainWindow.add(chanLabel);
                chanText = new JTextField();
                mainWindow.add(chanText);
                
                serverLabel = new JLabel("Bot's IRC server: ");
                mainWindow.add(serverLabel);
                serverText = new JTextField();
                mainWindow.add(serverText);
                
                connectButton = new JButton(CONNECT_TEXT);
                connectButton.addActionListener(new ConnectListener());
                mainWindow.add(connectButton);
                
                exitButton = new JButton("Exit");
                exitButton.addActionListener(new ActionListener(){
                        public void actionPerformed(ActionEvent e)
                        {
                                exit();
                        }
                });
                mainWindow.add(exitButton);
                
                // Get the default values for the text boxes
                loadSettings();
                
                // Show the window
                mainContainer.repaint();
                mainContainer.setVisible(true);
        }
        
        // Load the bot's parameters from its settings file.
        private void loadSettings()
        {
                int settingsLoaded = 0;
                try {
                        BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE));
                        String line;
                        int counter = 0;
                        while(true)
                        {
                                line = reader.readLine();
                                if(line == null || line.equals(SETTINGS_SENTINEL))
                                        break;

                                // Get the values specific to our interface
                                if(counter == SETTINGS_NICK) 
                                {
                                        nickText.setText(line);
                                        settingsLoaded++;
                                }
                                else if(counter == SETTINGS_CHAN)
                                {
                                        chanText.setText(line);
                                        settingsLoaded++;
                                }
                                else if(counter == SETTINGS_PASS)
                                {
                                        passText.setText(line);
                                        settingsLoaded++;
                                }
                                else if(counter == SETTINGS_SERVER)
                                {
                                        serverText.setText(line);
                                        settingsLoaded++;
                                }
                                counter++;
                        }
                        reader.close();
                } catch(IOException e) {
                        e.printStackTrace();
                }
                
                // If we didn't load in a setting, then the settings file is bad and we need to exit the program gracefully.
                if(settingsLoaded != NUM_TEXTBOXES)
                {
                        JOptionPane.showMessageDialog(null, "The settings file is improperly formatted.  There should be " +
                                        Integer.toString(NUM_TEXTBOXES - settingsLoaded) + " additional lines before the end of the file or the " +
                                        SETTINGS_SENTINEL + " character.  Exiting the bot...",
                                        "Improperly formatted settings file", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                }
        }
        
        private void saveSettings()
        {
                LinkedList<String> settingsContents = new LinkedList<String>();
                // Read in the file, replacing the appropriate lines with the new values from the text boxes.  Later on, overwrite the file.
                try {
                        BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE));
                        int counter = 0;
                        String line;
                        while(true)
                        {
                                line = reader.readLine();
                                if(line == null)
                                        break;
                                if(counter == SETTINGS_NICK)
                                        settingsContents.add(nickText.getText());
                                else if(counter == SETTINGS_CHAN)
                                        settingsContents.add(chanText.getText());
                                else if(counter == SETTINGS_PASS)
                                        settingsContents.add(passText.getText());
                                else if(counter == SETTINGS_SERVER)
                                        settingsContents.add(serverText.getText());
                                else
                                        settingsContents.add(line);
                                counter++;
                        }
                        reader.close();
                } catch(IOException e) {
                        e.printStackTrace();
                }
                
                // Get the last element in settings and save it separately so that we don't leave a trailing newLine.
                // We assume that there's at least one line in the settings file, otherwise the program would have terminated before.
                String lastElem = settingsContents.removeLast();
                
                try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(SETTINGS_FILE));
                        for(String s : settingsContents)
                        {
                                writer.write(s);
                                writer.flush();
                                writer.newLine();
                                writer.flush();
                        }
                        writer.write(lastElem);
                        writer.flush();
                        
                        writer.close();
                } catch(IOException e) {
                        e.printStackTrace();
                }
        }
        
        // Enable all of the text components and the connect button
        private void setEnable(boolean enabled)
        {
                JComponent[] components = {nickText, passText, chanText, serverText, connectButton};
                for(JComponent c : components)
                        c.setEnabled(enabled);
        }
        
        // Sets up the interface for when the bot gets disconnected
        private void botDisconnected()
        {
                connectButton.setText(CONNECT_TEXT);
                setEnable(true);
        }
        
        public void windowClosing(WindowEvent e)
        {
                exit();
        }
        
        private void exit()
        {
                // Ask if they want to leave.  If they do, ask them if they want to save.
                int choice = JOptionPane.showConfirmDialog(null, "Exit the bot?", "Exit?", 
                                JOptionPane.YES_NO_OPTION);
                if(choice == JOptionPane.YES_OPTION)
                {
                        choice = JOptionPane.showConfirmDialog(null, "Do you want to save your settings?", 
                                        "Save?", JOptionPane.YES_NO_OPTION);
                        if(choice == JOptionPane.YES_OPTION)
                                saveSettings();
                        System.exit(0);
                }
        }
        
        // Determines how the program acts when connectButton is clicked
        public class ConnectListener implements ActionListener
        {
                private RunBot rBot;
                
                @Override
                public void actionPerformed(ActionEvent arg0) {
                        if(connectButton.getText().equals(CONNECT_TEXT))
                        {
                                connectButton.setText(CONNECTING_TEXT);
                                setEnable(false);
                                rBot = new RunBot();
                                // run takes care of resetting the command button, as ugly as
                                        // that is.
                                rBot.start();
                        }
                        else if(connectButton.getText().equals(DISCONNECT_TEXT))
                        {
                                rBot.disconnect();
                                rBot = null;
                                botDisconnected();
                        }
                }
                
        }
        
        // Runs the bot and handles checking its connection.
        private class RunBot extends Thread
        {
                @Override
                public void run() {
                        TBot bot = new TBot(nickText.getText(), passText.getText(),
                                        chanText.getText(), serverText.getText());
                        if(bot.isConnected())
                        {
                                isConnected = true;
                                connectButton.setText(DISCONNECT_TEXT);
                                // Only reenable the connect button to allow for disconnecting
                                connectButton.setEnabled(true);
                                
                                synchronized(this) {
                                        while(isConnected) {
                                                try {
                                                        isConnected = bot.isConnected();
                                                        // Check in every so often to make sure that a disconnect caused by the bot is caught at some point.
                                                        wait(PING_TIME * SECOND);
                                                } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                }
                                        }
                                }
                                // Forces the bot to disconnect
                                bot.setAttemptReconnect(false);
                                bot.disconnect();
                                botDisconnected();
                                return;
                        }
                        else
                        {
                                // If we're here, we failed to connect.
                                JOptionPane.showMessageDialog(null, "The bot was unable to connect.",
                                                "Connection error", JOptionPane.ERROR_MESSAGE);
                                connectButton.setText(CONNECT_TEXT);
                                setEnable(true);
                                return;
                        }
                }
                
                public synchronized void disconnect()
                {
                        isConnected = false;
                        notify();
                }
        } 

}
