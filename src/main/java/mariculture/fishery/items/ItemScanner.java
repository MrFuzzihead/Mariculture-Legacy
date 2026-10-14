package mariculture.fishery.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;

import mariculture.api.core.MaricultureTab;
import mariculture.core.gui.InventoryStorage;
import mariculture.core.items.ItemMCStorage;
import mariculture.fishery.gui.ContainerScanner;
import mariculture.fishery.gui.GuiScanner;

public class ItemScanner extends ItemMCStorage {

    public ItemScanner() {
        super(1, "scanner");
        setCreativeTab(MaricultureTab.tabFishery);
    }

    @Override
    public Slot getSlot(InventoryStorage storage, int i) {
        switch (i) {
            case 0:
                return new Slot(storage, i, 179, 14);
        }

        return new Slot(storage, i, 100, 100);
    }

    @Override
    public Object getGUIContainer(EntityPlayer player) {
        return new ContainerScanner(player.inventory, new InventoryStorage(player, size), player.worldObj);
    }

    @Override
    public Object getGUIElement(EntityPlayer player) {
        return new GuiScanner(player.inventory, new InventoryStorage(player, size), player.worldObj, gui);
    }
}
