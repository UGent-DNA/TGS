import os, sys
import numpy as np 
if 'SUMO_HOME' in os.environ:
    tools = os.path.join(os.environ['SUMO_HOME'], 'tools')
    sys.path.append(tools)
else:   
    sys.exit("please declare environment variable 'SUMO_HOME'")
import sumolib


Port_files = "/home/idlab126/Sumo/Antwerp/"
SUMO_net = Port_files+"osm_Antwerp.net.xml"


print("Importing network..")
net = sumolib.net.readNet(SUMO_net)
edges = net.getEdges()
edges = set([edge for edge in edges if edge.allows('truck')])
print("Network imported.")



def getIndustrialEdges():
	industrial_edges = set()

	polys = [p for p in sumolib.shapes.polygon.read(Port_files + "osm.poly.xml") if p.type=="landuse.industrial"]
	for pol in polys:
		box = pol.getBoundingBox()
		middle = ((box[0]+box[2])/2,(box[1]+box[3])/2)

		enlarged_shape = []
		for p in pol.shape:
			p = list(p)
			p[0] = middle[0] + ((p[0]-middle[0])*1.1)
			p[1] = middle[1] + ((p[1]-middle[1])*1.1)
			enlarged_shape.append(tuple(p))

		for edge in edges:
			edge_shape = edge.getShape()
			for p in edge_shape:
				if sumolib.geomhelper.isWithin(p, enlarged_shape):
					industrial_edges.add(edge)
					break
		print(len(industrial_edges))

	industrial_xml = open(Port_files+"industrial_edges.add.xml", "w")
	print("<additionals>", file=industrial_xml)
	for e in industrial_edges:
		print(f"""    <edge id="{e.getID()}"/>""", file=industrial_xml)
	print("</additionals>", file=industrial_xml) 



getIndustrialEdges()

