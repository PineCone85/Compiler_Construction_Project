
public class Symbol{
    private String name;
    private String type;
    private int lvl;

    public Symbol(String name, String type/*, int lvl*/){
        this.name = name;
        this.type = type;
        // this.lvl = lvl;
    }


    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }

    // public int getLvl(){
    //     return lvl;
    // }
}