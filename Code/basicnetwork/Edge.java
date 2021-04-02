package basicnetwork;
import java.util.*;


public class Edge {
    // A class for (directed) edges in a network, fairly straightforward.

    private String name;
    private float weight;
    private Node from;
    private Node to;

    public Edge(String name, Node from, Node to, float weight) {
        if(name==null){
            this.name = UUID.randomUUID().toString();
        } else {
            this.name = name;
        }
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public String getName(){
        return this.name;
    }

    public Node getFrom(){
        return this.from;
    }

    public Node getTo(){
        return this.to;
    }

    public float getWeight(){
        return this.weight;
    }

    public void setWeight(float weight){
        if (weight>0.0){
            this.weight = weight;
        } else {
            throw new IllegalArgumentException("Edge weights must be positive");
        }
    }

    public Edge getReverse() {
        if (this.getTo().getOutgoing().containsKey(this.getFrom())){
            return this.getTo().getOutgoing().get(this.getFrom());
        } else {
            return null;
        }
    }
    
    public boolean equalsReverse(Edge e) {
        return (e.getTo() == this.getFrom() && e.getFrom() == this.getTo());
    }
}







