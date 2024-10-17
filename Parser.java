import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.xml.parsers.*;
import org.w3c.dom.*;



public class Parser{

    private List<GrammarRule> rules = Arrays.asList(
        new GrammarRule("PROG", new String[] {"main","GLOBVARS", "ALGO", "FUNCTIONS"}),
        
        //GLOBVARS is also NULLABLE
        new GrammarRule("GLOBVARS", new String[] {""}),
        new GrammarRule("GLOBVARS", new String[] {"VTYP","VNAME", ",", "GLOBVARS"}),

        new GrammarRule("VTYP", new String[] {"num"}),
        new GrammarRule("VTYP", new String[] {"text"}),
        
        new GrammarRule("VNAME", new String[] {"V_PLACEHOLDER"}),//[a-z]([a-z]|[0-9])*

        new GrammarRule("ALGO", new String[] {"begin", "INSTRUC", "end"}),
        
        //INSTRUC is also NULLABLE
        new GrammarRule("INSTRUC", new String[] {""}),
        new GrammarRule("INSTRUC", new String[] {"COMMAND", ";", "INSTRUC"}),

        new GrammarRule("COMMAND", new String[] {"skip"}),
        new GrammarRule("COMMAND", new String[] {"halt"}),
        new GrammarRule("COMMAND", new String[] {"print", "ATOMIC"}),
        new GrammarRule("COMMAND", new String[] {"ASSIGN"}),
        new GrammarRule("COMMAND", new String[] {"CALL"}),
        new GrammarRule("COMMAND", new String[] {"BRANCH"}),

        new GrammarRule("ATOMIC", new String[] {"VNAME"}),
        new GrammarRule("ATOMIC", new String[] {"CONST"}),

        new GrammarRule("CONST", new String[] {"N_PLACEHOLDER"}),//"0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]"
        new GrammarRule("CONST", new String[] {"T_PLACEHOLDER"}),//"\"[A-Z][a-z]{0,7}\""

        new GrammarRule("ASSIGN", new String[] {"VNAME"," < input"}),
        new GrammarRule("ASSIGN", new String[] {"VNAME"," ="," TERM"}),

        new GrammarRule("CALL", new String[] {"FNAME","(","ATOMIC",",","ATOMIC",",","ATOMIC",")"}),

        new GrammarRule("BRANCH", new String[] {"if","COND","then","ALGO","else","ALGO"}),

        new GrammarRule("TERM", new String[] {"ATOMIC"}),
        new GrammarRule("TERM", new String[] {"CALL"}),
        new GrammarRule("TERM", new String[] {"OP"}),

        new GrammarRule("OP", new String[] {"UNOP","(","ARG",")"}),
        new GrammarRule("OP", new String[] {"BINOP","(","ARG",",","ARG",")"}),

        new GrammarRule("ARG", new String[] {"ATOMIC"}),
        new GrammarRule("ARG", new String[] {"OP"}),

        new GrammarRule("COND", new String[] {"SIMPLE"}),
        new GrammarRule("COND", new String[] {"COMPOSIT"}),

        new GrammarRule("SIMPLE", new String[] {"BINOP","(","ATOMIC",",","ATOMIC",")"}),
        new GrammarRule("COMPOSIT", new String[] {"BINOP","(","SIMPLE",",","SIMPLE",")"}),
        new GrammarRule("COMPOSIT", new String[] {"UNOP","(","SIMPLE",")"}),

        new GrammarRule("UNOP", new String[] {"not"}),
        new GrammarRule("UNOP", new String[] {"sqrt"}),

        new GrammarRule("BINOP", new String[] {"or"}),
        new GrammarRule("BINOP", new String[] {"and"}),
        new GrammarRule("BINOP", new String[] {"eq"}),
        new GrammarRule("BINOP", new String[] {"grt"}),
        new GrammarRule("BINOP", new String[] {"add"}),
        new GrammarRule("BINOP", new String[] {"sub"}),
        new GrammarRule("BINOP", new String[] {"mul"}),
        new GrammarRule("BINOP", new String[] {"div"}),

        new GrammarRule("FNAME", new String[] {"F_PLACEHOLDER"}),//"F_[a-z]([a-z]|[0-9])*"

        new GrammarRule("FUNCTIONS", new String[] {""}),
        new GrammarRule("FUNCTIONS", new String[] {"DECL", "FUNCTIONS"}),

        new GrammarRule("DECL", new String[] {"HEADER","BODY"}),

        new GrammarRule("HEADER", new String[] {"FTYP","FNAME","(","VNAME",",","VNAME",",","VNAME",")"}),

        new GrammarRule("FTYP", new String[] {"num"}),
        new GrammarRule("FTYP", new String[] {"void"}),

        new GrammarRule("BODY", new String[] {"PROLOG", "LOCVARS", "ALGO", "EPILOG", "SUBFUNCS", "end"}),

        new GrammarRule("PROLOG", new String[] {"{"}),
        new GrammarRule("EPILOG", new String[] {"}"}),

        // new GrammarRule("LOCVARS", new String[] {"VTYP","VNAME",",","VTYP","VNAME",",","VTYP","VNAME",","}),

        new GrammarRule("SUBFUNCS", new String[] {"FUNCTIONS"})
    );

    // private List<Token> input;
    private Stack<String> stack = new Stack<>();
    private Stack<String> parseStack = new Stack<>();
    private int position = 0;

    private List<String> wordList = new ArrayList<>();
    private List<String> classList = new ArrayList<>();
    private List<String> copiedWList = new ArrayList<>();

    public Parser(){
    }

    public void xmlToToken(String fileName){
        try {
            File inputFile = new File("output.xml");

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile); 
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("TOK");

            for(int i = 0; i < nList.getLength(); i++){
                Node tokenNode = nList.item(i);
                if(tokenNode.getNodeType() == Node.ELEMENT_NODE){
                    Element tokenElement = (Element) tokenNode;

                    String id = tokenElement.getElementsByTagName("ID").item(0).getTextContent();
                    String className = tokenElement.getElementsByTagName("CLASS").item(0).getTextContent();
                    String word = tokenElement.getElementsByTagName("WORD").item(0).getTextContent();

                    wordList.add(word);
                    copiedWList.add(word); 
                    classList.add(className);

                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void parse(){
       while(position < wordList.size()){
            if(reduce()){
               continue;
            }
            else{
                shift();
            }    
       }
       if(stack.size() == 1 && stack.peek().equals("PROG")){
           System.out.println("Parsing successful");
       }
       else{
           System.out.println("Parsing failed");
       }
    }

    private void shift(){
        stack.push(wordList.get(position));
        position++;
        System.out.println("Shifted: "+stack);
    }

    private boolean reduce(){
        for(GrammarRule g: rules){
            if(stackMatches(g.getRight())){
                for(int i = 0; i < g.getRight().length; i++){
                    stack.pop();
                }
                stack.push(g.getLeft());
                return true;
            }
        }
        return false;
    }

    // private boolean stackMatches(String[] right){
    //     boolean flag = false;
    //     if(stack.size() < right.length){
    //         return false;
    //     }
    //     for(int i = 0; i < right.length; i++){
    //         if (!stack.get(stack.size() - right.length + i).equals(right[i])){
    //             return false;
    //         }
    //         // if(stack.get(stack.size() - right.length + i ).equals(right[i])){
    //         //     flag = true;
    //         // }
    //     }
    //     return /*flag ||*/ true;
    // }

    private boolean stackMatches(String[] right) {
        if (stack.size() < right.length) {
            return false;
        }
        for (int i = 0; i < right.length; i++) {
            String stackElement = stack.get(stack.size() - right.length + i);
            String ruleElement = right[i];
            
            if (ruleElement.equals("V_PLACEHOLDER")) {
                if (!isVName(stackElement)) {
                    return false;
                }
            }

            else if (ruleElement.equals("F_PLACEHOLDER")) {
                if (!isFName(stackElement)) {
                    return false;
                }
            }
        
            else if (ruleElement.equals("T_PLACEHOLDER")) {
                if (!isT(stackElement)) {
                    return false;
                }
            }

            else if(ruleElement.equals("N_PLACEHOLDER")) {
                if (!isN(stackElement)) {
                    return false;
                }
            }
            
            else if (!stackElement.equals(ruleElement)) {
                return false;
            }

        }
        return true;
    }

    private boolean isVName(String token) {
            return token.matches("V_[a-z]([a-z]|[0-9])*");
    }

    private boolean isFName(String token) {
        return token.matches("F_[a-z]([a-z]|[0-9])*");
    }

    private boolean isT(String token) {
            return token.matches("\"[A-Z][a-z]{0,7}\"");
    }

    private boolean isN(String token) {
        return token.matches("0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]");
    }

    private void parsePROG(){
        if(position < wordList.size()){
            if(wordList.get(position).equals("main")){
                position++;
                parseGLOBVARS();
                parseALGO();
                parseFUNCTIONS();
                parseStack.push("PROG");    
            }
            // System.out.println("Parsing successful");
        }

        if(!wordList.get(position).equals("}") /*|| !wordList.get(position).equals("end")*/){
            System.out.println("Parsing Stack: "+parseStack);
            System.out.println(wordList.get(position-1)+" at position: "+position);
            System.out.println("Parsing failed");
        }
        else{

            System.out.println("Parsing Successful");
        }
    }

    private void parseGLOBVARS(){
        if(position < wordList.size()){
            if(wordList.get(position).equals("")){
                // Nullable rule
                return;
            }
            else{
                parseVTYP();
                parseVNAME();
                // parseStack.push(", GLOBVARS");
                if(wordList.get(position).equals(",")){
                    position++;
                    parseGLOBVARS(); // Recursive call
                }
            }
        }
    }

    private void parseVTYP(){
        if(position < wordList.size()){
            if(wordList.get(position).equals("num") || wordList.get(position).equals("text")){
                position++;
                parseStack.push("VTYP");
            }
        }
    }
    
    private void parseVNAME(){
        if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
            position++;
            parseStack.push("VNAME");
        }
    }

    private void parseALGO(){
        if(position < wordList.size()){
            if(wordList.get(position).equals("begin")){
                position++;
                parseINSTRUC();
                if(wordList.get(position).equals("end")){
                    position++;
                }
            }
        }
    }

    private void parseINSTRUC(){
        if(position < wordList.size()){
            if(wordList.get(position).equals("")){
                // Nullable rule

                return;
            }
            else{
                parseCOMMAND();
                if(wordList.get(position).equals(";")){
                    position++;
                    parseINSTRUC(); // Recursive call
                }
            }
        }
    }

    private void parseCOMMAND(){
        if(position < wordList.size()){
            if(wordList.get(position).equals("skip") || wordList.get(position).equals("halt")){
                position++;
            }
            else if(wordList.get(position).equals("print")){
                position++;
                parseATOMIC();
            }
            else if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
                parseASSIGN();
            }
            else if(wordList.get(position).matches("F_[a-z]([a-z]|[0-9])*")){
                parseCALL();
            }
            else if(wordList.get(position).equals("if")){//LookAhead of COMMAND ::= BRANCH 
                //NO position++ as we do that in the parseBRANCH() function
                parseBRANCH();
            }
            else if(wordList.get(position).equals("return")){
                position++;
                parseATOMIC();
            }
        }
    }

    private void parseATOMIC(){
        if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*") || wordList.get(position).matches("\"[A-Z][a-z]{0,7}\"") || wordList.get(position).matches("0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]")){
            position++;
        }
    }

    private void parseASSIGN(){
        if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
            position++;
            if(wordList.get(position).equals("<input")){
                position++;
            }
            else if(wordList.get(position).equals("=")){
                position++;
                parseTERM();
            }
        }
    }

    private void parseCALL(){
        if(wordList.get(position).matches("F_[a-z]([a-z]|[0-9])*")){
            position++;
            if(wordList.get(position).equals("(")){
                position++;
                parseATOMIC();
                if(wordList.get(position).equals(",")){
                    position++;
                    parseATOMIC();
                    if(wordList.get(position).equals(",")){
                        position++;
                        parseATOMIC();
                        if(wordList.get(position).equals(")")){
                            position++;
                        }
                    }
                    else if(wordList.get(position).equals(")")){
                        position++;
                    }
                }
            }
        }
    }

    private void parseBRANCH(){
        if(wordList.get(position).equals("if")){
            position++;
            parseCOND();
            if(wordList.get(position).equals("then")){
                position++;
                parseALGO();
                if(wordList.get(position).equals("else")){
                    position++;
                    parseALGO();
                }
            }
        }
    }

    private void parseTERM(){
        if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*") || wordList.get(position).matches("\"[A-Z][a-z]{0,7}\"") || wordList.get(position).matches("0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]")){
            position++;
        }
        else if(wordList.get(position).matches("F_[a-z]([a-z]|[0-9])*")){
            parseCALL();
        }
        else if(wordList.get(position).equals("not") || wordList.get(position).equals("sqrt") || wordList.get(position).equals("or") || wordList.get(position).equals("and") || wordList.get(position).equals("eq") || wordList.get(position).equals("grt") || wordList.get(position).equals("add") || wordList.get(position).equals("sub") || wordList.get(position).equals("mul") || wordList.get(position).equals("div")){
            parseOP();
        }
    }

    //COND ::= SIMPLE | COMPOSIT
    private void parseCOND(){
        if(wordList.get(position).equals("or") || wordList.get(position).equals("and") || wordList.get(position).equals("eq") || wordList.get(position).equals("grt") || wordList.get(position).equals("add") || wordList.get(position).equals("sub") || wordList.get(position).equals("mul") || wordList.get(position).equals("div")){
            position++;
            if(wordList.get(position).equals("(")){
                position++;
                //SIMPLE ::= BINOP ( ATOMIC , ATOMIC )
                if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*") || wordList.get(position).matches("\"[A-Z][a-z]{0,7}\"") || wordList.get(position).matches("0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]")){
                    position++;
                    if(wordList.get(position).equals(",")){
                        position++;
                        if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*") || wordList.get(position).matches("\"[A-Z][a-z]{0,7}\"") || wordList.get(position).matches("0|0\\.([0-9])*[1-9]|-0\\.([0-9])*[1-9]|[1-9]([0-9])*|-?[1-9]([0-9])*|-?[1-9]([0-9])*.([0-9])*[1-9]")){
                            position++;
                            if(wordList.get(position).equals(")")){
                                position++;
                            }
                        }
                    }
                }
                //COMPOSIT ::= BINOP(SIMPLE, SIMPLE)
                else if(wordList.get(position).equals("or") || wordList.get(position).equals("and") || wordList.get(position).equals("eq") || wordList.get(position).equals("grt") || wordList.get(position).equals("add") || wordList.get(position).equals("sub") || wordList.get(position).equals("mul") || wordList.get(position).equals("div")){
                    position++;
                    if(wordList.get(position).equals("(")){
                        position++;
                        parseSIMPLE();
                        if(wordList.get(position).equals(",")){
                            position++;
                            parseSIMPLE();
                            if(wordList.get(position).equals(")")){
                                position++;
                            }
                        }
                    }
                }
                //COMPOSIT ::= UNOP(SIMPLE)
                else if(wordList.get(position).equals("not") || wordList.get(position).equals("sqrt")){
                    position++;
                    if(wordList.get(position).equals("(")){
                        position++;
                        parseSIMPLE();
                        if(wordList.get(position).equals(")")){
                            position++;
                        }
                    }
                }
            }
        }
    }

    private void parseOP(){
        if(wordList.get(position).equals("not") || wordList.get(position).equals("sqrt")){
            position++;
            if(wordList.get(position).equals("(")){
                position++;
                parseARG();
                if(wordList.get(position).equals(")")){
                    position++;
                }
            }
        }
        else if(wordList.get(position).equals("or") || wordList.get(position).equals("and") || wordList.get(position).equals("eq") || wordList.get(position).equals("grt") || wordList.get(position).equals("add") || wordList.get(position).equals("sub") || wordList.get(position).equals("mul") || wordList.get(position).equals("div")){
            position++;
            if(wordList.get(position).equals("(")){
                position++;
                parseARG();
                if(wordList.get(position).equals(",")){
                    position++;
                    parseARG();
                    if(wordList.get(position).equals(")")){
                        position++;
                    }
                }
            }
        }
    }

    private void parseARG(){
        if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*") || wordList.get(position).matches("\"[A-Z][a-z]{0,7}\"")){
            position++;
        }
        else if(wordList.get(position).equals("not") || wordList.get(position).equals("sqrt")){
            parseOP();
        }
    }

    private void parseSIMPLE(){
        if(wordList.get(position).equals("or") || wordList.get(position).equals("and") || wordList.get(position).equals("eq") || wordList.get(position).equals("grt") || wordList.get(position).equals("add") || wordList.get(position).equals("sub") || wordList.get(position).equals("mul") || wordList.get(position).equals("div")){
            position++;
            if(wordList.get(position).equals("(")){
                position++;
                parseATOMIC();
                if(wordList.get(position).equals(",")){
                    position++;
                    parseATOMIC();
                    if(wordList.get(position).equals(")")){
                        position++;
                    }
                }
            }
        }
    }

    // private void parseCOMPOSIT(){
    //      if(wordList.get(position).equals("or") || wordList.get(position).equals("and") || wordList.get(position).equals("eq") || wordList.get(position).equals("grt") || wordList.get(position).equals("add") || wordList.get(position).equals("sub") || wordList.get(position).equals("mul") || wordList.get(position).equals("div")){
    //         position++;
    //         if(wordList.get(position).equals("(")){
    //             position++;
    //             parseSIMPLE();
    //             if(wordList.get(position).equals(",")){
    //                 position++;
    //                 parseSIMPLE();
    //                 if(wordList.get(position).equals(")")){
    //                     position++;
    //                 }
    //             }
    //         }
    //     }
    //     else if(wordList.get(position).equals("not") || wordList.get(position).equals("sqrt")){
    //         position++;
    //         if(wordList.get(position).equals("(")){
    //             position++;
    //             parseSIMPLE();
    //             if(wordList.get(position).equals(")")){
    //                 position++;
    //             }
    //         }
    //     }
    // }

    private void parseFUNCTIONS(){
        try {
            if (position < wordList.size()) {
                if (wordList.get(position).equals("")) {
                    // Nullable rule
                    return;
                } else {
                    // System.out.print("Befoer DECL At Position: "+position+" "+wordList.get(position));
                    parseDECL();
                    parseFUNCTIONS(); // Recursive call
                }
        }
        } catch (StackOverflowError e) {
            System.err.println("Error: "+e.getMessage());
            return;
        }
    }

    private void parseDECL() {
        parseHEADER();
        parseBODY();
    }

    private void parseHEADER(){
        if(wordList.get(position).equals("num") || wordList.get(position).equals("void")){
            position++;
            if(wordList.get(position).matches("F_[a-z]([a-z]|[0-9])*")){
                position++;
                if(wordList.get(position).equals("(")){
                    position++;
                    if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
                        position++;
                        if(wordList.get(position).equals(",")){
                            position++;
                            if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
                                position++;
                                if(wordList.get(position).equals(",")){
                                    position++;
                                    if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
                                        position++;
                                        if(wordList.get(position).equals(")")){
                                            position++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseBODY(){
        if(wordList.get(position).equals("{")){
            position++;
            // parsePROLOG();
            parseLOCVARS();
            parseALGO();
            // System.out.println("Position: "+position);
            // parseEPILOG();
            if(wordList.get(position).equals("}") || wordList.get(position).equals("end")){
                if(position == wordList.size()-1){
                    return;
                }
                else{
                    System.out.println("Position: "+position);
                    position++;
                    parseSUBFUNCS();
                }
                // // position++;
                // // parseSUBFUNCS();
                // if(wordList.get(position).equals("end") || wordList.get(position).equals("}")){
                //     // position++;
                // }
            }
        }
    }

    private void parseLOCVARS(){
        if(wordList.get(position).equals("num") || wordList.get(position).equals("text")){
            position++;
            if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
                position++;
                if(wordList.get(position).equals(",")){
                    position++;
                    if(wordList.get(position).equals("num") || wordList.get(position).equals("text")){
                        position++;
                        if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
                            position++;
                            if(wordList.get(position).equals(",")){
                                position++;
                                if(wordList.get(position).equals("num") || wordList.get(position).equals("text")){
                                    position++;
                                    if(wordList.get(position).matches("V_[a-z]([a-z]|[0-9])*")){
                                        position++;
                                        if(wordList.get(position).equals(",")){
                                            position++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseSUBFUNCS(){
        if (position < wordList.size()) {
           parseFUNCTIONS(); 
        }
        else{
        
        }
    }

    private Map<Integer, Map<String, String>> actionTable = new HashMap<>();
    private Map<Integer, Map<String, Integer>> gotoTable = new HashMap<>();
    private Stack<String> symbolStack = new Stack<>();
    private Stack<Integer> stateStack = new Stack<>();

    public void loadParseTable(String filePath) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String [] headers = br.readLine().split(",");

        String line;
        while((line = br.readLine()) != null){
            String [] values = line.split(",");
            System.out.println(Arrays.toString(values));
            int state = Integer.parseInt(values[0]);

            Map<String, String> actionrow = new HashMap<>();
            Map<String, Integer> gotoRow = new HashMap<>();

            for(int i = 1; i < values.length; i++){
                String header = headers[i];
                String value = values[i];
                if(header.matches("[a-zA-Z]+")){
                    if(!value.isEmpty() || !value.equals("")){
                        actionrow.put(header, value);
                    }
                }
                else{
                    if(!value.isEmpty() || !value.equals("")){
                        gotoRow.put(header, Integer.parseInt(value));
                    }
                }
            }
            actionTable.put(state, actionrow);
            gotoTable.put(state, gotoRow);
        }

    }

    public String getAction(int state, String token) {
        return actionTable.getOrDefault(state, new HashMap<>()).get(token);
    }

    public Integer getGoto(int state, String nonTerminal) {
        return gotoTable.getOrDefault(state, new HashMap<>()).get(nonTerminal);
    }

    public void parsePROG1() {
        stateStack.push(0);

        while (true) {
            int state = stateStack.peek();
            String token = wordList.get(position);
            String action = getAction(state, token);

            if (action == null) {
                throw new RuntimeException("Syntax error at position " + position);
            } else if (action.startsWith("s")) {
                // Shift
                int nextState = Integer.parseInt(action.substring(1));
                stateStack.push(nextState);
                symbolStack.push(token);
                position++;
            } else if (action.startsWith("r")) {
                // Reduce
                int ruleIndex = Integer.parseInt(action.substring(1)) - 1;
                int ruleLength = rules.get(ruleIndex).getRight().length;
                for (int i = 0; i < ruleLength; i++) {
                    stateStack.pop();
                    symbolStack.pop();
                }
                String lhs = rules.get(ruleIndex).getLeft();
                symbolStack.push(lhs);
                int gotoState = getGoto(stateStack.peek(), lhs);
                stateStack.push(gotoState);
            } else if (action.equals("acc")) {
                System.out.println("Parsing successful!");
                return;
            }
        }
    }

    private String match(String expected) {
        if (position < wordList.size() && wordList.get(position).equals(expected)) {
            return wordList.get(position++);
        } else {
            throw new RuntimeException("Expected '" + expected + "' at position " + position);
        }
    }

    public static void main(String [] args) throws IOException{
        Parser parser = new Parser();
        parser.xmlToToken("output.xml");
        parser.loadParseTable("/home/auut1/Compiler_Construction_Project/SLR Parse Table.csv");
        // parser.parse();
        // parser.parsePROG();
        parser.parsePROG1();
    }
}

