package eu.wallhack.domkirkear.common;

import com.google.ar.sceneform.math.Vector3;

public class LocationUtils {
    private static final double EQUATORIAL_RADIUS_IN_METERS = 6378137.0;
    private static final double POLAR_RADIUS_IN_METERS = 6356752.3;

    // Calculate local location of node in a grid based from a main node point.
    public static Vector3 createLocalLocation(Vector3 nodeLocation, Vector3 mainNodeLocation, Vector3 offset) {
        double lat = Math.toRadians(mainNodeLocation.x);

        float localX = (float) (Math.toRadians(nodeLocation.x - mainNodeLocation.x) * getRadius(lat) + offset.x);
        float localY = (float) (Math.toRadians(nodeLocation.y - mainNodeLocation.y) * getRadius(lat) + offset.y);
        float localZ = (float) (Math.toRadians(nodeLocation.z - mainNodeLocation.z) * getRadius(lat) + offset.z);

        return new Vector3(localX, localY, localZ);
    }

    public static Vector3 createLocalLocation(Vector3 nodeLocation, Vector3 mainNodeLocation) {
        Vector3 offset = new Vector3(Vector3.zero());
        return createLocalLocation(nodeLocation, mainNodeLocation, offset);
    }

    private static double getRadius(double latitudeInRadians) {
        double An = EQUATORIAL_RADIUS_IN_METERS*EQUATORIAL_RADIUS_IN_METERS* Math.cos(latitudeInRadians);
        double Bn = POLAR_RADIUS_IN_METERS*POLAR_RADIUS_IN_METERS * Math.sin(latitudeInRadians);
        double Ad = EQUATORIAL_RADIUS_IN_METERS * Math.cos(latitudeInRadians);
        double Bd = POLAR_RADIUS_IN_METERS * Math.sin(latitudeInRadians);
        return Math.sqrt((An*An + Bn*Bn)/(Ad*Ad + Bd*Bd));
    }
}
