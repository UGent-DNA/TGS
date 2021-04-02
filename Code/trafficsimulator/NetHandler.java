package trafficsimulator;
import java.util.*;
import basicnetwork.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;


public class NetHandler extends DefaultHandler {

    private HashMap<String,Node> nodes = new HashMap<>();
    private HashMap<String,Edge> edges = new HashMap<>();
    private float total_length = 0.0f;  // The total length of roads in the network

    private Node node = null;
    private Edge edge = null;

    public Map<String,Node> getNodes() {
        System.out.println("# nodes = " +this.nodes.size());
        return this.nodes;
    }

    public Map<String,Edge> getEdges() {
        System.out.println("# edges = " + this.edges.size());
        return this.edges;
    }

    // return the total length of roads in the network
    public void printTotalLength(){
        System.out.println("Total road length = " +this.total_length + " m");
    }

    // Nodes are expected to precede the edges in the xml file!!!
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        String id = attributes.getValue("id");
        String type = attributes.getValue("type");
        if (qName.equalsIgnoreCase("node")) {
            if(!this.edges.isEmpty()){
                throw new IllegalArgumentException("Node " + id + " was behind some edges in the xml file");
            }
            node = new Junction(id, type);
        } else if (qName.equalsIgnoreCase("edge")) {
            Node from = nodes.get(attributes.getValue("from"));
            Node to = nodes.get(attributes.getValue("to"));
            if(from==null || to==null){
                throw new NullPointerException("The nodes that are connected by this edge are not yet loaded.");
            }
            float length = Float.parseFloat(attributes.getValue("length"));
            float speed = Float.parseFloat(attributes.getValue("speed"));
            int n_lanes = Integer.parseInt(attributes.getValue("lanes"));
            if (type=="highway.motorway" && n_lanes>2){
                n_lanes = n_lanes - 1;  // Trucks cannot use the leftmost lanes on highways
            }
            total_length += length;

            // edge = new Edge(id, nodes.get(from), nodes.get(to), length);
            edge = new Road(id, type, from, to, length, speed, n_lanes);
            from.addOutgoing(edge);
            to.addIncoming(edge);

            // update the capacity of the junctions at the up- and downstream end of the edge
            double cap = SimVars.tff/n_lanes;
            if (cap < ((Junction) from).getCap()){
                ((Junction) from).setCap(cap);
            }
            if (cap < ((Junction) to).getCap()){
                ((Junction) to).setCap(cap);
            }

        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {     
        if (qName.equalsIgnoreCase("node")) {
            // add node object to set
            nodes.put(node.getName(),node);
        }
        if (qName.equalsIgnoreCase("edge")) {
            // add edge object to set
            edges.put(edge.getName(),edge);
        }
    }

}



