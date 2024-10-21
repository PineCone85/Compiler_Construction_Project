import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class Parser {

    // Tokens from the input
    private static List<String> tokens;
    private static int index = 0;

    // A class representing the syntax tree node
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

        // Method to create an XML structure, marking terminals as leaf nodes
        void buildXML(Document doc, Element parentElement, boolean isRoot) {
            Element nodeElement;

            if (isRoot) {
                // For the root node, create a <ROOT> element
                nodeElement = doc.createElement("ROOT");
            } else {
                // For non-root nodes, create a <NODE> element
                nodeElement = doc.createElement("NODE");
            }

            // Add PARENT UID
            if (parentElement != null && !isRoot) {
                Element parentIdElement = doc.createElement("PARENT");
                parentIdElement.appendChild(doc.createTextNode(String.valueOf(getParentUID(parentElement))));
                nodeElement.appendChild(parentIdElement);
            }

            // Add UNID and SYMB for each node
            Element unidElement = doc.createElement("UNID");
            unidElement.appendChild(doc.createTextNode(String.valueOf(unid)));
            nodeElement.appendChild(unidElement);

            Element symbElement = doc.createElement("SYMB");
            symbElement.appendChild(doc.createTextNode(value));
            nodeElement.appendChild(symbElement);

            // If the node is a terminal, mark it as a leaf node
            if (isTerminal) {
                Element terminalElement = doc.createElement("TERMINAL");
                terminalElement.appendChild(doc.createTextNode(value));
                nodeElement.appendChild(terminalElement);
            } else {
                // Add children nodes if it's a non-terminal
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

            // Attach this node to the parent element
            parentElement.appendChild(nodeElement);

            // Recursively build the XML structure for each child
            for (TreeNode child : children) {
                child.buildXML(doc, nodeElement, false);
            }
        }

        // Method to get the parent UID from the parent element
        private int getParentUID(Element parentElement) {
            // The UID is stored in the first child element named "UNID"
            for (int i = 0; i < parentElement.getChildNodes().getLength(); i++) {
                if (parentElement.getChildNodes().item(i).getNodeName().equals("UNID")) {
                    return Integer.parseInt(parentElement.getChildNodes().item(i).getTextContent());
                }
            }
            return -1; // Return -1 if no parent UID is found
        }
    }
    // Method to tokenize the input string
    static List<String> tokenize(String input) {
        return Arrays.asList(input.split("\\s+"));
    }

    // Modify match to check for bounds
    static void match(String expected) {
        if (index < tokens.size() && tokens.get(index).equals(expected)) {
            index++;  // Increment index after a successful match
        } else {
            throw new RuntimeException("Expected " + expected + " but found " + 
                (index < tokens.size() ? tokens.get(index) : "end of input"));
        }
    }


    // PROG -> main GLOBVARS ALGO FUNCTIONS
    static TreeNode parsePROG() {
        TreeNode node = new TreeNode("PROG", false);
        match("main");
        node.addChild(new TreeNode("main", true)); // Add 'main' as a terminal node
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
            match(","); // Match and consume ','
            node.addChild(new TreeNode(",", true)); // Add ',' as a terminal node
            node.addChild(parseGLOBVARS());
        }
        return node;
    }



    static TreeNode parseVTYP() {
        TreeNode node = new TreeNode("VTYP", false);  // Create a non-terminal node for VTYP
        if (tokens.get(index).equals("num")) {
            match("num");  // Match 'num'
            node.addChild(new TreeNode("num", true));  // Add 'num' as a terminal node under VTYP
        } else if (tokens.get(index).equals("text")) {
            match("text");  // Match 'text'
            node.addChild(new TreeNode("text", true));  // Add 'text' as a terminal node under VTYP
        }
        return node;  // Return the VTYP node with either 'num' or 'text' as its child
    }

    // VNAME -> V
    static TreeNode parseVNAME() {
        TreeNode node = new TreeNode("VNAME", false);
        if (isVname()) {
            String vname = tokens.get(index);  // get the current token as a variable name
            match(vname);  // match the variable name
            node.addChild(new TreeNode(vname, true)); // Add terminal node with actual variable name
        } else {
            throw new RuntimeException("Expected variable name but found " + tokens.get(index));
        }
        return node;
    }
    // ALGO -> begin INSTRUC end
    static TreeNode parseALGO() {
        TreeNode node = new TreeNode("ALGO", false);
        match("begin");
        node.addChild(new TreeNode("begin", true)); // Add 'begin' as a terminal node
        node.addChild(parseINSTRUC());
        match("end");
        node.addChild(new TreeNode("end", true)); // Add 'end' as a terminal node
        return node;
    }

    static TreeNode parseINSTRUC() {
        TreeNode node = new TreeNode("INSTRUC", false);
    
        // If we hit "end" or have no tokens left, return an empty INSTRUC node
        if (index >= tokens.size() || tokens.get(index).equals("end")) {
            return node;  // Return empty node, indicating nullable INSTRUC
        }
    
        // Otherwise, parse instructions as usual
        if (isCommand()) {  // Ensure that the next token is a valid command
            node.addChild(parseCOMMAND());
            match(";");  // Match and consume ';'
            node.addChild(new TreeNode(";", true));  // Add ';' as a terminal node
    
            // Recursively check for more instructions until we reach "end"
            if (index < tokens.size() && !tokens.get(index).equals("end")) {
                node.addChild(parseINSTRUC());
            }
        }
        return node;
    }
    


    // COMMAND -> skip | halt | print ATOMIC | ASSIGN | CALL | BRANCH | return ATOMIC
    static TreeNode parseCOMMAND() {
        TreeNode node = new TreeNode("COMMAND", false);
        switch (tokens.get(index)) {
            case "skip":
                match("skip");
                node.addChild(new TreeNode("skip", true)); // Terminal node
                break;
            case "halt":
                match("halt");
                node.addChild(new TreeNode("halt", true)); // Terminal node
                break;
            case "print":
                match("print");
                node.addChild(new TreeNode("print", true)); // Terminal node
                node.addChild(parseATOMIC());
                break;
            case "return":
                match("return");
                node.addChild(new TreeNode("return", true)); // Terminal node
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

    // ATOMIC -> VNAME | CONST
    static TreeNode parseATOMIC() {
        TreeNode node = new TreeNode("ATOMIC", false);
        if (isVname()) {
            node.addChild(parseVNAME());
        } else {
            node.addChild(parseCONST());
        }
        return node;
    }

    // CONST -> N | T
    static TreeNode parseCONST() {
        TreeNode node = new TreeNode("CONST", false);
        if (isNumber()) {
            String num = tokens.get(index); // Capture the number
            match(num); // Match the number constant
            node.addChild(new TreeNode(num, true)); // Terminal node with the actual number
        } else if (isStringConst()) {
            String str = tokens.get(index); // Capture the string
            match(str); // Match the string constant
            node.addChild(new TreeNode(str, true)); // Terminal node with the actual string
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
            node.addChild(new TreeNode("< input", true)); // Terminal node
        } else {
            match("="); // Match and consume '='
            node.addChild(new TreeNode("=", true)); // Add '=' as a terminal node
            node.addChild(parseTERM()); // Handle the constant in TERM
        }
        return node;
    }
    
    

    // CALL -> FNAME ( ATOMIC , ATOMIC , ATOMIC )
    static TreeNode parseCALL() {
        TreeNode node = new TreeNode("CALL", false);
        node.addChild(parseFNAME());
        match("("); // Match and consume '('
        node.addChild(new TreeNode("(", true)); // Add '(' as a terminal node
        node.addChild(parseATOMIC());
        match(","); // Match and consume ','
        node.addChild(new TreeNode(",", true)); // Add ',' as a terminal node
        node.addChild(parseATOMIC());
        match(","); // Match and consume ','
        node.addChild(new TreeNode(",", true)); // Add ',' as a terminal node
        node.addChild(parseATOMIC());
        match(")"); // Match and consume ')'
        node.addChild(new TreeNode(")", true)); // Add ')' as a terminal node
        return node;
    }

    // BRANCH -> if COND then ALGO else ALGO
    static TreeNode parseBRANCH() {
        TreeNode node = new TreeNode("BRANCH", false);
        match("if");
        node.addChild(parseCOND());
        match("then");
        node.addChild(parseALGO());
        match("else");
        node.addChild(parseALGO());
        return node;
    }

    // TERM -> ATOMIC | CALL | OP
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
    
        // Check if it's a unary operation first (like 'not' or 'sqrt')
        if (isUnop()) {
            node.addChild(parseUNOP());
            match("("); // Match and consume '('
            node.addChild(new TreeNode("(", true));
            node.addChild(parseARG()); // Unary operation only has one argument
            match(")"); // Match and consume ')'
            node.addChild(new TreeNode(")", true));
        }
        // Otherwise, it must be a binary operation (like 'add', 'sub', etc.)
        else if (isBinop()) {
            node.addChild(parseBINOP());
            match("("); // Match and consume '('
            node.addChild(new TreeNode("(", true));
            node.addChild(parseARG()); // First argument
            match(","); // Match and consume ','
            node.addChild(new TreeNode(",", true));
            node.addChild(parseARG()); // Second argument
            match(")"); // Match and consume ')'
            node.addChild(new TreeNode(")", true));
        }
        return node;
    }
    


    // ARG -> ATOMIC | OP
    static TreeNode parseARG() {
        TreeNode node = new TreeNode("ARG", false);
        if (isVname() || isConst()) {
            node.addChild(parseATOMIC());
        } else {
            node.addChild(parseOP());
        }
        return node;
    }

    // COND -> SIMPLE | COMPOSIT
    static TreeNode parseCOND() {
        TreeNode node = new TreeNode("COND", false);
        if (isBinop()) {
            node.addChild(parseSIMPLE());
        } else {
            node.addChild(parseCOMPOSIT());
        }
        return node;
    }

    // SIMPLE -> BINOP ( ATOMIC , ATOMIC )
    static TreeNode parseSIMPLE() {
        TreeNode node = new TreeNode("SIMPLE", false);
        node.addChild(parseBINOP());
        match("("); // Match and consume '('
        node.addChild(new TreeNode("(", true)); // Add '(' as a terminal node
        node.addChild(parseATOMIC());
        match(","); // Match and consume ','
        node.addChild(new TreeNode(",", true)); // Add ',' as a terminal node
        node.addChild(parseATOMIC());
        match(")"); // Match and consume ')'
        node.addChild(new TreeNode(")", true)); // Add ')' as a terminal node
        return node;
    }


    static TreeNode parseCOMPOSIT() {
        TreeNode node = new TreeNode("COMPOSIT", false);
    
        if (isUnop()) {
            // Handle unary operation (e.g., not, sqrt)
            node.addChild(parseUNOP());
            match("("); // Match and consume '('
            node.addChild(new TreeNode("(", true)); // Add '(' as a terminal node
            node.addChild(parseSIMPLE());           // COMPOSIT can have SIMPLE as the argument to UNOP
            match(")");
            node.addChild(new TreeNode(")", true)); // Add ')' as a terminal node
        } else if (isBinop()) {
            // Handle binary operation
            node.addChild(parseBINOP());
            match("("); // Match and consume '('
            node.addChild(new TreeNode("(", true)); // Add '(' as a terminal node
            node.addChild(parseSIMPLE());           // First SIMPLE argument
            match(",");                             // Match and consume ','
            node.addChild(new TreeNode(",", true)); // Add ',' as a terminal node
            node.addChild(parseSIMPLE());           // Second SIMPLE argument
            match(")");                             // Match and consume ')'
            node.addChild(new TreeNode(")", true)); // Add ')' as a terminal node
        } else {
            throw new RuntimeException("Expected unary or binary operation but found " + tokens.get(index));
        }
    
        return node;
    }
    

    static TreeNode parseUNOP() {
        TreeNode node = new TreeNode("UNOP", false);
        String op = tokens.get(index); // 'not' or 'sqrt'
        if (op.equals("not") || op.equals("sqrt")) {
            match(op);
            node.addChild(new TreeNode(op, true)); // Terminal node for the operator
        } else {
            throw new RuntimeException("Expected unary operator but found " + op);
        }
        return node;
    }
    
    

    // BINOP -> or | and | eq | grt | add | sub | mul | div
    static TreeNode parseBINOP() {
        TreeNode node = new TreeNode("BINOP", false);
        String op = tokens.get(index);
        match(op);
        node.addChild(new TreeNode(op, true)); // Terminal node
        return node;
    }

    static TreeNode parseFNAME() {
        TreeNode node = new TreeNode("FNAME", false);
        String fname = tokens.get(index);  // get the full function name
        if (isFname()) {
            match(fname);  // match the full function name
            node.addChild(new TreeNode(fname, true)); // Add terminal node with the actual function name
        } else {
            throw new RuntimeException("Expected function name but found " + tokens.get(index));
        }
        return node;
    }
    

    // FUNCTIONS -> '' | DECL FUNCTIONS
    static TreeNode parseFUNCTIONS() {
        TreeNode node = new TreeNode("FUNCTIONS", false);
        if (isFtyp()) {
            node.addChild(parseDECL());
            node.addChild(parseFUNCTIONS());
        }
        return node;
    }

    // DECL -> HEADER BODY
    static TreeNode parseDECL() {
        TreeNode node = new TreeNode("DECL", false);
        node.addChild(parseHEADER());
        node.addChild(parseBODY());
        return node;
    }

    // HEADER -> FTYP FNAME ( VNAME , VNAME , VNAME )
    static TreeNode parseHEADER() {
        TreeNode node = new TreeNode("HEADER", false);
        node.addChild(parseFTYP());
        node.addChild(parseFNAME());
        match("("); // Match and consume '('
        node.addChild(new TreeNode("(", true)); // Add '(' as a terminal node
        node.addChild(parseVNAME());
        match(","); // Match and consume ','
        node.addChild(new TreeNode(",", true)); // Add ',' as a terminal node
        node.addChild(parseVNAME());
        match(","); // Match and consume ','
        node.addChild(new TreeNode(",", true)); // Add ',' as a terminal node
        node.addChild(parseVNAME());
        match(")"); // Match and consume ')'
        node.addChild(new TreeNode(")", true)); // Add ')' as a terminal node
        return node;
    }


    // FTYP -> num | void
    static TreeNode parseFTYP() {
        TreeNode node = new TreeNode("FTYP", false);
        if (tokens.get(index).equals("num")) {
            match("num");
            node.addChild(new TreeNode("num", true)); // Add terminal node
        } else {
            match("void");
            node.addChild(new TreeNode("void", true)); // Add terminal node
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
        
        if (index < tokens.size()) {  // Check if 'end' token is still available
            match("end");
            node.addChild(new TreeNode("end", true)); // Add terminal node
        } else {
            throw new RuntimeException("Expected 'end' but reached end of input.");
        }
    
        return node;
    }
    
    // PROLOG -> {
    static TreeNode parsePROLOG() {
        TreeNode node = new TreeNode("PROLOG", false);
        match("{");
        node.addChild(new TreeNode("{", true)); // Add terminal node
        return node;
    }

    // EPILOG -> }
    static TreeNode parseEPILOG() {
        TreeNode node = new TreeNode("EPILOG", false);
        match("}");
        node.addChild(new TreeNode("}", true)); // Add terminal node
        return node;
    }

    // LOCVARS -> VTYP VNAME , VTYP VNAME , VTYP VNAME ,
    static TreeNode parseLOCVARS() {
        TreeNode node = new TreeNode("LOCVARS", false);
        for (int i = 0; i < 3; i++) {
            node.addChild(parseVTYP());
            node.addChild(parseVNAME());
            match(","); // Match and consume ','
            node.addChild(new TreeNode(",", true)); // Add ',' as a terminal node
        }
        return node;
    }

    // SUBFUNCS -> FUNCTIONS
    static TreeNode parseSUBFUNCS() {
        TreeNode node = new TreeNode("SUBFUNCS", false);
        node.addChild(parseFUNCTIONS());
        return node;
    }

    // Helper functions to check token types
    static boolean isVtyp() {
        return tokens.get(index).equals("num") || tokens.get(index).equals("text");
    }

    static boolean isVname() {
        return tokens.get(index).matches("V_[a-zA-Z0-9_]*"); // Variable names must start with V_
    }
    

    static boolean isFname() {
        return tokens.get(index).matches("F_[a-zA-Z_][a-zA-Z0-9_]*");
    }
    
    
    static boolean isConst() {
        return isNumber() || isStringConst(); 
    }

    // Helper
    static boolean isNumber() {
        return tokens.get(index).matches("\\d+"); // Match numeric constants like 5, 123, etc.
    }

    static boolean isStringConst() {
        return tokens.get(index).matches("\"[A-Za-z ]+\""); // Handle strings in double quotes
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

    // Save the syntax tree to an XML file
    static void saveTreeToXML(TreeNode tree, String filePath) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            // Create the root element
            Element rootElement = doc.createElement("SYNTREE");
            doc.appendChild(rootElement);

            // Build the XML structure from the tree
            tree.buildXML(doc, rootElement, true); // Pass 'true' to indicate root node

            // Write the content to an XML file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));

            // Output to file
            transformer.transform(source, result);

            System.out.println("Syntax tree saved to " + filePath);

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    
    public static void main(String[] args) {
        // Read input from the user
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your input:");
        String input = scanner.nextLine();

        // Tokenize the input string
        tokens = tokenize(input);

        // Parse the tokens and generate the syntax tree
        TreeNode syntaxTree = parsePROG();

        // Save the tree as an XML file
        saveTreeToXML(syntaxTree, "syntax_tree.xml");
    }
    // main num V_variable1 , num V_variable2  , num V_variable3 , begin V_variable4 = not ( 5 ) ; end num F_function1 ( V_variable1  , V_variable2  , V_variable3 ) { num V_variable11 , num V_variable22 , num V_variable33 , begin end } num F_function2 ( V_variable22 , V_variable11 , V_variable33 ) { num V_variable1 , num V_variable2 , num V_variable3 , begin end } end end
    //main num V_variable1 , num V_variable2  , num V_variable3 , begin V_variable4 = and ( 5 , 5 ) ; F_function1 ( 5 , 5 , 900 ) ; end num F_function1 ( V_variable1  , V_variable2  , V_variable3 ) { num V_variable11 , num V_variable22 , num V_variable33 , begin end } num F_function2 ( V_variable22 , V_variable11 , V_variable33 ) { num V_variable1 , num V_variable2 , num V_variable3 , begin end } end end
    //main num V_count , text V_message , begin V_count = 5 ; V_message = "Hello" ; if grt ( V_count , 0 ) then begin print V_message ; V_count = sub ( V_count , 1 ) ; end else begin halt ; end ; F_factorial ( V_count , 1 , V_message ) ; end num F_factorial ( V_n , V_result , V_msg ) { num V_temp , num V_fun , text V_resultmsg , begin if eq ( V_n , 0 ) then begin V_resultmsg = "Factor" ; print V_resultmsg ; print V_result ; return V_result ; end else begin V_temp = mul ( V_n , V_result ) ; V_n = sub ( V_n , 1 ) ; V_fun = F_factorial ( V_n , V_temp , V_msg ) ; return V_fun ; end ; end } end
    // main num V_variable1 , num V_variable2  , num V_variable3 , begin V_variable4 = 5 ; end num F_function1 ( V_variable1  , V_variable2  , V_variable3 ) { num V_variable11 , num V_variable22 , num V_variable33 , begin end } num F_function2 ( V_variable22 , V_variable11 , V_variable33 ) { num V_variable1 , num V_variable2 , num V_variable3 , begin end } end end
}
