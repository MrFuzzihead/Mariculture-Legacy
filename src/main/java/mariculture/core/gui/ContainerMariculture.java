package mariculture.core.gui;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import cofh.api.energy.IEnergyReceiver;
import mariculture.api.core.IUpgradable;

public class ContainerMariculture extends Container {

    public TileEntity tile;

    public ContainerMariculture() {}

    public ContainerMariculture(TileEntity tile) {
        this.tile = tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return false;
    }

    protected void bindPlayerInventory(InventoryPlayer inventory) {
        bindPlayerInventory(inventory, 0);
    }

    protected void bindPlayerInventory(InventoryPlayer inventory, int yOffset) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + yOffset));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventory, i, 8 + i * 18, 142 + yOffset));
        }
    }

    protected void addUpgradeSlots(IUpgradable tile) {
        for (int i = 0; i < 3; i++) {
            addSlotToContainer(new SlotUpgrade((IInventory) tile, i, 179, 14 + i * 18));
        }
    }

    protected void addPowerSlot(IEnergyReceiver tile) {
        addSlotToContainer(new Slot((IInventory) tile, 3, 8, 62));
    }

    @Override
    public ItemStack slotClick(int slotID, int mouseButton, int modifier, EntityPlayer player) {
        // Vanilla sends -1 (outside any slot) and -999 (drop the cursor stack); those are handled by super. Any id
        // at or beyond the slot count is bogus and previously indexed out of bounds.
        if (slotID >= inventorySlots.size()) return null;

        Slot slot = slotID < 0 ? null : (Slot) inventorySlots.get(slotID);
        return slot instanceof SlotFake && ((SlotFake) slot).handle(player, mouseButton, modifier, slot) == null ? null
            : super.slotClick(slotID, mouseButton, modifier, player);
    }

    /**
     * Appends extra tooltip lines for an item hovered while this container is open.
     *
     * <p>
     * This lives on the container (rather than only on the client GUI) so that tooltip code does not have to
     * construct a throw-away {@code Gui}/{@code Container} for every rendered item tooltip. The default is a
     * no-op; machine containers override it.
     */
    public void addItemToolTip(ItemStack stack, List<String> list) {}
}
