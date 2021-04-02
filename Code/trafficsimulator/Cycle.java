package trafficsimulator;
import java.util.*;
import basicnetwork.*;

public class Cycle{

	private Set<Road> roads = new HashSet<Road>();	// The roads that are part of this critical cycle

	public Cycle(Set<Road> roads){
		for(Road r: roads){
			r.addCycle(this);
		}
		this.roads = roads;
		if(!this.isCritical()){
			for(Road r: roads){
				System.out.println(r.getName());
				System.out.println(r.getOccupancy() + "/" + r.getCapacity());
			}
			throw new IllegalStateException("A cycle that is not critical was created");
		}
	}

	@Override
    public String toString() { 
    	String s = "";
        for(Road r : this.roads){
        	s += r.getName() + " " + r.getOccupancy() + "/" + r.getCapacity() + ", ";
        }
        return s;
    } 

	@Override
	public boolean equals(Object o) { 
		Cycle c = (Cycle) o;
		return (this.roads.equals(c.getRoads()));
	} 

	@Override
	public int hashCode(){
	   // return this.roads.hashCode();
		return 0;
	}

	public Set<Road> getRoads(){
		return this.roads;
	}

	public Set<Road> getInRoads(){
		// return the roads that are incident upon this cycle but not roads in the cycle itself
		HashSet<Road> ins = new HashSet<Road>();
		for(Road r: this.roads){
			ins.addAll(r.getInRoads());
		}
		ins.removeAll(this.roads);
		return ins;
	}

	public boolean contains(Road r){
		return this.roads.contains(r);
	}

	public boolean isCritical(){
		// check whether the cycle is really critical
		int crit_edge = 0;
		for(Road r: this.roads){
			if(r.isQuasiFull()){
				crit_edge += 1;
			} else if(!r.isFull()){
				return false;
			}
		}
		if(crit_edge != 1){
			return false;
		}
		return true;
	}

	public boolean isLocked(){
		boolean lock = true;
		for(Road r: this.roads){
			lock = lock && r.isFull();
		}
		return lock;
	}

	public void clear(){
		// remove the references tot his cycle because it is no longer critical
		if(!this.isCritical()){
			for(Road r: this.roads){
				r.removeCycle(this);
			}
		}else{
			throw new IllegalStateException("Only cycles that are no longer critical are allowed to be cleared.");
		}
		
	}

}