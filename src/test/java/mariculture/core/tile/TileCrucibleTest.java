package mariculture.core.tile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the crucible melt-speed multiplier.
 *
 * <p>
 * The multiplier must depend on how far the crucible is <em>above</em> the recipe's melting point. The old
 * formula capped against {@code 2000 - recipeTemp} without subtracting {@code recipeTemp}, so it was constant for
 * a given recipe; {@link #atMeltingPointTheMultiplierIsOne()} fails against it.
 */
public class TileCrucibleTest {

    /** Convert a 0..2000 display temperature to the internal 0..MAX_TEMP scale used by the tile. */
    private static int internalTemp(int displayTemp) {
        return displayTemp * 25000 / 2000;
    }

    @Test
    public void atMeltingPointTheMultiplierIsOne() {
        assertEquals(1.0D, TileCrucible.getMeltingModifier(internalTemp(1538), 1538), 1.0e-9);
        assertEquals(1.0D, TileCrucible.getMeltingModifier(0, 0), 1.0e-9);
    }

    @Test
    public void hotterCrucibleMeltsFaster() {
        double atMeltingPoint = TileCrucible.getMeltingModifier(internalTemp(1000), 1000);
        double partway = TileCrucible.getMeltingModifier(internalTemp(1500), 1000);
        double fullHeat = TileCrucible.getMeltingModifier(internalTemp(2000), 1000);

        assertTrue("expected " + partway + " > " + atMeltingPoint, partway > atMeltingPoint);
        assertTrue("expected " + fullHeat + " > " + partway, fullHeat > partway);
    }

    @Test
    public void multiplierIsCappedByTheHeadRoomToMaximumHeat() {
        // recipe at 1538 leaves 462 degrees of overheat; 462 * 3.333333 / 2000 == 0.77
        assertEquals(1.77D, TileCrucible.getMeltingModifier(internalTemp(2000), 1538), 1.0e-3);
        // a zero-temperature recipe can use the whole 2000 degrees
        assertEquals(1.0D + 3.333333D, TileCrucible.getMeltingModifier(internalTemp(2000), 0), 1.0e-3);
    }

    @Test
    public void belowMeltingPointDoesNotGoNegative() {
        assertEquals(1.0D, TileCrucible.getMeltingModifier(internalTemp(500), 1000), 1.0e-9);
    }
}
