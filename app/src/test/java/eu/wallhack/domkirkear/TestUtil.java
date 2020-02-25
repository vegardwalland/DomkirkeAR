package eu.wallhack.domkirkear;

import static org.junit.Assert.assertTrue;

public class TestUtil {
    private static final int DEFAULT_TOLERANCE = 2;

    public static void assertAlmostEquals(String message, double expected, double actual, int tolerance) {
        assertTrue(message, actual >= expected - tolerance && actual <= expected + tolerance);
    }

    public static void assertAlmostEquals(String message, double expected, double actual) {
        assertAlmostEquals(message, expected, actual, DEFAULT_TOLERANCE);
    }

    public static void assertAlmostEquals(double expected, double actual, int tolerance) {
        assertAlmostEquals("", expected, actual, tolerance);
    }

    public static void assertAlmostEquals(double expected, double actual) {
        assertAlmostEquals("", expected, actual, DEFAULT_TOLERANCE);
    }

}
