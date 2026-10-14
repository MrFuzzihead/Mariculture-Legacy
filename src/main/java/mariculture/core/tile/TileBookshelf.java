package mariculture.core.tile;

import net.minecraft.item.ItemStack;

import mariculture.core.tile.base.TileStorage;
import mariculture.core.util.IHasGUI;

public class TileBookshelf extends TileStorage implements IHasGUI {

    public TileBookshelf() {
        inventory = new ItemStack[9];
    }
}
