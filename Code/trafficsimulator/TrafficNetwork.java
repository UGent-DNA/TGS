package trafficsimulator;
import java.util.*;
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import basicnetwork.*;


public class TrafficNetwork extends Network{
	
	// Create a network from a given XML, can be produced with sumo+python.
	public TrafficNetwork(String filepath){
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = saxParserFactory.newSAXParser();
			NetHandler nethandler = new NetHandler();

			saxParser.parse(new File(filepath), nethandler);

			this.setNodes(nethandler.getNodes());
			this.setEdges(nethandler.getEdges());
			nethandler.printTotalLength();

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}

	public void addExternalLoads(String filepath){
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = saxParserFactory.newSAXParser();
			ExternalLoadHandler loadhandler = new ExternalLoadHandler(this);
			saxParser.parse(new File(filepath), loadhandler);
		} catch (ParserConfigurationException | SAXException | IOException e) {
		e.printStackTrace();
		}
	}

	public Road getRoad(String name){
		return (Road) this.getEdge(name);
	}

	public Junction getJunction(String name){
		return (Junction) this.getNode(name);
	}


	static public void main(String[] args) {
		TrafficNetwork net = new TrafficNetwork("/home/idlab126/Sumo/Antwerp/Java_osm.net.xml");
		Dijkstra dijk = new Dijkstra(net);
		
		dijk.checkConnectivity(net.getNode("cluster_26735480_26735482"));

	}

}