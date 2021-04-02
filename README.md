
# Simulation of container transport in the port of Antwerp

Here you can find the code that was used for simulating and studying a potential truck guidance system for the port of Antwerp, see the project report: "A Simulation Of Truck Guidance For The Port Of Antwerp" (TGS-report-imec-IDLab-UGent-20201208.pdf).


## Used data

In order to define the traffic network for the simulation around Antwerp and its port, map data from OpenStreetMap (www.openstreetmap.org) is used. In order to download this map data in a traffic setting, osmWebWizard.py from SUMO (https://sumo.dlr.de/docs/index.html) is used. Other tools from SUMO, such as netedit, are used to clean up the network (remove redundant nodes, remove dead ends, ...), we refer to SUMO's documentation for further information on these tools and how to use them. An extra python script was written to filter out any roads on which trucks are not allowed to pass, see 'ConfigurationCode/Filter_network.py'. Since the data of OpenStreetMap can sometimes be incomplete, some important edges on which trucks are not allowed had to be removed manually (with the help of the netedit tool from SUMO), e.g. the "Waaslandtunnel", the quays in the centre of Antwerp, ... Another python script was written to correct some of the speed limits of OpenStreetMap (some where missing, these are inferred by the road type), see 'ConfigurationCode/CorrectOsmSpeeds.py'. The network is contained in the file 'osmAntwerp.net.xml'.

Next, a simplified format for the network to be used in the actual simulation in Java is generated. The script 'ConfigurationCode/ConvertNetXML.py' takes care of this and converts the original net file generated by osmWizzard.py to a simplified format: 'JaveOsm.net.xml'. 

For general information on the average number of containers handeled and the container terminals in the port of Antwerp, "Facts and Figures 2020" from the port of Antwerp was consulted (https://www.portofantwerp.com/en/publications/brochures/facts-and-figures-2020). Information on the average throughput of each container terminal can be found online (https://www.portofantwerp.com/en/intraport-terminal-tool). 

For the distribution of trucking companies and the size of their fleet, https://jshippingandtrade.springeropen.com/articles/10.1186/s41072-019-0054-5 was consulted. 

The parameters for the mescopic traffic simulator were based on "Vehicle-based modelling of traffic, Theory and application to environmental impact modelling" by Nils Eissfeldt (https://kups.ub.uni-koeln.de/1274/), rescaled to trucks with a standard length of 18m.

Obtaining data on the distribution of destinations of orders (apart from the 5 container terminals in the port of Antwerp) is very difficult. In order to model this, we assumed a uniform distibution over road located in areas that are marked as "industrial" by OpenStreetMap. The python script 'ConfigurationCode/SelectDestinations.py' precomputes these potential destinations which are written to the file 'industrial_edges.add.xml'. 




## Different parts of the code

The main part of the simulator is written in Java. The package 'basicnetwork' takes care of all the functionality needed for the underlying traffic network and the routing algorithms and doesn't have to be modified when doing a simulation.

The package 'trafficsimulator' handles the simulation of traffic on a given traffic network and is based on a mesoscopic traffic model described in (https://kups.ub.uni-koeln.de/1274/). See also 'TGS-report-imec-IDLab-UGent-20201208.pdf' for a detailed description of the model. The parameters used in the traffic model can be found in 'trafficsimulator/SimVars.java', these can be tuned further, however the cuurent parameters result in a realistic and stable model. The rest of the code in this package doesn't have to be modified. 

The other classes constitute the simulation of the container transport in and around the port and the planning strategies for the trucking companies. The role of most of the classes is clear from their name. The impartant parts are 'TruckCompany.java', in which individual planners for trucking companies can be found, as wel as their order books; 'MasterPlanner.java' contains the master set of orders (see TGS-report-imec-IDLab-UGent-20201208.pdf) and contains the globally optimizing real-time planner; 'TGS.java' is the main part of the simulator and combines al the other parts.

Visualisation of the evolution of truck traffic during the simulation is done after the simulation is completed and the results are written to a file, 'Visualisation/GenerateSnaps.py' constructs different snapshots of the simulation, which can then be put together into a video with e.g. ffmpeg. The script 'Visualisation/PlotRoadTimes.py' can be used to produce histograms of the times spent by the trucks in the simulation and in traffic. 


## Setting up a simulation

To set up a simulation one uses 'TGS.java'. In this piece of code, the input data is loaded, the simulation is initialized and ran, and the results from the simulation are written to some files. A TGS object is initialized with the correct paths for input files. Next some companies are added, each with their respective fleet size and number of orders for that day. The distribution of the fleet sizes is a power law, but this can be altered by changin the method 'TGS.addCompanies()'; trucking companies can also be added individually with the method 'TGS.addCompany()'. The boolean variable 'TGS.global_optimization' controls wether the simulation is ran in the globally optimizing mode (with a master set of orders) or not. The simulation can then be started with 'TGS.runSimulation()'. The times the trucks spent in traffic and/or in the simulation can also be written to a file with 'TGS.writeTrafficTimes()' and 'TGS.writeTotalTimes()'. 








































