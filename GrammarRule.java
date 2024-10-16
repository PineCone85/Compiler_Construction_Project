
public class GrammarRule {
    String left;
    String[] right;

    public GrammarRule(String l, String[] r){
        this.left = l;
        this.right = r;
    }

    public String[] getRight(){
        return right;
    }
    
     public String getLeft(){
        return left;
    }
}   