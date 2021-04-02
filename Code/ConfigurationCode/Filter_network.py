
"""Removes edges of the types in 'del_edge_types' from the network
It might take some time for a large network..
Isolated nodes can be removed via netedit: processing>clean junctions after this script has been called
"""

from xml.dom import minidom


mydoc = minidom.parse("/home/idlab126/Sumo/Antwerp/osm.net.xml")
print("XML parsed")
root = mydoc.documentElement


del_edge_types = ["highway.footway", "highway.bridleway", "highway.path", "highway.pedestrian"
                    , "highway.stairs", "highway.step", "highway.steps", "railway.subway", "highway.cycleway"]


deleted_edges = set()

# Delete the edges from net
print("deleting edges")
edges = mydoc.getElementsByTagName('edge')
for item in edges:
    if 'type' in item.attributes:
        if item.attributes['type'].value in del_edge_types:
            deleted_edges.add(item.attributes['id'].value)
            root.removeChild(item)


# Update junctions and connections
print("deleting connections")
connections = mydoc.getElementsByTagName('connection')
for item in connections:
    if item.attributes['from'].value in deleted_edges or item.attributes['to'].value in deleted_edges:
        root.removeChild(item)


# Update the edges of the roundabouts
print("updating roundabouts")
roundabouts = mydoc.getElementsByTagName('roundabout')
for item in roundabouts:
    round_edges = item.attributes['edges'].value
    for dedge in deleted_edges:
        if dedge in round_edges:
            round_edges = round_edges.replace(dedge, '')
            item.attributes['edges'].value = round_edges
            if len(round_edges) < 4: root.removeChild(item)



myfile = open("filtAntwerp.net.xml", "w")
myfile.write(mydoc.toxml())

