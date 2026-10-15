package mariculture.core.blocks.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import mariculture.core.blocks.base.ItemBlockMariculture;
import mariculture.core.events.MaricultureEvents;
import mariculture.core.lib.MachineRenderedMultiMeta;
import mariculture.lib.util.Text;

public class ItemBlockRenderedMachineMulti extends ItemBlockMariculture {

    public ItemBlockRenderedMachineMulti(Block block) {
        super(block);
    }

    @Override
    protected boolean hasTooltip() {
        return true;
    }

    @Override
    public String getName(ItemStack stack) {
        int meta = stack.getItemDamage();
        switch (meta) {
            case MachineRenderedMultiMeta.VAT:
                return "vat";
            case MachineRenderedMultiMeta.SIFTER:
                return "sifter";
        }

        return MaricultureEvents.getItemName(field_150939_a, meta, "renderedDoubleBlocks");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String unlocalized = field_150939_a.getUnlocalizedName()
            .replace("tile.", "")
            .replace("_", ".");
        String name = getName(stack).replaceAll("(.)([A-Z])", "$1$2")
            .toLowerCase();
        return Text.localize(
            unlocalized.replace(
                "mariculture.",
                MaricultureEvents.getMod(stack.getItem(), stack.getItemDamage(), "mariculture") + ".") + "." + name);
    }
}
