import java.util.ArrayList;           
import java.util.List;              


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