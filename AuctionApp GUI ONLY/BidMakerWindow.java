import java.awt.*;
import javax.swing.*;

public class BidMakerWindow extends JFrame {

    private JTextField nameField;
    private JButton joinButton;
    private JButton finalBidButton;
    private JTextArea logArea;
    private JTextField bidAmountField;
    private JButton bidButton;

    public BidMakerWindow() {
		
        setTitle("Bid Maker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        Font font = new Font("SansSerif", Font.PLAIN, 16);

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left side: Name and Join Auction
        JPanel leftTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel nameLabel = new JLabel("Name:");
        nameField = new JTextField(10);
        joinButton = new JButton("Join Auction");

        nameLabel.setFont(font);
        nameField.setFont(font);
        joinButton.setFont(font);

        leftTopPanel.add(nameLabel);
        leftTopPanel.add(nameField);
        leftTopPanel.add(joinButton);

        // Right side: Yes! Final Bid!
        finalBidButton = new JButton("Yes! Final Bid!");
        finalBidButton.setFont(font);
        JPanel rightTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightTopPanel.add(finalBidButton);

        topPanel.add(leftTopPanel, BorderLayout.WEST);
        topPanel.add(rightTopPanel, BorderLayout.EAST);

        // --- Center Text Area ---
        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 15));
        logArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Auction Log"));

        // --- Bottom Panel ---
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

        // --- Add everything ---
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new BidMakerWindow().setVisible(true);
            }
        });
    }
}
