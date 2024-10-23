import org.w3c.dom.*;                
import javax.xml.parsers.*;           
import java.io.File;
import java.util.ArrayList;           
import java.util.List;              
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    public LinkedHashMap<String, String> functionCalls = new LinkedHashMap<>();
    public boolean nextEndIsAfterSubFunc = false;
    public boolean endSubFunc = false;
    public int endCounter = 0;
    public boolean scopeSuccess = true;

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
            symbolTable = new LinkedHashMap<>();
        }

        void addEntry(String entryType, String name, String varOrFuncType, Scope scope) {
            SymbolEntry entry = new SymbolEntry(entryType, name, varOrFuncType, scope);
            symbolTable.put(name, entry);
        }

        void printSymbolTable() {
            System.out.println("\nVtable Table Contents:");
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
            private Map<String, FunctionSymbolEntry> functionTable = new LinkedHashMap<>();

            FunctionSymbolTable(){
                String[] parameters = {"none"}; 
                addFunction("main", parameters, "void", mainScope);
            }
            
            void addFunction(String functionName, String[] parameters,String type, Scope scope) {
                String functionID = UUID.randomUUID().toString();  // Generate unique identifier for the function
                FunctionSymbolEntry entry = new FunctionSymbolEntry(functionID, functionName, type, parameters, scope);
                functionTable.put(functionID, entry);
            }
    
            void printFunctionTable() {
                System.out.println("\nFtable Symbol Table Contents:");
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
            nextEndIsAfterSubFunc = true;
            Node funcNode = node.children.get(0);
            boolean hasSubFunction = false;
            if(funcNode.children.size() != 0){
                hasSubFunction = true;
            }
            if (hasSubFunction) {
                endSubFunc = false;
                isSubFunction = true;  
            } else {
                endSubFunc = true;
                isSubFunction = false;
            }
        }

        if (node.symb.equals("end")) {
            if(node.parent.symb.equals("BODY")){
                if(currentScope != mainScope){
                    currentScope = currentScope.parentScope;
                }
            }
        }

        if (node.symb.equals("VTYP")) {
            if (!node.children.isEmpty()) {
                currVarType = node.children.get(0).symb;
            }
        }

        if (node.symb.equals("FTYP")) {
            if (!node.children.isEmpty()) {
                funcType = node.children.get(0).symb;
            }
        }

        if (node.symb.equals("VNAME")) {
            if (!node.children.isEmpty()) {
                currVarName = node.children.get(0).symb;

                if(declarationCounter >= 3){
                    isFuncParameters = false;
                    declarationCounter = 0;
                }

                if(isFuncParameters){
                    boolean isDuplicate = false;

                    String varKey = currVarName + "@" + currentScope.scopeName + "@" + node.unid;

                    for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                        if (entry.name.equals(varKey) && entry.scope == currentScope) {
                            
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (isDuplicate) {
                        if(scopeSuccess){
                            System.err.println("Error: Variable '" + currVarName + "' has already been declared in the current scope.");
                            scopeSuccess = false;
                        }
                        return;
                    } else {
                        symbolTable.addEntry("VNAME", varKey, "num", currentScope);
                    }
                    declarationCounter++;
                }else if (!currVarType.isEmpty()) {
                    boolean isDuplicate = false;

                    String varKey = currVarName + "@" + currentScope.scopeName + "@" + node.unid;


                    for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                        if (entry.name.equals(varKey) && entry.scope == currentScope) {
                            
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (isDuplicate) {
                        if(scopeSuccess){
                            System.err.println("Error: Variable '" + currVarName + "' has already been declared in the current scope.");
                            scopeSuccess = false;
                        }
                        return;
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
                            String entryKey = entry.name.split("@")[0] + "@" + entry.scope.scopeName; 
                    
                            if (entryKey.equals(varKey) && entry.entryType.equals("VNAME") && entry.scope == scopeToCheck) {
                                isDeclared = true;
                                break;
                            }
                        }
                    
                        if (isDeclared) break;  
                        scopeToCheck = scopeToCheck.parentScope;  
                    }
                    

                    if (!isDeclared) {
                        if(scopeSuccess){
                            System.err.println("Error: Variable '" + currVarName + "' has not been declared in the current or ancestor scopes.");
                            scopeSuccess = false;
                        }
                        return;
                        
                    }

                    currVarName = "";
                }
            }
        }

        if (node.symb.equals("FNAME")) {

            String functionName = node.children.get(0).symb;

            if (isFuncCall) {
                functionCalls.put(functionName, currentScope.scopeName);
                isFuncCall = false;
            } else {

                isFuncParameters = true;             

                    if (functionName.equals(currentScope.scopeName)) {       
                        if(scopeSuccess){
                            System.err.println("Error: Function '" + functionName + "' cannot have the same name as its parent scope.");
                            scopeSuccess = false;
                        }
                        //System.exit(1);
                        return;
                    }else{
                        boolean siblingConflict = false;
    
                        for (FunctionSymbolEntry entry : functionTable.functionTable.values()) {
                            if (entry.functionName.equals(functionName) && entry.scope.parentScope.scopeName == currentScope.scopeName) {
                                siblingConflict = true;
                                break;
                            }
                        } 
                        if(siblingConflict){
                            if(scopeSuccess){
                                System.err.println("Error: Function " + functionName +  " cannot have the same name as a sibling in the scope.");
                                scopeSuccess = false;
                            }
                            //System.exit(1);
                            return;
                        }else{
                            Scope newFunctionScope = new Scope(functionName, currentScope);
                            currentScope.addChildScope(newFunctionScope);
                            
                            currentScope = newFunctionScope;
                            isSubFunction = false;  

                            String[] parameters = {"(num","num","num)"}; 
                            functionTable.addFunction(functionName, parameters, funcType, currentScope);
                        
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
                        if(scopeSuccess){
                            System.err.println("Error: Can't recurse main.");
                            scopeSuccess = false;
                        }
                        //System.exit(1);
                        return;
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
                        if(scopeSuccess){
                            System.err.println("Error: Function '" + functionName + "' not found in immediate scope or child scope of '" + callingScopeName + "'.");
                            scopeSuccess = false;
                        }
                        return;
                    }
                }
            } else {
                if(scopeSuccess){
                    System.err.println("Error: Scope '" + callingScopeName + "' not found.");
                    scopeSuccess = false;
                }
                return;
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
        currentNode.parent = parent;  
        
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
            
            for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                String entryKey = entry.name.split("@")[0] + "@" + entry.scope.scopeName;  
                if (entryKey.equals(varKey) && entry.entryType.equals("VNAME")) {
                    return true; 
                }
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
            
            for (SymbolEntry entry : symbolTable.symbolTable.values()) {
                String entryKey = entry.name.split("@")[0] + "@" + entry.scope.scopeName;  
    
                if (entryKey.equals(varKey) && entry.entryType.equals("VNAME")) {
                    return entry.varOrFuncType;  
                }
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
        for (FunctionSymbolEntry entry : functionTable.functionTable.values()) {
            if (entry.functionName.equals(functionName)) {
                return entry.scope;
            }
        }

        if(scopeSuccess){
            System.err.println("Error: Function '" + functionName + "' not found in the function table.");
            scopeSuccess = false;
        }
        //System.exit(1);
        return null;
    }
    
    
    

    public void scopeAndTypeCheck() {
        String filePath = "syntax_tree.xml";
        
        Node root = this.parseSyntaxTree(filePath); 
    
        this.depthFirstTraversal(root);  
        if (scopeSuccess == true) {
            this.checkFuncCall(); 
            this.printAllTables(); 
            System.out.println("\nScope analysis phase has Passed, now moving to Type checking...");
    
            TypeChecker typerChecker = new TypeChecker(this);
            if (typerChecker.depthFirstTraversal(root)) {
                System.out.println("\nProgram Correctly typed");
                System.out.println("\nType checking phase has Passed!"); 
            } else {
                System.out.println("\nProgram not correctly typed");
            }
        }
    }
    
}



