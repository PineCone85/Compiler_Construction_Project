import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Lexer {

    private static final Map<String, TokenType> reservedKeywords = new HashMap<>();
    
    private static final Pattern V_NAMES_PATTERN = Pattern.compile("V_[a-z]([a-z]|[0-9])*");
    private static final Pattern F_NAMES_PATTERN = Pattern.compile("F_[a-z]([a-z]|[0-9])*");
    private static final Pattern TEXT_SNIPPET_PATTERN = Pattern.compile("\"[A-Z][a-z]{0,7}\"");
    private static final Pattern N_NUMBERS_PATTERN = Pattern.compile(
        "0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]"
    );

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
        reservedKeywords.put("return", TokenType.RETURN);
    }

    public String tokenizeToXML(String filePath) throws IOException, IllegalArgumentException {
        StringBuilder xmlOutput = new StringBuilder();
        xmlOutput.append("<TOKENSTREAM>\n");
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        String line;
        int id = 1;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            for (int i = 0; i < line.length(); ) {
                if (Character.isWhitespace(line.charAt(i))) {
                    i++;
                    continue;
                }

                Token token = null;

                token = matchReserved(line.substring(i));
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                token = matchPattern(F_NAMES_PATTERN, line.substring(i), TokenType.F_NAMES);
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                token = matchPattern(V_NAMES_PATTERN, line.substring(i), TokenType.V_NAMES);
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                token = matchPattern(TEXT_SNIPPET_PATTERN, line.substring(i), TokenType.TEXT_SNIPPET);
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                token = matchPattern(N_NUMBERS_PATTERN, line.substring(i), TokenType.N_NUMBERS);
                if (token != null) {
                    xmlOutput.append(formatToken(token, id++));
                    i += token.getValue().length();
                    continue;
                }

                char currentChar = line.charAt(i);
                if (isSymbol(currentChar)) {
                    token = new Token(getTokenTypeForSymbol(currentChar), String.valueOf(currentChar));
                    xmlOutput.append(formatToken(token, id++));
                    i++;
                    continue;
                }

                throw new IllegalArgumentException("Lexical Error: Unrecognized token at index " + i + ": '" + line.substring(i) + "'");
            }
        }

        reader.close();
        xmlOutput.append("</TOKENSTREAM>");
        return xmlOutput.toString();
    }

    private Token matchReserved(String input) {
        for (Map.Entry<String, TokenType> entry : reservedKeywords.entrySet()) {
            String key = entry.getKey();
            if (input.startsWith(key)) {
                return new Token(entry.getValue(), key);
            }
        }
        return null;
    }

    private Token matchPattern(Pattern pattern, String input, TokenType type) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.lookingAt()) {
            return new Token(type, matcher.group());
        }
        return null;
    }

    private String formatToken(Token token, int id) {
        String tokenValue = escapeXMLCharacters(token.getValue());
        return "<TOK>\n" +
               "<ID>" + id + "</ID>\n" +
               "<CLASS>" + getTokenClass(token.getType()) + "</CLASS>\n" +
               "<WORD>" + tokenValue + "</WORD>\n" +
               "</TOK>\n";
    }
    
    private String escapeXMLCharacters(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
    

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
            case RETURN:
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

    private boolean isSymbol(char c) {
        return c == ';' || c == '(' || c == ')' || c == ',' || c == '{' || c == '}';
    }

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

    public void saveXMLToFile(String xmlOutput, String outputFilePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
        writer.write(xmlOutput);
        writer.close();
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        try {
            String xmlOutput = lexer.tokenizeToXML("lexerInput.txt");
            lexer.saveXMLToFile(xmlOutput, "output.xml");

            System.out.println("XML file saved to output.xml.");
        } catch (IOException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }
}
