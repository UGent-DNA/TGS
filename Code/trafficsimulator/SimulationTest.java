package trafficsimulator;
import java.util.*;
import java.io.*;
import basicnetwork.*;
import java.nio.charset.StandardCharsets;


public class SimulationTest{

	TrafficSimulation sim;

	public SimulationTest(TrafficSimulation sim){
		this.sim = sim;
	}

	public SimulationTest(TrafficNetwork net, String output){
		this.sim = new TrafficSimulation(net, output);
	}


	public void printDensities(){
		TrafficNetwork net = this.sim.getNetwork();
		Road first_edge = (Road) net.getEdge("0_1");
		Road e = first_edge;
		do {
			System.out.println(e.getName() + " occ = " + e.getOccupancy());
			e = this.getNextRoad(e);
		} while(e!=first_edge);
	}

	public Road getNextRoad(Road r){
		// circular road assumed
		for (Road e : r.getOutRoads()){
			return e;
		}
		return null;
	}

	public void makeFDRHomo(){
		// produce a fundamental diagram with flow against density for a certain edge and write the results to a text file
				TrafficNetwork net = this.sim.getNetwork();
		HashMap<Integer,Double> dens_flow = new HashMap<>();
		Dijkstra dijk = new Dijkstra(net);
		Set<Path> paths = new HashSet<>();
		int n_edges = net.getEdges().size();
		Path ref_path = null;
		Road ref_edge = null;

		// construct circular paths
		for (Edge e: net.getEdges().values()){
			Path p = dijk.getMinPath(e.getFrom(), e.getTo()).add(dijk.getMinPath(e.getTo(), e.getFrom()));
			for(int i=0;i<4;i++){
				p.add(p);
			}
			paths.add(p);
			if (ref_path == null){
				ref_path = p;
				ref_edge = (Road) e;
			}
		}

		for(double d=0.01f;d<=1.0f;d += 0.02f){
			System.out.println("density = " + d);

			int n = (int) (d*ref_edge.getCapacity());
			for(int i=0;i<n;i++){
				for (Path p: paths){
					this.sim.addVehicleRoute(p);
				}
			}
			for(int j=0;j<100;j++){
				this.sim.simStep();
			}

			double t_begin = ref_edge.getLastExitT();
			HashSet<Double> exit_times = new HashSet<Double>(); 
			while(!this.sim.getPriorityVehicles().isEmpty()&&this.sim.getTime()<50000){
				this.sim.simStep();
				exit_times.add(ref_edge.getLastExitT());

			}
			double q = exit_times.size()/(ref_edge.getLastExitT()-t_begin);
			dens_flow.put(n, q);
			this.sim.clearSimulation();
		}

		try{
			FileWriter writer = new FileWriter("Plots_and_data/FDR.txt"); 
			for(Map.Entry<Integer,Double> entry : dens_flow.entrySet()){
				writer.write(entry.getKey() + "    " + entry.getValue() + System.lineSeparator());
			}
			writer.close();
		} catch(Exception e) {
			System.out.println("writng failed");
		}
	}

	public void makeFDRHetro(){
		// produce a fundamental diagram with flow against density for a certain edge and write the results to a text file
		TrafficNetwork net = this.sim.getNetwork();
		HashMap<Integer,Double> dens_flow = new HashMap<>();
		Dijkstra dijk = new Dijkstra(net);
		Map<Road,Path> paths = new HashMap<>();
		int n_edges = net.getEdges().size();
		Road ref_edge = null;

		// construct circular paths
		for (Edge e: net.getEdges().values()){
			Path p = dijk.getMinPath(e.getFrom(), e.getTo()).add(dijk.getMinPath(e.getTo(), e.getFrom()));
			for(int i=0;i<6;i++){
				p.add(p);
			}
			paths.put((Road) e,p);
			if (ref_edge == null){
				ref_edge = (Road) e;
				System.out.println("ref edge = " + ref_edge.getName());
			}
		}

		for(double d=0.01f;d<=1.0f;d += 0.02f){
			System.out.println("density = " + d);
			int n = (int) (n_edges*d*ref_edge.getCapacity());
			
			Road r = ref_edge;
			for(int i=0;i<n;i++){
				if (!r.isFull()){
					this.sim.addVehicleRoute(paths.get(r));
				} else {
					r = this.getNextRoad(r);
					this.sim.addVehicleRoute(paths.get(r));
				}
			}
			for(int j=0;j<n;j++){
				this.sim.simStep();
			}

			double t_begin = this.sim.getTime();
			HashSet<Double> exit_times = new HashSet<Double>();
			while(!this.sim.getPriorityVehicles().isEmpty()&&this.sim.getTime()<7200){
				this.sim.simStep();
				exit_times.add(ref_edge.getLastExitT());
			}
			this.printDensities();
			double q = exit_times.size()/(this.sim.getTime()-t_begin);
			dens_flow.put(n, q);
			this.sim.clearSimulation();
		}

		try{
			FileWriter writer = new FileWriter("Plots_and_data/FDR.txt"); 
			for(Map.Entry<Integer,Double> entry : dens_flow.entrySet()){
				writer.write(entry.getKey() + "    " + entry.getValue() + System.lineSeparator());
			}
			writer.close();
		} catch(Exception e) {
			System.out.println("writng failed");
		}
	}


	static public void main(String[] args) {
		TrafficNetwork net = new TrafficNetwork("/home/idlab126/Documents/IDLAB/TGS/TGS_2.0/testd/test.net.xml");
		SimulationTest test = new SimulationTest(net, "/home/idlab126/Documents/IDLAB/TGS/Plots_and_data/");


		test.makeFDRHomo();
		// test.makeFDRHetro();

	}

}