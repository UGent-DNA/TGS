import basicnetwork.*;
import java.util.*;


public class TruckCompany{

	private String id = UUID.randomUUID().toString();
	private Set<Truck> trucks = new HashSet<Truck>();
	private Set<Truck> pending_trucks = new HashSet<Truck>();

	private Set<Order> pending_orders = new HashSet<Order>();	// a priority queue could be used here, but its already part of the planning
	private Map<Order,Truck> intransit_orders = new HashMap<Order,Truck>();
	private Set<Order> processed_orders = new HashSet<Order>();
	private Set<Order> cancelled_orders = new HashSet<Order>();

	private TGS tgs;

	private MasterPlanner master_planner;


	public TruckCompany(TGS tgs){
		this.tgs = tgs;
	}

	@Override
	public String toString() { 
		return String.format("ID: " + this.id + "%n" + "Trucks: " + this.trucks.size() + "%n" 
								+ "Pending trucks: " + this.pending_trucks.size() + "%n"
								+ "Pending orders: " + this.pending_orders.size() + "%n" + "In-transit orders: "
								+ this.intransit_orders.size() + "%n" + "Proccessed orders: "
								+ this.processed_orders.size() + "%n"
								+ "Cancelled orders: " + this.cancelled_orders.size()); 
	}

	public String getId(){
		return this.id;
	}

	public void addTruck(Truck truck){
		this.trucks.add(truck);
		this.pending_trucks.add(truck);
	}

	public void addOrder(Order o){
		if(o.getStatus() == "pending"){
			this.pending_orders.add(o);
		} else {
			throw new IllegalStateException("Only pending orders can be allocated to some company");
		}
	}

	public int numberOfTrucks(){
		return this.trucks.size();
	}

	public Set<Truck> getPendingTrucks(){
		return this.pending_trucks;
	}

	public Set<Order> getPendingOrders(){
		return this.pending_orders;
	}

	public Set<Order> getProcessedOrders(){
		return this.processed_orders;
	}

	public Set<Order> getCancelledOrders(){
		return this.cancelled_orders;
	}

	public Map<Order,Truck> getInTransitOrders(){
		return this.intransit_orders;
	}

	void processOrder(Order o){
		// not to be called directly
		Truck pending_truck = this.intransit_orders.get(o);
		this.intransit_orders.remove(o);
		this.processed_orders.add(o);
		pending_truck.clearOrder();
		this.pending_trucks.add(pending_truck);
	}

	void cancelOrder(Order o){
		// not to be called directly
		String old_status = o.getStatus();		
		if (old_status=="pending"){
			this.pending_orders.remove(o);
		} else if (old_status=="in_transit"){
			Truck pending_truck = this.intransit_orders.get(o);
			pending_truck.clearOrder();
			this.pending_trucks.add(pending_truck);
			this.intransit_orders.remove(o).clearOrder();
		}
		this.cancelled_orders.add(o);

	}

	private Truck getPendingTruck(){
		// return a pending truck, just for testing
		for (Truck tr : this.pending_trucks){
			return tr;
		}
		return null;
	}

	private Order getPendingOrder(){
		// return a pending truck, just for testing
		for (Order or : this.pending_orders){
			return or;
		}
		return null;
	}

	public HashMap<Order,Truck> allocateOrders0(){
		// random trucks to pending orders
		HashMap<Order,Truck> allocated_orders = new HashMap<Order,Truck>();
		while (!this.pending_orders.isEmpty()&&!this.pending_trucks.isEmpty()){
			Order or = this.getPendingOrder();
			Truck tr = this.getPendingTruck();
			this.pending_trucks.remove(tr);
			this.pending_orders.remove(or);
			tr.setOrder(or);
			or.setStatus("in_transit");
			allocated_orders.put(or, tr);
		} 
		this.intransit_orders.putAll(allocated_orders);
		return allocated_orders;
	}

	public HashMap<Order,Truck> allocateOrders1(){
		// greedy approach, allocate closest order/truck pairs ('local optimization')
		HashMap<Order,Truck> allocated_orders = new HashMap<Order,Truck>();
		Dijkstra dijk = this.tgs.getDijkstra();
		int ts = this.pending_trucks.size();
		int os = this.pending_orders.size();

		if (ts != 0 && os != 0) {
			if (ts <= os){
				HashMap<Edge,LinkedList<Order>> or_locs = new HashMap<>();

				for (Order or : this.pending_orders){
					if (or_locs.containsKey(or.getOrigin())){
						or_locs.get(or.getOrigin()).add(or);
					} else {
						LinkedList<Order> loc_list = new LinkedList<Order>();
						loc_list.add(or);
						or_locs.put(or.getOrigin(), loc_list);
					}
				}
				// Shuffle the order in which orders at the same location are choosen
				for (LinkedList<Order> loc_list : or_locs.values()){
					Collections.shuffle(loc_list);
				}

				// For each truck, pick the closest order
				for (Truck tr : this.pending_trucks){
					Edge closest = dijk.getClosestLeaf(tr.getLocation(), or_locs.keySet());
					Order closest_order = or_locs.get(closest).poll();
					if (or_locs.get(closest).isEmpty()){
						or_locs.remove(closest);
					}
					allocated_orders.put(closest_order,tr);
					this.pending_orders.remove(closest_order);

				}
				this.pending_trucks.clear();

			} else {
				HashMap<Edge,LinkedList<Truck>> tr_locs = new HashMap<>();

				for (Truck tr : this.pending_trucks){
					if (tr_locs.containsKey(tr.getLocation())){
						tr_locs.get(tr.getLocation()).add(tr);
					} else {
						LinkedList<Truck> loc_list = new LinkedList<Truck>();
						loc_list.add(tr);
						tr_locs.put(tr.getLocation(), loc_list);
					}
				}
				// Shuffle the order in which trucks at the same location are choosen
				for (LinkedList<Truck> loc_list : tr_locs.values()){
					Collections.shuffle(loc_list);
				}

				// For each order, pick the closest truck
				for (Order or : this.pending_orders){
					Edge closest = dijk.getClosestRoot(or.getOrigin(), tr_locs.keySet());
					Truck closest_truck = tr_locs.get(closest).poll();
					if (tr_locs.get(closest).isEmpty()){
						tr_locs.remove(closest);
					}
					allocated_orders.put(or,closest_truck);
					this.pending_trucks.remove(closest_truck);
				}
				this.pending_orders.clear();
			}

			for (Map.Entry<Order, Truck> entry : allocated_orders.entrySet()) {
	    		Order or = entry.getKey();
	    		Truck tr = entry.getValue();
				or.setStatus("in_transit");
				tr.clearOrder();
				tr.setOrder(or);
			}
			this.intransit_orders.putAll(allocated_orders);
		}
		return allocated_orders;
	}



	///////// Code for master planner /////////
	
	public void setMasterPlanner(MasterPlanner mp){
		this.master_planner = mp;
	}

	public void clearPendingOrders(){
		this.pending_orders.clear();
	}

	public void clearPendingTrucks(){
		this.pending_trucks.clear();
	}

	public void removePendingTruck(Truck tr){
		this.pending_trucks.remove(tr);
	}

	public void addInTransitOrder(Order o, Truck t){
		this.intransit_orders.put(o,t);
	}

}