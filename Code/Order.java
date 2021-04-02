import basicnetwork.*;
import java.util.*;
import java.io.FileWriter;


public class Order{

	private String id = UUID.randomUUID().toString();
	private Edge origin;
	private Edge destination;
	private String status = "pending";
	private TruckCompany truck_company;
	private boolean term_or; 	// true if the order is a pick-up at the terminal, false if it is a drop-off
	private boolean picked_up = false;
	private boolean dropped_off = false;

	private double init_time;
	private double finish_time = -1.0d;

	private static HashSet<String> statuses = new HashSet<>(Arrays.asList("pending", "in_transit", "processed", "cancelled"));

	public Order(TruckCompany truck_company, Edge origin, Edge destination, double init_time, boolean term_or){
		if (origin==destination || origin.equalsReverse(destination)){
			throw new IllegalArgumentException("An order must have a distinct origin and destination.");
		}
		this.origin = origin;
		this.destination = destination;
		this.init_time = init_time;
		this.truck_company = truck_company;
		this.truck_company.addOrder(this);
		this.term_or = term_or;
	}

	@Override
	public String toString() { 
		return String.format("ID: " + this.id + "%n" + "Origin: " + this.origin.getName() + "%n" 
								+ "Destination: " + this.destination.getName() + "%n" + "Status: "
								+ this.status + "%n" + "Time_placed: " + this.init_time + "%n"
								+ "Time_delivered: " + this.finish_time); 
	}
	

	public String getId(){
		return this.id;
	}

	public TruckCompany getTruckCompany(){
		return this.truck_company;
	}

	public void setTruckCompany(TruckCompany truck_company){
		this.truck_company = truck_company;
	}

	public Edge getOrigin(){
		return this.origin;
	}

	public Edge getDestination(){
		return this.destination;
	}

	public String getStatus(){
		return this.status;
	}

	public boolean isTermOrder(){
		return this.term_or;
	}

	public boolean isPickedUp(){
		return this.picked_up;
	}

	public void pickUp(){
		if (!this.picked_up) {
			this.picked_up = true;
		} else {
			throw new IllegalStateException("This order is already picked up!");
		}
	}

	public boolean isDroppedOff(){
		return this.dropped_off;
	}

	public void dropOff(){
		if (!this.picked_up){
			throw new IllegalStateException("This order is not yet picked up!");
		} else if (this.dropped_off) {
			throw new IllegalStateException("This order is already dropped off!");
		} else {
			this.dropped_off = true;
		}
	}	

	public void setStatus(String status){
		if (statuses.contains(status)){
			this.status = status;
		} else {
			throw new IllegalArgumentException("The given status is not an allowed status");
		}
	}

	public double getInitTime(){
		return this.init_time;
	}

	public double getFinishTime(){
		return this.finish_time;
	}

	public void process(double t){
		if(this.status=="in_transit"){
			this.setStatus("processed");
			this.truck_company.processOrder(this);
			this.finish_time = t;
		} else {
			throw new IllegalArgumentException("Only an order in transit can become processed");
		}
	}

	public void cancel(){
		String old_status = this.getStatus();
		if (old_status == "processed"){
			throw new IllegalArgumentException("Processed orders can not be cancelled");
		} else {
			this.truck_company.cancelOrder(this);
			this.setStatus("cancelled");
		}
	}

}