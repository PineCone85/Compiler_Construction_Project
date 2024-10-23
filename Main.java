import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;

public class Main {

    private JTextArea outputArea;

    public static void main(String[] args) {
        // Set up the GUI
        JFrame frame = new JFrame("Compiler Frontend");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        // Create a JTextArea for displaying output
        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);  // Users can't edit output
        JScrollPane scrollPane = new JScrollPane(outputArea);  // Scrollbar for the text area
        scrollPane.setPreferredSize(new Dimension(580, 400));

        // Redirect System.out to JTextArea
        PrintStream printStream = new PrintStream(new CustomOutputStream(outputArea));
        System.setOut(printStream);  // Redirect standard output
        System.setErr(printStream);  // Redirect standard error (optional)

        // Create buttons for each phase of the process
        JButton lexButton = new JButton("Run Lexing");
        JButton parseButton = new JButton("Run Parsing");
        JButton scopeCheckButton = new JButton("Run Scope Analysis and Type Checking");

        // Set button actions
        lexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Lexer lexer = new Lexer();
                lexer.Lex();
            }
        });

        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SLRParser parser = new SLRParser();
                parser.SLRParsing();
                Parser parserSynTree = new Parser();
                parserSynTree.parse();
            }
        });

        scopeCheckButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScopeAnalyzer analyzer = new ScopeAnalyzer();
                analyzer.scopeAndTypeCheck();
            }
        });

        // Layout the buttons and output area
        JPanel panel = new JPanel();  // Panel to hold buttons
        panel.add(lexButton);
        panel.add(parseButton);
        panel.add(scopeCheckButton);

        // Add components to the frame
        frame.add(panel, BorderLayout.NORTH);  // Buttons on top
        frame.add(scrollPane, BorderLayout.CENTER);  // Output area in the center

        // Display the frame
        frame.setVisible(true);
    }

    // Custom output stream to redirect console output to JTextArea
    public static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());  // Auto-scroll to bottom
        }
    }
}
