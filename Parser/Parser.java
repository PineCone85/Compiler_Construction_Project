// package COMPILER_CONSTRUCTION_PROJECT.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.regex.*;

// import compiler_construction_project.lexer.Token; 
// import COMPILER_CONSTRUCTION_PROJECT.lexer.TokenType; 


public class Parser{

    private List<Grammar> rules = Arrays.asList(
        new Grammar("PROG", new String[] {"main","GLOBVARS", "ALGO", "FUNCTIONS"}),
        
        //GLOBVARS is also NULLABLE
        new Grammar("GLOBVARS", new String[] {""}),
        new Grammar("GLOBVARS", new String[] {"VTYP","VNAME", ",", "GLOBVARS"}),

        new Grammar("VTYP", new String[] {"num"}),
        new Grammar("VTYP", new String[] {"text"}),
        
        //tokenclassV is a nonterminal token from the lexer
        new Grammar("VNAME", new String[] {"tokenclassV"}),

        new Grammar("ALGO", new String[] {"begin", "INSTRUC", "end"}),
        
        //INSTRUC is also NULLABLE
        new Grammar("INSTRUC", new String[] {""}),
        new Grammar("INSTRUC", new String[] {"COMMAND", ";", "INSTRUC"}),

        new Grammar("COMMAND", new String[] {"skip"}),
        new Grammar("COMMAND", new String[] {"halt"}),
        new Grammar("COMMAND", new String[] {"print ATOMIC"}),
        new Grammar("COMMAND", new String[] {"ASSIGN"}),
        new Grammar("COMMAND", new String[] {"CALL"}),
        new Grammar("COMMAND", new String[] {"BRANCH"}),

        new Grammar("ATOMIC", new String[] {"VNAME"}),
        new Grammar("COMMAND", new String[] {"CONST"}),

        //tokenclassN is a nonterminal token from the lexer
        new Grammar("CONST", new String[] {"tokenclassN"}),
        //tokenclassT is a nonterminal token from the lexer
        new Grammar("CONST", new String[] {"tokenclassT"}),

        new Grammar("ASSIGN", new String[] {"VNAME","<input"}),
        new Grammar("ASSIGN", new String[] {"VNAME","=","TERM"}),

        new Grammar("CALL", new String[] {"FNAME","(","ATOMIC",",","ATOMIC",",","ATOMIC",")"}),

        new Grammar("BRANCH", new String[] {"if","COND","then","ALGO","else","ALGO"}),

        new Grammar("TERM", new String[] {"ATOMIC"}),
        new Grammar("TERM", new String[] {"CALL"}),
        new Grammar("TERM", new String[] {"OP"}),

        new Grammar("OP", new String[] {"UNOP","(","ARG",")"}),
        new Grammar("OP", new String[] {"BINOP","(","ARG",",","ARG",")"}),

        new Grammar("ARG", new String[] {"ATOMIC"}),
        new Grammar("ARG", new String[] {"OP"}),

        new Grammar("COND", new String[] {"SIMPLE"}),
        new Grammar("COND", new String[] {"COMPOSIT"}),

        new Grammar("SIMPLE", new String[] {"BINOP","(","ATOMIC",",","ATOMIC",")"}),
        new Grammar("COMPOSIT", new String[] {"BINOP","(","SIMPLE",",","SIMPLE",")"}),
        new Grammar("COMPOSIT", new String[] {"UNOP","(","SIMPLE",")"}),

        new Grammar("UNOP", new String[] {"not"}),
        new Grammar("UNOP", new String[] {"sqrt"}),

        new Grammar("BINOP", new String[] {"or"}),
        new Grammar("BINOP", new String[] {"and"}),
        new Grammar("BINOP", new String[] {"eq"}),
        new Grammar("BINOP", new String[] {"grt"}),
        new Grammar("BINOP", new String[] {"add"}),
        new Grammar("BINOP", new String[] {"sub"}),
        new Grammar("BINOP", new String[] {"mul"}),
        new Grammar("BINOP", new String[] {"div"}),

        //tokenclassF is a nonterminal token from the lexer
        new Grammar("FNAME", new String[] {"tokenclassF"}),

        new Grammar("FUNCTIONS", new String[] {""}),
        new Grammar("FUNCTIONS", new String[] {"DECL", "FUNCTIONS"}),

        new Grammar("DECL", new String[] {"HEADER","BODY"}),

        new Grammar("HEADER", new String[] {"FTYP","FNAME","(","VNAME",",","VNAME",",","VNAME",")"}),

        new Grammar("FTYP", new String[] {"num"}),
        new Grammar("FTYP", new String[] {"void"}),

        new Grammar("BODY", new String[] {"PROLOG", "LOCVARS", "ALGO", "EPILOG", "SUBFUNCS", "end"}),

        new Grammar("PROLOG", new String[] {"{"}),
        new Grammar("EPILOG", new String[] {"}"}),

        new Grammar("LOCVARS", new String[] {"VTYP","VNAME",",","VTYP","VNAME",",","VTYP","VNAME",","}),

        new Grammar("SUBFUNCS", new String[] {"FUNCTIONS"})
    );

    private List<Token> input;
    private Stack<String> stack = new Stack<>();
    private int position = 0;


}