package mariculture.core.blocks.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import mariculture.core.blocks.base.ItemBlockMariculture;
import mariculture.core.lib.TransparentMeta;

public class ItemBlockTransparent extends ItemBlockMariculture {

    public ItemBlockTransparent(Block block) {
        super(block);
    }

    @Override
    public String getName(ItemStack itemstack) {
        switch (itemstack.getItemDamage()) {
            case TransparentMeta.PLASTIC:
                return "plastic";
            default:
                return "plastic";
        }
    }
}
