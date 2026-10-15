package mariculture.core.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mariculture.api.fishery.fish.FishSpecies;
import mariculture.fishery.fish.FishBlaasop;
import mariculture.fishery.fish.FishBlaze;
import mariculture.fishery.fish.FishCod;
import mariculture.fishery.fish.FishTuna;

/**
 * Tests for the world-free temperature profile.
 *
 * <p>
 * The profile is a flat surface band (60..68) with cooling above and below. The old code had the two altitude
 * constants swapped ({@code y > 60} / {@code y < 68}), so there was no flat band and the value stepped at the band
 * edge; {@link #surfaceBandIsFlat()} fails against that behaviour. Fish placability is checked against the real
 * species base/tolerance values.
 */
public class EnvironmentHandlerTemperatureTest {

    private static final int PLAINS = 10;
    private static final int OCEAN = 4;
    private static final int NETHER = 80;
    private static final float NOON = 1.0F;

    private static int surface(int biomeTemperature) {
        return EnvironmentHandler.calculateTemperature(biomeTemperature, NOON, 0.0F, 64);
    }

    private static boolean canLive(FishSpecies fish, int temperature) {
        return temperature >= fish.getTemperatureBase() - fish.getTemperatureTolerance()
            && temperature <= fish.getTemperatureBase() + fish.getTemperatureTolerance();
    }

    @Test
    public void surfaceBandIsFlat() {
        for (int y = 60; y <= 68; y++) {
            assertEquals(
                "y=" + y + " must stay in the flat band",
                0.0D,
                EnvironmentHandler.getAltitudeOffset(y),
                1.0e-9);
            assertEquals(
                "y=" + y + " must keep the base temperature",
                EnvironmentHandler.calculateTemperature(PLAINS, NOON, 0.0F, 64),
                EnvironmentHandler.calculateTemperature(PLAINS, NOON, 0.0F, y));
        }

        assertEquals(14, surface(PLAINS));
    }

    @Test
    public void coolsAboveAndBelowTheSurfaceBand() {
        assertTrue(EnvironmentHandler.getAltitudeOffset(69) < 0.0D);
        assertTrue(EnvironmentHandler.getAltitudeOffset(59) < 0.0D);
        assertTrue(EnvironmentHandler.getAltitudeOffset(200) < EnvironmentHandler.getAltitudeOffset(100));
        assertTrue(EnvironmentHandler.getAltitudeOffset(0) < EnvironmentHandler.getAltitudeOffset(59));
    }

    @Test
    public void rainCoolingIsAppliedNotTruncated() {
        double dry = EnvironmentHandler.calculateTemperatureRaw(PLAINS, NOON, 0.0F, 64);
        double wet = EnvironmentHandler.calculateTemperatureRaw(PLAINS, NOON, 1.0F, 64);
        assertEquals(0.15D, dry - wet, 1.0e-9);
    }

    @Test
    public void representativeFishArePlacableInTheirHomes() {
        // Temperate surface: cod and blaasop are fine, tuna is not (it wants cold water).
        int temperate = surface(PLAINS);
        assertTrue(canLive(new FishCod(), temperate));
        assertTrue(canLive(new FishBlaasop(), temperate));
        assertFalse(canLive(new FishTuna(), temperate));

        // Cold surface: tuna is fine.
        assertTrue(canLive(new FishTuna(), surface(OCEAN)));

        // Nether heat: blaze is fine.
        assertTrue(canLive(new FishBlaze(), surface(NETHER)));
    }

    @Test
    public void altitudeExcludesSurfaceFish() {
        int high = EnvironmentHandler.calculateTemperature(PLAINS, NOON, 0.0F, 200);
        assertFalse(canLive(new FishCod(), high));
        assertFalse(canLive(new FishTuna(), high));
        assertFalse(canLive(new FishBlaasop(), high));
    }
}
