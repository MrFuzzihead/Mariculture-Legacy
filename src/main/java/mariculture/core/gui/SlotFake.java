package mariculture.core.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public abstract class SlotFake extends Slot {

    public SlotFake(IInventory inv, int id, int x, int y) {
        super(inv, id, x, y);
    }

    /**
     * Handles a click on this fake slot.
     *
     * @param modifier the vanilla click mode (1 = shift-click), passed through from the container's
     *                 {@code slotClick} so implementations do not have to query client-only input state.
     * @return a non-null stack to fall through to the vanilla slot logic, or {@code null} when the click was handled
     */
    public abstract ItemStack handle(EntityPlayer player, int mouseButton, int modifier, Slot slot);
}
