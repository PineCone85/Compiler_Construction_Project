import org.w3c.dom.*;                
import javax.xml.parsers.*;           
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;           
import java.util.List;              
import java.util.HashMap;             
import java.util.Map;                 
import java.util.UUID;

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