import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ScopeAnalyzer{
    SymbolTable symbolTable = new SymbolTable();

    public void parseXMLFile(String XML){
        StringBuilder inputString = new StringBuilder();
        try {

            File inputFile = new File(XML);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
    
            Element root = doc.getDocumentElement();
            System.out.println("Root element :" + root.toString());
            Node prog = doc.getElementsByTagName("PARENT").item(0);

            for(int i = 0; i < prog.getChildNodes().getLength(); i++){
                System.out.println("Child Node: " + prog.getChildNodes().item(i).getNodeValue());
            }

            System.out.println("Root Node: " + prog.getNextSibling().getNodeValue());

            traverseSyntaxTree(prog);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void traverseSyntaxTree(Node node){
        Node rootChild = node.getChildNodes().item(0);
        System.out.println("Root Child: " + rootChild.getNodeName().toString());
        if(node.getNodeType() == Node.ELEMENT_NODE){

            org.w3c.dom.Element element = (org.w3c.dom.Element) node;
            String nodeName = element.getNodeValue();

            // System.out.println("Node Name: " + nodeName);

            if(nodeName.equals("main")){
                handleMain(element);
            }
            else if(nodeName.equals("GLOBVARS")){
                handleGLOBVARS(element);
            }
            else if (nodeName.equals("ALGO")){
                
            }
            else if(nodeName.equals("FUNCTIONS")){

            }
            
            // Node child = element.getElementsByTagName("CHILDREN").item(0);
            // NodeList children = child.getChildNodes();
            // //Need to move to the The CHILDREN node that holds the UNID of the value in the CHILD 
            // for(int i = 0; i < children.getLength(); i++){

            //     traverseSyntaxTree(children.item(i));
            // }
        }
    }

    private void handleMain(Element e){
        symbolTable.enterScope();
    }

    private void handleGLOBVARS(Element e){
         
    }

    public static void main(String [] args){
        ScopeAnalyzer scopeAnalyzer = new ScopeAnalyzer();
        scopeAnalyzer.parseXMLFile("syntax_tree.xml");
    }
}