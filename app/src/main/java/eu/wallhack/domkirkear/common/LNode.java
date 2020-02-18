package eu.wallhack.domkirkear.common;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;

public class LNode extends AnchorNode {

    
    private final Vector3 localNodeLocation;

    public LNode(Vector3 nodeLocation, Vector3 mainNodeLocation, Vector3 offset) {
        localNodeLocation = LocationUtils.createLocalLocation(nodeLocation, mainNodeLocation, offset);
    }

    public Vector3 getLocalLocation(){
        return localNodeLocation;
    }


}
