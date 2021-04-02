from xml.dom import minidom
import numpy as np


# Assumed to contain only edges where trucks are allowed (can be done with netcovert).
# 2019-09-09-12-00-50/osm.net.xml
datapath = "/home/idlab126/Sumo/Antwerp/"
mydoc = minidom.parse(datapath+"osm_Antwerp.net.xml")
print("XML parsed")
root = mydoc.documentElement




edges = mydoc.getElementsByTagName('edge')
for item in edges:
    # ignore internal edges
    if not 'function' in item.attributes:
        ttype = item.attributes['type'].value
        lanes = [child for child in item.childNodes if child.nodeType==minidom.Node.ELEMENT_NODE and child.tagName=='lane']

        max_speed = 13.89
        if ttype == 'highway.motorway':
        	max_speed = 33.33
        elif ttype == 'highway.trunk':
        	max_speed = 25.00
        elif ttype == 'highway.secondary':
        	max_speed = 19.44
        elif ttype == 'highway.secondary_link':
        	max_speed = 19.44
        elif ttype == 'highway.tertiary':
        	max_speed = 19.44
        elif ttype == 'highway.tertiary_link':
        	max_speed = 19.44
        elif ttype == 'highway.primary':
        	max_speed = 19.44
        elif ttype == 'highway.primary_link':
        	max_speed = 19.44


        for lane in lanes:
        	new_speed = str(np.minimum(max_speed, float(lane.attributes['speed'].value)))
        	lane.setAttribute("speed", new_speed)




newfile = open(datapath+"osm_Antwerp.net.xml", "w")
root.writexml(newfile)
newfile.close()





