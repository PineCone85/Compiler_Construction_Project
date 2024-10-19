import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable{

    
    // private HashMap<Integer, Symbol> scopes;
    private Stack<Map<String, Symbol>> scopes;

    public SymbolTable(){
        // scopes = new HashMap<Integer, Symbol>();
        scopes = new Stack<>();
        enterScope();
    }

    public void enterScope(){
        scopes.push(new HashMap<>());
    }

    public void exitScope(){
        if(!scopes.isEmpty()){
            scopes.pop();
        }
        else{
            throw new RuntimeException("No scope to exit");
        }
        
    }

    public void addSymbol(String name, Symbol symbol){
        if(!scopes.isEmpty()){
            scopes.peek().put(name , symbol);
        }
    }

    public Symbol lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }
}


/*
    Scoping rules:
    - Main forms the highest Scope with no parent
    - Every Function declaration has its own scope
    - Child scopes may not have the same name as a parent scope
    - Child scopes may not have the same name as a sibling scope under the same parent

    
    Read the XML Table and add all Variables and their parents to a data strucuture
    if parent == -1 then it is ROOT - SAVE ROOT ID 
    if parent ==  ROOTID then it is child of ROOT


*/ 
