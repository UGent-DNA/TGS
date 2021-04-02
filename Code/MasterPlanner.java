import basicnetwork.*;
import java.util.*;


public class MasterPlanner{
	// The class for a globally optimizing planner, the actual core of the truck guidance system

	private TGS tgs;

	// Truck companies that are in the system, value represents how many orders they have left in the system
	private Map<TruckCompany,Integer> truck_companies_credit = new HashMap<TruckCompany,Integer>();	
	private Set<Order> master_pending_orders = new HashSet<Order>();


	public MasterPlanner(TGS tgs, Set<TruckCompany> tcs){
		// Initialize the master planner, has to be done before running
		// the simulation and after TruckCompanies have been initialized.
		this.tgs = tgs;
		for (TruckCompany tc: tcs) {
			this.truck_companies_credit.put(tc, tc.getPendingOrders().size()); // The value is the amount of orders this TC has in "credit"
			this.master_pending_orders.addAll(tc.getPendingOrders());
			tc.clearPendingOrders();
			tc.setMasterPlanner(this);
		}
	}


	public HashMap<Order,Truck> allocateOrders(){
		// greedy approach, allocate closest order/truck pairs, each company can get as much orders 
		// from the master list as it has put in
		HashMap<Order,Truck> allocated_orders = new HashMap<Order,Truck>();
		Dijkstra dijk = this.tgs.getDijkstra();

		Set<Truck> pending_trucks = new HashSet<Truck>();
		for (Map.Entry<TruckCompany, Integer> entry: this.truck_companies_credit.entrySet()){
			// only add trucks of companies that still have credit
			if (entry.getValue() > 0){
				pending_trucks.addAll(entry.getKey().getPendingTrucks());
			}
		}

		int ts = pending_trucks.size();
		int os = this.master_pending_orders.size();
		
		
		if (ts != 0 && os != 0) {
			if (ts <= os){
				HashMap<Edge,LinkedList<Order>> or_locs = new HashMap<>();

				for (Order or : this.master_pending_orders){
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
				
				for (Truck tr : pending_trucks){
					TruckCompany tc = tr.getTruckCompany();
					int credit = this.truck_companies_credit.get(tc);
					if (credit < 1) {
						continue;
					}
					Edge closest = dijk.getClosestLeaf(tr.getLocation(), or_locs.keySet());
					Order closest_order = or_locs.get(closest).poll();
					if (or_locs.get(closest).isEmpty()){
						or_locs.remove(closest);
					}
					allocated_orders.put(closest_order,tr);

					// Reduce credit for corresponding TC
					this.truck_companies_credit.put(tc, credit-1);
				}

			} else {
				HashMap<Edge,LinkedList<Truck>> tr_locs = new HashMap<>();

				for (Truck tr : pending_trucks){
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

				for (Order or : this.master_pending_orders){
					int credit = 0;
					Edge closest;
					Truck closest_truck;
					TruckCompany tc;

					while(credit < 1){
						closest = dijk.getClosestRoot(or.getOrigin(), tr_locs.keySet());
						closest_truck = tr_locs.get(closest).poll();
						if (tr_locs.get(closest).isEmpty()){
							tr_locs.remove(closest);
						}
						tc = closest_truck.getTruckCompany();
						credit = this.truck_companies_credit.get(tc);
						if (credit > 0){
							allocated_orders.put(or,closest_truck);
							// Reduce credit for corresponding TC
							this.truck_companies_credit.put(tc, credit-1);
						}
					}
				}
			}

			for (Map.Entry<Order, Truck> entry : allocated_orders.entrySet()) {
	    		Order or = entry.getKey();
	    		Truck tr = entry.getValue();
	    		TruckCompany tc = tr.getTruckCompany();
	    		this.master_pending_orders.remove(or);
				or.setStatus("in_transit");
				or.setTruckCompany(tc);
				tr.clearOrder();
				tr.setOrder(or);
				tc.removePendingTruck(tr);
				tc.addInTransitOrder(or,tr);
			}
		}
		return allocated_orders;
	}

}



