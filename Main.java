import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class Main {


    public static void main(String[] args) {
        JFrame frame = new JFrame("Compiler Frontend");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 500); 

        // Create the output area
        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setPreferredSize(new Dimension(480, 400));  

        PrintStream printStream = new PrintStream(new CustomOutputStream(outputArea));
        System.setOut(printStream);  
        System.setErr(printStream);


        JTextArea inputArea = new JTextArea();
        inputArea.setLineWrap(true); 
        inputArea.setWrapStyleWord(true);
        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        inputScrollPane.setPreferredSize(new Dimension(480, 400)); 

        JButton compileButton = new JButton("Compile");

        compileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    outputArea.setText("");

                    String userInput = inputArea.getText();
                    FileWriter writer = new FileWriter("source_code.txt");
                    writer.write(userInput);
                    writer.close();

                    Lexer lexer = new Lexer();
                    lexer.Lex();

                    SLRParser parser = new SLRParser();
                    parser.SLRParsing();
                    Parser parserSynTree = new Parser();
                    parserSynTree.parse();

                    ScopeAnalyzer analyzer = new ScopeAnalyzer();
                    analyzer.scopeAndTypeCheck();

                } catch (Exception ex) {
                    System.err.println("Error during compilation: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(compileButton);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));  
        mainPanel.add(inputScrollPane); 
        mainPanel.add(outputScrollPane); 

        frame.add(buttonPanel, BorderLayout.NORTH);  
        frame.add(mainPanel, BorderLayout.CENTER);  

        frame.setVisible(true);
    }

    public static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) throws IOException {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}
