package trafficsimulator;
import java.util.*;
import basicnetwork.*;


public class Vehicle implements Comparable<Vehicle>{
	// A class for vehicles in the traffic simulation (not yet 'vehicles' in the TGS itself)

	private String name;
	private double t_min = 0.0d;     // Minimum time the vehicle can exit its current queue in the simulation
	private double t_exit = 0.0d;    // The time at which a vehicle can exit its current queue
	private LinkedList<Road> route;  // Contains the current route the vehicle has 
									 // (the first edge is the vehicles current edge, the last is its destination)

	private double t_enter_sim;
	private double t_exit_sim = -1.0d;

	public Vehicle(Path path, double t_enter_sim){
		this(UUID.randomUUID().toString(), path, t_enter_sim);
	}

	public Vehicle(String name, Path path, double t_enter_sim){
		this.name = name;
		@SuppressWarnings("unchecked")
		ArrayList<Road> roads = (ArrayList) path.getEdges();
		this.route = new LinkedList<Road>(roads);
		this.t_enter_sim = t_enter_sim;
	}

	public int compareTo(Vehicle veh){    
		if (this.t_exit < veh.getTExit()){
			return -1;
		} else if (this.t_exit > veh.getTExit()){
			return 1;
		} else {
			return 0;
		}
	}

	public String getName(){
		return this.name;
	}

	public void setTExitSim(double tex){
		this.t_exit_sim = tex;
	}

	public double getTInSim(){
		return (this.t_exit_sim - this.t_enter_sim);
	}

	public Road getCurrentEdge(){
		return this.route.peek();
	}

	public Road getNextEdge(){
		return this.route.get(1);
	}

	public Road getDestination(){
		// return the last edge of the vehicles route
		return this.route.getLast();
	}

	public void advanceRoute(){
		this.route.remove();
	}

	public int getRouteLength(){
		// return the number of edges in the vehicles remaining route
		return this.route.size();
	}

	public double getTMin(){
		return this.t_min;
	}

	public void setTMin(double t_min){
		this.t_min = t_min;
	}

	public double getTExit(){
		return this.t_exit;
	}

	public void setTExit(double t){
		this.t_exit = t;
	}

	public boolean isAtFront(){
		return(this.getCurrentEdge().getFront()==this);
	}

	public void extendRoute(Edge[] extra_edges){
		for(Edge e: extra_edges){
			if(this.getDestination().getOutRoads().contains(e)){
				this.route.add((Road) e);
			}else{
				throw new IllegalArgumentException("The extension of the route must be connected to the current route.");
			}
			
		}
	}

	public boolean arrived(){
		return (this.route.size()==1);
	}
}




