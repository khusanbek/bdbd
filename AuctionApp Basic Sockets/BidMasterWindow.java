import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.*;
import javax.swing.*;

/**
 * BidMasterWindow
 * - GUI + ServerSocket
 * - Start Auction: reads item, appends to textarea, starts server on port 5000
 * - End Auction: closes server socket and all clients
 * - Final Bid?: requests final confirmation; server expects FINAL_CONFIRM from last bidder
 *
 * Protocol (plain text lines):
 * JOIN|<name>
 * START|<item>            (server -> clients)
 * BID|<name>|<amount>     (client -> server, server -> all)
 * FINAL_REQUEST           (server -> clients)
 * FINAL_CONFIRM|<name>    (client -> server)
 * END                     (server -> clients)
 */
public class BidMasterWindow extends JFrame {

    private JTextField itemField;
    private JButton startButton;
    private JButton endButton;
    private JButton finalBidButton;
    private JTextArea logArea;

    // Networking
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final List<ClientHandler> clients = new ArrayList<ClientHandler>(); // guarded by clients
    private volatile boolean serverRunning = false;

    // Last bid tracker
    private volatile String lastBidderName = null;
    private volatile String lastBidAmount = null;
    private volatile boolean waitingForFinal = false;

    // Port
    private final int PORT = 5000;

    public BidMasterWindow() {
        setTitle("Bid Master");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        final Font font = new Font("SansSerif", Font.PLAIN, 16);

        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel leftTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel itemLabel = new JLabel("Item:");
        itemField = new JTextField(12);
        startButton = new JButton("Start Auction");

        itemLabel.setFont(font);
        itemField.setFont(font);
        startButton.setFont(font);

        leftTopPanel.add(itemLabel);
        leftTopPanel.add(itemField);
        leftTopPanel.add(startButton);

        endButton = new JButton("End Auction");
        endButton.setFont(font);
        JPanel rightTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightTopPanel.add(endButton);

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
        finalBidButton = new JButton("Final Bid?");
        finalBidButton.setFont(font);
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.add(finalBidButton);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Button actions
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onStartAuction();
            }
        });

        endButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onEndAuction();
            }
        });

        finalBidButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onFinalBidRequest();
            }
        });

        // Ensure sockets close on exit
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                shutdownServer();
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

    private void onStartAuction() {
        final String item = itemField.getText().trim();
        if (item.length() == 0) {
            appendLog("Please enter an item before starting the auction.");
            return;
        }
        appendLog("Auction started for item: " + item);
        broadcastToAll("START|" + item);

        if (serverRunning) {
            appendLog("Server already running on port " + PORT);
            return;
        }

        startServer();
    }

    private void onEndAuction() {
        appendLog("Ending auction...");
        broadcastToAll("END");
        shutdownServer();
        appendLog("Auction ended. All clients disconnected.");
    }

    private void onFinalBidRequest() {
        synchronized (this) {
            if (!serverRunning) {
                appendLog("Server not running. Start auction first.");
                return;
            }
            if (lastBidderName == null) {
                appendLog("No bids have been placed yet; no last bidder to confirm final bid.");
                return;
            }
            if (waitingForFinal) {
                appendLog("Already waiting for final confirmation from " + lastBidderName);
                return;
            }
            waitingForFinal = true;
        }

        appendLog("Requesting final confirmation from last bidder: " + lastBidderName + " (amount: $" + lastBidAmount + ")");
        // Notify all clients (they will enable their final-confirm UI)
        broadcastToAll("FINAL_REQUEST|" + lastBidderName + "|" + lastBidAmount);

        // We will not block the EDT waiting here. Server will accept FINAL_CONFIRM when received.
        // Once FINAL_CONFIRM is received and name matches lastBidderName, server will append to log and reset waitingForFinal.
    }

    private void startServer() {
        serverRunning = true;
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException ex) {
            appendLog("Failed to open server socket on port " + PORT + ": " + ex.getMessage());
            serverRunning = false;
            return;
        }

        appendLog("Server listening on port " + PORT);

        acceptThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (serverRunning && !serverSocket.isClosed()) {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(clientSocket);
                        synchronized (clients) {
                            clients.add(handler);
                        }
                        appendLog("Client connected: " + clientSocket.getRemoteSocketAddress());
                        handler.start();
                    }
                } catch (IOException ioe) {
                    if (serverRunning) {
                        appendLog("Server accept error: " + ioe.getMessage());
                    } // else server socket closed during shutdown
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void shutdownServer() {
        serverRunning = false;
        // close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        } finally {
            serverSocket = null;
        }

        // close client handlers
        synchronized (clients) {
            for (ClientHandler ch : new ArrayList<ClientHandler>(clients)) {
                ch.closeConnection();
            }
            clients.clear();
        }

        // interrupt accept thread
        if (acceptThread != null && acceptThread.isAlive()) {
            try {
                acceptThread.interrupt();
            } catch (Exception ex) {
                // ignore
            }
        }
        waitingForFinal = false;
        lastBidderName = null;
        lastBidAmount = null;
    }

    private void broadcastToAll(String message) {
        synchronized (clients) {
            for (ClientHandler ch : new ArrayList<ClientHandler>(clients)) {
                ch.sendMessage(message);
            }
        }
    }

    // ClientHandler to manage each connected client
    private class ClientHandler {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private Thread readThread;
        private String clientName = null;

        ClientHandler(Socket s) {
            this.socket = s;
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            } catch (IOException ex) {
                appendLog("Error creating IO for client: " + ex.getMessage());
                closeConnection();
            }
        }

        void start() {
            readThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        String line;
                        while (socket != null && !socket.isClosed() && (line = in.readLine()) != null) {
                            handleClientMessage(line);
                        }
                    } catch (IOException ioe) {
                        // connection closed or error
                    } finally {
                        closeConnection();
                    }
                }
            });
            readThread.setDaemon(true);
            readThread.start();
        }

        void handleClientMessage(String line) {
            if (line == null) return;
            appendLog("Received from client: " + line);

            // Parse messages
            if (line.startsWith("JOIN|")) {
                String[] parts = line.split("\\|", 2);
                if (parts.length >= 2) {
                    clientName = parts[1];
                    appendLog("Client joined as: " + clientName);
                    // Optionally broadcast join to others
                    broadcastToAll("BIDMASTER|INFO|" + clientName + " joined.");
                }
            } else if (line.startsWith("BID|")) {
                // BID|<name>|<amount>
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    String name = parts[1];
                    String amount = parts[2];
                    lastBidderName = name;
                    lastBidAmount = amount;
                    String announce = "BID|" + name + "|" + amount;
                    appendLog("Bid received: " + name + " -> $" + amount);
                    broadcastToAll(announce);
                }
            } else if (line.startsWith("FINAL_CONFIRM|")) {
                // FINAL_CONFIRM|<name>
                String[] parts = line.split("\\|", 2);
                if (parts.length >= 2) {
                    String name = parts[1];
                    handleFinalConfirm(name);
                }
            } else {
                appendLog("Unknown message from client: " + line);
            }
        }

        void handleFinalConfirm(String name) {
            synchronized (BidMasterWindow.this) {
                if (!waitingForFinal) {
                    appendLog("Received FINAL_CONFIRM from " + name + " but no final was requested.");
                    return;
                }
                if (lastBidderName != null && lastBidderName.equals(name)) {
                    appendLog("Final bid confirmed by " + name + " for $" + lastBidAmount);
                    broadcastToAll("BIDMASTER|FINAL_CONFIRMED|" + name + "|" + lastBidAmount);
                    waitingForFinal = false;
                    // After final confirmed, we may want to end auction or disable further bids.
                    // We'll not automatically end auction here; user can press End Auction manually.
                } else {
                    appendLog("FINAL_CONFIRM received from " + name + " but last bidder is " + lastBidderName + ". Ignoring.");
                }
            }
        }

        void sendMessage(String msg) {
            try {
                if (out != null) {
                    out.println(msg);
                }
            } catch (Exception ex) {
                appendLog("Failed to send to client: " + ex.getMessage());
                closeConnection();
            }
        }

        void closeConnection() {
            try {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            } finally {
                socket = null;
            }
            try {
                if (in != null) {
                    try { in.close(); } catch (IOException ex) {}
                }
            } catch (Exception e) {}
            try { if (out != null) out.close(); } catch (Exception e) {}

            synchronized (clients) {
                clients.remove(this);
            }
            appendLog("Client disconnected: " + clientName);
        }
    }

    // main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new BidMasterWindow().setVisible(true);
            }
        });
    }
}
