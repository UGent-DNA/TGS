from xml.dom import minidom


# Assumed to contain only edges where trucks are allowed (can be done with netcovert).
# 2019-09-09-12-00-50/osm.net.xml
datapath = "/home/idlab126/Sumo/Antwerp/"
mydoc = minidom.parse(datapath+"osm_Antwerp.net.xml") # The original net file obtained with the sumo wizzard
print("XML parsed")
root = mydoc.documentElement

newdoc = minidom.Document()
# root of the new xml file
newroot = newdoc.createElement('net')

# set containing the nodes that are used by the network (excluding isolated nodes that is)
used_nodes = set()
parallel_edges = set()  # parallel edges can occur and we want to deal with them

xml_nodes = set()
xml_edges = set()



print("Edges...")
edges = mydoc.getElementsByTagName('edge')
for item in edges:
    # ignore internal edges
    if not 'function' in item.attributes:
        iid = item.attributes['id'].value
        ffrom = item.attributes['from'].value
        tto = item.attributes['to'].value
        ttype = item.attributes['type'].value

        lanes = [child for child in item.childNodes if child.nodeType==minidom.Node.ELEMENT_NODE and child.tagName=='lane']
        length = str(float(lanes[0].attributes['length'].value)+10.0)   # edges in the xml file are consistently smaller than when viewed in netedit
        speed = lanes[0].attributes['speed'].value
        nlanes = str(len(lanes))

        if (ffrom,tto) in parallel_edges:
            split_node = newdoc.createElement('node')
            split_node.setAttribute("id", f"split_{iid}")
            split_node.setAttribute("type", "parallel_split")
            xml_nodes.add(split_node)

            extra_edge = newdoc.createElement('edge')
            extra_edge.setAttribute("id", iid+"s")
            extra_edge.setAttribute("from", ffrom)
            extra_edge.setAttribute("to", split_node.attributes['id'].value)
            extra_edge.setAttribute("type", ttype)
            extra_edge.setAttribute("length", str(0.1)) 
            extra_edge.setAttribute("speed", speed)
            extra_edge.setAttribute("lanes", nlanes)

            xml_edges.add(extra_edge)
            ffrom = split_node.attributes['id'].value


        edge = newdoc.createElement('edge')
        edge.setAttribute("id", iid)
        edge.setAttribute("from", ffrom)
        edge.setAttribute("to", tto)
        edge.setAttribute("type", ttype)
        edge.setAttribute("length", length) 
        edge.setAttribute("speed", speed)
        edge.setAttribute("lanes", nlanes)

        used_nodes.add(ffrom)
        used_nodes.add(tto)

        parallel_edges.add((ffrom, tto))
        xml_edges.add(edge)

        

print("Junctions...")
nodes = mydoc.getElementsByTagName('junction')
for item in nodes:
    if item.attributes['id'].value in used_nodes:
        node = newdoc.createElement('node')
        node.setAttribute("id", item.attributes['id'].value)
        node.setAttribute("type", item.attributes['type'].value)
        xml_nodes.add(node)


# first write the nodes, then the edges
for n in xml_nodes:
    newroot.appendChild(n)
for e in xml_edges:
    newroot.appendChild(e)




newfile = open(datapath+"Java_osm.net.xml", "w")
newfile.write(newroot.toprettyxml())
