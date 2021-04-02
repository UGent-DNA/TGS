import os, sys
import matplotlib.pyplot as plt
from matplotlib.collections import LineCollection, PolyCollection
import matplotlib.colors as colors
import matplotlib.cm as cm
import numpy as np 

if 'SUMO_HOME' in os.environ:
	tools = os.path.join(os.environ['SUMO_HOME'], 'tools')
	sys.path.append(tools)
else:   
	sys.exit("please declare environment variable 'SUMO_HOME'")
import sumolib


data_path = "/home/idlab126/Documents/IDLAB/TGS/Plots_and_data/Densities.txt"
port_directory = "/home/idlab126/Sumo/Antwerp/"
original_net_file = port_directory + "/osm_Antwerp.net.xml"
polygons_file = port_directory + "osm.poly.xml"
snapshot_dir = "/home/idlab126/Documents/IDLAB/TGS/Plots_and_data/Snapshots/"

print("Loading network..")
net = sumolib.net.readNet(original_net_file)
edges = net.getEdges()
N = len(edges)
edge_IDs = [e.getID() for e in edges]
id_index = {edge_IDs[i]:i for i in range(N)}
existing_ids = set(edge_IDs)
print("Network loaded")


def getWaterShapes():
	print("Loading polygons..")
	polys = sumolib.shapes.polygon.read(polygons_file)
	water_bodies = []
	for poly in polys:
		if poly.getType() in ("natural.water","waterway.dock"):
			water_bodies.append(poly.getShape())
	print("Polygons loaded..")
	return water_bodies


def getTerminalSymbols(scale = 170):
	symbol = [(0,0),(1,0),(0.5,0.866)]
	for i in range(len(symbol)):
		symbol[i] = (symbol[i][0]*scale, symbol[i][1]*scale)
	
	shapes = []
	terminal_coordinates = [(15777,24744), (12018,26485), (10576,28185), (11122,20712), (9809,20491)]
	for co in terminal_coordinates:
		t = []
		for p in symbol:
			t.append((co[0]+p[0],co[1]+p[1]))
		shapes.append(t)
	return shapes


def truncate_colormap(cmap, minval=0.0, maxval=1.0, n=100):
    new_cmap = colors.LinearSegmentedColormap.from_list(
        'trunc({n},{a:.2f},{b:.2f})'.format(n=cmap.name, a=minval, b=maxval),
        cmap(np.linspace(minval, maxval, n)[::-1]))
    return new_cmap


print("Reading snapshot data..")
time = []
trucks = []
occ_edges = []
with open(data_path) as file:
	for line in file:
		if line.startswith("step"):
			vals = line.split(", ")
			time.append(float(vals[1].split(":")[1][0:6]))
			trucks.append(int(vals[2].split(":")[1]))
			occ_edges.append({})	# new snapshot
		elif line.startswith("edge"):
			edge_id = line.split(", ")[0].split(":")[1]
			dens = 	float(line.split(", ")[1])
			if edge_id in existing_ids:
				occ_edges[-1][edge_id] = dens
n = len(occ_edges)
print("Snapshot data read")	


shapes = []
c = []
w = []
for e in edges:
	shapes.append(e.getShape())
	c.append("black")
	w.append(0.2)

cmap = plt.get_cmap('hsv')
cmap = truncate_colormap(cmap, 0.0, 0.8)

plt.figure(figsize=(14,10))
plt.colorbar(cm.ScalarMappable(cmap=cmap))
ax = plt.gca()
ax.set_aspect("equal", None, 'C')
ax.set_xlim (5000,35100)
ax.set_ylim (5000,31000)
plt.tight_layout(rect=[0, 0, 1.05, 1])

water_polys = PolyCollection(getWaterShapes(), color=(0.7,0.9,1.0))
term_symbols = PolyCollection(getTerminalSymbols(), color=(0.3,0.0,0.0))
line_segments = LineCollection(shapes, linewidths=w, colors=c)
ax.add_collection(water_polys)
ax.add_collection(term_symbols)
ax.add_collection(line_segments)


print("Generating snapshots..")
for j in range(n):
	print(f"snapshot: {j}/{n}", end='\r')
	snap_edges = occ_edges[j]
	c = ["black"]*N
	w = [0.2]*N
	for e in snap_edges:
		c[id_index[e]] = cmap(snap_edges[e])
		w[id_index[e]] = 0.5 + 3*snap_edges[e] + np.heaviside(snap_edges[e],0)

	line_segments.set_color(c)
	line_segments.set_linewidths(w)
	ax.text(5700, 30000, f"Time: {time[j]} s   ", backgroundcolor="white")
	ax.text(5700, 29500, f"# Trucks: {trucks[j]}    ", backgroundcolor="white")
	plt.savefig(snapshot_dir + '%06d.png'%j)


# then do: ffmpeg -r 16 -i Snapshots/%06d.png -c:v libx264 -vf fps=16 -b 7000k -pix_fmt yuv420p out.mp4 -y
# -b controls the bitrate: compression vs quality tradeoff





