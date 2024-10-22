import org.w3c.dom.*;                
import javax.xml.parsers.*;           
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;           
import java.util.List;              
import java.util.HashMap;             
import java.util.Map;                 
import java.util.UUID;

class ScopeAnalyzer {

    class Scope {
        String scopeName;
        Scope parentScope;
        List<Scope> childScopes;  

        Scope(String scopeName, Scope parentScope) {
            this.scopeName = scopeName;
            this.parentScope = parentScope;
            this.childScopes = new ArrayList<>(); 
        }

        // Method to add a child scope
        void addChildScope(Scope childScope) {
            this.childScopes.add(childScope);
        }
    }

    public Scope mainScope = new Scope("main", null); 
    public Scope currentScope = mainScope;  
    public String currVarType = "";
    public String currVarName = "";
    public String funcType = "";
    public boolean isFuncCall = false;
    public boolean isSubFunction = false;
    public boolean isFuncParameters = false;
    public int declarationCounter = 0;
    public Map<String, String> functionCalls = new HashMap<>();

    class SymbolEntry {
        String entryType;
        String name;
        String varOrFuncType; 
        Scope scope; 

        SymbolEntry(String entryType, String name, String varOrFuncType, Scope scope) {
            this.entryType = entryType;
            this.name = name;
            this.varOrFuncType = varOrFuncType;
            this.scope = scope;
        }
    }

    class SymbolTable {
        private Map<String, SymbolEntry> symbolTable;

        SymbolTable() {
            symbolTable = new HashMap<>();
        }

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

    private SymbolTable symbolTable = new SymbolTable(); 

        class FunctionSymbolEntry {
            String functionID;
            String functionName;
            String[] parameters;  
            String type;
            Scope scope;
    
            FunctionSymbolEntry(String functionID, String functionName,String type, String[] parameters, Scope scope) {
                this.functionID = functionID;
                this.functionName = functionName;
                this.type = type;
                this.parameters = parameters;
                this.scope = scope;
            }
        }

        class FunctionSymbolTable {
            private Map<String, FunctionSymbolEntry> functionTable = new HashMap<>();
    
            void addFunction(String functionName, String[] parameters,String type, Scope scope) {
                String functionID = UUID.randomUUID().toString();  // Generate unique identifier for the function
                FunctionSymbolEntry entry = new FunctionSymbolEntry(functionID, functionName, type, parameters, scope);
                functionTable.put(functionID, entry);
                System.out.println("Added to Function Symbol Table: " + functionName + " with ID: " + functionID);
            }
    
            void printFunctionTable() {
                System.out.println("\nFunction Symbol Table Contents:");
                for (Map.Entry<String, FunctionSymbolEntry> entry : functionTable.entrySet()) {
                    FunctionSymbolEntry function = entry.getValue();
                    System.out.println("Function ID: " + function.functionID + ",Return Type:" + function.type + ", Name: " + function.functionName + ", Scope: " + function.scope.scopeName);
                    System.out.print("Parameters: ");
                    for (String param : function.parameters) {
                        System.out.print(param + " ");
                    }
                    System.out.println();
                }
            }
        }


    private FunctionSymbolTable functionTable = new FunctionSymbolTable(); 

    public void depthFirstTraversal(Node node) {
        if (node == null) return;

        if (node.symb.equals("CALL")) {
            isFuncCall = true; 
        }

        if (node.symb.equals("SUBFUNCS")) {
            isSubFunction = true;  
        }



        if (node.symb.equals("VTYP")) {
            if (!node.children.isEmpty()) {
                currVarType = node.children.get(0).symb;
                System.out.println("Variable Type found: " + currVarType);
            }
        }

        if (node.symb.equals("FTYP")) {
            if (!node.children.isEmpty()) {
                funcType = node.children.get(0).symb;
                System.out.println("Function Type found: " + funcType);
            }
        }

        if (node.symb.equals("VNAME")) {
            if (!node.children.isEmpty()) {
                currVarName = node.children.get(0).symb;
                System.out.println("Variable Name found: " + currVarName);

                if(declarationCounter >= 3){
                    isFuncParameters = false;
                    declarationCounter = 0;
                }

                if(isFuncParameters){
                    boolean isDuplicate = false;

                    String varKey = currVarName + "@" + currentScope.scopeName;

                    for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                        if (entry.name.equals(varKey) && entry.scope == currentScope) {
                            
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (isDuplicate) {
                        System.err.println("Error: Variable '" + currVarName + "' has already been declared in the current scope.");
                    } else {
                        symbolTable.addEntry("VNAME", varKey, "num", currentScope);
                    }
                    declarationCounter++;
                }else if (!currVarType.isEmpty()) {
                    boolean isDuplicate = false;

                    String varKey = currVarName + "@" + currentScope.scopeName;


                    for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                        if (entry.name.equals(varKey) && entry.scope == currentScope) {
                            
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (isDuplicate) {
                        System.err.println("Error: Variable '" + currVarName + "' has already been declared in the current scope.");
                    } else {
                        symbolTable.addEntry("VNAME", varKey, currVarType, currentScope);
                    }
                    currVarType = "";
                    currVarName = "";

                } else {
                    boolean isDeclared = false;
                    Scope scopeToCheck = currentScope;

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
                functionCalls.put(functionName, currentScope.scopeName);
                isFuncCall = false;
            } else {
                System.out.println("Function declaration detected: " + functionName);
                isFuncParameters = true;
                if (functionName.equals(currentScope.scopeName)) {
                    System.err.println("Error: Function '" + functionName + "' cannot have the same name as its parent scope.");
                } else {
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
                        if (!isSubFunction) {
                            currentScope = mainScope;
                            Scope newFunctionScope = new Scope(functionName, currentScope);
                            currentScope.addChildScope(newFunctionScope);
                            System.out.println("New function scope opened: " + functionName);
                            currentScope = newFunctionScope;

                            String[] parameters = {"(num","num","num)"}; 
                            functionTable.addFunction(functionName, parameters, funcType, currentScope);

                        }else{
                            Scope newFunctionScope = new Scope(functionName, currentScope);
                            currentScope.addChildScope(newFunctionScope);
                            System.out.println("New function scope opened: " + functionName);
                            currentScope = newFunctionScope;
                            isSubFunction = false;

                            String[] parameters = {"(num","num","num)"}; 
                            functionTable.addFunction(functionName, parameters, funcType, currentScope);
                        }
                    }
                }
            }
        }
        for (Node child : node.children) {
            depthFirstTraversal(child);
        }
    }

    public void checkFuncCall() {
        for (Map.Entry<String, String> funcCallEntry : functionCalls.entrySet()) {
            String functionName = funcCallEntry.getKey();
            String callingScopeName = funcCallEntry.getValue();

            Scope callingScope = findScopeByName(callingScopeName, mainScope);
    
            if (callingScope != null) {
                boolean isChildScope = false;
                if (callingScope.scopeName.equals(functionName)) {
                    if (functionName.equals("main")) {
                        System.err.println("Error: Can't recurse main.");
                    } else {
                        System.out.println("Recursion detected in function: " + functionName);
                    }
                } else {
                    for (Scope childScope : callingScope.childScopes) {
                        if (childScope.scopeName.equals(functionName)) {
                            isChildScope = true;
                            break;
                        }
                    }
    
                    if (isChildScope) {
                        System.out.println("Function call '" + functionName + "' is in an immediate child scope of '" + callingScopeName + "'.");
                    } else {
                        System.err.println("Error: Function '" + functionName + "' not found in immediate scope or child scope of '" + callingScopeName + "'.");
                    }
                }
            } else {
                System.err.println("Error: Scope '" + callingScopeName + "' not found.");
            }
        }
    }

    private Scope findScopeByName(String scopeName, Scope currentScope) {
        if (currentScope.scopeName.equals(scopeName)) {
            return currentScope;
        }
    
        for (Scope childScope : currentScope.childScopes) {
            Scope foundScope = findScopeByName(scopeName, childScope);
            if (foundScope != null) {
                return foundScope;
            }
        }
    
        return null;
    }

    public void printAllTables() {
        symbolTable.printSymbolTable();
        functionTable.printFunctionTable();
    }
    
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
        String unid = getTagValue("UNID", element);
        String symb = getTagValue("SYMB", element);

        Node currentNode = new Node(symb, unid);

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

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    public void printTree(Node root) {
        if (root != null) {
            root.printNode("");
        }
    }

    public boolean isVariableDeclared(String varName, Scope currentScope) {
        Scope scopeToCheck = currentScope;
        while (scopeToCheck != null) {
            String varKey = varName + "@" + scopeToCheck.scopeName;
            if (symbolTable.symbolTable.containsKey(varKey)) {
                return true;  
            }
            scopeToCheck = scopeToCheck.parentScope;  
        }
        return false;  
    }

    public boolean isFunctionDeclared(String functionName, Scope currentScope) {
        Scope scopeToCheck = currentScope;
        while (scopeToCheck != null) {
            for (FunctionSymbolEntry entry : functionTable.functionTable.values()) {
                if (entry.functionName.equals(functionName)) {
                    return true;  
                }
            }
            scopeToCheck = scopeToCheck.parentScope;  
        }
        return false;  
    }

    public String getVariableType(String varName, Scope currentScope) {
        Scope scopeToCheck = currentScope;
        while (scopeToCheck != null) {
            String varKey = varName + "@" + scopeToCheck.scopeName;
            if (symbolTable.symbolTable.containsKey(varKey)) {
                return symbolTable.symbolTable.get(varKey).varOrFuncType;  
            }
            scopeToCheck = scopeToCheck.parentScope;  
        }
        return null;  
    }

    public String getFunctionReturnType(String functionName, Scope currentScope) {
        Scope scopeToCheck = currentScope;
        while (scopeToCheck != null) {
            for (FunctionSymbolEntry entry : functionTable.functionTable.values()) {
                if (entry.functionName.equals(functionName)) {
                    return entry.type;  
                }
            }
            scopeToCheck = scopeToCheck.parentScope;  
        }
        return null;  
    }

    public Scope getScopeForFunction(String functionName) {
        // Iterate through the function table to find the function by name
        for (FunctionSymbolEntry entry : functionTable.functionTable.values()) {
            if (entry.functionName.equals(functionName)) {
                return entry.scope;  // Return the scope associated with the function
            }
        }
        
        // If no function with the given name is found, return null
        System.err.println("Error: Function '" + functionName + "' not found in the function table.");
        return null;
    }
    
    
    

    public static void main(String[] args) {
        ScopeAnalyzer analyzer = new ScopeAnalyzer();
        String filePath = "syntax_tree.xml";
        
        Node root = analyzer.parseSyntaxTree(filePath);
        analyzer.printTree(root);

        analyzer.depthFirstTraversal(root);
        
        analyzer.checkFuncCall();
        analyzer.printAllTables();

        TypeChecker typerChecker = new TypeChecker(analyzer);
        typerChecker.depthFirstTraversal(root);
        
    }
}



