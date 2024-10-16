

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        // new GrammarRule("COMMAND", new String[] {"ASSIGN"}),
        new GrammarRule("COMMAND", new String[] {"CALL"}),
        new GrammarRule("COMMAND", new String[] {"BRANCH"}),

        // new GrammarRule("ATOMIC", new String[] {"VNAME"}),
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

        // new GrammarRule("ARG", new String[] {"ATOMIC"}),
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
    private int position = 0;

    private List<String> wordList = new ArrayList<>();
    private List<String> classList = new ArrayList<>();

    public Parser(){
    }

    private void parseGLOBALVARS(){

    }

    private void parseINSTRUC(){

    }

    private void parseFUNCTIONS(){

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


    public static void main(String [] args){
        Parser parser = new Parser();
        parser.xmlToToken("output.xml");
        parser.parse();
    }
}