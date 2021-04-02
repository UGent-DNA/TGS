import basicnetwork.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;


// Parser for the parking areas in an xml file
public class ParkingHandler extends DefaultHandler {

    private Network net;
    private Set<ParkingLot> parkings = new HashSet<ParkingLot>();
    private ParkingLot parking = null;

    public ParkingHandler(Network net){
        this.net = net;
    }

    public Set<ParkingLot> getParkings() {
        System.out.println("# parkings = " + this.parkings.size());
        return this.parkings;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("parking")) {
            String id = attributes.getValue("id");
            Edge edge = this.net.getEdge(attributes.getValue("edge"));
            int capacity = Integer.parseInt(attributes.getValue("capacity"));
            parking = new ParkingLot(id, edge, capacity);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {     
        if (qName.equalsIgnoreCase("parking")) {
            parkings.add(parking);
        }
    }

}



