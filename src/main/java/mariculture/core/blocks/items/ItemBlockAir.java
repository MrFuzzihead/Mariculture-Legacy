package mariculture.core.blocks.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import mariculture.core.blocks.base.ItemBlockMariculture;
import mariculture.core.lib.AirMeta;

public class ItemBlockAir extends ItemBlockMariculture {

    public ItemBlockAir(Block block) {
        super(block);
    }

    @Override
    public String getName(ItemStack stack) {
        switch (stack.getItemDamage()) {
            case AirMeta.NATURAL_GAS:
                return "naturalGas";
            case AirMeta.FAKE_AIR:
                return "air";
            default:
                return "air";
        }
    }
}
