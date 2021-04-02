import basicnetwork.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;


// Parser for the potential destinations of orders in an xml file
public class DestinationHandler extends DefaultHandler {

    private Network net;
    private Set<Edge> destinations = new HashSet<Edge>();
    private Edge destination = null;

    public DestinationHandler(Network net){
        this.net = net;
    }

    public Set<Edge> getDestinations() {
        System.out.println("# destinations = " + this.destinations.size());
        return this.destinations;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("edge")) {
            destination = this.net.getEdge(attributes.getValue("id"));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {     
        if (qName.equalsIgnoreCase("edge")) {
            destinations.add(destination);
        }
    }

}