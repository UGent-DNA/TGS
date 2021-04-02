import basicnetwork.*;
import java.util.*;


public class Truck{
	// Class for trucks in the truck guidance system

	private String id = UUID.randomUUID().toString();
	private Edge location;	
	private Order executing_order = null;
	private double time_on_road = 0.0d;
	private double time_in_terminal = 0.0d;

	private TruckCompany truck_company;


	public Truck(Edge location, TruckCompany tc){
		this.location = location;
		this.truck_company = tc;
	}

	public String getId(){
		return this.id;
	}

	public Edge getLocation(){
		return this.location;
	}

	public void setLocation(Edge loc){
		this.location = loc;
	}

	public Order getOrder(){
		return this.executing_order;
	}

	public void setOrder(Order o){
		if (this.executing_order == null){
			this.executing_order = o;
		} else {
			throw new IllegalStateException("A truck can only execute 1 order at a time.");
		}
	}

	public void clearOrder(){
		this.executing_order = null;
	}

	public TruckCompany getTruckCompany(){
		return this.truck_company;
	}

	public double getTimeOnRoad(){
		return this.time_on_road;
	}

	public void increaseTimeOnRoad(double rt){
		this.time_on_road += rt;
	}

	public double getTimeInTerminal(){
		return this.time_in_terminal;
	}

	public void increaseTimeInTerminal(double tt){
		this.time_in_terminal += tt;
	}

}