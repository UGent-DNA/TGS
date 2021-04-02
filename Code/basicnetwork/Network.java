package basicnetwork;
import java.util.*;


public class Network {
	// A class for graphs/networks
	
	private Map<String, Edge> edges = new HashMap<String, Edge>();
	private Map<String, Node> nodes = new HashMap<String, Node>();


	public Network(){
	}

	/**  
	* Create a network from a given (square!) adjacency matrix A.
	* The matrix will be very sparse for a road network, so the network could be constructed more efficiently.
	*/
	public Network(float[][] A){
		Map<Integer, Node> index_to_node = new HashMap<Integer, Node>(); 

		// Adding the nodes to the network (one for each row of the adjacency matrix)
		for (int i = 0; i < A.length; i++) {
			Node new_node = new Node(null);
			this.nodes.put(new_node.getName(), new_node);
			index_to_node.put(i, new_node);
		}

		for(int i=0; i<A.length; i++) {
			for(int j=0; j<A[0].length; j++) {
				if (A[i][j] != 0.0){
					addEdge(index_to_node.get(i), index_to_node.get(j), A[i][j]);
				}
			}
		}
	}

	// Add a new node to the network.
	public void addNode(Node new_node){
		if(this.nodes.containsKey(new_node.getName())){
			throw new IllegalArgumentException("Node is already in the network.");
		} else {
			this.nodes.put(new_node.getName(), new_node);
		}
	}

	// Add an edge between two existing nodes of the network.
	public void addEdge(Node from, Node to, float weight){
		if (!this.nodes.containsKey(from.getName()) || !this.nodes.containsKey(to.getName())){
			throw new IllegalArgumentException("One of the nodes is not included in the network.");
		} else {
			Edge new_edge = new Edge(null, from, to, weight);
			this.edges.put(new_edge.getName(), new_edge);
			from.addOutgoing(new_edge);
			to.addIncoming(new_edge);
		}
	}

	public Map<String, Node> getNodes(){
		return this.nodes;
	}

	public Map<String, Edge> getEdges(){
		return this.edges;
	}

	public void setNodes(Map<String, Node> nodes){
		this.nodes = nodes;
	}

	public void setEdges(Map<String, Edge> edges){
		this.edges = edges;
	}

	public Node getNode(String name){
		// return a node with the given name
		return this.nodes.get(name);
	}

	public Edge getEdge(String name){
		// return an edge with the given name
		return this.edges.get(name);
	}

	public Node getRandomNode(){
		// Returns a random node from the network
		int item = new Random().nextInt(this.nodes.size());
		int i = 0;
		for(Node node: this.nodes.values()) {
		    if (i == item){
		        return node;
		    }
		    i++;
		}
		return null;
	}

	public Edge getRandomEdge(){
		int item = new Random().nextInt(this.edges.size());
		int i = 0;
		for(Edge edge: this.edges.values()) {
		    if (i == item){
		        return edge;
		    }
		    i++;
		}
		return null;
	}

	// Some sanity checks on a created network
	public void testNetwork(){
		boolean nodes_check = true;
		boolean edges_check = true;
		int bads = 0;

		System.out.println("Checking nodes...");
		for(Node n: this.nodes.values()){
			// incomming edges
			for(Edge e: n.getIncomingEdges()){
				nodes_check &= (e.getTo()==n)&&(this.edges.values().contains(e));
				nodes_check &= (e.getFrom().getOutgoing().containsKey(n));
			}
			// outgoing edges
			for(Edge e: n.getOutgoingEdges()){
				nodes_check &= (e.getFrom()==n)&&(this.edges.values().contains(e));
				nodes_check &= (e.getTo().getIncoming().containsKey(n));
			}
		}

		System.out.println("Checking edges...");
		for(Edge e: this.edges.values()){
			edges_check &= (this.nodes.values().contains(e.getFrom()))&&(this.nodes.values().contains(e.getTo()));
			edges_check &= (e.getFrom().getOutgoingEdges().contains(e)&&e.getTo().getIncomingEdges().contains(e));
			if (!e.getFrom().getOutgoingEdges().contains(e)){
				System.out.println("Node " + e.getFrom().getName() + " does not point to edge " + e.getName());
				bads++;
			}
			if (!e.getTo().getIncomingEdges().contains(e)){
				System.out.println("Node " + e.getTo().getName() + " does not point to edge " + e.getName());
				bads++;
			}
		}

		System.out.println("Check nodes: "+nodes_check);
		System.out.println("Check edges: "+edges_check);
		System.out.println("Bad refs: "+bads);
	}


}




















