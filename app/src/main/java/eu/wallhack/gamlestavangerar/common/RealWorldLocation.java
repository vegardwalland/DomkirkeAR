package eu.wallhack.gamlestavangerar.common;

public class RealWorldLocation {
    private String name;
    private String description;
    private double lat;
    private double lon;

    public RealWorldLocation(String name, String description, double lat, double lon) {
        this.name = name;
        this.description = description;
        this.lat = lat;
        this.lon = lon;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }


}
