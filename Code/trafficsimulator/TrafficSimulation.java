package trafficsimulator;
import java.util.*;
import java.io.FileWriter;
import basicnetwork.*;


public class TrafficSimulation {
	// A mesoscopic traffic model based on queueing theory (from the work of Nils Gustaf Eissfeldt)
	// Considering the mu3-Queue and using the event-driven update scheme.

	private TrafficNetwork net;
	private HashMap<String,Vehicle> vehicles = new HashMap<String,Vehicle>();		// vehicles in the simulation
	private PriorityQueue<Vehicle> priority_vehicles = new PriorityQueue<Vehicle>();	// the priority queue for the event-based update scheme

	private double t = 0.0d;    // Time of the simulation (last event t)
	private int step; 			// The simulation steps

	private Vehicle last_hopper = null;		// The vehicle that made a move last
	private Edge last_hopper_edge = null;	// The edge last_hopper moved to

	private String output_folder;

	public TrafficSimulation(TrafficNetwork net, String output) {
		this.net = net;
		this.output_folder = output;
	}

	public TrafficNetwork getNetwork() {
		return this.net;
	}

	public double getTime() {
		return this.t;
	}

	public void setTime(double t){
		if (t>=this.t) {
			this.t = t;
		} else {
			throw new IllegalArgumentException("Time can only increase (" + this.t + " -> " + t + ")");
		}
	}

	public double getNextTime(){
		if (this.priority_vehicles.isEmpty()){
			return Double.POSITIVE_INFINITY;
		} else {
			return this.priority_vehicles.peek().getTExit();
		}
	}

	public int getStep(){
		return this.step;
	}

	public Vehicle getLastMover(){
		return this.last_hopper;
	}

	public Edge getLastMoverEdge(){
		return this.last_hopper_edge;
	}

	public Set<Vehicle> getVehicles(){
		return new HashSet<Vehicle>(this.vehicles.values());
	}

	public int getNumberOfVehicles(){
		return this.vehicles.size();
	}

	public PriorityQueue<Vehicle> getPriorityVehicles() {
		return this.priority_vehicles;
	}

	public boolean priorityQueueEmpty(){
		return this.priority_vehicles.isEmpty();
	}

	public void addVehicleRoute(String name, Path path) {
		// Add a new vehicle with a certain path/route to the simulation
		Road origin = (Road) path.getEdges().get(0);
		Vehicle new_vehicle = new Vehicle(name, path, this.t);
		if(this.vehicles.containsKey(name)){
			throw new IllegalArgumentException("A vehicle with ID: " + name + " is already in the simulation.");
		}
		this.vehicles.put(name,new_vehicle);

		if (!origin.isFull() && !origin.isCritical()) {
			new_vehicle.setTMin(this.t + origin.getTtr());
			origin.addVehicle(new_vehicle, this.t);
			if (origin.getOccupancy() == 1) {
				this.putInExitQueue(new_vehicle, false);    // If origin was empty, the vehicle becomes the front of that road
			}

			Set<Road> update_roads = origin.getInRoads();
			if (origin.isCritical()) {
				update_roads.addAll(origin.getCyclesInRoads());
			}
			this.updateFronts(update_roads);    // update the fronts (densities changed)
		} else {
			origin.addDelayed(new_vehicle);        // If the roads queue is full, add it to the delayed queue
		}
	}

	public void addVehicleRoute(Path path) {
		// Add a new vehicle with a certain path/route to the simulation
		this.addVehicleRoute(UUID.randomUUID().toString(), path);
	}

	private double f(Road j){
		int nj = j.getOccupancy();
		int nj_jam = j.getJamOccupancy();
		if (nj < nj_jam) {
			return SimVars.tjf;
		} else {
			return SimVars.tjj*nj + j.getCapacity()*(SimVars.tjf - SimVars.tjj); 
		}		
	}

	private double getTHead(Road i, Road j) {
		// Return the parameter tau_i, but lets call it tau_ij, as it depends on the current edge
		// and the edge the vehicle is moving to (at junctions, there are more than 1 outgoing edges).
		if (i.getOutRoads().contains(j)) {
			int ni = i.getOccupancy();
			int ni_jam = i.getJamOccupancy();    // Jamming density must be greater than 0
			// Rescale the time-headways by the number of lanes of the edge with the least lanes
			int q = 1;

			if(i.getType()=="highway.motorway" && j.getType()=="highway.motorway_link"){ // an off-ramp
				return SimVars.tff / q;
			}

			if (ni < ni_jam) {
				return SimVars.tff / q;
			} else {
				return this.f(j) / q;
			}
		} else {
			throw new IllegalArgumentException("The next edge has to be an outgoing edge of the previous one.");
		}
	}


	private void putInExitQueue(Vehicle veh, boolean remove) {
		// Compute the time at which a vehicle should hop to its next edge (veh should be at the front
		// of its current edge), then add the vehicle to the priority queue of the simulation (if it is allowed to move).
		if (remove) {
			this.priority_vehicles.remove(veh);   // have to do this to update the java priority queue (is O(N))
		}
		Road i = veh.getCurrentEdge();
		Road j = veh.getNextEdge();
		int li = i.getNLanes();
		int lj = j.getNLanes();
		if (!j.isFull() && (!j.isCritical() || j.getComplementCycles(i).isEmpty())) {    // moves inside a critical cycle are allowed
			if (i.getFront() == veh) {
				double t_ij = this.getTHead(i, j);
				double t_hop = Math.max(veh.getTMin(), i.getLastExitT()+(t_ij/li));	// exit conditions
				t_hop = Math.max(t_hop, j.getLastEnterT()+(t_ij/lj));	// enter conditions
				t_hop = Math.max(t_hop, i.getTo().getTLast() + i.getTo().getCap()/Math.max(li,lj));	// capacity constraint of junction
				t_hop = Math.max(t_hop, this.t);	// increasing time condition
				veh.setTExit(t_hop);
				this.priority_vehicles.add(veh);
			} else {
				throw new IllegalArgumentException("Only vehicles at the front of their current queue are allowed to exit.");
			}
		}
	}

	private void updateFronts(Set<Road> roads) {
		// For a set of roads, update the exit times of the vehicles at the fronts of the queues
		// and update the priority queue. To be used when the densities of the these edges or
		// the next edges changed
		for (Road r : roads) {
			if (r.isOccupied()) {
				this.putInExitQueue(r.getFront(), true);
			}
		}
	}

	private void addDelayedVehicle(Road i) {
		if (i.hasDelayedAdd() && !i.isFull() && !i.isCritical()) {
			Vehicle delayed_add = i.getNextDelayed(this.t);    // the delayed vehicle is added to i's queue
			delayed_add.setTMin(this.t + i.getTtr());
			if (i.getOccupancy() == 1) {        // only important for roads of capacity 1
				this.putInExitQueue(delayed_add, false);
			}
			Set<Road> update_roads = i.getInRoads();
			// check if i became critical
			if(i.isCritical()){
				update_roads.addAll(i.getCyclesInRoads());
			}
			this.updateFronts(update_roads);
		}
	}

	public String checkArrival(Vehicle veh){
		// remove arrived vehicles
		if (veh.arrived()) {
			veh.getCurrentEdge().removeBack();
			this.vehicles.remove(veh.getName());
			veh.setTExitSim(this.t);

			try {
				FileWriter writer = new FileWriter(this.output_folder+"Arrivals.txt", true);
				writer.write(System.lineSeparator());
				writer.write(this.t + System.lineSeparator());
				writer.close();
			} catch (Exception e) {
				System.out.println(e);
			}

			return veh.getName();
		}
		return null;
	}

	public boolean simStep() {
		// Move the next vehicle (smallest t in the priority queue of the simulation).
		// Return false if the priority queue is emtpy
		this.step++;
		String arrived = null;
		if (!this.priority_vehicles.isEmpty()) {
			Vehicle hopper = this.priority_vehicles.remove();
			Road i = hopper.getCurrentEdge();
			Road j = hopper.getNextEdge();

			if (!j.isFull() && (!j.isCritical() || j.getComplementCycles(i).isEmpty())) {
				this.last_hopper = hopper;
				this.last_hopper_edge = j;
				i.removeFront();
				hopper.advanceRoute();      	// remove the old edge from the vehicles route queue
				this.setTime(hopper.getTExit());

				hopper.setTMin(this.t + j.getTtr());
				j.addVehicle(hopper, this.t);
				arrived = this.checkArrival(hopper);	// if j is the destination of hopper, remove hopper from j

				i.getTo().setTLast(this.t);		// update the junction crossing time

				// Update the time-headways (tau_ij's) of the vehicles at the fronts of the edges
				// that come in in edge i and edge j (the occupancies changed and thus tau_ij).
				Set<Road> update_roads = i.getInRoads();	// All roads that need their fronts to be updated
				update_roads.addAll(j.getInRoads());
				update_roads.add(j);    // If the new edge j was empty before, hopper becomes the new front of this edge

				// if this move caused hopper to exit its current critical cycle, the cycle can be cleared (no longer critical)
				// and the fronts of the incoming roads upon this cycle can be updated
				Set<Cycle> non_cycles = new HashSet<>();
				if (arrived != null){
					non_cycles.addAll(i.getCycles());
				}else{	
					non_cycles.addAll(i.getComplementCycles(j));
				}
				for(Cycle c : non_cycles){
					update_roads.addAll(c.getInRoads());
					c.clear();
				}

				// Check if j became critical due to this move and update the incomming edges upon the created critical cycle
				if(j.isCritical()){
					update_roads.addAll(j.getCyclesInRoads());
				}

				this.updateFronts(update_roads);
				this.addDelayedVehicle(i);    // a spot came free on edge i, so a delayed vehicle add can now be carried out
			}
			return true;
		}
		return false;
	}

	public void updateEdgeWeights(){
		for (Edge e: this.net.getEdges().values()){
			Road r = (Road) e;
			r.setWeight(r.getEMAWeight(this.t));
		}
	}

	public void writeDensities(){
		try {
			// if step == 0, the existing file is cleared or a new one is created
			FileWriter writer = new FileWriter(this.output_folder+"Densities.txt", (this.step!=0));
			writer.write(System.lineSeparator());
			writer.write("step: " + this.step + ", time: " + this.t + ", trucks: "
								 + this.vehicles.size() + System.lineSeparator());
			HashSet<Road> occ_edges = new HashSet<Road>();
			for (Vehicle v: this.vehicles.values()) {
				Road r = v.getCurrentEdge();
				if (!occ_edges.contains(r)){
					occ_edges.add(r);
					writer.write("edge:" + r.getName() + ", " + Math.min(((float) r.getOccupancy())/r.getCapacity(),1.0f) 
									+ System.lineSeparator());
				}
			}
			writer.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void writeLocks(){
		// Write full edges and critical cycles to a file
		Set<Cycle> remaining_cycles = new HashSet<>();
		try {
			FileWriter writer = new FileWriter("/home/idlab126/Sumo/Antwerp/jammed_edges.txt");
			for (Vehicle v : this.vehicles.values()) {
				if (v.getCurrentEdge().isFull()) {
					writer.write("edge:" + v.getCurrentEdge().getName() + System.lineSeparator());
				}
				if (v.isAtFront()) {
					remaining_cycles.addAll(v.getNextEdge().getCycles());
				}
			}
			writer.close();
		} catch (Exception e) {
			System.out.println("writng locks failed");
		}

		System.out.println("cycles: " + remaining_cycles.size());
		for(Cycle c: remaining_cycles){
			System.out.println(c);
		}

		try {
			FileWriter writer = new FileWriter("/home/idlab126/Sumo/Antwerp/cycles.txt");
			for (Cycle c: remaining_cycles) {
				for (Road r : c.getRoads()){
					writer.write("edge:" + r.getName() + System.lineSeparator());
				}
			}
			writer.close();
		} catch (Exception e) {
			System.out.println("writng locks failed");
		}
	}

	public void clearSimulation() {
		// clear/reset the simulation
		this.priority_vehicles.clear();
		this.vehicles.clear();
		for (Edge e : net.getEdges().values()) {
			((Road) e).clear();
		}
		this.t = 0.0d;
		this.step = 0;
	}


	static public void main(String[] args) {
		TrafficNetwork net = new TrafficNetwork("/home/idlab126/Sumo/Antwerp/Java_osm.net.xml");
		TrafficSimulation sim = new TrafficSimulation(net, "/home/idlab126/Documents/IDLAB/TGS/Plots_and_data/");
		Dijkstra dijk = new Dijkstra(net);

		int nv = 7000;
		int added = 0;
		while (added < nv) {
			Node start = net.getRandomNode();
			Node stop = net.getRandomNode();
			Path path = dijk.getMinPath(start, stop);
			if ((path != null) && (path.getEdges().size() > 1)) {
				added++;
				sim.addVehicleRoute(path);
			}
		}


		double s = System.nanoTime();
		while (!sim.priority_vehicles.isEmpty()) {
			sim.simStep();
			if(sim.step%500==0){
				System.out.print("step: " + sim.step + ", time: " + ((int) sim.t) + ", hopper: "
							 + sim.getLastMover().getName() + "\r");
			}
			
		}
		double f = System.nanoTime();
		System.out.println("time: " + (f - s));
		System.out.println("Steps: " + sim.step);
		System.out.println("added vehicles: " + added);
		System.out.println("locked: " + sim.vehicles.size());

		sim.writeLocks();
		
	}

}


