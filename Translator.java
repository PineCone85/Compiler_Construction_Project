import java.util.HashMap;

public class Translator {
    private ScopeAnalyzer scopeAnalyzer;
    public int count = 0;
    public int count1 = 0;

    
    public Translator(ScopeAnalyzer symbolTable){
        this.scopeAnalyzer = symbolTable;
    }

    public String newvar(){
        count++;
        return "Var" + count;
    }

    public String newlabel(){
        count1++;
        return "Label" + count1;
    }
    
    // Translation for PROG
    public String translatePROG(Node Prog) {
        Node AlgoNode = Prog.children.get(2);
        String aCode = translateALGO(AlgoNode);
        return aCode + " STOP\r\n ";
    }


    public String translateALGO(Node AlgoNode) {
        Node IntrucNode = AlgoNode.children.get(1);
        return translateINSTRUC(IntrucNode);
    }

    // Translation for INSTRUC
    public String translateINSTRUC1(Node instrucNode) {
        return " REM END\r\n";
    }

    // Translation for INSTRUC1 ::= COMMAND ; INSTRUC2
    public String translateINSTRUC(Node instrucNode) {
        String code2 = "";
        if(instrucNode.children.size() == 0){
            return translateINSTRUC1(instrucNode);
        }
        Node CommandNode = instrucNode.children.get(0);
        String code1 = translateCOMMAND(CommandNode);
        if(instrucNode.children.size() == 3){
            Node instruc1Node = instrucNode.children.get(2);
            code2 += translateINSTRUC(instruc1Node);
        }else{
            Node instruc1Node = instrucNode.children.get(1);
            code2 += translateINSTRUC1(instruc1Node);
        }
        return code1 + code2;
    }


    // Translation for COMMAND
    public String translateCOMMAND(Node CommandNode) {
        Node key = CommandNode.children.get(0);
        switch (key.symb) {
            case "skip":
                return "\r\nREM DO NOTHING ";

            case "halt":
                return "\r\nSTOP ";

            case "print":
                Node AtomicNode = CommandNode.children.get(1);
                String codeString = translateATOMIC(AtomicNode);
                return "\r\nPrint" +  " " + codeString;
            case "ASSIGN":
                return translateASSIGN(key);
            case "CALL":
                return translateCALL(key);
            case "BRANCH":
                return translateBRANCH(key);
            default:
                break;
        }
        return "";
    }

    public String translateATOMIC(Node AtomicNode){
        Node child = AtomicNode.children.get(0);
        if(child.symb.equals("VNAME")){
            return translateVNAME(child.children.get(0));
        }else{
            return translateCONST(child);
        }
    }

    public String translateATOMIC1(Node AtomicNode, String place){
        Node child = AtomicNode.children.get(0);
        if(child.symb.equals("VNAME")){
            return "(" + place + ":=" + translateVNAME(child.children.get(0)) + ")\r\n";
        }else{
            return "(" + place + ":=" + translateCONST(child) + ")\r\n";
        }
    }
    

    public String translateCONST(Node constNode){
        String constant = constNode.children.get(0).symb;
        return constant;
    }

    public String translateASSIGN(Node AssignNode){
        Node vnameNode = AssignNode.children.get(0);
        if(AssignNode.children.size() == 2){
            String codeString = translateVNAME(vnameNode.children.get(0));
            return "INPUT" + " " + codeString;
        }else{
            Node TermNode = AssignNode.children.get(2);
            String place = newvar();
            String x = scopeAnalyzer.getVar(vnameNode.children.get(0).symb,scopeAnalyzer.mainScope);
            return translateTERM(TermNode,place) + "[" + x +"= " + place + "]\r\n";
        }   
    }

    public String translateTERM(Node TermNode, String place){
        Node term = TermNode.children.get(0);
        switch (term.symb) {
            case "ATOMIC":
                return translateATOMIC1(term,place);
            case "CALL":
                return translateCALL(term);
            case "OP":
                return translateOP(term,place);
            default:
                break;
        }
        return "";
    }

    public String translateCALL(Node callNode){
        Node fnameNode = callNode.children.get(0);
        Node atomic1Node = callNode.children.get(2);
        Node atomic2Node = callNode.children.get(4);
        Node atomic3Node = callNode.children.get(6);

        String fname = scopeAnalyzer.getFunc(fnameNode.symb, scopeAnalyzer.mainScope);

        return "CALL_" + fname + "(" + translateATOMIC(atomic1Node) + translateATOMIC(atomic2Node) + translateATOMIC(atomic3Node) + ")\r\n";
    }

    public String translateOP(Node opNode, String place){
        Node op = opNode.children.get(0);
        switch (op.symb) {
            case "UNOP":
                String place1 = newvar();
                String code1 = translateARG(opNode.children.get(2),place1);
                String operator = translateUNOP(op.children.get(0));
                return code1 + place + " := " + operator + "(" + place1 + ")\r\n";
            case "BINOP":
                String place2 = newvar();
                String place3 = newvar();
                String code2 = translateARG(opNode.children.get(2),place2);
                String code3 = translateARG(opNode.children.get(4),place3);
                String operator1 = translateBINOP(op.children.get(0));
                return code2 + code3 + "(" + place + " := " + place2 +" "+ operator1 +" "+ place3 + ")\r\n";
            default:
                break;
        }
        return "";
    }

    public String translateARG(Node argNode, String place){
        Node child = argNode.children.get(0);
        if(child.symb.equals("ATOMIC")){
            return translateATOMIC1(child,place);
        }else{
            return translateOP(child,place);
        }
    }

    public String translateUNOP(Node op){
        switch (op.symb) {
            case "sqrt":
                return "SQR";
            case "not":
                
        }
        return "";
    }

    public String translateVNAME(Node vnameNode){
        return scopeAnalyzer.getVar(vnameNode.symb,scopeAnalyzer.mainScope);
    }

    public String translateBINOP(Node binopNode){
        switch (binopNode.symb) {
            case "eq":
                return "=";
            case "grt":
                return ">";
            case "add":
                return "+";
            case "sub":
                return "-";
            case "mul":
                return "*";
            case "div":
                return "/";
            default:
                break;
        }
        return "";
    }

    public String translateBRANCH(Node branchNode){
        Node condNode = branchNode.children.get(1);
        Node algo1Node = branchNode.children.get(3);
        Node algo2Node = branchNode.children.get(5);
        String label1 = newlabel();
        String label2 = newlabel();
        String label3 = newlabel();

        String code1 = translateCOND(condNode,label1,label2);
        String code2 = translateALGO(algo1Node);
        String code3 = translateALGO(algo2Node);

        return code1 + "(LABEL " + label1 + " ) " + code2 + " (GOTO " + label3 + ", LABEL " + label2 + " ) " + code3 + " (LABEL " + label3 + ")\r\n";  

    }

    public String translateCOND(Node condNode, String label1, String label2){
        Node cond = condNode.children.get(0);

        if(cond.symb.equals("SIMPLE")){
            return translateSIMPLE(cond,label1,label2);
        }else{
            return translateCOMPOSIT(cond,label1,label2);
        }

    }

    public String translateSIMPLE(Node simpleNode, String label1, String label2){
        Node binopNode = simpleNode.children.get(0);
        Node atomic1Node = simpleNode.children.get(2);
        Node atomic2Node = simpleNode.children.get(4);

        String t1 = newvar();
        String t2 = newvar();
        String code1 = translateATOMIC1(atomic1Node, t1);
        String code2 = translateATOMIC1(atomic2Node, t2);
        String operator = translateBINOP(binopNode.children.get(0));

        return code1+code2+"( IF " + t1 +" "+ operator +" "+ t2 + " THEN " + label1 + " ELSE " + label2 + " )\r\n";
        
    }

    public String translateCOMPOSIT(Node composNode, String label1, String label2 ){
        Node opNode = composNode.children.get(0);
        if(opNode.symb.equals("UNOP")){
            Node SimpleNode1 = composNode.children.get(2);
            return translateSIMPLE(SimpleNode1, label2, label1);
        }else{
            Node SimpleNode1 = composNode.children.get(2);
            Node SimpleNode2 = composNode.children.get(4);
            if(opNode.children.get(0).equals("and")){
                String arg2 = newlabel();
                String code1 = translateSIMPLE(SimpleNode1, arg2, label2);
                String code2 = translateSIMPLE(SimpleNode2, label1, label2);
                return code1 + "( LABEL " + arg2 + ") " + code2; 
            }else{
                String arg2 = newlabel();
                String code1 = translateSIMPLE(SimpleNode1, label1, arg2);
                String code2 = translateSIMPLE(SimpleNode2, label1, label2);
                return code1 + "( LABEL " + arg2 + ") " + code2; 
            }
        }
    }
}
