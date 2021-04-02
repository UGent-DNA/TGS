import basicnetwork.*;
import java.util.*;


public class Terminal{
	// Basic model for terminals, modelled as a queue

	private String name;
	private Edge location;
	private float capacity;	// The capacity of the terminal resembling how big it is and how many orders pass through it

	private double next_service_time = 0.0d;
	private double service_time = 2.0d;	// 1/service_rate
	private double av_handling_time = 1800.0d;	// extra time needed for loading etc. appart from the service times
	private double stdv_handling_time = 200.0d;	// standard deviation on the extra time needed for internal operations
	private Random rand = new Random(); 


	public Terminal(String name, Edge location, int teu){
		this.name = name;
		this.location = location;
		this.capacity = teu;
		this.service_time *= (9000000/this.capacity);
	}

	public String getName(){
		return this.name;
	}

	public Edge getLocation(){
		return this.location;
	}

	public float getCapacity(){
		return this.capacity;
	}

	public double getTimeInTerminal(double t_enter){
		this.next_service_time = Math.max(t_enter, this.next_service_time) + Math.log(1-rand.nextDouble())*(-this.service_time);
		double handle_time = Math.max(this.av_handling_time-3*this.stdv_handling_time,
							(this.av_handling_time + this.stdv_handling_time*this.rand.nextGaussian()));
		return (this.next_service_time - t_enter + handle_time);
	}

}