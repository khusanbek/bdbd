import java.awt.*;
import javax.swing.*;

public class BidMasterWindow extends JFrame {

    private JLabel itemLabel;
    private JTextField itemField;
    private JButton startButton;
    private JButton endButton;
    private JButton finalBidButton;
    private JTextArea logArea;

    public BidMasterWindow() {
		
        setTitle("Bid Master");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        Font font = new Font("SansSerif", Font.PLAIN, 16);

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left side: Item label, textfield, Start Auction
        JPanel leftTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        itemLabel = new JLabel("Item:");
        itemField = new JTextField(12);
        startButton = new JButton("Start Auction");

        itemLabel.setFont(font);
        itemField.setFont(font);
        startButton.setFont(font);

        leftTopPanel.add(itemLabel);
        leftTopPanel.add(itemField);
        leftTopPanel.add(startButton);

        // Right side: End Auction button
        endButton = new JButton("End Auction");
        endButton.setFont(font);
        JPanel rightTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightTopPanel.add(endButton);

        topPanel.add(leftTopPanel, BorderLayout.WEST);
        topPanel.add(rightTopPanel, BorderLayout.EAST);

        // --- Text Area ---
        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 15));
        logArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Auction Log"));

        // --- Bottom Panel ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        finalBidButton = new JButton("Final Bid?");
        finalBidButton.setFont(font);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.add(finalBidButton);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // --- Add panels ---
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new BidMasterWindow().setVisible(true);
            }
        });
    }
}
