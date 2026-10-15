package mariculture.core.blocks.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import mariculture.core.blocks.base.ItemBlockMariculture;
import mariculture.core.lib.WaterMeta;

public class ItemBlockWater extends ItemBlockMariculture {

    public ItemBlockWater(Block block) {
        super(block);
    }

    @Override
    protected boolean hasTooltip() {
        return true;
    }

    @Override
    public String getName(ItemStack stack) {
        switch (stack.getItemDamage()) {
            case WaterMeta.OYSTER:
                return "oyster";
            default:
                return getUnlocalizedName();
        }
    }
}
