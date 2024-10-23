import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;

public class Main {

    private JTextArea outputArea;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Compiler Frontend");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(580, 400));
   
        PrintStream printStream = new PrintStream(new CustomOutputStream(outputArea));
        System.setOut(printStream);  
        System.setErr(printStream);  

        // Create a single "Compile" button
        JButton compileButton = new JButton("Compile");

        // Add action listener for "Compile" button
        compileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Lexing Phase
                    Lexer lexer = new Lexer();
                    lexer.Lex();

                    // Parsing Phase
                    SLRParser parser = new SLRParser();
                    parser.SLRParsing();
                    Parser parserSynTree = new Parser();
                    parserSynTree.parse();

                    // Scope Analysis and Type Checking Phase
                    ScopeAnalyzer analyzer = new ScopeAnalyzer();
                    analyzer.scopeAndTypeCheck();
                    
                } catch (Exception ex) {
                    System.err.println("Error during compilation: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        // Panel to hold the "Compile" button
        JPanel panel = new JPanel();  
        panel.add(compileButton);

        // Add components to frame
        frame.add(panel, BorderLayout.NORTH); 
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    public static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength()); 
        }
    }
}
