import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class Parser {

    public static List<String> tokens;
    public static int index = 0;

    static class TreeNode {
        static int nodeCounter = 0;
        int unid;
        String value;
        List<TreeNode> children;
        boolean isTerminal;

        TreeNode(String value, boolean isTerminal) {
            this.unid = nodeCounter++;
            this.value = value;
            this.isTerminal = isTerminal;
            this.children = new ArrayList<>();
        }

        void addChild(TreeNode child) {
            children.add(child);
        }

        void buildXML(Document doc, Element parentElement, boolean isRoot) {
            Element nodeElement;

            if (isRoot) {
                nodeElement = doc.createElement("ROOT");
            } else {
                nodeElement = doc.createElement("NODE");
            }

            if (parentElement != null && !isRoot) {
                Element parentIdElement = doc.createElement("PARENT");
                parentIdElement.appendChild(doc.createTextNode(String.valueOf(getParentUID(parentElement))));
                nodeElement.appendChild(parentIdElement);
            }

            Element unidElement = doc.createElement("UNID");
            unidElement.appendChild(doc.createTextNode(String.valueOf(unid)));
            nodeElement.appendChild(unidElement);

            Element symbElement = doc.createElement("SYMB");
            symbElement.appendChild(doc.createTextNode(value));
            nodeElement.appendChild(symbElement);

            if (isTerminal) {
                Element terminalElement = doc.createElement("TERMINAL");
                terminalElement.appendChild(doc.createTextNode(value));
                nodeElement.appendChild(terminalElement);
            } else {
                if (!children.isEmpty()) {
                    Element childrenElement = doc.createElement("CHILDREN");
                    for (TreeNode child : children) {
                        Element childIdElement = doc.createElement("ID");
                        childIdElement.appendChild(doc.createTextNode(String.valueOf(child.unid)));
                        childrenElement.appendChild(childIdElement);
                    }
                    nodeElement.appendChild(childrenElement);
                }
            }

            parentElement.appendChild(nodeElement);

            for (TreeNode child : children) {
                child.buildXML(doc, nodeElement, false);
            }
        }

        private int getParentUID(Element parentElement) {
            for (int i = 0; i < parentElement.getChildNodes().getLength(); i++) {
                if (parentElement.getChildNodes().item(i).getNodeName().equals("UNID")) {
                    return Integer.parseInt(parentElement.getChildNodes().item(i).getTextContent());
                }
            }
            return -1; 
        }
    }

    static List<String> tokenize(String input) {
        return Arrays.asList(input.split("\\s+"));
    }

    static void match(String expected) {
        if (index < tokens.size() && tokens.get(index).equals(expected)) {
            index++;
        } else {
            index = 0;
            throw new RuntimeException("Expected " + expected + " but found " + 
                (index < tokens.size() ? tokens.get(index) : "end of input"));
                
        }
    }

    static TreeNode parsePROG() {
        TreeNode node = new TreeNode("PROG", false);
        match("main");
        node.addChild(new TreeNode("main", true));
        node.addChild(parseGLOBVARS());
        node.addChild(parseALGO());
        node.addChild(parseFUNCTIONS());
        return node;
    }

    static TreeNode parseGLOBVARS() {
        TreeNode node = new TreeNode("GLOBVARS", false);
        if (isVtyp()) {
            node.addChild(parseVTYP());
            node.addChild(parseVNAME());
            match(",");
            node.addChild(new TreeNode(",", true));
            node.addChild(parseGLOBVARS());
        }
        return node;
    }

    static TreeNode parseVTYP() {
        TreeNode node = new TreeNode("VTYP", false); 
        if (tokens.get(index).equals("num")) {
            match("num");
            node.addChild(new TreeNode("num", true)); 
        } else if (tokens.get(index).equals("text")) {
            match("text");
            node.addChild(new TreeNode("text", true));
        }
        return node;
    }

    static TreeNode parseVNAME() {
        TreeNode node = new TreeNode("VNAME", false);
        if (isVname()) {
            String vname = tokens.get(index); 
            match(vname);
            node.addChild(new TreeNode(vname, true));
        } else {
            throw new RuntimeException("Expected variable name but found " + tokens.get(index));
        }
        return node;
    }

    static TreeNode parseALGO() {
        TreeNode node = new TreeNode("ALGO", false);
        match("begin");
        node.addChild(new TreeNode("begin", true));
        node.addChild(parseINSTRUC());
        match("end");
        node.addChild(new TreeNode("end", true));
        return node;
    }

    static TreeNode parseINSTRUC() {
        TreeNode node = new TreeNode("INSTRUC", false);

        if (index >= tokens.size() || tokens.get(index).equals("end")) {
            return node;
        }

        if (isCommand()) { 
            node.addChild(parseCOMMAND());
            match(";");
            node.addChild(new TreeNode(";", true));

            if (index < tokens.size() && !tokens.get(index).equals("end")) {
                node.addChild(parseINSTRUC());
            }
        }
        return node;
    }

    static TreeNode parseCOMMAND() {
        TreeNode node = new TreeNode("COMMAND", false);
        switch (tokens.get(index)) {
            case "skip":
                match("skip");
                node.addChild(new TreeNode("skip", true));
                break;
            case "halt":
                match("halt");
                node.addChild(new TreeNode("halt", true));
                break;
            case "print":
                match("print");
                node.addChild(new TreeNode("print", true)); 
                node.addChild(parseATOMIC());
                break;
            case "return":
                match("return");
                node.addChild(new TreeNode("return", true));
                node.addChild(parseATOMIC());
                break;
            default:
                if (isVname()) {
                    node.addChild(parseASSIGN());
                } else if (isFname()) {
                    node.addChild(parseCALL());
                } else {
                    node.addChild(parseBRANCH());
                }
                break;
        }
        return node;
    }

    static TreeNode parseATOMIC() {
        TreeNode node = new TreeNode("ATOMIC", false);
        if (isVname()) {
            node.addChild(parseVNAME());
        } else {
            node.addChild(parseCONST());
        }
        return node;
    }

    static TreeNode parseCONST() {
        TreeNode node = new TreeNode("CONST", false);
        if (isNumber()) {
            String num = tokens.get(index);
            match(num);
            node.addChild(new TreeNode(num, true));
        } else if (isStringConst()) {
            String str = tokens.get(index);
            match(str);
            node.addChild(new TreeNode(str, true)); 
        } else {
            throw new RuntimeException("Expected constant but found " + tokens.get(index));
        }
        return node;
    }

    static TreeNode parseASSIGN() {
        TreeNode node = new TreeNode("ASSIGN", false);
        node.addChild(parseVNAME());
        if (tokens.get(index).equals("<")) {
            match("<");
            match("input");
            node.addChild(new TreeNode("< input", true));
        } else {
            match("=");
            node.addChild(new TreeNode("=", true));
            node.addChild(parseTERM());
        }
        return node;
    }

    static TreeNode parseCALL() {
        TreeNode node = new TreeNode("CALL", false);
        node.addChild(parseFNAME());
        match("(");
        node.addChild(new TreeNode("(", true));
        node.addChild(parseATOMIC());
        match(",");
        node.addChild(new TreeNode(",", true));
        node.addChild(parseATOMIC());
        match(",");
        node.addChild(new TreeNode(",", true));
        node.addChild(parseATOMIC());
        match(")");
        node.addChild(new TreeNode(")", true));
        return node;
    }

    static TreeNode parseBRANCH() {
        TreeNode node = new TreeNode("BRANCH", false);
        match("if");
        node.addChild(new TreeNode("if", true));
        node.addChild(parseCOND());
        match("then");
        node.addChild(new TreeNode("then", true));
        node.addChild(parseALGO());
        match("else");
        node.addChild(new TreeNode("else", true));
        node.addChild(parseALGO());
        return node;
    }

    static TreeNode parseTERM() {
        TreeNode node = new TreeNode("TERM", false);
        if (isVname() || isConst()) {
            node.addChild(parseATOMIC());
        } else if (isFname()) {
            node.addChild(parseCALL());
        } else {
            node.addChild(parseOP());
        }
        return node;
    }

    static TreeNode parseOP() {
        TreeNode node = new TreeNode("OP", false);
    
        if (isUnop()) {
            node.addChild(parseUNOP());
            match("(");
            node.addChild(new TreeNode("(", true));
            node.addChild(parseARG());
            match(")");
            node.addChild(new TreeNode(")", true));
        } else if (isBinop()) {
            node.addChild(parseBINOP());
            match("(");
            node.addChild(new TreeNode("(", true));
            node.addChild(parseARG());
            match(",");
            node.addChild(new TreeNode(",", true));
            node.addChild(parseARG());
            match(")");
            node.addChild(new TreeNode(")", true));
        }
        return node;
    }

    static TreeNode parseARG() {
        TreeNode node = new TreeNode("ARG", false);
        if (isVname() || isConst()) {
            node.addChild(parseATOMIC());
        } else {
            node.addChild(parseOP());
        }
        return node;
    }

    static TreeNode parseCOND() {
        TreeNode node = new TreeNode("COND", false);
        
        if (isBinop()) {
            if (isAtomicAhead()) {
                node.addChild(parseSIMPLE());
            } else {
                node.addChild(parseCOMPOSIT());
            }
        } else if (isUnop()) {
            node.addChild(parseCOMPOSIT());
        }
        return node;
    }

    static TreeNode parseSIMPLE() {
        TreeNode node = new TreeNode("SIMPLE", false);
        node.addChild(parseBINOP());
        match("(");
        node.addChild(new TreeNode("(", true));
        node.addChild(parseATOMIC());
        match(",");
        node.addChild(new TreeNode(",", true));
        node.addChild(parseATOMIC());
        match(")");
        node.addChild(new TreeNode(")", true));
        return node;
    }

    static TreeNode parseCOMPOSIT() {
        TreeNode node = new TreeNode("COMPOSIT", false);
    
        if (isUnop()) {
            node.addChild(parseUNOP());
            match("(");
            node.addChild(new TreeNode("(", true));
            node.addChild(parseSIMPLE());
            match(")");
            node.addChild(new TreeNode(")", true));
        } else if (isBinop()) {
            node.addChild(parseBINOP());
            match("(");
            node.addChild(new TreeNode("(", true));
            node.addChild(parseSIMPLE());
            match(",");
            node.addChild(new TreeNode(",", true));
            node.addChild(parseSIMPLE());
            match(")");
            node.addChild(new TreeNode(")", true));
        } else {
            throw new RuntimeException("Expected unary or binary operation but found " + tokens.get(index));
        }
    
        return node;
    }

    static TreeNode parseUNOP() {
        TreeNode node = new TreeNode("UNOP", false);
        String op = tokens.get(index);
        if (op.equals("not") || op.equals("sqrt")) {
            match(op);
            node.addChild(new TreeNode(op, true));
        } else {
            throw new RuntimeException("Expected unary operator but found " + op);
        }
        return node;
    }

    static TreeNode parseBINOP() {
        TreeNode node = new TreeNode("BINOP", false);
        String op = tokens.get(index);
        match(op);
        node.addChild(new TreeNode(op, true));
        return node;
    }

    static TreeNode parseFNAME() {
        TreeNode node = new TreeNode("FNAME", false);
        String fname = tokens.get(index);
        if (isFname()) {
            match(fname);
            node.addChild(new TreeNode(fname, true));
        } else {
            throw new RuntimeException("Expected function name but found " + tokens.get(index));
        }
        return node;
    }

    static TreeNode parseFUNCTIONS() {
        TreeNode node = new TreeNode("FUNCTIONS", false);
        if (isFtyp()) {
            node.addChild(parseDECL());
            node.addChild(parseFUNCTIONS());
        }
        return node;
    }

    static TreeNode parseDECL() {
        TreeNode node = new TreeNode("DECL", false);
        node.addChild(parseHEADER());
        node.addChild(parseBODY());
        return node;
    }

    static TreeNode parseHEADER() {
        TreeNode node = new TreeNode("HEADER", false);
        node.addChild(parseFTYP());
        node.addChild(parseFNAME());
        match("(");
        node.addChild(new TreeNode("(", true));
        node.addChild(parseVNAME());
        match(",");
        node.addChild(new TreeNode(",", true));
        node.addChild(parseVNAME());
        match(",");
        node.addChild(new TreeNode(",", true));
        node.addChild(parseVNAME());
        match(")");
        node.addChild(new TreeNode(")", true));
        return node;
    }

    static TreeNode parseFTYP() {
        TreeNode node = new TreeNode("FTYP", false);
        if (tokens.get(index).equals("num")) {
            match("num");
            node.addChild(new TreeNode("num", true));
        } else {
            match("void");
            node.addChild(new TreeNode("void", true));
        }
        return node;
    }

    static TreeNode parseBODY() {
        TreeNode node = new TreeNode("BODY", false);
        node.addChild(parsePROLOG());
        node.addChild(parseLOCVARS());
        node.addChild(parseALGO());
        node.addChild(parseEPILOG());
        node.addChild(parseSUBFUNCS());
        
        if (index < tokens.size()) {
            match("end");
            node.addChild(new TreeNode("end", true));
        } else {
            throw new RuntimeException("Expected 'end' but reached end of input.");
        }
    
        return node;
    }

    static TreeNode parsePROLOG() {
        TreeNode node = new TreeNode("PROLOG", false);
        match("{");
        node.addChild(new TreeNode("{", true));
        return node;
    }

    static TreeNode parseEPILOG() {
        TreeNode node = new TreeNode("EPILOG", false);
        match("}");
        node.addChild(new TreeNode("}", true));
        return node;
    }

    static TreeNode parseLOCVARS() {
        TreeNode node = new TreeNode("LOCVARS", false);
        for (int i = 0; i < 3; i++) {
            node.addChild(parseVTYP());
            node.addChild(parseVNAME());
            match(",");
            node.addChild(new TreeNode(",", true));
        }
        return node;
    }

    static TreeNode parseSUBFUNCS() {
        TreeNode node = new TreeNode("SUBFUNCS", false);
        node.addChild(parseFUNCTIONS());
        return node;
    }

    static boolean isVtyp() {
        return tokens.get(index).equals("num") || tokens.get(index).equals("text");
    }

    static boolean isVname() {
        return tokens.get(index).matches("V_[a-zA-Z0-9_]*");
    }

    static boolean isFname() {
        return tokens.get(index).matches("F_[a-zA-Z_][a-zA-Z0-9_]*");
    }

    static boolean isConst() {
        return isNumber() || isStringConst(); 
    }

    static boolean isNumber() {
        return tokens.get(index).matches("-?\\d+(\\.\\d+)?");
    }

    static boolean isStringConst() {
        return tokens.get(index).matches("\"[A-Za-z ]+\"");
    }

    static boolean isCommand() {
        return tokens.get(index).equals("skip") || tokens.get(index).equals("halt") || tokens.get(index).equals("print") || tokens.get(index).equals("return") || isVname() || isFname() || tokens.get(index).equals("if");
    }

    static boolean isFtyp() {
        return index < tokens.size() && (tokens.get(index).equals("num") || tokens.get(index).equals("void"));
    }

    static boolean isUnop() {
        return tokens.get(index).equals("not") || tokens.get(index).equals("sqrt");
    }

    static boolean isBinop() {
        return tokens.get(index).equals("or") || tokens.get(index).equals("and") || tokens.get(index).equals("eq") || tokens.get(index).equals("grt") || tokens.get(index).equals("add") || tokens.get(index).equals("sub") || tokens.get(index).equals("mul") || tokens.get(index).equals("div");
    }

    static boolean isAtomicAhead() {
        int tempindex = index;
        return tokens.get(tempindex + 2).matches("V_[a-zA-Z0-9_]*") || tokens.get(tempindex + 2).matches("\"[A-Za-z ]+\"") || tokens.get(tempindex + 2).matches("-?\\d+(\\.\\d+)?"); 
    }

    static void saveTreeToXML(TreeNode tree, String filePath) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element rootElement = doc.createElement("SYNTREE");
            doc.appendChild(rootElement);

            tree.buildXML(doc, rootElement, true);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));

            transformer.transform(source, result);

            System.out.println("Syntax tree saved to " + filePath);

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    public void parse() {
        String inputFilePath = "source_code.txt";
        String parsedInputFilePath = "parsedCode.txt";
    
        StringBuilder input = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                input.append(line.trim()).append(" ");
            }
        } catch (IOException e) {
            System.err.println("Error reading the input file: " + e.getMessage());

        }
    
        String inputString = input.toString().trim();
    
        // Add spaces around specific symbols using regular expressions
        inputString = inputString
            .replaceAll(",", " , ")
            .replaceAll("\\(", " ( ")
            .replaceAll("\\)", " ) ")
            .replaceAll(";", " ; ");
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(parsedInputFilePath))) {
            writer.write(inputString.trim());
        } catch (IOException e) {
            System.err.println("Error writing the parsed input to file: " + e.getMessage());
            System.exit(1);
            return;
        }
    
        tokens = tokenize(inputString);
    
        TreeNode syntaxTree = parsePROG();
        index = 0;
    
        System.out.println("Parsing phase has passed! Syntax tree saved to 'syntax_tree.xml'");
    
        saveTreeToXML(syntaxTree, "syntax_tree.xml");
    }
    
}
