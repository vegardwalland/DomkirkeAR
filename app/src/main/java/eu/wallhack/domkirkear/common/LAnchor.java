package eu.wallhack.domkirkear.common;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.math.Vector3;

public class LAnchor extends Anchor {

    public LAnchor(Vector3 nodeLocation, Vector3 mainNodeLocation, Vector3 offset) {
        Vector3 location = LocationUtils.createLocalLocation(nodeLocation, mainNodeLocation, offset);


    }

}
