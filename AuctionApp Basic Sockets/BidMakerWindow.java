import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 * BidMakerWindow (Client)
 * - Join Auction: connects to localhost:5000 and sends JOIN|<name>
 * - Bid: sends BID|<name>|<amount>
 * - Yes! Final Bid!: sends FINAL_CONFIRM|<name> (only when a FINAL_REQUEST is received)
 *
 * Client listens to server messages and appends them to textarea.
 */
public class BidMakerWindow extends JFrame {

    private JTextField nameField;
    private JButton joinButton;
    private JButton finalBidButton;
    private JTextArea logArea;
    private JTextField bidAmountField;
    private JButton bidButton;

    // Networking
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread readThread;
    private volatile boolean connected = false;
    private volatile boolean finalRequested = false;

    // Server host/port
    private final String HOST = "localhost";
    private final int PORT = 5000;

    public BidMakerWindow() {
        setTitle("Bid Maker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        final Font font = new Font("SansSerif", Font.PLAIN, 16);

        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel leftTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel nameLabel = new JLabel("Name:");
        nameField = new JTextField(12);
        joinButton = new JButton("Join Auction");

        nameLabel.setFont(font);
        nameField.setFont(font);
        joinButton.setFont(font);

        leftTopPanel.add(nameLabel);
        leftTopPanel.add(nameField);
        leftTopPanel.add(joinButton);

        finalBidButton = new JButton("Yes! Final Bid!");
        finalBidButton.setFont(font);
        finalBidButton.setEnabled(false); // only enabled when FINAL_REQUEST received
        JPanel rightTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightTopPanel.add(finalBidButton);

        topPanel.add(leftTopPanel, BorderLayout.WEST);
        topPanel.add(rightTopPanel, BorderLayout.EAST);

        // Center
        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 15));
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Auction Log"));

        // Bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JPanel rightBottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JLabel dollarLabel = new JLabel("$");
        bidAmountField = new JTextField(10);
        bidButton = new JButton("Bid");

        dollarLabel.setFont(font);
        bidAmountField.setFont(font);
        bidButton.setFont(font);

        rightBottomPanel.add(dollarLabel);
        rightBottomPanel.add(bidAmountField);
        rightBottomPanel.add(bidButton);

        bottomPanel.add(rightBottomPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Actions
        joinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onJoin();
            }
        });

        bidButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onBid();
            }
        });

        finalBidButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onFinalConfirm();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });
    }

    private void appendLog(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logArea.append(text + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    private void onJoin() {
        final String name = nameField.getText().trim();
        if (name.length() == 0) {
            appendLog("Please enter your name before joining.");
            return;
        }
        appendLog("You joined as: " + name);

        if (connected) {
            appendLog("Already connected to server.");
            return;
        }

        // Connect in background thread
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    socket = new Socket(HOST, PORT);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                    connected = true;
                    // Send JOIN message
                    out.println("JOIN|" + name);
                    appendLog("Sent JOIN|" + name + " to server.");
                    // start reading thread
                    startReadThread();
                } catch (IOException ex) {
                    appendLog("Failed to connect to server: " + ex.getMessage());
                    closeConnection();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void startReadThread() {
        readThread = new Thread(new Runnable() {
            public void run() {
                try {
                    String line;
                    while (connected && (line = in.readLine()) != null) {
                        handleServerMessage(line);
                    }
                } catch (IOException ex) {
                    // connection closed or error
                } finally {
                    closeConnection();
                }
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    private void handleServerMessage(final String msg) {
        appendLog("Server: " + msg);

        if (msg.startsWith("START|")) {
            // Example: START|<item>
            // nothing special on client UI besides log
        } else if (msg.startsWith("BID|")) {
            // BID|name|amount
            // nothing extra
        } else if (msg.startsWith("FINAL_REQUEST")) {
            // Could be "FINAL_REQUEST" or "FINAL_REQUEST|<name>|<amount>" (master implementation sends latter)
            finalRequested = true;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    finalBidButton.setEnabled(true);
                }
            });
            appendLog("Final confirmation requested by master. Press 'Yes! Final Bid!' if you are the last bidder.");
        } else if (msg.startsWith("FINAL_REQUEST|")) {
            // MASTER sends "FINAL_REQUEST|<lastBidderName>|<lastAmount>"
            finalRequested = true;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    finalBidButton.setEnabled(true);
                }
            });
            appendLog("Final confirmation requested by master: " + msg.substring("FINAL_REQUEST|".length()));
        } else if (msg.startsWith("BIDMASTER|FINAL_CONFIRMED|")) {
            // Confirmation broadcast
            appendLog("Final confirmed: " + msg.substring("BIDMASTER|FINAL_CONFIRMED|".length()));
            // After final confirmed, client should disable final button
            finalRequested = false;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    finalBidButton.setEnabled(false);
                }
            });
        } else if (msg.startsWith("END")) {
            appendLog("Server ended the auction.");
            closeConnection();
        } else {
            // Other informational messages
        }
    }

    private void onBid() {
        final String amount = bidAmountField.getText().trim();
        if (amount.length() == 0) {
            appendLog("Please enter a bid amount.");
            return;
        }
        if (!connected || out == null) {
            appendLog("You are not connected to server. Press Join Auction first.");
            return;
        }
        final String name = nameField.getText().trim();
        if (name.length() == 0) {
            appendLog("Name is empty. Please enter your name.");
            return;
        }

        // Send BID|name|amount
        out.println("BID|" + name + "|" + amount);
        appendLog("Your bid: $" + amount + " (sent)");
    }

    private void onFinalConfirm() {
        if (!finalRequested) {
            appendLog("No final request active.");
            return;
        }
        if (!connected || out == null) {
            appendLog("Not connected to server.");
            return;
        }
        final String name = nameField.getText().trim();
        if (name.length() == 0) {
            appendLog("Name is empty.");
            return;
        }
        // Send FINAL_CONFIRM|name
        out.println("FINAL_CONFIRM|" + name);
        appendLog("You confirmed the final bid (sent).");
        // disable button until next final request
        finalRequested = false;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                finalBidButton.setEnabled(false);
            }
        });
    }

    private void closeConnection() {
        connected = false;
        finalRequested = false;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                finalBidButton.setEnabled(false);
            }
        });

        try {
            if (in != null) {
                try { in.close(); } catch (IOException ex) {}
            }
        } catch (Exception e) {}
        try {
            if (out != null) out.close();
        } catch (Exception e) {}
        try {
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException ex) {}
            }
        } catch (Exception e) {}
        in = null;
        out = null;
        socket = null;
        appendLog("Disconnected from server.");
    }

    // main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new BidMakerWindow().setVisible(true);
            }
        });
    }
}
