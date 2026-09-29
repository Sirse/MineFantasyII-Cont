package minefantasy.mf2.gametest;

import java.util.Arrays;
import java.util.Objects;

import com.gtnewhorizons.horizonqa.api.GameTestAssertException;

/**
 * Assertions for the game tests with the familiar argument order (message first). A failure throws Horizon-QA's
 * assertion error, so the test is reported as failed rather than errored.
 */
public final class Assert {

    private Assert() {}

    /** Code that may throw anything; for {@link #assertThrows}. */
    public interface Action {

        void run() throws Exception;
    }

    public static void fail(String message) {
        throw new GameTestAssertException(message, 0, 0, 0);
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }

    public static void assertTrue(boolean condition) {
        assertTrue("expected true", condition);
    }

    public static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    public static void assertFalse(boolean condition) {
        assertFalse("expected false", condition);
    }

    public static void assertNull(String message, Object actual) {
        if (actual != null) {
            fail(message + ": expected null, was <" + actual + ">");
        }
    }

    public static void assertNull(Object actual) {
        assertNull("expected null", actual);
    }

    public static void assertNotNull(String message, Object actual) {
        assertTrue(message, actual != null);
    }

    public static void assertNotNull(Object actual) {
        assertNotNull("expected a value, was null", actual);
    }

    public static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            fail(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals("not equal", expected, actual);
    }

    public static void assertEquals(String message, long expected, long actual) {
        if (expected != actual) {
            fail(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals("not equal", expected, actual);
    }

    public static void assertEquals(String message, double expected, double actual, double delta) {
        // Written so NaN fails: any comparison with NaN is false, so "difference > delta" would let it through
        boolean same = Double.compare(expected, actual) == 0 || Math.abs(expected - actual) <= delta;
        if (!same) {
            fail(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals("not equal", expected, actual, delta);
    }

    public static void assertSame(String message, Object expected, Object actual) {
        if (expected != actual) {
            fail(message + ": expected the same <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertSame(Object expected, Object actual) {
        assertSame("not the same", expected, actual);
    }

    public static void assertNotSame(Object unexpected, Object actual) {
        if (unexpected == actual) {
            fail("expected another object than <" + actual + ">");
        }
    }

    public static void assertArrayEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            fail("expected " + Arrays.toString(expected) + " but was " + Arrays.toString(actual));
        }
    }

    public static <T extends Throwable> T assertThrows(Class<T> expected, Action action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return expected.cast(thrown);
            }
            fail("expected " + expected.getSimpleName() + " but got " + thrown);
        }
        fail("expected " + expected.getSimpleName() + " but nothing was thrown");
        return null;
    }
}
