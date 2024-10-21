import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.*;

public class TypeChecker {

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

    private ScopeAnalyzer scopeAnalyzer;
    private ScopeAnalyzer.Scope currentScope; 

    public TypeChecker(ScopeAnalyzer scopeAnalyzer) {
        this.scopeAnalyzer = scopeAnalyzer;
        this.currentScope = scopeAnalyzer.mainScope;  
    }

    // Method to update the current scope when entering a new one
    public void enterScope(ScopeAnalyzer.Scope newScope) {
        currentScope = newScope;
    }

    // Method to revert to the parent scope when leaving a scope
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
        String varName = vnameNode.symb;             
    
        String actualType = scopeAnalyzer.getVariableType(varName, scopeAnalyzer.mainScope);
    
        if (!expectedType.equals(actualType)) {
            System.err.println("Type Error: Variable '" + varName + "' expected to be of type '" + expectedType + "', but found type '" + actualType + "'.");
            return false;
        }
    
        return typecheckGLOBVARS(globVarsNode.children.get(2));
    }
    

    private String typecheckVTYP(Node vtypNode) {
        if (vtypNode.symb.equals("num")) {
            return "n";  
        } else if (vtypNode.symb.equals("text")) {
            return "t";  
        }
        return "u";
    }

    public String typecheckVNAME(Node vnameNode) {
        String varName = vnameNode.symb;
    
        // Look up the variable in the current scope, moving up the scope chain if necessary
        String varType = scopeAnalyzer.getVariableType(varName, currentScope);
    
        // If the variable is not found, report an error
        if (varType == null) {
            System.err.println("Type Error: Variable '" + varName + "' is not declared in the current or ancestor scopes.");
            return "u";  // Return 'u' for undefined type
        }
    
        // Return the variable's type
        return varType;
    }

    public boolean typecheckALGO(Node algoNode) {
        // ALGO ::= begin INSTRUC end
        Node instrucNode = algoNode.children.get(1);  // The INSTRUC node is the second child (begin, INSTRUC, end)
        
        // Typecheck the instructions
        return typecheckINSTRUC(instrucNode);
    }

    public boolean typecheckINSTRUC(Node instrucNode) {
        // Base case: if INSTRUC is empty, return true
        if (instrucNode.children.isEmpty()) {
            return true;
        }
    
        // Recursive case: INSTRUC1 ::= COMMAND ; INSTRUC2
        Node commandNode = instrucNode.children.get(0);  // First child is COMMAND
        Node nextInstrucNode = instrucNode.children.get(1);  // Second child is the next INSTRUC
    
        // Typecheck the COMMAND and the next INSTRUC
        return typecheckCOMMAND(commandNode) && typecheckINSTRUC(nextInstrucNode);
    }

    public boolean typecheckCOMMAND(Node commandNode) {
        switch (commandNode.symb) {
            case "skip":
            case "halt":
                return true;  // These are base cases and always pass
    
            case "print":
                return typecheckPRINT(commandNode);
    
            case "return":
                return typecheckRETURN(commandNode);
    
            case "ASSIGN":
                return typecheckASSIGN(commandNode);
    
            case "CALL":
                // Call typecheckCALL and verify if the function is of type 'void' ('v')
                String callReturnType = typecheckCALL(commandNode);
                if (callReturnType.equals("v")) {
                    return true;  // Valid CALL with void return type
                } else {
                    System.err.println("Type Error: Function call must return 'void', but got type '" + callReturnType + "'.");
                    return false;
                }
    
            case "BRANCH":
                return typecheckBRANCH(commandNode);
    
            default:
                System.err.println("Error: Unrecognized command '" + commandNode.symb + "'.");
                return false;
        }
    }
    

    public boolean typecheckPRINT(Node printNode) {
        Node atomicNode = printNode.children.get(1);  // ATOMIC is the second child
    
        // Get the type of the ATOMIC value
        String atomicType = typecheckATOMIC(atomicNode);
    
        // Check if ATOMIC is either 'n' (numeric) or 't' (text)
        if (atomicType.equals("n") || atomicType.equals("t")) {
            return true;  // Valid types for print
        } else {
            System.err.println("Type Error: print expects a numeric or text value.");
            return false;
        }
    }

    public boolean typecheckRETURN(Node returnNode) {
        Node atomicNode = returnNode.children.get(1);  // ATOMIC is the second child
    
        // Get the type of the ATOMIC value (the value being returned)
        String atomicType = typecheckATOMIC(atomicNode);
    
        // For now, assume that functions can only return 'n' (numeric)
        String functionReturnType = "n";
    
        // Compare the return type of the function with the type of the returned value
        if (atomicType.equals(functionReturnType)) {
            return true;  // Valid return
        } else {
            System.err.println("Type Error: Function must return a numeric value, but found type '" + atomicType + "'.");
            return false;
        }
    }

    public String typecheckCALL(Node callNode) {
        // Get the function name (FNAME is the first child of CALL)
        String functionName = callNode.children.get(0).symb;
    
        // Look up the function's return type from the symbol table
        String returnType = scopeAnalyzer.getFunctionReturnType(functionName, scopeAnalyzer.mainScope);
    
        // Ensure the function exists
        if (returnType == null) {
            System.err.println("Type Error: Function '" + functionName + "' is not declared.");
            return "u";  // Undefined if function is not found
        }
    
        // Check the types of the three parameters (ATOMIC1, ATOMIC2, ATOMIC3)
        String atomicType1 = typecheckATOMIC(callNode.children.get(1));
        String atomicType2 = typecheckATOMIC(callNode.children.get(2));
        String atomicType3 = typecheckATOMIC(callNode.children.get(3));
    
        // Ensure all three parameters are numeric ('n')
        if (atomicType1.equals("n") && atomicType2.equals("n") && atomicType3.equals("n")) {
            return returnType;  // Return the function's return type
        } else {
            System.err.println("Type Error: Function '" + functionName + "' expects numeric arguments.");
            return "u";  // Undefined if any argument is not numeric
        }
    }
    

    public String typecheckATOMIC(Node atomicNode) {
        Node childNode = atomicNode.children.get(0);  // ATOMIC has one child, either VNAME or CONST
    
        // Determine whether ATOMIC is a VNAME or a CONST
        if (childNode.symb.equals("VNAME")) {
            return typecheckVNAME(childNode);  // Delegate to typecheckVNAME
        } else if (childNode.symb.equals("CONST")) {
            return typecheckCONST(childNode);  // Delegate to typecheckCONST
        } else {
            System.err.println("Type Error: Unrecognized ATOMIC type.");
            return "u";  // Return 'u' for undefined
        }
    }

    public String typecheckCONST(Node constNode) {
        String constType = constNode.children.get(0).symb;
    
        // Determine the type of the constant
        if (constType.equals("N")) {
            return "n";  // Numeric type
        } else if (constType.equals("T")) {
            return "t";  // Text type
        } else {
            System.err.println("Type Error: Unrecognized constant type.");
            return "u";  // Undefined type
        }
    }

    public boolean typecheckASSIGN(Node assignNode) {
        Node vnameNode = assignNode.children.get(0);  // VNAME is the first child
        String vnameType = typecheckVNAME(vnameNode);  // Get the type of VNAME
    
        // ASSIGN ::= VNAME < input
        if (assignNode.children.size() == 3 && assignNode.children.get(1).symb.equals("<") && assignNode.children.get(2).symb.equals("input")) {
            if (vnameType.equals("n")) {
                return true;  // Valid input assignment
            } else {
                System.err.println("Type Error: Only numeric input is allowed, but '" + vnameNode.symb + "' is of type '" + vnameType + "'.");
                return false;
            }
        }
    
        // ASSIGN ::= VNAME = TERM
        if (assignNode.children.size() == 3 && assignNode.children.get(1).symb.equals("=")) {
            Node termNode = assignNode.children.get(2);  // TERM is the third child
            String termType = typecheckTERM(termNode);  // Get the type of the TERM
    
            // Check if the types of VNAME and TERM match
            if (vnameType.equals(termType)) {
                return true;  // Valid assignment
            } else {
                System.err.println("Type Error: Variable '" + vnameNode.symb + "' is of type '" + vnameType +
                                   "', but assigned a value of type '" + termType + "'.");
                return false;
            }
        }
    
        // If no valid assignment rule matches, return false
        System.err.println("Type Error: Invalid assignment syntax.");
        return false;
    }

    public String typecheckTERM(Node termNode) {
        // TERM has one child, which could be ATOMIC, CALL, or OP
        Node childNode = termNode.children.get(0);
    
        // Determine the type of TERM based on its child
        switch (childNode.symb) {
            case "ATOMIC":
                return typecheckATOMIC(childNode);  // Return the type of ATOMIC (e.g., 'n', 't')
            case "CALL":
                return typecheckCALL(childNode);    // Return the return type of CALL (e.g., 'n', 'v')
            case "OP":
                return typecheckOP(childNode);      // Return the type of OP (e.g., 'n', 'b')
            default:
                System.err.println("Type Error: Unrecognized TERM type '" + childNode.symb + "'.");
                return "u";  // Return 'u' for undefined type
        }
    }

    public String typecheckOP(Node opNode) {
        Node operatorNode = opNode.children.get(0);  // First child is the operator
    
        if (operatorNode.symb.equals("UNOP")) {
            // OP ::= UNOP( ARG )
            Node argNode = opNode.children.get(2);  // ARG is the third child (index 2, skipping '(')
    
            // Get the types of UNOP and ARG
            String unopType = typecheckUNOP(operatorNode);
            String argType = typecheckARG(argNode);
    
            // Check if both UNOP and ARG are of the same type (boolean or numeric)
            if (unopType.equals("b") && argType.equals("b")) {
                return "b";  // Boolean result
            } else if (unopType.equals("n") && argType.equals("n")) {
                return "n";  // Numeric result
            } else {
                return "u";  // Undefined type
            }
        } else if (operatorNode.symb.equals("BINOP")) {
            // OP ::= BINOP( ARG1 , ARG2 )
            Node arg1Node = opNode.children.get(2);  // ARG1 is the third child (index 2, skipping '(')
            Node arg2Node = opNode.children.get(4);  // ARG2 is the fifth child (index 4, skipping ',')
    
            // Get the types of BINOP, ARG1, and ARG2
            String binopType = typecheckBINOP(operatorNode);
            String arg1Type = typecheckARG(arg1Node);
            String arg2Type = typecheckARG(arg2Node);
    
            // Check if BINOP and both arguments are of the same type
            if (binopType.equals("b") && arg1Type.equals("b") && arg2Type.equals("b")) {
                return "b";  // Boolean result
            } else if (binopType.equals("n") && arg1Type.equals("n") && arg2Type.equals("n")) {
                return "n";  // Numeric result
            } else if (binopType.equals("c") && arg1Type.equals("n") && arg2Type.equals("n")) {
                return "b";  // Comparison result is boolean
            } else {
                return "u";  // Undefined type
            }
        } else {
            return "u";  // Undefined if not UNOP or BINOP
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
        switch (binopNode.symb) {
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
                return "u";  // Undefined type
        }
    }

    public String typecheckSIMPLE(Node simpleNode) {
        // SIMPLE ::= BINOP( ATOMIC1 , ATOMIC2 )
        Node binopNode = simpleNode.children.get(0);  // BINOP is the first child
        Node atomic1Node = simpleNode.children.get(2); // ATOMIC1 is the third child (index 2, skipping '(')
        Node atomic2Node = simpleNode.children.get(4); // ATOMIC2 is the fifth child (index 4, skipping ',')
    
        // Get the types of BINOP, ATOMIC1, and ATOMIC2
        String binopType = typecheckBINOP(binopNode);
        String atomic1Type = typecheckATOMIC(atomic1Node);
        String atomic2Type = typecheckATOMIC(atomic2Node);
    
        // Check if BINOP, ATOMIC1, and ATOMIC2 are boolean
        if (binopType.equals("b") && atomic1Type.equals("b") && atomic2Type.equals("b")) {
            return "b";  // Boolean result
        }
        // Check if BINOP is a comparison and both ATOMICs are numeric
        else if (binopType.equals("c") && atomic1Type.equals("n") && atomic2Type.equals("n")) {
            return "b";  // Comparison result is boolean
        }
        // Otherwise, the type is undefined
        else {
            System.err.println("Type Error: Mismatch between BINOP and ATOMIC types in SIMPLE.");
            return "u";  // Undefined type
        }
    }

    public String typecheckCOMPOSIT(Node compositNode) {
        Node firstChild = compositNode.children.get(0);  // COMPOSIT has either BINOP or UNOP as the first child
    
        if (firstChild.symb.equals("BINOP")) {
            // COMPOSIT ::= BINOP( SIMPLE1 , SIMPLE2 )
            Node binopNode = compositNode.children.get(0);  // BINOP is the first child
            Node simple1Node = compositNode.children.get(2); // SIMPLE1 is the third child (index 2, skipping '(')
            Node simple2Node = compositNode.children.get(4); // SIMPLE2 is the fifth child (index 4, skipping ',')
    
            // Get the types of BINOP, SIMPLE1, and SIMPLE2
            String binopType = typecheckBINOP(binopNode);
            String simple1Type = typecheckSIMPLE(simple1Node);
            String simple2Type = typecheckSIMPLE(simple2Node);
    
            // Check if BINOP and both SIMPLEs are boolean
            if (binopType.equals("b") && simple1Type.equals("b") && simple2Type.equals("b")) {
                return "b";  // Boolean result
            } else {
                System.err.println("Type Error: Mismatch between BINOP and SIMPLE types in COMPOSIT.");
                return "u";  // Undefined type
            }
    
        } else if (firstChild.symb.equals("UNOP")) {
            // COMPOSIT ::= UNOP( SIMPLE )
            Node unopNode = compositNode.children.get(0);   // UNOP is the first child
            Node simpleNode = compositNode.children.get(2); // SIMPLE is the third child (index 2, skipping '(')
    
            // Get the types of UNOP and SIMPLE
            String unopType = typecheckUNOP(unopNode);
            String simpleType = typecheckSIMPLE(simpleNode);
    
            // Check if UNOP and SIMPLE are both boolean
            if (unopType.equals("b") && simpleType.equals("b")) {
                return "b";  // Boolean result
            } else {
                System.err.println("Type Error: Mismatch between UNOP and SIMPLE types in COMPOSIT.");
                return "u";  // Undefined type
            }
    
        } else {
            System.err.println("Type Error: Unrecognized COMPOSIT structure.");
            return "u";  // Undefined type
        }
    }

    public String typecheckFNAME(Node fnameNode) {
        // FNAME is a token from the lexer
        String functionName = fnameNode.children.get(0).symb;
    
        // Look up the function's return type in the symbol table
        String returnType = scopeAnalyzer.getFunctionReturnType(functionName, scopeAnalyzer.mainScope);
    
        if (returnType != null) {
            return returnType;  // Return the function's return type from the symbol table
        } else {
            System.err.println("Type Error: Function '" + functionName + "' not found in the symbol table.");
            return "u";  // Undefined type if the function is not found
        }
    }

    public boolean typecheckFUNCTIONS(Node functionsNode) {
        // FUNCTIONS1 ::= DECL FUNCTIONS2
    
        Node declNode = functionsNode.children.get(0);  // DECL is the first child
        Node functions2Node = functionsNode.children.get(1);  // FUNCTIONS2 is the second child
    
        return typecheckDECL(declNode) && typecheckFUNCTIONS(functions2Node);
    }

    public boolean typecheckDECL(Node declNode) {
        // DECL ::= HEADER BODY
    
        Node headerNode = declNode.children.get(0);  // HEADER is the first child
        Node bodyNode = declNode.children.get(1);    // BODY is the second child
    
        // Typecheck both HEADER and BODY
        return typecheckHEADER(headerNode) && typecheckBODY(bodyNode);
    }
    
    public boolean typecheckHEADER(Node headerNode) {
        // HEADER ::= FTYP FNAME( VNAME1 , VNAME2 , VNAME3 )
        
        Node ftypNode = headerNode.children.get(0);   // FTYP is the first child
        Node fnameNode = headerNode.children.get(1);  // FNAME is the second child
        Node vname1Node = headerNode.children.get(3); // VNAME1 is the fourth child (index 3)
        Node vname2Node = headerNode.children.get(4); // VNAME2 is the fifth child (index 4)
        Node vname3Node = headerNode.children.get(5); // VNAME3 is the sixth child (index 5)
    
        // Get the type of FTYP (return type of the function)
        String ftypType = typecheckFTYP(ftypNode);
    
        // Get the function name (FNAME) and look up its type in the symbol table
        String functionName = fnameNode.children.get(0).symb;
        String symbolTableReturnType = scopeAnalyzer.getFunctionReturnType(functionName, scopeAnalyzer.mainScope);
    
        // Ensure that the function's return type matches the declared FTYP
        if (!ftypType.equals(symbolTableReturnType)) {
            System.err.println("Type Error: Function '" + functionName + "' has a mismatched return type.");
            return false;
        }
    
        // Check if all VNAME arguments are numeric ('n')
        String vname1Type = scopeAnalyzer.getVariableType(vname1Node.children.get(0).symb, scopeAnalyzer.mainScope);
        String vname2Type = scopeAnalyzer.getVariableType(vname2Node.children.get(0).symb, scopeAnalyzer.mainScope);
        String vname3Type = scopeAnalyzer.getVariableType(vname3Node.children.get(0).symb, scopeAnalyzer.mainScope);
    
        if (vname1Type.equals("n") && vname2Type.equals("n") && vname3Type.equals("n")) {
            return true;  // All arguments are numeric
        } else {
            System.err.println("Type Error: Function '" + functionName + "' has non-numeric arguments.");
            return false;
        }
    }

    public String typecheckFTYP(Node ftypNode) {
        // FTYP ::= num | void
        switch (ftypNode.children.get(0).symb) {
            case "num":
                return "n";  
            case "void":
                return "v";  
            default:
                System.err.println("Type Error: Unrecognized FTYP '" + ftypNode.children.get(0).symb + "'.");
                return "u";
        }
    }

    public boolean typecheckBODY(Node bodyNode) {
        // BODY ::= PROLOG LOCVARS ALGO EPILOG SUBFUNCS end
    
        Node prologNode = bodyNode.children.get(0);   // PROLOG is the first child
        Node locvarsNode = bodyNode.children.get(1);  // LOCVARS is the second child
        Node algoNode = bodyNode.children.get(2);     // ALGO is the third child
        Node epilogNode = bodyNode.children.get(3);   // EPILOG is the fourth child
        Node subfuncsNode = bodyNode.children.get(4); // SUBFUNCS is the fifth child
    
        // Typecheck all components
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
        // EPILOG ::= }
        return true;  // Base-case: always returns true
    }

    public boolean typecheckLOCVARS(Node locvarsNode) {
        // LOCVARS ::= VTYP1 VNAME1 , VTYP2 VNAME2 , VTYP3 VNAME3 ,
    
        // Get the nodes for VTYP and VNAME
        Node vtyp1Node = locvarsNode.children.get(0);  // VTYP1 is the first child
        Node vname1Node = locvarsNode.children.get(1); // VNAME1 is the second child
        Node vtyp2Node = locvarsNode.children.get(3);  // VTYP2 is the fourth child (after ',')
        Node vname2Node = locvarsNode.children.get(4); // VNAME2 is the fifth child
        Node vtyp3Node = locvarsNode.children.get(6);  // VTYP3 is the seventh child (after ',')
        Node vname3Node = locvarsNode.children.get(7); // VNAME3 is the eighth child
    
        // Typecheck the first variable
        String vtyp1Type = typecheckVTYP(vtyp1Node);
        String vname1Type = scopeAnalyzer.getVariableType(vname1Node.children.get(0).symb, scopeAnalyzer.mainScope);
        if (!vtyp1Type.equals(vname1Type)) {
            System.err.println("Type Error: Variable '" + vname1Node.children.get(0).symb + "' does not match its declared type.");
            return false;
        }
    
        // Typecheck the second variable
        String vtyp2Type = typecheckVTYP(vtyp2Node);
        String vname2Type = scopeAnalyzer.getVariableType(vname2Node.children.get(0).symb, scopeAnalyzer.mainScope);
        if (!vtyp2Type.equals(vname2Type)) {
            System.err.println("Type Error: Variable '" + vname2Node.children.get(0).symb + "' does not match its declared type.");
            return false;
        }
    
        // Typecheck the third variable
        String vtyp3Type = typecheckVTYP(vtyp3Node);
        String vname3Type = scopeAnalyzer.getVariableType(vname3Node.children.get(0).symb, scopeAnalyzer.mainScope);
        if (!vtyp3Type.equals(vname3Type)) {
            System.err.println("Type Error: Variable '" + vname3Node.children.get(0).symb + "' does not match its declared type.");
            return false;
        }
    
        return true;  // All variables match their declared types
    }
    
    public boolean typecheckSUBFUNCS(Node subfuncsNode) {
        // SUBFUNCS ::= FUNCTIONS
        return typecheckFUNCTIONS(subfuncsNode.children.get(0));  // Delegate to typecheckFUNCTIONS
    }
    
    public void depthFirstTraversal(Node currentNode) {
       if(typecheckPROG(currentNode)){
        System.out.println("Program is correctly typed");
       }else{
        System.out.println("Program is not correctly typed");
       } 
    }
    
}
