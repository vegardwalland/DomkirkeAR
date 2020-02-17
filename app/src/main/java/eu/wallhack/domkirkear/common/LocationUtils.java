package eu.wallhack.domkirkear.common;

import com.google.ar.sceneform.math.Vector3;

public class LocationUtils {
    private static final int EARTH_RADIUS_IN_METERS = 6371000;

    // Calculate local location of node in a grid based from a main node point.
    public static Vector3 createLocalLocation(Vector3 nodeLocation, Vector3 mainNodeLocation, Vector3 offset) {
        float localX = (float)Math.toRadians(nodeLocation.x - mainNodeLocation.x) * EARTH_RADIUS_IN_METERS + offset.x;
        float localY = (float)Math.toRadians(nodeLocation.y - mainNodeLocation.y) * EARTH_RADIUS_IN_METERS + offset.y;
        float localZ = (float)Math.toRadians(nodeLocation.z - mainNodeLocation.z) * EARTH_RADIUS_IN_METERS + offset.z;
        Vector3 localLocation = new Vector3(localX, localY, localZ);
        return localLocation;
    }

    public static Vector3 createLocalLocation(Vector3 nodeLocation, Vector3 mainNodeLocation) {
        Vector3 offset = new Vector3(Vector3.zero());
        return createLocalLocation(nodeLocation, mainNodeLocation, offset);
    }
}
