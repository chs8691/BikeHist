package de.egh.bikehist.model;

/** Data Type: Geological position. */
public class GeoLocation {
	private final double longitude;
	private final double latitude;
	private final double altitude;

	/**
	 Represents a geographical point
	 @param longitude double
	 @param latitude double
	 @param altitude double, optional
	 */
	public GeoLocation( double longitude, double latitude, double altitude) {
		this.longitude = longitude;
		this.latitude = latitude;
		this.altitude = altitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getAltitude() {
		return altitude;
	}
}