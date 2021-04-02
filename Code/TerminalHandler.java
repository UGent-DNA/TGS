import basicnetwork.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;


// Parser for the terminals in an xml file
public class TerminalHandler extends DefaultHandler {

    private Network net;
    private HashMap<Edge,Terminal> terminals = new HashMap<>();
    private Terminal terminal = null;

    public TerminalHandler(Network net){
        this.net = net;
    }

    public HashMap<Edge,Terminal> getTerminals() {
        System.out.println("# terminals = " + this.terminals.size());
        return this.terminals;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("terminal")) {
            String id = attributes.getValue("id");
            Edge edge = this.net.getEdge(attributes.getValue("edge"));
            int teu = Integer.parseInt(attributes.getValue("capacity"));
            terminal = new Terminal(id, edge, teu);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {     
        if (qName.equalsIgnoreCase("terminal")) {
            terminals.put(terminal.getLocation(), terminal);
        }
    }

}