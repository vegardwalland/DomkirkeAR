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
        float localZ = (float) (Math.toRadians(nodeLocation.z - mainNodeLocation.z) * getRadius(lat)*Math.cos(Math.toRadians(mainNodeLocation.x)) + offset.z);

        return new Vector3(localX, localY, localZ);
    }

    public static Vector3 createLocalLocation(Vector3 nodeLocation, Vector3 mainNodeLocation) {
        Vector3 offset = new Vector3(Vector3.zero());
        return createLocalLocation(nodeLocation, mainNodeLocation, offset);
    }

    public static float calculateLengthInWorld(Vector3 nodeLocation, Vector3 mainNodeLocation) {
        float lat1 = (float)Math.toRadians(nodeLocation.x);
        float lat2 = (float)Math.toRadians(mainNodeLocation.x);
        float lon1 = (float)Math.toRadians(nodeLocation.z);
        float lon2 = (float)Math.toRadians(mainNodeLocation.z);


        float length = (float)(0.5 - (Math.cos(lat2-lat1))/2 + Math.cos(lat1)*Math.cos(lat2)*((1-Math.cos(lon2-lon1))/2));
        length = (float)(2*getRadius(lat1)*Math.asin(Math.sqrt(length)));
        return length;
    }

    private static double getRadius(double latitudeInRadians) {
        double An = EQUATORIAL_RADIUS_IN_METERS*EQUATORIAL_RADIUS_IN_METERS* Math.cos(latitudeInRadians);
        double Bn = POLAR_RADIUS_IN_METERS*POLAR_RADIUS_IN_METERS * Math.sin(latitudeInRadians);
        double Ad = EQUATORIAL_RADIUS_IN_METERS * Math.cos(latitudeInRadians);
        double Bd = POLAR_RADIUS_IN_METERS * Math.sin(latitudeInRadians);
        return Math.sqrt((An*An + Bn*Bn)/(Ad*Ad + Bd*Bd));
    }
}
