package eu.wallhack.domkirkear;

import com.google.ar.sceneform.math.Vector3;

import org.junit.Test;

import eu.wallhack.domkirkear.common.LocationUtils;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testLocalGridCoordinates() {
        Vector3 mainVector = new Vector3(58.938198f, 0f, 5.695740f);
        Vector3 nodeVector = new Vector3(58.938329f, 0f, 5.695950f);

        Vector3 localVector = LocationUtils.createLocalLocation(nodeVector, mainVector);
        //Double length = Math.sqrt(Math.pow(localVector.x, 2) + Math.pow(localVector.z, 2));

        Float length = LocationUtils.calculateRealWorldLength(nodeVector, mainVector);

        System.out.println("X: " + localVector.x + " Y: " + localVector.y + " Z: " + localVector.z + " Length: " + length);

        //assertBetween(length, 19.0, 20.0);
        //assertBetween(localVector.x, 19f, 20f);
        //assertBetween(localVector.z,);

    }

    private static void assertBetween(Double testValue, Double lowerParameter, Double higherParameter) {
        assertTrue(testValue > lowerParameter && testValue < higherParameter);
    }
}