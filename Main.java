

public class Main {

    public static void main(String[] args) {

        //========================================================================================================================================================================================
        // 1. Lexing
        Lexer lexer = new Lexer();
        lexer.Lex();
        //========================================================================================================================================================================================
        // 2. Parsing
        SLRParser parser = new SLRParser();
        parser.SLRParsing();
        Parser parserSynTree = new Parser();
        parserSynTree.parse();;
        //========================================================================================================================================================================================
        // 3. Scope Analysis and Type checking
        ScopeAnalyzer analyzer = new ScopeAnalyzer();
        analyzer.scopeAndTypeCheck();
    }
}
