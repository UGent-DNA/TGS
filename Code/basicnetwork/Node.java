package basicnetwork;
import java.util.*;


public class Node {
    // A class for nodes in a directed graph, has a set for the incoming edges and a set for the outgoing edges.
    // Incoming and outgoing edges are not included in the constructor and have to be added afterwards with addIncoming and addOutgoing.
    // Also has an attribute priority, which is used in the Dijkstra routing algorithm.

    private String name;
    private Map<Node,Edge> incoming = new HashMap<Node,Edge>(); // Map containing the incoming neighbour nodes and the corresponding edge
    private Map<Node,Edge> outgoing = new HashMap<Node,Edge>(); // Map containing the outgoing neighbour nodes and the corresponding edge
    private double priority;

    public Node(String name) {
        if(name==null){
            this.name = UUID.randomUUID().toString();
        } else {
            this.name = name;
        }
    }

    public String getName(){
        return this.name;
    }
    
    public Map<Node,Edge> getIncoming(){
        return this.incoming;
    }

    public Map<Node,Edge> getOutgoing(){
        return this.outgoing;
    }

    public Edge getEdgeTo(Node outnode){
        // Return the edge that connects this node with outnode
        if (this.outgoing.keySet().contains(outnode)){
            return this.outgoing.get(outnode);
        } else {
            throw new IllegalArgumentException("Cannot return edge, outnode is not a neighbour of this node.");
        }
    }

    public Edge getEdgeFrom(Node innode){
        // Return the edge that connects innode with this node
        if (this.incoming.keySet().contains(innode)){
            return this.incoming.get(innode);
        } else {
            throw new IllegalArgumentException("Cannot return edge, innode is not a neighbour of this node.");
        }
    }

    public Set<Edge> getIncomingEdges(){
        return new HashSet<Edge>(this.incoming.values());
    }

    public Set<Edge> getOutgoingEdges(){
        return new HashSet<Edge>(this.outgoing.values());
    }

    public void addIncoming(Edge in){
        if (this.incoming.containsKey(in.getFrom())){
            throw new IllegalArgumentException("Node already has an incoming edge that is parallel to the given one");
        } else if (this.incoming.values().contains(in)){
            throw new IllegalArgumentException("Node already has this edge as incoming.");
        } else {
            this.incoming.put(in.getFrom(), in);
        }
    }

    public void addOutgoing(Edge out){
        if (this.outgoing.containsKey(out.getTo())){
            throw new IllegalArgumentException("Node already has an outgoing edge that is parallel to the given one");
        } else if (this.outgoing.values().contains(out)){
            throw new IllegalArgumentException("Node already has this edge as outgoing.");
        } else {
            this.outgoing.put(out.getTo(), out);
        }
    }

    public int getInDegree(){
        return this.incoming.size();
    }

    public int getOutDegree(){
        return this.outgoing.size();
    }
    
    public double getPriority(){
        return this.priority;
    }

    public void setPriority(double priority){
        this.priority = priority;
    }

}












