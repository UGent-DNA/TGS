package trafficsimulator;
import java.util.*;
import basicnetwork.*;


public class Junction extends Node {
	// A class for junctions in a traffic network as an extension of basicnetwork.Node.

	private String type;
	private double t_last_cross = 0.0d;
	private double cap = SimVars.tff; 	// The capacity of the junction, i.e. the minimum time-headway between crossings

	public Junction(String name, String type) {
		super(name);    
		this.type = type;
	}

	public String getType(){
		return this.type;
	}

	public double getTLast(){
		return this.t_last_cross;
	}

	public void setTLast(double tl){
		if (tl > this.t_last_cross){
			this.t_last_cross = tl;
		} else {
			throw new IllegalArgumentException("The new crossing time must be greater than the last one.");
		}
	}

	public void setCap(double cap){
		this.cap = cap;
	}

	public double getCap(){
		return this.cap;
	}

}