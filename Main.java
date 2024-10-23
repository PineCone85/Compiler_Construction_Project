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

     
        JButton lexButton = new JButton("Run Lexing");
        JButton parseButton = new JButton("Run Parsing");
        JButton scopeCheckButton = new JButton("Run Scope Analysis and Type Checking");

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

        JPanel panel = new JPanel();  
        panel.add(lexButton);
        panel.add(parseButton);
        panel.add(scopeCheckButton);

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
