package mariculture.core.gui;

import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Regression test for the {@code slotClick} bounds guard.
 *
 * <p>
 * The guard used to be {@code slotID > inventorySlots.size()} (strict), so a click id equal to the slot count
 * indexed off the end of the slot list. Vanilla should never send such an id, but the guard exists precisely to
 * tolerate junk input, and the old comparison then threw instead of ignoring it.
 */
public class ContainerMaricultureTest {

    @Test
    public void outOfRangeClickIsIgnored() {
        ContainerMariculture container = new ContainerMariculture();

        assertNull(container.slotClick(0, 0, 0, null));
        assertNull(container.slotClick(54, 0, 0, null));
        assertNull(container.slotClick(Integer.MAX_VALUE, 0, 0, null));
    }
}
