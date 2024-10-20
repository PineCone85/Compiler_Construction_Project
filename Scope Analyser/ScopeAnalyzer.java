import org.w3c.dom.*;                 // For working with the XML DOM (Document Object Model)
import javax.xml.parsers.*;           // For the XML parser (DocumentBuilderFactory, DocumentBuilder)
import java.io.File;                  // For handling file input
import java.util.ArrayList;           // For the ArrayList class used in the Node structure
import java.util.List;                // For the List interface used to manage children of nodes
import java.util.HashMap;             // For the HashMap used to store the symbol table
import java.util.Map;                 // For the Map interface used to define the symbol table
import java.util.UUID;

class ScopeAnalyzer {

    class Scope {
        String scopeName;
        Scope parentScope;
        List<Scope> childScopes;  // New: Track child scopes

        Scope(String scopeName, Scope parentScope) {
            this.scopeName = scopeName;
            this.parentScope = parentScope;
            this.childScopes = new ArrayList<>();  // Initialize the list of child scopes
        }

        // Method to add a child scope
        void addChildScope(Scope childScope) {
            this.childScopes.add(childScope);
        }
    }
    private Scope mainScope = new Scope("main", null); 
    private Scope currentScope = mainScope;  // Start with the main scope and no parent
    private String currVarType = "";
    private String currVarName = "";
    private boolean isFuncCall = false;
    private boolean isSubFunction = false;

    // Class to represent a node in the syntax tree
    class Node {
        String symb;
        String unid;
        List<Node> children;

        Node(String symb, String unid) {
            this.symb = symb;
            this.unid = unid;
            this.children = new ArrayList<>();
        }

        // Method to add a child node
        void addChild(Node child) {
            this.children.add(child);
        }

        // For debugging: print the tree structure
        void printNode(String indent) {
            System.out.println(indent + "Symbol: " + symb + ", UNID: " + unid);
            for (Node child : children) {
                child.printNode(indent + "  ");
            }
        }
    }

    // Class to represent an entry in the symbol table
    class SymbolEntry {
        String entryType;
        String name;
        String varOrFuncType; 
        Scope scope;  // Updated to reference the current scope

        SymbolEntry(String entryType, String name, String varOrFuncType, Scope scope) {
            this.entryType = entryType;
            this.name = name;
            this.varOrFuncType = varOrFuncType;
            this.scope = scope;
        }
    }

    // Class to represent the symbol table
    class SymbolTable {
        private Map<String, SymbolEntry> symbolTable;

        SymbolTable() {
            symbolTable = new HashMap<>();
        }

        // Method to add an entry to the symbol table
        void addEntry(String entryType, String name, String varOrFuncType, Scope scope) {
            SymbolEntry entry = new SymbolEntry(entryType, name, varOrFuncType, scope);
            symbolTable.put(name, entry);
            System.out.println("Added to Symbol Table: " + name + ", Type: " + varOrFuncType + ", Scope: " + scope.scopeName);
        }

        void printSymbolTable() {
            System.out.println("\nSymbol Table Contents:");
            for (Map.Entry<String, SymbolEntry> entry : symbolTable.entrySet()) {
                SymbolEntry symbol = entry.getValue();
                System.out.println("Name: " + symbol.name + ", Type: " + symbol.varOrFuncType + ", Scope: " + symbol.scope.scopeName);
            }
        }
    }

    private SymbolTable symbolTable = new SymbolTable(); // SymbolTable instance

        // Class to represent an entry in the function symbol table
        class FunctionSymbolEntry {
            String functionID;
            String functionName;
            String[] parameters;  
            Scope scope;
    
            FunctionSymbolEntry(String functionID, String functionName, String[] parameters, Scope scope) {
                this.functionID = functionID;
                this.functionName = functionName;
                this.parameters = parameters;
                this.scope = scope;
            }
        }
    
        // Class to represent the function symbol table
        class FunctionSymbolTable {
            private Map<String, FunctionSymbolEntry> functionTable = new HashMap<>();
    
            void addFunction(String functionName, String[] parameters, Scope scope) {
                String functionID = UUID.randomUUID().toString();  // Generate unique identifier for the function
                FunctionSymbolEntry entry = new FunctionSymbolEntry(functionID, functionName, parameters, scope);
                functionTable.put(functionID, entry);
                System.out.println("Added to Function Symbol Table: " + functionName + " with ID: " + functionID);
            }
    
            void printFunctionTable() {
                System.out.println("\nFunction Symbol Table Contents:");
                for (Map.Entry<String, FunctionSymbolEntry> entry : functionTable.entrySet()) {
                    FunctionSymbolEntry function = entry.getValue();
                    System.out.println("Function ID: " + function.functionID + ", Name: " + function.functionName + ", Scope: " + function.scope.scopeName);
                    System.out.print("Parameters: ");
                    for (String param : function.parameters) {
                        System.out.print(param + " ");
                    }
                    System.out.println();
                }
            }
        }


    private FunctionSymbolTable functionTable = new FunctionSymbolTable(); // Function symbol table instance

    public void depthFirstTraversal(Node node) {
        if (node == null) return;

        // Check if the node symbol is "CALL" (Function Call)
        if (node.symb.equals("CALL")) {
            isFuncCall = true;  // Set to true when a function call is encountered
        }

        // Check if the node symbol is "SUBFUNCS" (Subfunctions)
        if (node.symb.equals("SUBFUNCS")) {
            isSubFunction = true;  // Set flag indicating that upcoming FNAMEs are subfunctions
        }

        // Check if the node symbol is "VTYP" (Variable Type)
        if (node.symb.equals("VTYP")) {
            if (!node.children.isEmpty()) {
                currVarType = node.children.get(0).symb;
                System.out.println("Variable Type found: " + currVarType);
            }
        }

        // Check if the node symbol is "VNAME" (Variable Name)
        if (node.symb.equals("VNAME")) {
            if (!node.children.isEmpty()) {
                currVarName = node.children.get(0).symb;
                System.out.println("Variable Name found: " + currVarName);

                // If this is a variable declaration
                if (!currVarType.isEmpty()) {
                    boolean isDuplicate = false;

                    // Create a unique key combining the variable name and scope name
                    String varKey = currVarName + "@" + currentScope.scopeName;

                    // Check for duplicate declarations in the same scope
                    for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                        if (entry.name.equals(currVarName) && entry.scope == currentScope) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (isDuplicate) {
                        System.err.println("Error: Variable '" + currVarName + "' has already been declared in the current scope.");
                    } else {
                        symbolTable.addEntry("VNAME", varKey, currVarType, currentScope); // Use varKey for unique entries
                    }
                    currVarType = "";
                    currVarName = "";

                // If this is a variable usage, check if it is declared in the current or ancestor scopes
                } else {
                    boolean isDeclared = false;
                    Scope scopeToCheck = currentScope;

                    // Traverse through the current and parent scopes to find the variable
                    while (scopeToCheck != null) {
                        String varKey = currVarName + "@" + scopeToCheck.scopeName;
                        for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                            if (entry.name.equals(varKey) && entry.entryType.equals("VNAME") && entry.scope == scopeToCheck) {
                                System.out.println("Variable '" + currVarName + "' found in scope: " + scopeToCheck.scopeName);
                                isDeclared = true;
                                break;
                            }
                        }
                        if (isDeclared) break;  
                        scopeToCheck = scopeToCheck.parentScope;  
                    }

                    if (!isDeclared) {
                        System.err.println("Error: Variable '" + currVarName + "' has not been declared in the current or ancestor scopes.");
                    }

                    currVarName = "";
                }
            }
        }

        if (node.symb.equals("FNAME")) {

            String functionName = node.children.get(0).symb;

            if (isFuncCall) {
                System.out.println("Function call detected: " + functionName);

                boolean isChildScope = false;

                // Check if the function is a child of the current scope
                for (Scope childScope : currentScope.childScopes) {
                    if (childScope.scopeName.equals(functionName)) {
                        isChildScope = true;
                        break;
                    }
                }

                if (isChildScope) {
                    System.out.println("Function call refers to immediate child scope: " + functionName);
                } else if (functionName.equals(currentScope.scopeName)) {
                    if (functionName.equals("main")) {
                        System.err.println("Error: Can't recurse main.");
                    } else {
                        System.out.println("Recursion detected: " + functionName);
                    }
                } else {
                    System.err.println("Error: CALL is not referring to function in immediate scope or child scope.");
                }

                isFuncCall = false;  

            } else {
                System.out.println("Function declaration detected: " + functionName);

                if (functionName.equals(currentScope.scopeName)) {
                    System.err.println("Error: Function '" + functionName + "' cannot have the same name as its parent scope.");
                } else {
                    // Check for sibling function name conflicts (same parent scope)
                    boolean siblingConflict = false;
                    for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                        if (entry.entryType.equals("FNAME") && entry.name.equals(functionName) && entry.scope == currentScope.parentScope) {
                            siblingConflict = true;
                            break;
                        }
                    }
                    if (siblingConflict) {
                        System.err.println("Error: Function '" + functionName + "' cannot have the same name as a sibling scope.");
                    } else {
                        // If this is not a subfunction, reset the current scope to "main"
                        if (!isSubFunction) {
                            currentScope = mainScope;
                            Scope newFunctionScope = new Scope(functionName, currentScope);
                            currentScope.addChildScope(newFunctionScope);
                            System.out.println("New function scope opened: " + functionName);
                            currentScope = newFunctionScope;

                            // Add the function to the function symbol table
                            String[] parameters = {"num","num","num"}; // Modify to retrieve actual parameters
                            functionTable.addFunction(functionName, parameters, currentScope);

                        }else{
                            // Create a new function scope and set it as the current scope
                            Scope newFunctionScope = new Scope(functionName, currentScope);
                            currentScope.addChildScope(newFunctionScope);
                            System.out.println("New function scope opened: " + functionName);
                            currentScope = newFunctionScope;
                            isSubFunction = false;

                            // Add the function to the function symbol table
                            String[] parameters = {"num","num","num"}; // Modify to retrieve actual parameters
                            functionTable.addFunction(functionName, parameters, currentScope);
                        }
                    }
                }
            }
        }

        // Recursively visit each child node
        for (Node child : node.children) {
            depthFirstTraversal(child);
        }
    }

    // Function to print both symbol tables after traversal
    public void printAllTables() {
        symbolTable.printSymbolTable();
        functionTable.printFunctionTable();
    }
    
    

    // Function to parse the XML syntax tree file and build the tree structure
    public Node parseSyntaxTree(String filePath) {
        Node root = null;

        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            Element rootElement = (Element) doc.getElementsByTagName("ROOT").item(0);
            root = parseNode(rootElement, null);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }
    

    private Node parseNode(Element element, Node parent) {
        // Extract UNID and SYMB for the current node
        String unid = getTagValue("UNID", element);
        String symb = getTagValue("SYMB", element);

        // Create a new node object
        Node currentNode = new Node(symb, unid);

        // Process child nodes if the CHILDREN tag exists
        NodeList childrenElements = element.getElementsByTagName("CHILDREN");
        if (childrenElements != null && childrenElements.getLength() > 0) {
            NodeList childIDs = childrenElements.item(0).getChildNodes();
            for (int i = 0; i < childIDs.getLength(); i++) {
                org.w3c.dom.Node childNode = childIDs.item(i);
                if (childNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    String childId = childNode.getTextContent();
                    Element childElement = findElementByUNID(childId, element);
                    if (childElement != null) {
                        Node parsedChild = parseNode(childElement, currentNode);
                        currentNode.addChild(parsedChild);
                    }
                }
            }
        }

        return currentNode;
    }

    // Function to find the child element by its UNID in the XML tree
    private Element findElementByUNID(String unid, Element parent) {
        NodeList nodes = parent.getElementsByTagName("NODE");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element nodeElement = (Element) nodes.item(i);
            if (nodeElement.getElementsByTagName("UNID").item(0).getTextContent().equals(unid)) {
                return nodeElement;
            }
        }
        return null;
    }

    // Utility function to safely get tag value
    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    // Debug function to print the tree
    public void printTree(Node root) {
        if (root != null) {
            root.printNode("");
        }
    }

    // Main function to test the parsing and symbol table generation
    public static void main(String[] args) {
        ScopeAnalyzer analyzer = new ScopeAnalyzer();
        String filePath = "syntax_tree.xml";

        // Parse the syntax tree
        Node root = analyzer.parseSyntaxTree(filePath);

        // Perform a depth-first traversal
        analyzer.depthFirstTraversal(root);
        analyzer.printAllTables();
    }
}

