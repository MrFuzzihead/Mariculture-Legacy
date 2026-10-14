package mariculture.core.blocks.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import mariculture.core.blocks.base.ItemBlockMariculture;
import mariculture.core.lib.GlassMeta;

public class ItemBlockGlass extends ItemBlockMariculture {

    public ItemBlockGlass(Block block) {
        super(block);
    }

    @Override
    public String getName(ItemStack itemstack) {
        switch (itemstack.getItemDamage()) {
            case GlassMeta.HEAT:
                return "heatglass";
            default:
                return "heatglass";
        }
    }
}
