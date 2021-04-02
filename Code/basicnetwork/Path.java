package basicnetwork;
import java.util.*;
import java.util.stream.*; 

public class Path {
        // A class for paths in a network, used by Dijkstra. Contains the edges which form the path
        private double cost = 0;
        private ArrayList<Edge> edges;

        public Path(ArrayList<Edge> edges){            
            this.edges = edges;
            
            for (Edge e : edges){
                this.cost += e.getWeight();
            }

        }

        public Path(Node start, int len){
            // construct a random path
            ArrayList<Edge> pathlist = new ArrayList<Edge>();
            for(int i=0; i<len; i++){
                Set<Edge> outs = new HashSet<Edge>(start.getOutgoingEdges());
                outs.removeAll(pathlist);
                if(outs.size()>0){
                    int item = new Random().nextInt(outs.size());
                    int j = 0;
                    for(Edge e: outs){
                        if(j==item){
                            pathlist.add(e);
                            start = e.getTo();
                            this.cost += e.getWeight();
                            break;
                        }
                        j++;
                    }
                } else {
                    break;
                }   
            }
            this.edges = pathlist;
        }

        @Override
        public String toString() { 
            String s = "";
            for(Edge e: this.edges){
                s += e.getName() + ", ";
            }
            return s;
        }

        public double getCost(){
            return this.cost;
        }

        public int size(){
            return this.edges.size();
        }

        public ArrayList<Edge> getEdges(){
            return this.edges;
        }

        public Edge getFront(){
            return this.edges.get(0);
        }

        public Edge getBack(){
            return this.edges.get(this.edges.size()-1);
        }

        public void extendBack(Edge e){
            // check for U-truns
            if (this.edges.size()==0 || !e.equalsReverse(this.getBack())){
                this.edges.add(e);
                this.cost += e.getWeight();
            }
        }

        public void extendFront(Edge e){
            // check for U-truns
            if (this.edges.size()==0 || !e.equalsReverse(this.getFront())){
                this.edges.add(0,e);
                this.cost += e.getWeight();
            }
        }

        public Path add(Path path){
            // append path to this Path
            if(this.getBack().getTo() == path.getFront().getFrom()){
                this.edges.addAll(path.getEdges());
                this.cost += path.getCost();
                return this;
            } else {
                throw new IllegalArgumentException("Paths can't be added as the end of the first is not connected to the end of the second.");
            }
        }

    }