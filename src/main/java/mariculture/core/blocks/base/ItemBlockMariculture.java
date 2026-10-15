package mariculture.core.blocks.base;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import mariculture.api.core.MaricultureTab;
import mariculture.core.config.GeneralStuff;
import mariculture.lib.base.ItemBlockBase;
import mariculture.lib.util.Text;

public abstract class ItemBlockMariculture extends ItemBlockBase {

    public ItemBlockMariculture(Block block) {
        super(block);
        setHasSubtypes(true);
    }

    @Override
    public CreativeTabs[] getCreativeTabs() {
        return new CreativeTabs[] { MaricultureTab.tabCore, MaricultureTab.tabFactory, MaricultureTab.tabFishery,
            MaricultureTab.tabMagic, MaricultureTab.tabWorld };
    }

    /**
     * Adds a one-line description of what the block does when the {@code Functional Block Tooltips} config is on
     * and {@link #hasTooltip()} is overridden. The description is looked up by convention as
     * {@code mariculture.tooltip.<lowercase name>}, so blocks without an entry simply show no extra line.
     */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (!GeneralStuff.BLOCK_TOOLTIPS || !hasTooltip()) return;

        String name = getName(stack);
        if (name == null || name.isEmpty()) return;

        // By convention mariculture.tooltip.<lowercase name>; blocks without an entry show no extra line.
        String key = "mariculture.tooltip." + name.toLowerCase();
        String description = Text.localize(key);
        // StatCollector returns the key unchanged when there is no translation for it
        if (description == null || description.isEmpty() || description.equals(key)) return;

        list.add(Text.GREY + description);
    }

    /**
     * Whether this item block should show a one-line functional description. Opt-in so that the name based lookup
     * cannot accidentally put a machine description on a decorative or building block with the same meta name.
     */
    protected boolean hasTooltip() {
        return false;
    }
}
