package trafficsimulator;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;


public class ExternalLoadHandler extends DefaultHandler {

    private TrafficNetwork net;

    public ExternalLoadHandler(TrafficNetwork net){
        this.net = net;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("edge")) {
            Road road = this.net.getRoad(attributes.getValue("id"));
            float load = (float) Integer.parseInt(attributes.getValue("load"))/(24*(1+road.getNLanes()));  // Cars per lane per hour            
            road.setLoad(load);
        }
    }
}