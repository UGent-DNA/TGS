import basicnetwork.*;


public class ParkingLot{
	// Class for parking lots

	private String name;
	private Edge location;
	private int capacity;
	private int occupancy;

	public ParkingLot(String name, Edge location, int capacity){
		this.name = name;
		this.location = location;
		this.capacity = capacity;
	}

	public String getName(){
		return this.name;
	}

	public Edge getLocation(){
		return this.location;
	}

	public int getCapacity(){
		return this.capacity;
	}

	public boolean isFull(){
		return (this.occupancy == this.capacity);
	}

	public boolean parkTruck(){
		// return true if a truck could be parked
		if (this.occupancy < this.capacity){
			this.occupancy++;
			return true;
		} else {
			return false;
		}
	}

	public void deparkTruck(){
		if (this.occupancy > 0){
			this.occupancy--;
		}
	}

}

