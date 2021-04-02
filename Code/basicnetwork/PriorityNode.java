package basicnetwork;


public class PriorityNode {

	private Node node;
	private double priority;

	public PriorityNode(Node node, double priority){
		this.node = node;
		this.priority = priority;
	}

	public double getPriority(){
		return this.priority;
	}

	public Node getNode(){
		return this.node;
	}

}