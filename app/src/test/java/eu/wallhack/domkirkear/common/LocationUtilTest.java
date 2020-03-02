package eu.wallhack.domkirkear.common;

import com.google.ar.sceneform.math.Vector3;

import org.junit.Test;

import eu.wallhack.domkirkear.TestUtil;

public class LocationUtilTest {

    @Test
    public void testLocalGridCoordinates() {
        Vector3 mainVector = new Vector3(37.929847f,0f, -8.286010f);
        Vector3 nodeVector = new Vector3(76.992330f, 0f, 67.814456f);

        Vector3 localVector = LocationUtils.createLocalLocation(nodeVector, mainVector);

        float length = LocationUtils.calculateLengthInWorld(nodeVector, mainVector);
        System.out.println("Length of localVector using localVector.length() = " + localVector.length());
        System.out.println("X: " + localVector.x + " Y: " + localVector.y + " Z: " + localVector.z + " Length: " + length);

        TestUtil.assertAlmostEquals(length, localVector.length());
    }
}
