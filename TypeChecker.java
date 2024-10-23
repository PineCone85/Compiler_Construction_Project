public class TypeChecker {

    private ScopeAnalyzer scopeAnalyzer;
    private ScopeAnalyzer.Scope currentScope; 
    private boolean isCall = false;

    public TypeChecker(ScopeAnalyzer scopeAnalyzer) {
        this.scopeAnalyzer = scopeAnalyzer;
        this.currentScope = scopeAnalyzer.mainScope;  
    }

    public void enterScope(ScopeAnalyzer.Scope newScope) {
        currentScope = newScope;
    }

    public void exitScope() {
        if (currentScope.parentScope != null) {
            currentScope = currentScope.parentScope;
        }
    }

    public boolean typecheckPROG(Node progNode) {
        return typecheckGLOBVARS(progNode.children.get(1)) && 
               typecheckALGO(progNode.children.get(2)) &&
               typecheckFUNCTIONS(progNode.children.get(3));
    }

    public boolean typecheckGLOBVARS(Node globVarsNode) {
        if (globVarsNode.children.isEmpty()) {
            return true;
        }
    
        Node vtypNode = globVarsNode.children.get(0);  
        Node vnameNode = globVarsNode.children.get(1);  
        String expectedType = typecheckVTYP(vtypNode);  
        String varName = vnameNode.children.get(0).symb;   
        String actualType = scopeAnalyzer.getVariableType(varName, currentScope);
    
        if (!expectedType.equals(actualType)) {
            System.err.println("Type Error: Variable '" + varName + "' expected to be of type '" + expectedType + "', but found type '" + actualType + "'.");
            return false;
        }
    
        return typecheckGLOBVARS(globVarsNode.children.get(2));
    }

    private String typecheckVTYP(Node vtypNode) {
        if (vtypNode.children.get(0).symb.equals("num")) {
            return "num";  
        } else if (vtypNode.children.get(0).symb.equals("text")) {
            return "text";  
        }
        return "u";
    }

    public String typecheckVNAME(Node vnameNode) {
        String varName = vnameNode.children.get(0).symb;
        String varType = scopeAnalyzer.getVariableType(varName, currentScope);
    
        if (varType == null) {
            System.err.println("Type Error: Variable '" + varName + "' is not declared in the current or ancestor scopes.");
            return "u"; 
        }
    
        return varType;
    }

    public boolean typecheckALGO(Node algoNode) {
        Node instrucNode = algoNode.children.get(1);  
        return typecheckINSTRUC(instrucNode);
    }

    public boolean typecheckINSTRUC(Node instrucNode) {
        if (instrucNode.children.isEmpty()) {
            return true;
        }
        Node nextInstrucNode;
        Node commandNode = instrucNode.children.get(0);  
        if(instrucNode.children.size() <= 2){
            nextInstrucNode = instrucNode.children.get(1);
        }else{
            nextInstrucNode = instrucNode.children.get(2);
        }
        return typecheckCOMMAND(commandNode) && typecheckINSTRUC(nextInstrucNode);
    }

    public boolean typecheckCOMMAND(Node commandNode) {
        switch (commandNode.children.get(0).symb) {
            case "skip":
            case "halt":
                return true;  
    
            case "print":
                return typecheckPRINT(commandNode.children.get(1));
    
            case "return":
                return typecheckRETURN(commandNode.children.get(1));
    
            case "ASSIGN":
                return typecheckASSIGN(commandNode.children.get(0));
    
            case "CALL":
                String callReturnType = typecheckCALL(commandNode.children.get(0));
                if (callReturnType.equals("void")) {
                    return true;  
                } else {
                    System.err.println("Type Error: Function call must return 'void', but got type '" + callReturnType + "'.");
                    return false;
                }
    
            case "BRANCH":
                return typecheckBRANCH(commandNode.children.get(0));
    
            default:
                System.err.println("Error: Unrecognized command '" + commandNode.symb + "'.");
                return false;
        }
    }

    public boolean typecheckPRINT(Node atmoicNode) {
        Node atomicNode = atmoicNode; 
        String atomicType = typecheckATOMIC(atomicNode);
    
        if (atomicType.equals("num") || atomicType.equals("text")) {
            return true;  
        } else {
            System.err.println("Type Error: print expects a numeric or text value.");
            return false;
        }
    }

    public boolean typecheckRETURN(Node returnNode) {
        Node atomicNode = returnNode; 
        String functionName = currentScope.scopeName;
        String CurrentFuncReturnType = scopeAnalyzer.getFunctionReturnType(functionName,currentScope);
        String atomicType = typecheckATOMIC(atomicNode);

        if(atomicType.equals(CurrentFuncReturnType)){
            return true;
        }else{
            return false;
        }
    }

    public String typecheckCALL(Node callNode) {
        isCall = true;
        Node fnameNode = callNode.children.get(0);
        Node atomicNode1 = callNode.children.get(2);
        Node atomicNode2 = callNode.children.get(4);
        Node atomicNode3 = callNode.children.get(6);

        String atomic1Type = typecheckATOMIC(atomicNode1);
        String atomic2Type = typecheckATOMIC(atomicNode2);
        String atomic3Type = typecheckATOMIC(atomicNode3);

        if( atomic1Type.equals("num") && atomic3Type.equals("num") && atomic2Type.equals("num")){
            return typecheckFNAME(fnameNode);
        }else{
            return "u";
        }
    }

    public String typecheckATOMIC(Node atomicNode) {
        Node childNode = atomicNode.children.get(0);  
        if (childNode.symb.equals("VNAME")) {
            return typecheckVNAME(childNode);  
        } else if (childNode.symb.equals("CONST")) {
            return typecheckCONST(childNode);  
        } else {
            System.err.println("Type Error: Unrecognized ATOMIC type.");
            return "u";  
        }
    }

    public String typecheckCONST(Node constNode) {
        String numberPattern = "0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*(\\.[0-9]*[1-9])?";
        String textPattern = "\"[A-Z][a-z]{0,7}\"";

        String constType = constNode.children.get(0).symb;
       
        if (constType.matches(numberPattern)) {
            return "num";
        } else if (constType.matches(textPattern)) {
            return "text"; 
        } else {
            System.err.println("Type Error: Unrecognized constant type.");
            return "u";
        }
    }

    public boolean typecheckASSIGN(Node assignNode) {
        Node vnameNode = assignNode.children.get(0);  
        String vnameType = typecheckVNAME(vnameNode);  
    
        if (assignNode.children.size() == 3 && assignNode.children.get(1).symb.equals("<") && assignNode.children.get(2).symb.equals("input")) {
            if (vnameType.equals("n")) {
                return true;  
            } else {
                System.err.println("Type Error: Only numeric input is allowed, but '" + vnameNode.symb + "' is of type '" + vnameType + "'.");
                return false;
            }
        }
    
        if (assignNode.children.size() == 3 && assignNode.children.get(1).symb.equals("=")) {
            Node termNode = assignNode.children.get(2);
            String termType = typecheckTERM(termNode);  
       
            if (vnameType.equals(termType)) {
                return true;  
            } else {
                System.err.println("Type Error: Variable '" + vnameNode.children.get(0).symb + "' is of type '" + vnameType +
                                   "', but assigned a value of type '" + termType + "'.");
                return false;
            }
        }
    
        System.err.println("Type Error: Invalid assignment syntax.");
        return false;
    }

    public String typecheckTERM(Node termNode) {
        Node childNode = termNode.children.get(0);
        switch (childNode.symb) {
            case "ATOMIC":
                return typecheckATOMIC(childNode);  
            case "CALL":
                return typecheckCALL(childNode);    
            case "OP":
                return typecheckOP(childNode);      
            default:
                System.err.println("Type Error: Unrecognized TERM type '" + childNode.symb + "'.");
                return "u";  
        }
    }

    public String typecheckOP(Node opNode) {
        Node operatorNode = opNode.children.get(0);  
        if (operatorNode.symb.equals("UNOP")) {
            Node argNode = opNode.children.get(2);  
    
            String unopType = typecheckUNOP(operatorNode);
            String argType = typecheckARG(argNode);
    
            if (unopType.equals("b") && argType.equals("b")) {
                return "b";  
            } else if (unopType.equals("n") && argType.equals("n")) {
                return "n";  
            } else {
                return "u";  
            }
        } else if (operatorNode.symb.equals("BINOP")) {
            Node arg1Node = opNode.children.get(2);  
            Node arg2Node = opNode.children.get(4);  
            String binopType = typecheckBINOP(operatorNode);
            String arg1Type = typecheckARG(arg1Node);
            String arg2Type = typecheckARG(arg2Node);
    
            if (binopType.equals("b") && arg1Type.equals("num") && arg2Type.equals("num")) {
                return "b";  
            } else if (binopType.equals("n") && arg1Type.equals("num") && arg2Type.equals("num")) {
                return "num";  
            } else if (binopType.equals("c") && arg1Type.equals("num") && arg2Type.equals("num")) {
                return "b";  
            } else {
                return "u";  
            }
        } else {
            return "u";  
        }
    }

    public String typecheckARG(Node argNode) {
        Node childNode = argNode.children.get(0); 
        switch (childNode.symb) {
            case "ATOMIC":
                return typecheckATOMIC(childNode); 
            case "OP":
                return typecheckOP(childNode);
            default:
                System.err.println("Type Error: Unrecognized ARG type '" + childNode.symb + "'.");
                return "u"; 
        }
    }

    public String typecheckUNOP(Node unopNode) {
        switch (unopNode.symb) {
            case "not":
                return "b";  
            case "sqrt":
                return "n";  
            default:
                return "u";  
        }
    }
    
    public String typecheckBINOP(Node binopNode) {
        switch (binopNode.children.get(0).symb) {
            case "or":
            case "and":
                return "b";  
            case "eq":
            case "grt":
                return "c";  
            case "add":
            case "sub":
            case "mul":
            case "div":
                return "n";  
            default:
                return "u";  
        }
    }

    public boolean typecheckBRANCH(Node branchNode) {
        Node condNode = branchNode.children.get(1); 
        Node algo1Node = branchNode.children.get(3); 
        Node algo2Node = branchNode.children.get(5); 

        if (typecheckCOND(condNode).equals("b")) {
            return typecheckALGO(algo1Node) && typecheckALGO(algo2Node);
        } else {
            System.err.println("Type Error: Condition in BRANCH must be of type 'b' (boolean).");
            return false;  
        }
    }

    public String typecheckCOND(Node condNode) {
        Node childNode = condNode.children.get(0);
        switch (childNode.symb) {
            case "SIMPLE":
                return typecheckSIMPLE(childNode);  
            case "COMPOSIT":
                return typecheckCOMPOSIT(childNode);  
            default:
                System.err.println("Type Error: Unrecognized COND type '" + childNode.symb + "'.");
                return "u";  
        }
    }

    public String typecheckSIMPLE(Node simpleNode) {
        Node binopNode = simpleNode.children.get(0);  
        Node atomic1Node = simpleNode.children.get(2); 
        Node atomic2Node = simpleNode.children.get(4); 

        String binopType = typecheckBINOP(binopNode);
        String atomic1Type = typecheckATOMIC(atomic1Node);
        String atomic2Type = typecheckATOMIC(atomic2Node);
        if (binopType.equals("b") && atomic1Type.equals("b") && atomic2Type.equals("b")) {
            return "b";  
        } else if (binopType.equals("c") && atomic1Type.equals("num") && atomic2Type.equals("num")) {
            return "b";  
        } else {
            System.err.println("Type Error: Mismatch between BINOP and ATOMIC types in SIMPLE.");
            return "u";  
        }
    }

    public String typecheckCOMPOSIT(Node compositNode) {
        Node firstChild = compositNode.children.get(0);  
    
        if (firstChild.symb.equals("BINOP")) {
            Node binopNode = compositNode.children.get(0);  
            Node simple1Node = compositNode.children.get(2); 
            Node simple2Node = compositNode.children.get(4); 
            String binopType = typecheckBINOP(binopNode);
            String simple1Type = typecheckSIMPLE(simple1Node);
            String simple2Type = typecheckSIMPLE(simple2Node);
    
            if (binopType.equals("b") && simple1Type.equals("b") && simple2Type.equals("b")) {
                return "b";  
            } else {
                System.err.println("Type Error: Mismatch between BINOP and SIMPLE types in COMPOSIT.");
                return "u";  
            }
    
        } else if (firstChild.symb.equals("UNOP")) {
            Node unopNode = compositNode.children.get(0);   
            Node simpleNode = compositNode.children.get(2); 
            String unopType = typecheckUNOP(unopNode);
            String simpleType = typecheckSIMPLE(simpleNode);
    
            if (unopType.equals("b") && simpleType.equals("b")) {
                return "b";  
            } else {
                System.err.println("Type Error: Mismatch between UNOP and SIMPLE types in COMPOSIT.");
                return "u";  
            }
    
        } else {
            System.err.println("Type Error: Unrecognized COMPOSIT structure.");
            return "u";  
        }
    }

    public String typecheckFNAME(Node fnameNode) {
        String functionName = fnameNode.children.get(0).symb;
        if(!isCall){
            enterScope(scopeAnalyzer.getScopeForFunction(functionName));
        }
        isCall = false;
        String returnType = scopeAnalyzer.getFunctionReturnType(functionName, currentScope);
        if (returnType != null) {
            return returnType;  
        } else {
            System.err.println("Type Error: Function '" + functionName + "' not found in the symbol table.");
            return "u";  
        }
    }

    public boolean typecheckFUNCTIONS(Node functionsNode) {
        if (functionsNode.children.isEmpty()) {
            return true;  
        }
        
        Node declNode = functionsNode.children.get(0); 
        Node functions2Node = functionsNode.children.get(1);  
    
        return typecheckDECL(declNode) && typecheckFUNCTIONS(functions2Node);
    }
    
    public boolean typecheckDECL(Node declNode) {
        Node headerNode = declNode.children.get(0);  
        Node bodyNode = declNode.children.get(1);    

        return typecheckHEADER(headerNode) && typecheckBODY(bodyNode);
    }
    
    public boolean typecheckHEADER(Node headerNode) {
        Node ftypNode = headerNode.children.get(0);   
        Node fnameNode = headerNode.children.get(1);  
        Node vname1Node = headerNode.children.get(3); 
        Node vname2Node = headerNode.children.get(5); 
        Node vname3Node = headerNode.children.get(7); 
    
        String ftypType = typecheckFTYP(ftypNode);
        String functionName = fnameNode.children.get(0).symb;
        String symbolTableReturnType = typecheckFNAME(fnameNode);
    
        if (!ftypType.equals(symbolTableReturnType)) {
            System.err.println("Type Error: Function '" + functionName + "' has a mismatched return type.");
            return false;
        }
 
        String vname1Type = scopeAnalyzer.getVariableType(vname1Node.children.get(0).symb, currentScope);
        String vname2Type = scopeAnalyzer.getVariableType(vname2Node.children.get(0).symb, currentScope);
        String vname3Type = scopeAnalyzer.getVariableType(vname3Node.children.get(0).symb, currentScope);
    
        if (vname1Type.equals("num") && vname2Type.equals("num") && vname3Type.equals("num")) {
            return true;  
        } else {
            System.err.println("Type Error: Function '" + functionName + "' has non-numeric arguments.");
            return false;
        }
    }

    public String typecheckFTYP(Node ftypNode) {
        switch (ftypNode.children.get(0).symb) {
            case "num":
                return "num";  
            case "void":
                return "void";  
            default:
                System.err.println("Type Error: Unrecognized FTYP '" + ftypNode.children.get(0).symb + "'.");
                return "u";
        }
    }

    public boolean typecheckBODY(Node bodyNode) {
        Node prologNode = bodyNode.children.get(0);   
        Node locvarsNode = bodyNode.children.get(1);  
        Node algoNode = bodyNode.children.get(2);     
        Node epilogNode = bodyNode.children.get(3);   
        Node subfuncsNode = bodyNode.children.get(4); 
    
        return typecheckPROLOG(prologNode) &&
               typecheckLOCVARS(locvarsNode) &&
               typecheckALGO(algoNode) &&
               typecheckEPILOG(epilogNode) &&
               typecheckSUBFUNCS(subfuncsNode);
    }

    public boolean typecheckPROLOG(Node prologNode) {
        return true;
    }

    public boolean typecheckEPILOG(Node epilogNode) {
        return true;  
    }

    public boolean typecheckLOCVARS(Node locvarsNode) {
        Node vtyp1Node = locvarsNode.children.get(0);  
        Node vname1Node = locvarsNode.children.get(1); 
        Node vtyp2Node = locvarsNode.children.get(3);  
        Node vname2Node = locvarsNode.children.get(4); 
        Node vtyp3Node = locvarsNode.children.get(6);  
        Node vname3Node = locvarsNode.children.get(7); 
    
        String vtyp1Type = typecheckVTYP(vtyp1Node);
        String vname1Type = scopeAnalyzer.getVariableType(vname1Node.children.get(0).symb, currentScope);
        if (!vtyp1Type.equals(vname1Type)) {
            System.err.println("Type Error: Variable '" + vname1Node.children.get(0).symb + "' does not match its declared type.");
            return false;
        }
    
        String vtyp2Type = typecheckVTYP(vtyp2Node);
        String vname2Type = scopeAnalyzer.getVariableType(vname2Node.children.get(0).symb, currentScope);
        if (!vtyp2Type.equals(vname2Type)) {
            System.err.println("Type Error: Variable '" + vname2Node.children.get(0).symb + "' does not match its declared type.");
            return false;
        }
    
        String vtyp3Type = typecheckVTYP(vtyp3Node);
        String vname3Type = scopeAnalyzer.getVariableType(vname3Node.children.get(0).symb, currentScope);
        if (!vtyp3Type.equals(vname3Type)) {
            System.err.println("Type Error: Variable '" + vname3Node.children.get(0).symb + "' does not match its declared type.");
            return false;
        }
    
        return true;  
    }
    
    public boolean typecheckSUBFUNCS(Node subfuncsNode) {
        return typecheckFUNCTIONS(subfuncsNode.children.get(0));  
    }
    
    public void depthFirstTraversal(Node currentNode) {
       if(typecheckPROG(currentNode)){
        System.out.println("Program is correctly typed");
       }else{
        System.out.println("Program is not correctly typed");
        System.exit(1);
       } 
    }
    
}
