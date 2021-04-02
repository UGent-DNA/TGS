import basicnetwork.*;
import trafficsimulator.*;
import java.util.*;
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;


public class TGS{
	// Main code to run the complete Truck Guidance System simulation

	private String input_folder;
	private String output_folder;

	private String id = UUID.randomUUID().toString();
	private HashMap<String,Truck> trucks = new HashMap<String,Truck>();	// map from truck id to truck
	private Set<TruckCompany> truck_companies = new HashSet<TruckCompany>();
	private HashMap<Edge,Terminal> terminals = new HashMap<>();	// Container terminals
	private Set<Edge> industrial_edges = new HashSet<Edge>();	// Edges/roads in an industrial zone
	private Set<ParkingLot> parkings = new HashSet<ParkingLot>();	// Parkings

	private TrafficNetwork net;	// The road network
	private Dijkstra dijkstra; 	// Contains all information concerning distances and shortest paths (~weightwise) in the network
	private TrafficSimulation traffic_sim;	// The traffic simulator
	private double t = 0.0d;		// The internal clock, has to be syncronised with the one from the traffic simulation

	private Random rand = new Random(); // random
	private PriorityQueue<BusyTruck> busy_trucks = new PriorityQueue<>();	// Contains trucks to be added into the traffic simulation
	private HashSet<Truck> busy_trucks_set = new HashSet<>();

	private MasterPlanner master_planner;
	private boolean global_optimization;	// True if the simulation should be ran using global optimization


	public TGS(String input, String output, boolean global_opt){
		this.input_folder = input;
		this.output_folder = output;

		this.net = new TrafficNetwork(this.input_folder+"JavaOsm.net.xml");
		this.dijkstra = new Dijkstra(this.net);
		this.traffic_sim = new TrafficSimulation(this.net, output);

		// this.addParkingsFromFile(this.input_folder+"parkings.add.xml");
		this.addTerminalsFromFile(this.input_folder+"terminals.add.xml");
		this.addIndustrialEdgesFromFile(this.input_folder+"industrialEdges.add.xml");
		this.updateRouting();

		this.global_optimization = global_opt;
		if (global_opt){
			System.out.println("Global optimization");
		} else {
			System.out.println("Local optimization");
		}
	}


	static private class BusyTruck implements Comparable<BusyTruck>{
		// For trucks not in the road network

		private Truck truck;
		private double exit_time;	// exit time of the truck, i.e. time when truck is back on the road

		public BusyTruck(Truck truck, double exit_time){
			this.truck = truck;
			this.exit_time = exit_time;
		}

		public int compareTo(BusyTruck truck){    
			if (this.exit_time < truck.getFinnishTime()){
				return -1;
			} else if (this.exit_time > truck.getFinnishTime()){
				return 1;
			} else {
				return 0;
			}
		}

		public Truck getTruck(){
			return this.truck;
		}

		public double getFinnishTime(){
			return this.exit_time;
		}
	}


	///////// Getters and setters /////////

	public String getId(){
		return this.id;
	}

	private void setTime(double t){
		if (t>=this.t) {
			this.t = t;
			this.traffic_sim.setTime(this.t);	// Update the traffic simulation time
		} else {
			throw new IllegalArgumentException("Time can only increase (" + this.t + " -> " + t + ")");
		}
	}

	public Network getNetwork(){
		return this.net;
	}

	public Dijkstra getDijkstra(){
		return this.dijkstra;
	}

	public double getChiSquared(int k, double mean){
		// get a random variable from a chi square distribution
		double s = Math.pow(mean/k, 0.5);
		double x = 0.0d;
		for (int i=1; i <= k; i++){
			x += Math.pow(this.rand.nextGaussian(),2);
		}
		return s*s*x;
	}


	///////// Intialization and configuration /////////

	public TruckCompany addCompany(int n_trucks){
		// Add a trucking company with a certain number of trucks to the simulation
		TruckCompany new_company = new TruckCompany(this);
		for (int i=0;i<n_trucks;i++){
			Truck new_truck = new Truck(this.net.getRandomEdge(), new_company);
			this.trucks.put(new_truck.getId(), new_truck);
			new_company.addTruck(new_truck);
			double depart_delay = this.getChiSquared(3,3600.0d);	// chi^2 distributed depart times
			this.addTruckToBusyQueue(new_truck, depart_delay);
		}
		this.truck_companies.add(new_company);
		return new_company;
	}

	public void addCompanies(int n_trucks, int orders_per_truck){
		// Add companies with the right distribution of trucks

		// Power law with alpha = 1.3
		int k = 50; 	// cut-off, max trucks for a single company
		double alpha = 1.3d;
		int[] number_truck_companies = new int[k];
		double sum = 0d;
		for(int i=1; i<=k; i++){sum += Math.pow(i,-alpha+1);}
		for(int i=0; i<number_truck_companies.length; i++){
			number_truck_companies[i] = (int) Math.round(((n_trucks/sum)*Math.pow(i+1,-alpha)));
		}

		int c = 0;
		Set<TruckCompany> gg = new HashSet<TruckCompany>();

		List<Integer> comps = new ArrayList<Integer>();
		for (int i=0; i<k;i++){
			for (int j=0; j<number_truck_companies[i]; j++){
				comps.add(i+1);
			}	
		}

		for(int i : comps){
			TruckCompany ntc = this.addCompany(i);
			if (this.global_optimization) {
 				gg.add(ntc);
			}
			c += i+1;
		}

		this.generateOrders(orders_per_truck);

		if (this.global_optimization){
			this.master_planner = new MasterPlanner(this, gg);
		}
	}

	public void addParkingsFromFile(String filepath){
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = saxParserFactory.newSAXParser();
			ParkingHandler handler = new ParkingHandler(this.net);
			saxParser.parse(new File(filepath), handler);
			this.parkings.addAll(handler.getParkings());
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}	
	}

	public void addTerminalsFromFile(String filepath){
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = saxParserFactory.newSAXParser();
			TerminalHandler handler = new TerminalHandler(this.net);
			saxParser.parse(new File(filepath), handler);
			this.terminals.putAll(handler.getTerminals());
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}	
	}

	public void addIndustrialEdgesFromFile(String filepath){
		// Were computed in python using some sumo tools
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = saxParserFactory.newSAXParser();
			DestinationHandler handler = new DestinationHandler(this.net);
			saxParser.parse(new File(filepath), handler);
			this.industrial_edges.addAll(handler.getDestinations());
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}	
	}

	public void generateOrders(double optd){
		// Generate optd orders per truck for each truck company (per day ~ duration of the simulation), to be done at beginning of simulation
		System.out.print("Generating orders...");
		for (TruckCompany tc : this.truck_companies){
			for (int i=0;i < (int) (optd*tc.numberOfTrucks());i++){
				Edge e1 = this.getRandomTerminalByCapacity().getLocation();
				Edge e2 = null;
				while(e2==null){
					Edge can = this.getRandomIndustrialEdgeByLength();
					if(e1!=can && !e1.equalsReverse(can)){
						e2 = can;
					} 
				}
				double u = Math.random();
				if(u<0.5){
					new Order(tc, e1, e2, this.t, true);
				} else {
					new Order(tc, e2, e1, this.t, false);
				}
			}
		}
		System.out.println(" Done.");
	}

	public Terminal getRandomTerminal(){
		// return a random terminal
		int item = new Random().nextInt(this.terminals.size());
		int i = 0;
		for(Terminal term: this.terminals.values()){
		    if (i == item){
		        return term;
		    }
		    i++;
		}
		return null;
	}

	public Terminal getRandomTerminalByCapacity(){
		// return a random terminal, weighted by the capacity of the terminals
		double total = 0.0d;
		for (Terminal term: this.terminals.values()){
			total += term.getCapacity();
		}
		double random = Math.random()*total;
		for (Terminal term: this.terminals.values()){
			random -= term.getCapacity();
			if (random <= 0.0d){
				return term;
			}
		}	
		return null;
	}

	public Edge getRandomIndustrialEdge(){
		// return a random industrial edge (i.e. potential destination for order)
		int item = new Random().nextInt(this.industrial_edges.size());
		int i = 0;
		for(Edge e: this.industrial_edges){
		    if (i == item){
		        return e;
		    }
		    i++;
		}
		return null;
	}

	public Edge getRandomIndustrialEdgeByLength(){
		// return a random industrial edge, weighted by its length
		double total = 0.0d;
		for (Edge ie: this.industrial_edges){
			total += ((Road) ie).getLength();
		}
		double random = Math.random()*total;
		for (Edge ie: this.industrial_edges){
			random -= ((Road) ie).getLength();
			if (random <= 0.0d){
				return ie;
			}
		}	
		return null;
	}

	public ParkingLot getClosestParking(Edge ref){
		// return the closest parking
		ParkingLot closest_parking = null;
		double dist = Double.POSITIVE_INFINITY;
		for (ParkingLot parking : this.parkings){
			if (!parking.isFull()){
				Edge park_edge = parking.getLocation();
				Path path = this.dijkstra.getMinPathCheck(ref, park_edge);
				if (path.getCost() < dist){
					dist = path.getCost();
					closest_parking = parking;
				}
			}
		}
		return closest_parking;
	}


	///////// Code for the dynamics of the simulation /////////

	private void addTruckToBusyQueue(Truck truck, double delay){
		if(!this.busy_trucks_set.contains(truck)){
			this.busy_trucks.add(new BusyTruck(truck, this.t + delay));
			this.busy_trucks_set.add(truck);
		} else {
			throw new IllegalArgumentException("The given truck is already in the busy queue.");
		}
	}

	private boolean truckIsBusy(Truck truck){
		return this.busy_trucks_set.contains(truck);
	}

	private Truck popBusyTruck(){
		BusyTruck ready_truck = this.busy_trucks.remove();
		this.setTime(ready_truck.getFinnishTime());
		Truck truck = ready_truck.getTruck();
		this.busy_trucks_set.remove(truck);
		return truck;
	}

	private void giveRoute(Truck truck, Edge dest){
		// sends a truck on its way to a dstination (adds a truck to the traffic simulator)
		if (this.truckIsBusy(truck)){
			throw new IllegalStateException("The given truck is busy and cannot be inserted in the road network.");
		}

		Path p = this.dijkstra.getMinPathCheck(truck.getLocation(), dest);
		if (p==null){
			truck.getOrder().cancel();
			throw new IllegalStateException("No path exists from edge " + truck.getLocation().getName()
											 + " to edge " + dest.getName());
		} else if (p.size()==1) {
			truck.setLocation(dest);
			this.advanceTruckOrder(truck);
		} else {
			this.traffic_sim.addVehicleRoute(truck.getId(), p);
		}
	}

	private void advanceTruckOrder(Truck truck){
		Order or = truck.getOrder();
		if (or!=null){
			if(truck.getLocation()==or.getOrigin() || truck.getLocation().equalsReverse(or.getOrigin())){
				or.pickUp();
				if (or.isTermOrder()){		// terminal delay
					Terminal term = this.terminals.get(or.getOrigin());
					double term_time = term.getTimeInTerminal(this.t);
					this.addTruckToBusyQueue(truck, term_time); 
					truck.increaseTimeInTerminal(term_time);
				} else {	// other delay
					this.addTruckToBusyQueue(truck, 1800.0d + 200.0d*this.rand.nextGaussian());
				}
			} else if(truck.getLocation()==or.getDestination() || truck.getLocation().equalsReverse(or.getDestination())){
				or.dropOff();
				if (or.isTermOrder()){
					this.addTruckToBusyQueue(truck, 1800.0d + 200.0d*this.rand.nextGaussian());
				} else {
					Terminal term = this.terminals.get(or.getDestination());
					double term_time = term.getTimeInTerminal(this.t);
					this.addTruckToBusyQueue(truck, term_time); 
					truck.increaseTimeInTerminal(term_time);
				}
			}
		}
	}

	public void updateTruckLocation(){
		Vehicle sim_mover = this.traffic_sim.getLastMover();
		Truck mover = this.trucks.get(sim_mover.getName());
		mover.setLocation(this.traffic_sim.getLastMoverEdge());
		if (sim_mover.getTInSim()>0.0) {	// sim_mover has finished its route in the simulation
			mover.increaseTimeOnRoad(sim_mover.getTInSim());
			this.advanceTruckOrder(mover);
		}
	}

	public boolean step(){
		// A simulation step

		// Allocate orders to trucks, either for local or global optimization
		HashMap<Order,Truck> allocated = new HashMap<Order,Truck>();
		if(this.global_optimization){
			allocated = this.master_planner.allocateOrders();
		}
		for (TruckCompany tc : this.truck_companies){	
			allocated.putAll(tc.allocateOrders1());
		}

		for (Truck tr : allocated.values()){
			if (!this.truckIsBusy(tr)){
				this.giveRoute(tr, tr.getOrder().getOrigin());
			}
		}

		boolean st = false;
		if (!this.busy_trucks.isEmpty() && this.busy_trucks.peek().getFinnishTime() < this.traffic_sim.getNextTime()){
			Truck truck = this.popBusyTruck();
			Order or = truck.getOrder();
			if (or!=null){
				if (!or.isPickedUp()){
					this.giveRoute(truck, or.getOrigin());
				} else if (or.isPickedUp() && !or.isDroppedOff()){
					this.giveRoute(truck, or.getDestination());
				} else if (or.isDroppedOff()) {
					or.process(this.t);
				}
			}
			st = true;
		} else {
			st = this.traffic_sim.simStep();
			if (st) {
				this.setTime(this.traffic_sim.getTime());
				this.updateTruckLocation();
			}
		}
		return st;	
	}

	public void updateRouting(){
		this.traffic_sim.updateEdgeWeights();
		this.dijkstra.clearDijkstraTrees();
		this.computeTerminalPaths();
	}

	public void computeTerminalPaths(){
		// construct the Dijkstra trees in the class Dijkstra for all terminals
		for (Edge te : this.terminals.keySet()){
			this.dijkstra.computeRootPaths(te.getTo());
			this.dijkstra.computeLeafPaths(te.getFrom());
		}
	}

	public void computeParkingPaths(){
		// construct the Dijkstra trees in the class Dijkstra for all parkings
		for (ParkingLot pl : this.parkings){
			this.dijkstra.computeRootPaths(pl.getLocation().getTo());
			this.dijkstra.computeLeafPaths(pl.getLocation().getFrom());
		}
	}

	public void runSimulation(){
		// Run the simulation
		double start_time = System.nanoTime();

		boolean st = true;	// true if a simulation step was done
		int i = 0;	// counter for simulation steps

		double t_last_routing_update = 0.0d;
		double dt_routing_update = 300.0d;	// interval of simulation time (s) at which routing is updated

		double t_last_snap = 0.0d;
		double dt_snap = 30.0d;		// time steps for snapshots

		System.out.println("");
		while (st) {
			// Routing update (update weights)
			if(this.t >= t_last_routing_update + dt_routing_update){
				this.updateRouting();
				t_last_routing_update = this.t;
			}

			// print some information
			if (i%500==0){
				System.out.print("Time: " + (int) this.t + ", #Trucks in traffic: "
				 				+ this.traffic_sim.getNumberOfVehicles() + " , Busy trucks: "
				 				 + this.busy_trucks_set.size() + "       " +  "\r");
			}
			i++;

			st = this.step(); 	// Do a simulation step

			// Write densities to file for visualisation
			if (this.t >= t_last_snap + dt_snap){
				this.traffic_sim.writeDensities();
				t_last_snap = this.t;
			}

			if (this.t>86400d){
				System.out.println(this.trucks.get(this.traffic_sim.getLastMover().getName()).getLocation().getName());
				System.out.println("Simulation time exploded.");
				break;
			}
		}

		System.out.println("Time: " + (int) this.t + ", #Trucks in traffic: " 
							+ this.traffic_sim.getNumberOfVehicles() + " , Busy trucks: " + this.busy_trucks_set.size()
							+ "       ");

		this.traffic_sim.writeLocks();	// Write full/jammed edges and critical/jammed cycles to a file

		double finish_time = System.nanoTime();
		System.out.println("Simulation time: " + (finish_time - start_time) + " ns");

		System.out.println("Average traffic time per truck: " + this.getAverageTrafficTime());
		System.out.println("Average terminal time per truck: " + this.getAverageTerminalTime());
		System.out.println("Average total time per truck: " + (this.getAverageTerminalTime() + this.getAverageTrafficTime()));
	}


	///////// Code for extracting information from the simulation /////////

	public double getAverageTrafficTime(){
		double traffic_time = 0.0d;
		for (Truck tr : this.trucks.values()){
			traffic_time += tr.getTimeOnRoad();
		}
		return (traffic_time/this.trucks.size());
	}

	public double getAverageTerminalTime(){
		double terminal_time = 0.0d;
		for (Truck tr : this.trucks.values()){
			terminal_time += tr.getTimeInTerminal();
		}
		return (terminal_time/this.trucks.size());
	}

	public void writeTrafficTimes(String file_name){
		// Write the times that the trucks spent in traffic to a file
		try {
			FileWriter writer = new FileWriter(this.output_folder + file_name, true);
			for (Truck tr : this.trucks.values()){
				writer.write(System.lineSeparator());
				writer.write(tr.getTimeOnRoad() + System.lineSeparator());
			}
			writer.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void writeTotalTimes(String file_name){
		// Write the times that the trucks spent in total
		try {
			FileWriter writer = new FileWriter(this.output_folder + file_name, true);
			for (Truck tr : this.trucks.values()){
				writer.write(System.lineSeparator());
				writer.write((tr.getTimeOnRoad()+tr.getTimeInTerminal()) + System.lineSeparator());
			}
			writer.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	static public void resetFile(String name){
		try {	// clear the files
				FileWriter writer = new FileWriter(name, false);
			} catch (Exception e) {
				System.out.println(e);
			}
	}


	static public void main(String[] args) {
		String input_folder = "/home/idlab126/Sumo/Antwerp/";
		String output_folder = "/home/idlab126/Documents/IDLAB/TGS/Plots_and_data/";
		
		TGS tgs = new TGS(input_folder, output_folder, false);

		// Reset output files of the simulation
		resetFile(tgs.output_folder+"Arrivals.txt");
		resetFile(tgs.output_folder+"Densities.txt");

		tgs.addCompanies(4586,3);

		tgs.runSimulation();


		// for (TruckCompany tc : tgs.truck_companies){
		// 	System.out.println(tc);
		// }
		
		tgs.writeTrafficTimes("Traffic_times_plan1.txt");
		tgs.writeTotalTimes("Total_times_plan1.txt");

	}

}