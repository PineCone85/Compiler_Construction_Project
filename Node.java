import java.util.ArrayList;           
import java.util.List;              


class Node {
    String symb;
    String unid;
    List<Node> children;
    Node parent;  // Reference to the parent node

    // Constructor with parent node
    Node(String symb, String unid) {
        this.symb = symb;
        this.unid = unid;
        this.children = new ArrayList<>();
        this.parent = null;  // Initialize parent as null
    }

    // Method to add a child and set its parent
    void addChild(Node child) {
        child.parent = this;  // Set this node as the parent of the child
        this.children.add(child);
    }

    // Print the tree structure for debugging
    void printNode(String indent) {
        System.out.println(indent + "Symbol: " + symb + ", UNID: " + unid);
        for (Node child : children) {
            child.printNode(indent + "  ");
        }
    }
}
