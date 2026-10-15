package mariculture.core.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mariculture.api.core.Environment.Salinity;

/**
 * Pure-logic tests for the fish environment check. Salinity has three values (FRESH=0, BRACKISH=1, SALINE=2), so
 * the upper bound of a tolerance range must be clamped with {@code Math.min(2, ...)}; the previous
 * {@code Math.max(2, ...)} made the upper bound a no-op and let fish match water they should not tolerate.
 */
public class EnvironmentHandlerTest {

    private final EnvironmentHandler handler = new EnvironmentHandler();

    private boolean matches(Salinity biomeSalt, Salinity baseSalt, int saltTolerance, int biomeTemp, int baseTemp,
        int tempTolerance) {
        return handler.matches(biomeSalt, biomeTemp, baseSalt, saltTolerance, baseTemp, tempTolerance);
    }

    @Test
    public void exactMatchPasses() {
        assertTrue(matches(Salinity.FRESH, Salinity.FRESH, 0, 10, 10, 0));
        assertTrue(matches(Salinity.SALINE, Salinity.SALINE, 0, 10, 10, 0));
    }

    @Test
    public void zeroToleranceRejectsOtherSalinities() {
        assertFalse(matches(Salinity.BRACKISH, Salinity.FRESH, 0, 10, 10, 0));
        assertFalse(matches(Salinity.SALINE, Salinity.FRESH, 0, 10, 10, 0));
        assertFalse(matches(Salinity.FRESH, Salinity.SALINE, 0, 10, 10, 0));
    }

    @Test
    public void toleranceWidensTheRangeButStaysWithinTheEnum() {
        // BRACKISH +/- 2 -> [0, 2] under the fix; the old Math.max(2, ...) also produced >= 2 and hid the error.
        assertTrue(matches(Salinity.FRESH, Salinity.BRACKISH, 2, 10, 10, 0));
        assertTrue(matches(Salinity.SALINE, Salinity.BRACKISH, 2, 10, 10, 0));

        // SALINE tolerance must not reach FRESH.
        assertFalse(matches(Salinity.FRESH, Salinity.SALINE, 1, 10, 10, 0));
        assertTrue(matches(Salinity.BRACKISH, Salinity.SALINE, 1, 10, 10, 0));
    }

    @Test
    public void temperatureRangeIsEnforced() {
        assertFalse(matches(Salinity.FRESH, Salinity.FRESH, 0, 20, 10, 0));
        assertTrue(matches(Salinity.FRESH, Salinity.FRESH, 0, 15, 10, 5));
        assertFalse(matches(Salinity.FRESH, Salinity.FRESH, 0, 16, 10, 5));
    }
}
