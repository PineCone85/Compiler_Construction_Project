import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Lexer {

    // Reserved Keywords and their corresponding TokenType
    private static final Map<String, TokenType> reservedKeywords = new HashMap<>();
    
    // Patterns for token classes
    private static final Pattern V_NAMES_PATTERN = Pattern.compile("V_[a-z]([a-z]|[0-9])*");
    private static final Pattern F_NAMES_PATTERN = Pattern.compile("F_[a-z]([a-z]|[0-9])*");
    private static final Pattern TEXT_SNIPPET_PATTERN = Pattern.compile("\"[A-Z][a-z]{0,7}\"");
    private static final Pattern N_NUMBERS_PATTERN = Pattern.compile(
        "0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]"
    );

    // Static block to initialize reserved keywords
    static {
        reservedKeywords.put("main", TokenType.MAIN);
        reservedKeywords.put("num", TokenType.NUM);
        reservedKeywords.put("text", TokenType.TEXT);
        reservedKeywords.put("begin", TokenType.BEGIN);
        reservedKeywords.put("end", TokenType.END);
        reservedKeywords.put(";", TokenType.SEMICOLON);
        reservedKeywords.put("skip", TokenType.SKIP);
        reservedKeywords.put("halt", TokenType.HALT);
        reservedKeywords.put("print", TokenType.PRINT);
        reservedKeywords.put("< input", TokenType.INPUT);
        reservedKeywords.put("(", TokenType.LPAREN);
        reservedKeywords.put(")", TokenType.RPAREN);
        reservedKeywords.put(",", TokenType.COMMA);
        reservedKeywords.put("if", TokenType.IF);
        reservedKeywords.put("then", TokenType.THEN);
        reservedKeywords.put("else", TokenType.ELSE);
        reservedKeywords.put("not", TokenType.NOT);
        reservedKeywords.put("sqrt", TokenType.SQRT);
        reservedKeywords.put("or", TokenType.OR);
        reservedKeywords.put("and", TokenType.AND);
        reservedKeywords.put("=", TokenType.EQ);
        reservedKeywords.put("grt", TokenType.GRT);
        reservedKeywords.put("add", TokenType.ADD);
        reservedKeywords.put("sub", TokenType.SUB);
        reservedKeywords.put("mul", TokenType.MUL);
        reservedKeywords.put("div", TokenType.DIV);
        reservedKeywords.put("void", TokenType.VOID);
        reservedKeywords.put("{", TokenType.LBRACE);
        reservedKeywords.put("}", TokenType.RBRACE);
    }

    // Method to tokenize the input from a text file and return an XML representation
    public String tokenizeToXML(String filePath) throws IOException {
        StringBuilder xmlOutput = new StringBuilder();
        xmlOutput.append("<TOKENSTREAM>\n");
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        String line;
        int id = 1;

        while ((line = reader.readLine()) != null) {
            // Remove any spaces or newlines
            line = line.trim();

            // Tokenize by looping through characters and matching patterns
            for (int i = 0; i < line.length(); ) {
                // Skip spaces and newlines
                if (Character.isWhitespace(line.charAt(i))) {
                    i++;
                    continue;
                }

                Token token = null;

                // Check for reserved keywords or symbols
                token = matchReserved(line.substring(i));
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                // Check for function names (F_NAMES)
                token = matchPattern(F_NAMES_PATTERN, line.substring(i), TokenType.F_NAMES);
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                // Check for variable names (V_NAMES)
                token = matchPattern(V_NAMES_PATTERN, line.substring(i), TokenType.V_NAMES);
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                // Check for text snippets (TEXT_SNIPPET)
                token = matchPattern(TEXT_SNIPPET_PATTERN, line.substring(i), TokenType.TEXT_SNIPPET);
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                // Check for numbers (N_NUMBERS)
                token = matchPattern(N_NUMBERS_PATTERN, line.substring(i), TokenType.N_NUMBERS);
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                // Handle individual symbols as separate tokens
                char currentChar = line.charAt(i);
                if (isSymbol(currentChar)) {
                    token = new Token(getTokenTypeForSymbol(currentChar), String.valueOf(currentChar));
                    xmlOutput.append(formatToken(token, id++));
                    i++;  // Move to the next character
                    continue;
                }

                // Handle unrecognized tokens
                System.err.println("Unrecognized token at: " + line.substring(i));
                i++;
            }
        }

        reader.close();
        xmlOutput.append("</TOKENSTREAM>");
        return xmlOutput.toString();
    }

    // Method to match a reserved keyword or symbol
    private Token matchReserved(String input) {
        for (Map.Entry<String, TokenType> entry : reservedKeywords.entrySet()) {
            String key = entry.getKey();
            if (input.startsWith(key)) {
                return new Token(entry.getValue(), key);
            }
        }
        return null;
    }

    // Method to match a pattern for token classes
    private Token matchPattern(Pattern pattern, String input, TokenType type) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.lookingAt()) {
            return new Token(type, matcher.group());
        }
        return null;
    }

    // Helper method to format tokens into XML
    private String formatToken(Token token, int id) {
        return "<TOK>\n" +
               "<ID>" + id + "</ID>\n" +
               "<CLASS>" + getTokenClass(token.getType()) + "</CLASS>\n" +
               "<WORD>" + token.getValue() + "</WORD>\n" +
               "</TOK>\n";
    }

    // Helper method to get the class of the token based on its TokenType
    private String getTokenClass(TokenType type) {
        switch (type) {
            case MAIN:
            case NUM:
            case TEXT:
            case BEGIN:
            case END:
            case SEMICOLON:
            case SKIP:
            case HALT:
            case PRINT:
            case INPUT:
            case LPAREN:
            case RPAREN:
            case COMMA:
            case IF:
            case THEN:
            case ELSE:
            case NOT:
            case SQRT:
            case OR:
            case AND:
            case EQ:
            case GRT:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case VOID:
            case LBRACE:
            case RBRACE:
                return "reserved_keyword";
            case V_NAMES:
                return "V";
            case F_NAMES:
                return "F";
            case TEXT_SNIPPET:
                return "T";
            case N_NUMBERS:
                return "N";
            default:
                return "unknown";
        }
    }

    // Helper method to check if the character is a symbol
    private boolean isSymbol(char c) {
        return c == ';' || c == '(' || c == ')' || c == ',' || c == '{' || c == '}';
    }

    // Helper method to get the TokenType for a symbol
    private TokenType getTokenTypeForSymbol(char c) {
        switch (c) {
            case ';': return TokenType.SEMICOLON;
            case '(': return TokenType.LPAREN;
            case ')': return TokenType.RPAREN;
            case ',': return TokenType.COMMA;
            case '{': return TokenType.LBRACE;
            case '}': return TokenType.RBRACE;
            default: return null;
        }
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        try {
            String xmlOutput = lexer.tokenizeToXML("lexerInput.txt");  // Path to your .txt file
            System.out.println(xmlOutput);  // Print the XML to the console
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
