package basicnetwork;
import java.util.*;


public class Dijkstra {
    // Class for the Dijkstra algorithm on a network, can be generalized.
    // A datstructure has to be added to keep track of the intermediate computed paths.

    private String name = UUID.randomUUID().toString();
    private Network net;
    // A map for saving intermediate computed trees from a root; map: root->(map: leaf->prev). Assumed to be complete
    private HashMap<Node,HashMap<Node,Node>> root_paths = new HashMap<Node,HashMap<Node,Node>>();
    // A map for saving intermediate computed trees to a leaf; map: root->(map: leaf->prev). Assumed to be complete
    private HashMap<Node,HashMap<Node,Node>> leaf_paths = new HashMap<Node,HashMap<Node,Node>>();

    public Dijkstra(Network net){
        this.net = net;
    }

    class NodeComparator implements Comparator<Node>{
        @Override
        public int compare(Node node1, Node node2) {
            if (node1.getPriority() < node2.getPriority()){
                return -1;
            } else if (node1.getPriority() > node2.getPriority()){
                return 1;
            } else {
                return 0;
            }
        }
    }

    class PriorityNodeComparator implements Comparator<PriorityNode>{
        @Override
        public int compare(PriorityNode node1, PriorityNode node2) {
            if (node1.getPriority() < node2.getPriority()){
                return -1;
            } else if (node1.getPriority() > node2.getPriority()){
                return 1;
            } else {
                return 0;
            }
        }
    }

    public Path getMinPath2(Node root, Node leaf){
        // Returns a path object which contains the cost and the edges of the path (with separate priority node)
        Set<Node> nodes = new HashSet<>(this.net.getNodes().values());   // Nodes of the network
        Set<Node> visited_nodes = new HashSet<Node>();
        Map<Node, Double> distance = new HashMap<Node, Double>();   // A map which contains the shortest distance to reach this node
        Map<Node, Node> prev = new HashMap<Node, Node>();   // A map which points to the previous node on the shortes path

        PriorityQueue<PriorityNode> pq = new PriorityQueue<PriorityNode>(new PriorityNodeComparator());  // The frontier of the search, a priority queue

        distance.put(root, 0.0d);
        pq.add(new PriorityNode(root, distance.get(root)));

        for(Node n: nodes){
            if(n!=root){
                distance.put(n, Double.POSITIVE_INFINITY);
            }
        }

        // Actual search
        while(!pq.isEmpty()){
            Node u = pq.remove().getNode();
            if(visited_nodes.contains(u)){
                continue;
            }
            visited_nodes.add(u);
            if(u==leaf){
                return this.constructPathFromPrev(root, leaf, prev);
            }
            for(Map.Entry<Node, Edge> entry: u.getOutgoing().entrySet()){
                Node v = entry.getKey();
                double prior = distance.get(u) + entry.getValue().getWeight();
                if(prior<distance.get(v)){
                    distance.put(v, prior);
                    pq.add(new PriorityNode(v, distance.get(v)));
                    prev.put(v, u);
                }
            }
        }
        // System.out.println("No path found from node "+root.getName()+" to "+leaf.getName());
        return null;
    }

    public Path getMinPath(Node root, Node leaf){
        // Returns a path object which contains the cost and the edges of the path, standard Dijkstra
        Set<Node> nodes = new HashSet<>(this.net.getNodes().values());
        Map<Node, Double> distance = new HashMap<Node, Double>();   // A map which contains the shortest distance to reach this node
        HashMap<Node,Node> prev = new HashMap<Node, Node>();   // A map which points to the previous node on the shortes path
        PriorityQueue<Node> pq = new PriorityQueue<Node>(new NodeComparator());  // The frontier of the search, a priority queue

        distance.put(root, 0.0d);
        root.setPriority(distance.get(root));
        pq.add(root);

        for(Node n: nodes){
            if(n!=root){
                distance.put(n, Double.POSITIVE_INFINITY);
            }
        }

        // Actual search
        while(!pq.isEmpty()){
            Node u = pq.remove();
            if(u==leaf){
                // this.root_paths.put(root,prev); 
                return this.constructPathFromPrev(root, leaf, prev);
            }
            for(Map.Entry<Node, Edge> entry: u.getOutgoing().entrySet()){
                Node v = entry.getKey();
                double prior = distance.get(u) + entry.getValue().getWeight();
                if(prior<distance.get(v)){
                    distance.put(v, prior);
                    v.setPriority(distance.get(v));       
                    pq.add(v);
                    prev.put(v, u);
                }
            }
        }

        // System.out.println("No path found from node "+root.getName()+" to "+leaf.getName());
        return null;       
    }

    private Path constructPathFromPrev(Node root, Node leaf, Map<Node,Node> prev){
        // Construct a path from the dict/map that point to the previous node
    	ArrayList<Edge> pathlist = new ArrayList<Edge>();
    	Node n = leaf;
        while(n!=root){
            pathlist.add(0, n.getEdgeFrom(prev.get(n)));
            n = prev.get(n);
        }
        return new Path(pathlist);
    }

    private Path constructPathFromNext(Node root, Node leaf, Map<Node,Node> next){
        // Construct a path from the dict/map that point to the Next node
    	ArrayList<Edge> pathlist = new ArrayList<Edge>();
    	Node n = root;
        while(n!=leaf){
            pathlist.add(n.getEdgeTo(next.get(n)));
            n = next.get(n);
        }
        return new Path(pathlist);
    }

    public Path getMinPathCheck(Node root, Node leaf){
        // return the shortest path, keeping the precomputed paths in mind
        if (root==leaf){
            return new Path(new ArrayList<Edge>());
        }
    	HashMap<Node,Node> prev = root_paths.get(root);
    	HashMap<Node,Node> next = leaf_paths.get(leaf);
    	if(prev!=null){
    		if (prev.containsKey(leaf)) {
    			return this.constructPathFromPrev(root, leaf, prev);
    		} else {
    			return null;	// No path exists
    		}
    	} else if(next!=null){
    		if (next.containsKey(root)){
    			return this.constructPathFromNext(root, leaf, next);
    		} else {
    			return null;	// No path exists
    		}
    	} else {
    		return this.getMinPath(root,leaf);
    	}
    }

    public Path getMinPathCheck(Edge root, Edge leaf){
        Path p = this.getMinPathCheck(root.getTo(), leaf.getFrom());
        if (p!=null) {
            p.extendBack(leaf);
            p.extendFront(root);
        }
        return p;
    }

    public void computeRootPaths(Node root){
        Set<Node> nodes = new HashSet<>(this.net.getNodes().values());
        Map<Node, Double> distance = new HashMap<Node, Double>();   // A map which contains the shortest distance to reach this node
        HashMap<Node,Node> prev = new HashMap<Node, Node>();   // A map which points to the previous node on the shortes path
        PriorityQueue<Node> pq = new PriorityQueue<Node>(new NodeComparator());  // The frontier of the search, a priority queue
        Set<Node> visited_nodes = new HashSet<Node>();

        distance.put(root, 0.0d);
        root.setPriority(distance.get(root));
        pq.add(root);

        for(Node n: nodes){
            if(n!=root){
                distance.put(n, Double.POSITIVE_INFINITY);
            }
        }

        while(!pq.isEmpty()){
            Node u = pq.remove();
            if (visited_nodes.contains(u)){
                continue;
            }
            visited_nodes.add(u);
            for(Map.Entry<Node, Edge> entry: u.getOutgoing().entrySet()){
                Node v = entry.getKey();
                double prior = distance.get(u) + entry.getValue().getWeight();
                if(prior<distance.get(v)){
                    distance.put(v, prior);
                    v.setPriority(distance.get(v));       
                    pq.add(v);
                    prev.put(v, u);
                }
            }
        }
        this.root_paths.put(root,prev);
    }

    public void computeLeafPaths(Node leaf){
        Set<Node> nodes = new HashSet<>(this.net.getNodes().values());
        Map<Node, Double> distance = new HashMap<Node, Double>();   // A map which contains the shortest distance to reach this node
        HashMap<Node,Node> next = new HashMap<Node, Node>();	// A map which points to the next node on the shortes path
        PriorityQueue<Node> pq = new PriorityQueue<Node>(new NodeComparator());  // The frontier of the search, a priority queue
        Set<Node> visited_nodes = new HashSet<Node>();

        distance.put(leaf, 0.0d);
        leaf.setPriority(distance.get(leaf));
        pq.add(leaf);

        for(Node n: nodes){
            if(n!=leaf){
                distance.put(n, Double.POSITIVE_INFINITY);
            }
        }

        while(!pq.isEmpty()){
            Node u = pq.remove();
            if (visited_nodes.contains(u)){
                continue;
            }
            visited_nodes.add(u);
            for(Map.Entry<Node, Edge> entry: u.getIncoming().entrySet()){
                Node v = entry.getKey();
                double prior = distance.get(u) + entry.getValue().getWeight();
                if(prior<distance.get(v)){
                    distance.put(v, prior);
                    v.setPriority(distance.get(v));       
                    pq.add(v);
                    next.put(v, u);
                }
            }
        }
        this.leaf_paths.put(leaf,next);
    }

    public void checkConnectivity(Node ref){
        // Checks if the network is connected, prints non-connected nodes
        this.computeLeafPaths(ref);
        this.computeRootPaths(ref);
        HashSet<Node> non_connected_nodes = new HashSet<Node>(this.net.getNodes().values());
        Set<Node> good_nodes = this.root_paths.get(ref).keySet();
        good_nodes.retainAll(this.leaf_paths.get(ref).keySet());
        non_connected_nodes.removeAll(good_nodes);
        for (Node n : non_connected_nodes){
            System.out.println(n.getName());
        }
        this.root_paths.remove(ref);
        this.leaf_paths.remove(ref);
    }

    public void clearDijkstraTrees(){
        this.leaf_paths.clear();
        this.root_paths.clear();
    }

    public Edge getClosestLeaf(Edge roote, Set<Edge> leafs){
        Set<Node> nodes = new HashSet<>(this.net.getNodes().values());
        Map<Node, Double> distance = new HashMap<Node, Double>();   // A map which contains the shortest distance to reach this node
        PriorityQueue<Node> pq = new PriorityQueue<Node>(new NodeComparator());  // The frontier of the search, a priority queue

        Node root = roote.getTo();

        distance.put(root, 0.0d);
        root.setPriority(distance.get(root));
        pq.add(root);

        for(Node n: nodes){
            if(n!=root){
                distance.put(n, Double.POSITIVE_INFINITY);
            }
        }

        while(!pq.isEmpty()){
            Node u = pq.remove();

            for(Map.Entry<Node, Edge> entry: u.getOutgoing().entrySet()){
                Node v = entry.getKey();
                double prior = distance.get(u) + entry.getValue().getWeight();

                if (leafs.contains(entry.getValue())){
                    return entry.getValue();
                }

                if(prior<distance.get(v)){
                    distance.put(v, prior);
                    v.setPriority(distance.get(v));       
                    pq.add(v);
                }
            }
        }
        return null;
    }

    public Edge getClosestRoot(Edge leafe, Set<Edge> roots){
        Set<Node> nodes = new HashSet<>(this.net.getNodes().values());
        Map<Node, Double> distance = new HashMap<Node, Double>();   // A map which contains the shortest distance to reach this node
        PriorityQueue<Node> pq = new PriorityQueue<Node>(new NodeComparator());  // The frontier of the search, a priority queue

        Node leaf = leafe.getFrom();

        distance.put(leaf, 0.0d);
        leaf.setPriority(distance.get(leaf));
        pq.add(leaf);

        for(Node n: nodes){
            if(n!=leaf){
                distance.put(n, Double.POSITIVE_INFINITY);
            }
        }

        while(!pq.isEmpty()){
            Node u = pq.remove();

            for(Map.Entry<Node, Edge> entry: u.getIncoming().entrySet()){
                Node v = entry.getKey();
                double prior = distance.get(u) + entry.getValue().getWeight();

                if (roots.contains(entry.getValue())){
                    return entry.getValue();
                }

                if(prior<distance.get(v)){
                    distance.put(v, prior);
                    v.setPriority(distance.get(v));       
                    pq.add(v);
                }
            }
        }
        return null;
    }

}




