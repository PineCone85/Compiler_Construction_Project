import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class Parser {

    // Tokens from the input
    private static List<String> tokens;
    private static int index = 0;

    private static final Pattern V_NAMES_PATTERN = Pattern.compile("V_[a-z]([a-z]|[0-9])*");
    private static final Pattern F_NAMES_PATTERN = Pattern.compile("F_[a-z]([a-z]|[0-9])*");
    private static final Pattern TEXT_SNIPPET_PATTERN = Pattern.compile("\"[A-Z][a-z]{0,7}\"");
    private static final Pattern N_NUMBERS_PATTERN = Pattern.compile(
        "0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]"
    );

    // A class representing the syntax tree node
    static class TreeNode {
        static int nodeCounter = 0; // Counter to assign unique IDs to nodes
        int unid; // Unique Node ID
        String value; // The symbol for the node
        List<TreeNode> children; // List of child nodes
        boolean isTerminal; // Marks if the node is a terminal (leaf)

        TreeNode(String value, boolean isTerminal) {
            this.unid = nodeCounter++; // Assign a unique ID to this node
            this.value = value;
            this.isTerminal = isTerminal;
            this.children = new ArrayList<>();
        }

        void addChild(TreeNode child) {
            children.add(child);
        }

        // Method to create an XML structure, marking terminals as leaf nodes
        void buildNonTerminalXML(Document doc, Element parentElement) {
            Element nodeElement = doc.createElement("IN");

            if (parentElement != null) {
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
            
            // If the node is a terminal, mark it as a leaf node
            // if (isTerminal) {
            //     Element terminalElement = doc.createElement("TERMINAL");
            //     terminalElement.appendChild(doc.createTextNode(value));
            //     nodeElement.appendChild(terminalElement);
            // } else {

                
            if (!children.isEmpty()) {
                Element childrenElement = doc.createElement("CHILDREN");
                for (TreeNode child : children) {
                    Element childIdElement = doc.createElement("ID");
                    childIdElement.appendChild(doc.createTextNode(String.valueOf(child.unid)));
                    childrenElement.appendChild(childIdElement);
                }
                nodeElement.appendChild(childrenElement);
            }
            // }

            parentElement.appendChild(nodeElement);

            for (TreeNode child : children) {
                child.buildNonTerminalXML(doc, parentElement);
            }
        }

        void buildRootXML(Document doc, Element pElement) {
            Element rootElement = doc.createElement("ROOT");

            Element uniElement = doc.createElement("UNID");
            uniElement.appendChild(doc.createTextNode(String.valueOf(unid)));
            rootElement.appendChild(uniElement);

            Element SYMBOElement = doc.createElement("SYMB");
            SYMBOElement.appendChild(doc.createTextNode(value));
            rootElement.appendChild(SYMBOElement);

            if(!children.isEmpty()){
                Element childrenElement = doc.createElement("CHILDREN");
                for (TreeNode child : children) {
                    Element childIdElement = doc.createElement("ID");
                    childIdElement.appendChild(doc.createTextNode(String.valueOf(child.unid)));
                    childrenElement.appendChild(childIdElement);
                }
                rootElement.appendChild(childrenElement);
            }
            pElement.appendChild(rootElement);

            Element innerNodes = doc.createElement("INNERNODES");

            Element leafNodes = doc.createElement("LEAFNODES");

            pElement.appendChild(innerNodes);
            pElement.appendChild(leafNodes);

            finalBuildXML(doc, innerNodes , leafNodes);
        }

        void buildLeafNodes(Document doc, Element pElement){
            Element leaf = doc.createElement("LEAF");

            Element parElement = doc.createElement("PARENT");
            parElement.appendChild(doc.createTextNode(String.valueOf(getParentUID(pElement))));

            Element unidElement = doc.createElement("UNID");
            unidElement.appendChild(doc.createTextNode(String.valueOf(unid)));

            Element terminalElement = doc.createElement("TERMINAL");
            terminalElement.appendChild(doc.createTextNode(value));

            leaf.appendChild(parElement);
            leaf.appendChild(unidElement);
            leaf.appendChild(terminalElement);

            pElement.appendChild(leaf);
        }

        void finalBuildXML(Document doc, Element parentElement, Element leafElement){
            if(isTerminal){
                buildLeafNodes(doc, leafElement);
            }
            else{
                buildNonTerminalXML(doc, parentElement);
            }

            for(TreeNode child : children){
                child.finalBuildXML(doc, parentElement, leafElement);
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

        // private void findParent(Document doc) {
        //     NodeList inNodes = doc.getElementsByTagName("IN");

        //     for (int i = 0; i < inNodes.getLength(); i++) {
        //         Element parentElement = (Element) inNodes.item(i);
        //         int parentUID = getParentUID(parentElement);

        //         NodeList children = parentElement.getElementsByTagName("CHILDREN");
        //         if (children.getLength() > 0) {
        //             NodeList childIDs = ((Element) children.item(0)).getElementsByTagName("ID");
        //             for (int j = 0; j < childIDs.getLength(); j++) {
        //                 String childID = childIDs.item(j).getTextContent();
        //                 setParentUID(doc, Integer.parseInt(childID), parentUID);
        //             }
        //         }
        //     }
        // }

        private void findParent(Document doc) {
            NodeList inNodes = doc.getElementsByTagName("IN");

            for (int i = 0; i < inNodes.getLength(); i++) {
                Element parentElement = (Element) inNodes.item(i);
                int parentUID = getParentUID(parentElement);

                // Set parent UID for all child nodes
                NodeList children = parentElement.getElementsByTagName("CHILDREN");
                if (children.getLength() > 0) {
                    NodeList childIDs = ((Element) children.item(0)).getElementsByTagName("ID");
                    for (int j = 0; j < childIDs.getLength(); j++) {
                        String childID = childIDs.item(j).getTextContent();
                        setParentUID(doc, Integer.parseInt(childID), parentUID, false);
                    }
                }

                // Also set parent UID for leaf nodes
                NodeList leafNodes = parentElement.getElementsByTagName("LEAF");
                if (leafNodes.getLength() > 0) {
                    for (int j = 0; j < leafNodes.getLength(); j++) {
                        Element leafElement = (Element) leafNodes.item(j);
                        String leafID = leafElement.getElementsByTagName("ID").item(0).getTextContent();
                        setParentUID(doc, Integer.parseInt(leafID), parentUID, true);
                    }
                }
            }
        }

        private void setParentUID(Document doc, int childUID, int parentUID, boolean isLeaf) {
            NodeList inNodes ;//= doc.getElementsByTagName("IN");
            if(isLeaf == false){
                inNodes = doc.getElementsByTagName("IN");
            }
            else{
                inNodes = doc.getElementsByTagName("LEAF");
            }
            for (int i = 0; i < inNodes.getLength(); i++) {
                Element childElement = (Element) inNodes.item(i);
                int currentUID = Integer.parseInt(childElement.getElementsByTagName("UNID").item(0).getTextContent());
                if (currentUID == childUID) {
                    NodeList parentNodes = childElement.getElementsByTagName("PARENT");
                    if (parentNodes.getLength() > 0) {
                        parentNodes.item(0).setTextContent(String.valueOf(parentUID));
                    } else {
                        Element parentIDElement = doc.createElement("PARENT");
                        parentIDElement.setTextContent(String.valueOf(parentUID));
                        childElement.appendChild(parentIDElement);
                    }
                    break;
                }
            }
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

    // Method to tokenize the input string
    static List<String> tokenize(String input) {
        return Arrays.asList(input.split("\\s+"));
    }

    // Match the current token with an expected token
    static void match(String expected) {
        if (tokens.get(index).equals(expected)) {
            index++;
        } else {
            throw new RuntimeException("Expected " + expected + " but found " + tokens.get(index));
        }
    }

    static void matchRegex(String expected){
        if(tokens.get(index).matches(expected)){
            index++;
        }
        else{
            throw new RuntimeException("Expected " + expected + " but found " + tokens.get(index));
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

    // GLOBVARS -> '' | VTYP VNAME , GLOBVARS
    static TreeNode parseGLOBVARS() {
        TreeNode node = new TreeNode("GLOBVARS", false);
        if (isVtyp()) {
            node.addChild(parseVTYP());
            node.addChild(parseVNAME());
            match(",");
            node.addChild(parseGLOBVARS());
        }
        return node;
    }

    // VTYP -> num | text
    static TreeNode parseVTYP() {
        TreeNode node = new TreeNode("VTYP", false);
        if (tokens.get(index).equals("num")) {
            match("num");
            node.addChild(new TreeNode("num", true)); // Add terminal node
        } else if (tokens.get(index).equals("text")) {
            match("text");
            node.addChild(new TreeNode("text", true)); // Add terminal node
        }
        return node;
    }

    // VNAME -> V
    static TreeNode parseVNAME() {
        TreeNode node = new TreeNode("VNAME", false);
        // match("V");
        matchRegex(V_NAMES_PATTERN.toString());
        // node.addChild(new TreeNode("N", true));
        node.addChild(new TreeNode(tokens.get(index-1), true)); // Add terminal node
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

    // INSTRUC -> '' | COMMAND ; INSTRUC
    static TreeNode parseINSTRUC() {
        TreeNode node = new TreeNode("INSTRUC", false);
        if (isCommand()) {
            node.addChild(parseCOMMAND());
            match(";");
            node.addChild(parseINSTRUC());
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
        // if (tokens.get(index).equals("N")) {
        if(tokens.get(index).matches(N_NUMBERS_PATTERN.toString())){
            // match("N");
            matchRegex(N_NUMBERS_PATTERN.toString());
            // node.addChild(new TreeNode("N", true)); // Terminal node
            node.addChild(new TreeNode(tokens.get(index-1), true)); // Terminal node
        } //else if (tokens.get(index).equals("T")) {
        else if(tokens.get(index).matches(TEXT_SNIPPET_PATTERN.toString())){
            // match("T");
            matchRegex(TEXT_SNIPPET_PATTERN.toString());
            // node.addChild(new TreeNode("T", true)); // Terminal node
            node.addChild(new TreeNode(tokens.get(index-1), true)); // Terminal node
        }
        return node;
    }

    // ASSIGN -> VNAME < input | VNAME = TERM
    static TreeNode parseASSIGN() {
        TreeNode node = new TreeNode("ASSIGN", false);
        node.addChild(parseVNAME());
        if (tokens.get(index).equals("<")) {
            match("<");
            match("input");
            node.addChild(new TreeNode("< input", true)); // Terminal node
        } else {
            match("=");
            node.addChild(parseTERM());
        }
        return node;
    }

    // CALL -> FNAME ( ATOMIC , ATOMIC , ATOMIC )
    static TreeNode parseCALL() {
        TreeNode node = new TreeNode("CALL", false);
        node.addChild(parseFNAME());
        match("(");
        node.addChild(parseATOMIC());
        match(",");
        node.addChild(parseATOMIC());
        match(",");
        node.addChild(parseATOMIC());
        match(")");
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

    // OP -> UNOP ( ARG ) | BINOP ( ARG , ARG )
    static TreeNode parseOP() {
        TreeNode node = new TreeNode("OP", false);
        if (isUnop()) {
            node.addChild(parseUNOP());
            match("(");
            node.addChild(parseARG());
            match(")");
        } else {
            node.addChild(parseBINOP());
            match("(");
            node.addChild(parseARG());
            match(",");
            node.addChild(parseARG());
            match(")");
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
        match("(");
        node.addChild(parseATOMIC());
        match(",");
        node.addChild(parseATOMIC());
        match(")");
        return node;
    }

    // COMPOSIT -> BINOP ( SIMPLE , SIMPLE ) | UNOP ( SIMPLE )
    static TreeNode parseCOMPOSIT() {
        TreeNode node = new TreeNode("COMPOSIT", false);
        if (isUnop()) {
            node.addChild(parseUNOP());
            match("(");
            node.addChild(parseSIMPLE());
            match(")");
        } else {
            node.addChild(parseBINOP());
            match("(");
            node.addChild(parseSIMPLE());
            match(",");
            node.addChild(parseSIMPLE());
            match(")");
        }
        return node;
    }

    // UNOP -> not | sqrt
    static TreeNode parseUNOP() {
        TreeNode node = new TreeNode("UNOP", false);
        if (tokens.get(index).equals("not")) {
            match("not");
            node.addChild(new TreeNode("not", true)); // Terminal node
        } else {
            match("sqrt");
            node.addChild(new TreeNode("sqrt", true)); // Terminal node
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

    // FNAME -> F
    static TreeNode parseFNAME() {
        TreeNode node = new TreeNode("FNAME", false);
        // match("F");
        matchRegex(F_NAMES_PATTERN.toString());
        // node.addChild(new TreeNode("F", true)); // Add terminal node
        node.addChild(new TreeNode(tokens.get(index-1), true)); // Add terminal node
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
        match("(");
        node.addChild(parseVNAME());
        match(",");
        node.addChild(parseVNAME());
        match(",");
        node.addChild(parseVNAME());
        match(")");
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

    // BODY -> PROLOG LOCVARS ALGO EPILOG SUBFUNCS end
    static TreeNode parseBODY() {
        TreeNode node = new TreeNode("BODY", false);
        node.addChild(parsePROLOG());
        node.addChild(parseLOCVARS());
        node.addChild(parseALGO());
        node.addChild(parseEPILOG());
        node.addChild(parseSUBFUNCS());
        match("end");
        node.addChild(new TreeNode("end", true)); // Add terminal node
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
            match(",");
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
        // return tokens.get(index).equals("V");
        return tokens.get(index).matches(V_NAMES_PATTERN.toString());
    }

    static boolean isFname() {
        // return tokens.get(index).equals("F");
        return tokens.get(index).matches(F_NAMES_PATTERN.toString());
    }

    static boolean isConst() {
        // return tokens.get(index).equals("N") || tokens.get(index).equals("T");
        return tokens.get(index).matches(N_NUMBERS_PATTERN.toString()) || tokens.get(index).matches(TEXT_SNIPPET_PATTERN.toString());
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
            // tree.buildXML(doc, rootElement);

            tree.buildRootXML(doc, rootElement);

            // tree.finalBuildXML(doc, rootElement);
            tree.findParent(doc);

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
}
