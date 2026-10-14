package mariculture.core.blocks.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import mariculture.core.blocks.base.ItemBlockMariculture;
import mariculture.core.lib.GroundMeta;

public class ItemBlockGround extends ItemBlockMariculture {

    public ItemBlockGround(Block block) {
        super(block);
    }

    @Override
    public String getName(ItemStack itemstack) {
        switch (itemstack.getItemDamage()) {
            case GroundMeta.BUBBLES:
                return "gas";
            case GroundMeta.ANCIENT:
                return "ancientSand";
            default:
                return "groundBlocks";
        }
    }
}
