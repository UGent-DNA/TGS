package trafficsimulator;

public final class SimVars{

	public static final float jam_density = 1.0f/42.0f; 	// Density at which the jamming transition takes place (for vehicles) in 1/m at v_max = 70 km/h
	public static final float max_density = 1.0f/18.0f;		// The maximum possible density of trucks (in 1/m, a spacing of 20 m is assumed)

	public static final float tff = 2.5f;    // The parameter tau_ff = tua_fj is s
	public static final float tjf = 3.5f;    // The parameter tau_jf in s, should be greater than tff in order to produce stable jams
	// public static final float tjj = tjf; // 4.0f;    // The parameter tau_jj in s, should be greater than tff in order to produce stable jams
	
	// The parameter tau_jj in s, should be greater than tff in order to produce stable jams
	public static final float tjj = (max_density/(max_density-jam_density))*tjf;   

	public static final float max_speed = 25.0f;	// The maximum speed of the vehicles in the simulation (90 km/h for trucks)

}