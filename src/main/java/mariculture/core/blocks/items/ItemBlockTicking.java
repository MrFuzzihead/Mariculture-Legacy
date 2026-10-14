package mariculture.core.blocks.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import mariculture.core.blocks.base.ItemBlockMariculture;
import mariculture.core.lib.TickingMeta;

public class ItemBlockTicking extends ItemBlockMariculture {

    public ItemBlockTicking(Block block) {
        super(block);
    }

    @Override
    protected boolean hasTooltip() {
        return true;
    }

    @Override
    public String getName(ItemStack stack) {
        switch (stack.getItemDamage()) {
            case TickingMeta.NET:
                return "net";
            default:
                return getUnlocalizedName();
        }
    }
}
