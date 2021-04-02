package trafficsimulator;
import java.util.*;
import basicnetwork.*;


public class Road extends Edge {
	// A class for roads in a traffic network as an extension of basicnetwork.Edge.

	private String type;
	private final float length;
	private float speed;
	private int n_lanes;

	private float load = 0f;	// average external load of raod segment (0 to 1)

	private LinkedList<Vehicle> vehicles = new LinkedList<Vehicle>();   // The queue that contains the vehicles on the edge

	// Contains vehicles that could not be added immediately because the road was full, can be added later
	private Queue<Vehicle> delayed_adds = new LinkedList<Vehicle>();

	private int capacity;   // The number of vehicles the road can contain
	private int jam_occupancy;	// occupancy of jam transition
	private int occupancy = 0;    // The number of vehicles on the road
	private double t_last_enter = 0.0d;  // The last time a vehicle entered the edge/segment
	private double t_last_exit = 0.0d;   // The last time a vehicle left the edge/segment

	// parameters to compute exponential moving averages of the edge weight
	private float w_last;
	private static float mu = 600.0f;	// scale of exponential averaging (is s)

	private Set<Cycle> critical_cycles = new HashSet<Cycle>();	// Critical cycles this road is part of



	public Road(String name, String type, Node from, Node to, float length, float speed, int n_lanes) {
		super(name, from, to, length / Math.min(speed, SimVars.max_speed));    // Set the initial weight to the travel time of the empty road.
		this.type = type;
		this.length = length;
		this.speed = speed; // Math.min(speed, SimVars.max_speed);
		this.n_lanes = n_lanes;
		this.capacity = Math.max(1, ((int) (SimVars.max_density * length))) * n_lanes;    // Capacity should at least be 1, no matter how short the edge
		// this.jam_occupancy = Math.max(1, ((int) (SimVars.jam_density * this.length)) * this.n_lanes);
		float alpha = (SimVars.max_density-SimVars.jam_density)/SimVars.jam_density;
		this.jam_occupancy = Math.max(n_lanes,
							 (int) (this.capacity/(1.0f+alpha*Math.min(speed, SimVars.max_speed)/19.44f)));
		this.w_last = this.getWeight();
	}

	public Junction getTo(){
		return (Junction) super.getTo();
	}

	public Junction getFrom(){
		return (Junction) super.getFrom();
	}

	public String getType(){
		return this.type;
	}

	public float getLength() {
		
		return this.length;
	}

	public float getSpeed() {
		return this.speed;
	}

	public int getNLanes() {
		return this.n_lanes;
	}

	public int getCapacity() {
		return this.capacity;
	}

	public void setLoad(float load){
		this.load = load;
	}

	public float getLoad(){
		return this.load;
	}

	public int getJamOccupancy() {
		return this.jam_occupancy;
	}

	public Queue<Vehicle> getVehicles() {
		return this.vehicles;
	}

	public int getOccupancy() {
		// Return the amount of vehicles on this road/edge
		return this.occupancy;
	}

	public boolean isOccupied() {
		return !this.vehicles.isEmpty();
	}

	public Vehicle getFront() {
		// Return the first vehicle in the queue of this egde
		return this.vehicles.peek();
	}

	public void removeFront() {
		// Remove the first vehicle in the queue (because it has moved)

		double t = this.vehicles.remove().getTExit();
		this.occupancy -= 1;
		this.updateEMA(t);
		this.setLastExitT(t);
	}

	public void removeBack() {
		// Remove the last vehicle in the queue (because it has reached its destination)
		this.vehicles.removeLast();
		this.occupancy -= 1;
		this.updateEMA(this.t_last_enter);
	}

	public void addVehicle(Vehicle vehicle, double t) {
		if (this.vehicles.size() < this.capacity) {
			this.vehicles.add(vehicle);
			this.occupancy += 1;
			this.updateEMA(t);
			this.setLastEnterT(t);
		} else {
			throw new IndexOutOfBoundsException("Road occupation has reached its capacity.");
		}
	}

	private void updateEMA(double t_n){
		float w_n = (float) this.getTtr();
		int n = Math.min(this.occupancy, this.capacity-1);
		if (n > this.jam_occupancy){
			w_n *= n*(this.capacity-this.jam_occupancy)/(this.jam_occupancy*(this.capacity-n));
		}
		float a = (float) Math.exp(-1.0*(t_n - Math.max(this.getLastExitT(), this.getLastEnterT()))/mu);
		this.w_last = (1-a)*w_n + a*this.w_last;
	}

	public float getEMAWeight(double t){
		this.updateEMA(t);
		return this.w_last;
	}

	public void addDelayed(Vehicle vehicle) {
		this.delayed_adds.add(vehicle);
	}

	public boolean hasDelayedAdd() {
		return (!this.delayed_adds.isEmpty());
	}

	public Vehicle getNextDelayed(double t) {
		// move a delayed vehicle from the delayed queue to the active queue and return it
		Vehicle delayed = this.delayed_adds.remove();
		this.addVehicle(delayed, t);
		return delayed;
	}

	public double getLastEnterT() {
		return this.t_last_enter;
	}

	public void setLastEnterT(double t) {
		if (t >= this.t_last_enter) {
			this.t_last_enter = t;
		} else {
			throw new IllegalArgumentException("New last enter time has to be greater than the previous one.");
		}
	}

	public double getLastExitT() {
		return this.t_last_exit;
	}

	public void setLastExitT(double t) {
		if (t > this.t_last_exit) {
			this.t_last_exit = t;
		} else {
			throw new IllegalArgumentException("New last exit time has to be greater than the previous one.");
		}
	}

	public double getTtr() {
		// return the variable t_tr for this edge
		return (this.getLength()/(Math.min(this.speed, SimVars.max_speed) * 0.80d)); 	// the factor of 0.85 is to correct for acceleration etc
	}

	public Set<Road> getOutRoads() {
		// Return the edges that leave from the end-node of this edge
		Set<Road> out_roads = new HashSet<Road>();
		for (Edge e : this.getTo().getOutgoingEdges()) {
			out_roads.add((Road) e);
		}
		return out_roads;
	}

	public Set<Road> getInRoads() {
		// Return the edges that come in in the start-node of this edge
		Set<Road> in_roads = new HashSet<Road>();
		for (Edge e : this.getFrom().getIncomingEdges()) {
			in_roads.add((Road) e);
		}
		return in_roads;
	}

	@Override
	public Road getReverse() {
		return (Road) super.getReverse();
	}

	public boolean isFull() {
		return (this.getOccupancy() == this.capacity);
	}

	public boolean isQuasiFull() {
		// return true if there is exactly 1 place left in the queue/road
		return (this.getOccupancy() == (this.capacity - 1));
	}

	///////// Code for avoiding gridlocks by detecting critical cycles /////////

	public void findCriticalCycle(){
		// Do a depth first search to find a possible critical cylce of full edges and containing at most 1 quasi full edge
		if ((this.isQuasiFull() || this.isFull()) && this.critical_cycles.isEmpty()) {
			Stack<Road> stack = new Stack<>();
			Stack<HashSet<Road>> path = new Stack<>();
			Stack<Boolean> quasi_path = new Stack<>();

			stack.add(this);
			HashSet<Road> p0 = new HashSet<>();
			p0.add(this);
			path.add(p0);
			quasi_path.add(this.isQuasiFull());

			while (!stack.isEmpty()){
				Road r = stack.pop();
				HashSet<Road> p = path.pop();
				boolean quasi = quasi_path.pop();
				
				if (r.getOutRoads().contains(this)){
						new Cycle(p);
					continue;
				}
				for (Road rn : r.getOutRoads()) {
					if ((rn.isFull()||(rn.isQuasiFull() && !quasi)) && !p.contains(rn) && !p.contains(rn.getReverse())) {//
						stack.add(rn);
						HashSet<Road> pn = new HashSet<>(p);
						pn.add(rn);
						path.add(pn);
						quasi_path.add(quasi || rn.isQuasiFull());
					}
				}
			}
		}
	}

	public boolean isCritical() {
		// return true if this edge is part of a cycle that almost causes a gridlock. i.e. if this edge
		// is QuasiFull, then return true if there exists a closed loop with only full edges (except this edge itself).
		this.findCriticalCycle();

		if(!this.critical_cycles.isEmpty()){
			for(Cycle c: this.critical_cycles){
				if(!c.isCritical()){
					if(c.isLocked()){
						throw new IllegalStateException("A locked cycle appeared");
					}
					for(Road r: c.getRoads()){
						System.out.println(r.getName() + " " + r.getOccupancy() + "/" + r.getCapacity());
					}
					throw new IllegalStateException("A cycle that is not critical appeared");
				}
			}
		}
		return (!this.critical_cycles.isEmpty());
	}

	public Set<Cycle> getCycles() {
		if(this.critical_cycles.isEmpty()){
			this.findCriticalCycle();
		}
		return this.critical_cycles;
	}

	public Set<Road> getCyclesInRoads(){
		// return all roads that are incident upon the cycles this road is a part of
		HashSet<Road> ins = new HashSet<Road>();
		for(Cycle c: this.critical_cycles){
			ins.addAll(c.getInRoads());
		}
		return ins;
	}

	public void addCycle(Cycle cycle) {
		this.critical_cycles.add(cycle);
	}

	public void removeCycle(Cycle cycle){
		this.critical_cycles.remove(cycle);
	}

	public Set<Cycle> getComplementCycles(Road r){
		// return the cycles of this road that are not a cycle of road r
		Set<Cycle> complement = new HashSet<Cycle>(this.critical_cycles);
		complement.removeAll(r.getCycles());
		return complement;

	}

	/////////

	public boolean isCongested() {
		// returns true if this road is in a congested state (n >= njam)
		return (this.getOccupancy() >= Math.max((int) (SimVars.jam_density * this.length * this.n_lanes), 1));
	}

	public void clear() {
		// reset/clear this road
		this.critical_cycles.clear();
		this.vehicles.clear();
		this.delayed_adds.clear();
		this.t_last_exit = 0.0d;
		this.t_last_enter = 0.0d;
		this.occupancy = 0;
	}

}