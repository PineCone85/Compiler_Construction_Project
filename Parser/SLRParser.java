import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SLRParser {

    private Map<String, String> parseTable = new HashMap<>();
    private Stack<Integer> stateStack = new Stack<>();
    private Stack<String> symbolStack = new Stack<>();

    public void loadParseTable(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            if (line == null) {
                throw new IOException("Empty file");
            }
    
            String[] tokens = line.split(",", -1);
    
            while ((line = br.readLine()) != null) {
                String[] row = line.split(",", -1);
                String state = row[0];
    

                for (int i = 1; i < tokens.length && i < row.length; i++) {
                    if (!row[i].isEmpty()) {
                        String token = tokens[i];
                        

                        if (token.equals("\"")) {
                            token = ",";
                        }
    
                        String key = state + "," + token; 
                        String action = row[i];
                        parseTable.put(key, action);
    
                        System.out.println("Key: " + key + " -> Action: " + action);
                    }
                }
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

public void parseInput(String input) {
    stateStack.push(0);  
    String[] tokens = input.split("\\s+"); 

    int i = 0; 
    while (i <= tokens.length) {
        if (stateStack.isEmpty()) {
            System.out.println("Error: State stack is empty, cannot proceed.");
            return;
        }

        int currentState = stateStack.peek();  
        String currentToken = (i < tokens.length) ? tokens[i] : "$"; 

        // Get the action from the parse table
        String action = parseTable.get(currentState + "," + currentToken);
        System.out.println("Current State: " + currentState + ", Current Token: " + currentToken);
        System.out.println("Action: " + action);

        if (action == null) {
            System.out.println("Error: No valid action for state " + currentState + " and token " + currentToken);
            return;
        }

        if (action.startsWith("s")) {
            int newState = Integer.parseInt(action.substring(1)); 
            stateStack.push(newState);  
            symbolStack.push(currentToken); 
            i++; 
        } else if (action.startsWith("r")) {

            int productionIndex = Integer.parseInt(action.substring(1)); 
            String production = getProduction(productionIndex);


            int popCount = getPopCount(production);
            if (stateStack.size() < popCount || symbolStack.size() < popCount) {
                System.out.println("Error: Stack underflow during reduction. State or symbol stack is too small.");
                return;
            }

            for (int j = 0; j < popCount; j++) {
                stateStack.pop();
                symbolStack.pop();
            }

            String lhs = production.split("::=")[0].trim();  
            currentState = stateStack.peek(); 
            String gotoStateKey = currentState  + "," + lhs; 
            String gotoState = parseTable.get(gotoStateKey);

            if (gotoState != null) {
                stateStack.push(Integer.parseInt(gotoState));
                symbolStack.push(lhs);
            } else {
                System.out.println("Error: No valid goto for state " + currentState + " and non-terminal " + lhs);
                return;
            }
        } else if (action.equals("acc")) {
            System.out.println("Input accepted!");
            return;
        } else {
            System.out.println("Error: Invalid action " + action);
            return;
        }
    }
}
    private String getProduction(int index) {
        switch (index) {
            case 0: return "PROG ::= main GLOBVARS ALGO FUNCTIONS";
            case 1: return "GLOBVARS ::= "; 
            case 2: return "GLOBVARS ::= VTYP VNAME , GLOBVARS";
            case 3: return "VTYP ::= num";
            case 4: return "VTYP ::= text";
            case 5: return "VNAME ::= V";
            case 6: return "ALGO ::= begin INSTRUC end";
            case 7: return "INSTRUC ::= "; 
            case 8: return "INSTRUC ::= COMMAND ; INSTRUC";
            case 9: return "COMMAND ::= skip";
            case 10: return "COMMAND ::= halt";
            case 11: return "COMMAND ::= print ATOMIC";
            case 12: return "COMMAND ::= ASSIGN";
            case 13: return "COMMAND ::= CALL";
            case 14: return "COMMAND ::= BRANCH";
            case 15: return "ATOMIC ::= VNAME";
            case 16: return "ATOMIC ::= CONST";
            case 17: return "CONST ::= N";
            case 18: return "CONST ::= T";
            case 19: return "ASSIGN ::= VNAME < input";
            case 20: return "ASSIGN ::= VNAME = TERM";
            case 21: return "CALL ::= FNAME ( ATOMIC , ATOMIC , ATOMIC )";
            case 22: return "BRANCH ::= if COND then ALGO else ALGO";
            case 23: return "TERM ::= ATOMIC";
            case 24: return "TERM ::= CALL";
            case 25: return "TERM ::= OP";
            case 26: return "OP ::= UNOP( ARG )";
            case 27: return "OP ::= BINOP( ARG , ARG )";
            case 28: return "ARG ::= ATOMIC";
            case 29: return "ARG ::= OP";
            case 30: return "COND ::= SIMPLE";
            case 31: return "COND ::= COMPOSIT";
            case 32: return "SIMPLE ::= BINOP ( ATOMIC , ATOMIC )";
            case 33: return "COMPOSIT ::= BINOP ( SIMPLE , SIMPLE )";
            case 34: return "COMPOSIT ::= UNOP ( SIMPLE )";
            case 35: return "UNOP ::= not";
            case 36: return "UNOP ::= sqrt";
            case 37: return "BINOP ::= or";
            case 38: return "BINOP ::= and";
            case 39: return "BINOP ::= eq";
            case 40: return "BINOP ::= grt";
            case 41: return "BINOP ::= add";
            case 42: return "BINOP ::= sub";
            case 43: return "BINOP ::= mul";
            case 44: return "BINOP ::= div";
            case 45: return "FNAME ::= F";
            case 46: return "FUNCTIONS ::= ";
            case 47: return "FUNCTIONS ::= DECL FUNCTIONS";
            case 48: return "DECL ::= HEADER BODY";
            case 49: return "HEADER ::= FTYP FNAME ( VNAME , VNAME , VNAME )";
            case 50: return "FTYP ::= num";
            case 51: return "FTYP ::= void";
            case 52: return "BODY ::= PROLOG LOCVARS ALGO EPILOG SUBFUNCS end";
            case 53: return "PROLOG ::= {";
            case 54: return "EPILOG ::= }";
            case 55: return "LOCVARS ::= VTYP VNAME , VTYP VNAME , VTYP VNAME ,";
            case 56: return "SUBFUNCS ::= FUNCTIONS";
            default: return "";
        }
    }


    

    private int getPopCount(String production) {
        String rhs = production.split("::=")[1].trim(); 
    
        if (rhs.equals("")) {

            return 0;
        }

        String[] symbols = rhs.split("\\s+");
    
        int popCount = 0;
        for (String symbol : symbols) {
            if (!symbol.equals("")) {
                popCount++;  
            }
        }
    
        return popCount; 
    }
    

    public static void main(String[] args) {
        SLRParser parser = new SLRParser();
        parser.loadParseTable("P6.csv");
        String input = "main begin end num F ( V , V , V ) { num V , text V , num V , begin end } end";
                        
        parser.parseInput(input);
    }
}
