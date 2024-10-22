import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ICG{

    class Node {
        String symb;
        String unid;
        List<Node> children;

        Node(String symb, String unid) {
            this.symb = symb;
            this.unid = unid;
            this.children = new ArrayList<>();
        }

        void addChild(Node child) {
            this.children.add(child);
        }

        void printNode(String indent) {
            System.out.println(indent + "Symbol: " + symb + ", UNID: " + unid);
            for (Node child : children) {
                child.printNode(indent + "  ");
            }
        }
    }

    private List<String> intermediateCode;
    private int labelCount = 0;
    private int varCount = 0;


    public ICG(){
        this.intermediateCode = new ArrayList<>();
    }
    
    //Traverse SyntaxTree to find the respective translation required
    //IF ARG then do the ARG translation - GET THE NODE AND ITS CHILDREN
    private void translate(Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        String var = node.symb;
        switch (var) {
            // case "GLOBVARS" -> {
            // }
            case "ALGO" -> translateALGO(node, symbolTable, functionSymbolTable);
            // case "FUNCTIONS" -> translateFUNCS(node, symbolTable, functionSymbolTable);   
        }

        for(Node child : node.children){
            translate(child, symbolTable, functionSymbolTable);
        }
    }

    private String translateALGO(Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        intermediateCode.add("begin");
        Node child = node.children.get(1);
        // String s = transINSTRUC(node, symbolTable, functionSymbolTable);
        intermediateCode.add(transINSTRUC(child, symbolTable, functionSymbolTable));
        intermediateCode.add("end");

        return intermediateCode.toString();
    }

    private String transINSTRUC(Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        if(node == null){
            return "";
        }

        if(node.children.size() == 0){
            // intermediateCode.add("REM END");
            return "REM END";
        }

        String code1 = translateCOMMAND(node.children.get(0), symbolTable, functionSymbolTable);
        String code2 = "";
        if (node.children.size() > 1) {
            code2 = transINSTRUC(node.children.get(1), symbolTable, functionSymbolTable);
        }

        // intermediateCode.add(code1+";"+code2);
        return code1+";"+code2;
    }

    private String translateCOMMAND(Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        System.out.println("COMMAND: "+node.symb);
        Node child = node.children.get(0);
        String childCOND = child.symb;

        if (childCOND.equals("skip")) {
            // intermediateCode.add("REM DO NOTHING");
            return "REM DO NOTHING";
        }
        if (childCOND.equals("halt")) {
            // intermediateCode.add("STOP");
            return "STOP";
        }
        if(childCOND.equals("print")){
            String atomic = translateATOMIC(child, symbolTable, functionSymbolTable);
            // intermediateCode.add("PRINT "+atomic);
            return "PRINT "+atomic;
        }
        if(childCOND.equals("ASSIGN")){
            String code = translateASSIGN(child, symbolTable, functionSymbolTable);
            // intermediateCode.add(code);
            return code;
        }

        //Section 5.b
        if(childCOND.equals("return")){
            
        }

        return "";
    }

    private String translateATOMIC(Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        String atomic = node.children.get(0).symb;
        if(atomic.equals("num")){
            return node.children.get(0).children.get(0).symb;
        }
        if(atomic.equals("id")){
            return symbolTable.lookup(node.children.get(0).children.get(0).symb);
        }
        if(atomic.equals("VNAME")){
            return symbolTable.lookup(node.children.get(0).children.get(0).symb);
        }
        return "";

    }

    private String translateASSIGN(Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        String name = node.children.get(0).symb;

        //Creating the expression
        String expr = "";
        for(int i = 0; i < node.children.size(); i++){
            expr += node.children.get(i).symb;
        }


        if(expr.contains("<")){
            Node child = node.children.get(0);
            String vname = translateVNAME(child, symbolTable, functionSymbolTable);
            // intermediateCode.add("INPUT "+vname);
            return ("INPUT "+vname); 
        }

        if(expr.contains("=")){
            // place = newvar()
            // x = lookup(vtable, getname(id))
            // TransExp(Exp, vtable, ftable, place)++[x := place]
            String place = newVar();
            String x = symbolTable.lookup(node.children.get(0).symb);
            String codeString = transEXPR(expr, node, symbolTable, functionSymbolTable, place);

            // intermediateCode.add(codeString+" ["+x+" := "+place+"]");
            return (codeString+" ["+x+" := "+place+"]");
        }
        return "";
    }

    private String translateVNAME(Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        String vname = node.children.get(0).symb;
        return symbolTable.lookup(vname);
    }

    //TABLES
    private String transCOND(String COND, String labelT, String labelF, Node node ,ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        //SET A TRUE FALSE FLAG SO THAT IN RECURSIVE CALLS THE EXPR COND WILL BE TRUE
        if(COND.equals("EXPR")){
            String t = newVar();
            String exp = "";
            String code1 = transEXPR(exp, node, symbolTable, functionSymbolTable, t);
            return (code1+ "IF "+(Integer.parseInt(t)!=0)+" GOTO "+labelT+"\nGOTO "+labelF);
        }
        if(COND.equals("not")){
            String t = newVar();
            String code1 = transEXPR(COND, node, symbolTable, functionSymbolTable, t);
            boolean tEquals = (Integer.parseInt(t)!=0);

            intermediateCode.add(code1+"[IF"+(Integer.parseInt(t)!=0)+"THEN"+labelT+"ELSE"+labelF+" ]");
            return code1+"[IF"+tEquals+"THEN"+labelT+"ELSE"+labelF+" ]";
        }
        if(COND.equals("and")){
            String arg2 = newlabel();
            String code1 = transCOND("and", arg2, labelF, node, symbolTable, functionSymbolTable);
            String code2 = transCOND("and", labelT, labelF, node, symbolTable, functionSymbolTable);

            intermediateCode.add(code1+"[IF"+arg2+"THEN"+labelT+"ELSE"+labelF+" ]"+code2);
            return code1+"[IF"+arg2+"THEN"+labelT+"ELSE"+labelF+" ]"+code2;
        }
        if(COND.equals("or")){
            String COND1 = node.children.get(0).symb;
            String COND2 = node.children.get(1).symb;
            String arg2 = newlabel();
            String code1 = transCOND(COND1, labelT, arg2, node, symbolTable, functionSymbolTable);
            String code2 = transCOND(COND2, labelT, labelF, node, symbolTable, functionSymbolTable);
            intermediateCode.add(code1+"[LABEL "+arg2+"]"+code2);
        }
        return "";
    }

    private String transEXPR(String expr, Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable, String place){
        if(expr.contains("num")){
            //  v = getvalue(num)
            // [place := v] 
            String v = symbolTable.lookup(node.children.get(0).symb);
            intermediateCode.add("["+place+" := "+v+"]");
            return "["+place+" := "+v+"]";
        }
        if(expr.contains("id") || expr.contains("VNAME")){
            // x = lookup(vtable, getname(id))
            // [place := x]
            System.out.println("EXPR : "+expr);
            Node child = node.children.get(0);
            System.out.println("IN transEXPR: "+child.children.get(0).symb);
            String x = functionSymbolTable.lookup(child.children.get(0).symb);
            // intermediateCode.add("["+place+" := "+x+"]");
            return "["+place+" := "+x+"]";
        }
        if(expr.contains("UNOP")){
            // place1 = newvar()
            // code1 = TransExp(Exp1, vtable, ftable, place1)
            // op = transop(getopname(unop))
            // code1++[place := op place1]
            String place1 = newVar();
            String code1 = transEXPR(expr, node, symbolTable, functionSymbolTable, place1);
            // String op = "";
            String op = transOP("UNOP", node, symbolTable, functionSymbolTable);
            intermediateCode.add(code1+"["+place+" := "+op+" "+place1+"]");
            return code1+"["+place+" := "+op+" "+place1+"]";
        }
        if(expr.contains("BINOP")){
            // place1 = newvar()
            // place2 = newvar()
            // code1 = TransExp(Exp1, vtable, ftable, place1)
            // code2 = TransExp(Exp2, vtable, ftable, place2)
            // op = transop(getopname(binop))
            // code1++code2++[place := place1 op place2]
            String expr1 = "";
            String expr2 = "";
            String place1 = newVar();
            String place2 = newVar();
            String code1 = transEXPR(expr1, node, symbolTable, functionSymbolTable, place1);
            String code2 = transEXPR(expr2, node, symbolTable, functionSymbolTable, place2);
            String op = transOP("BINOP",node, symbolTable, functionSymbolTable);
            // intermediateCode.add(code1+code2+"["+place+" := "+place1+" "+op+" "+place2+"]");
        }
        return "";
    }

    private String transOP(String op, Node node, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        if(op.equals("UNOP")){
            
        }
        if(op.equals("BINOP")){
            
        }
        return "";
    }



    private String newlabel(){
        // labelCount++;
        return "L"+labelCount++;
    }
    
    private String newVar(){
        // varCount++;
        return "V"+varCount++;
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

    private void printCode(){
        for(String code : intermediateCode){
            System.out.println(code);
        }
    }

    private void writeToFile(){
        //Write the intermediate code to a file
        try {
            File file = new File("intermediate_code.txt");
            FileWriter writer = new FileWriter(file);
            for(String code : intermediateCode){
                writer.write(code+"\n");
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Error writing to file"+e);
        }
    }

    public void translateProg(Node root, ScopeAnalyzer.SymbolTable symbolTable, ScopeAnalyzer.FunctionSymbolTable functionSymbolTable){
        intermediateCode.add("main");
        translate(root, symbolTable, functionSymbolTable);
        intermediateCode.add("STOP");
    }

    public static void main(String [] args){
        ScopeAnalyzer scope = new ScopeAnalyzer();
        ScopeAnalyzer.Node rootScope = scope.parseSyntaxTree("syntax_tree.xml");
        scope.depthFirstTraversal(rootScope);
        scope.checkFuncCall();
        scope.printAllTables();

        // ICG code = new ICG();
        // Node root = code.parseSyntaxTree("syntax_tree.xml");
        // code.translateProg(root, scope.getSymbolTable(), scope.getFunctionTable());
        // code.printCode();
        // code.writeToFile();
    }
}