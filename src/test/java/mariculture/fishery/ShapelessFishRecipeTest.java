package mariculture.fishery;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Units for the egg-to-caviar conversion.
 *
 * <p>
 * The count is one caviar per 650 remaining fertility, rounded up, with a minimum of one. The previous expression
 * {@code Math.ceil(eggs / 650)} performed integer division first, so 651-1299 eggs produced one caviar.
 */
public class ShapelessFishRecipeTest {

    @Test
    public void roundsUpInsteadOfTruncating() {
        assertEquals(1, ShapelessFishRecipe.getCaviarCount(0));
        assertEquals(1, ShapelessFishRecipe.getCaviarCount(1));
        assertEquals(1, ShapelessFishRecipe.getCaviarCount(650));
        assertEquals(2, ShapelessFishRecipe.getCaviarCount(651));
        assertEquals(2, ShapelessFishRecipe.getCaviarCount(1299));
        assertEquals(2, ShapelessFishRecipe.getCaviarCount(1300));
        assertEquals(3, ShapelessFishRecipe.getCaviarCount(1301));
    }

    @Test
    public void neverReturnsLessThanOne() {
        assertEquals(1, ShapelessFishRecipe.getCaviarCount(-100));
    }
}
