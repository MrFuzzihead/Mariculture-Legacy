package mariculture.core.items;

import net.minecraft.creativetab.CreativeTabs;

import mariculture.api.core.MaricultureTab;
import mariculture.core.lib.MCModInfo;
import mariculture.lib.base.ItemBaseMeta;

public abstract class ItemMCMeta extends ItemBaseMeta {

    public ItemMCMeta() {
        super(MCModInfo.MODPATH, MaricultureTab.tabCore);
    }

    @Override
    public CreativeTabs[] getCreativeTabs() {
        return new CreativeTabs[] { MaricultureTab.tabCore, MaricultureTab.tabFactory, MaricultureTab.tabFishery,
            MaricultureTab.tabMagic, MaricultureTab.tabWorld };
    }
}
