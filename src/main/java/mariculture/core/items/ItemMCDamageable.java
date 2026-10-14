package mariculture.core.items;

import net.minecraft.creativetab.CreativeTabs;

import mariculture.api.core.MaricultureTab;
import mariculture.core.lib.MCModInfo;
import mariculture.lib.base.ItemBaseDamageable;

public class ItemMCDamageable extends ItemBaseDamageable {

    public ItemMCDamageable(int dmg) {
        super(MCModInfo.MODPATH, MaricultureTab.tabCore, dmg);
    }

    public ItemMCDamageable(String mod, CreativeTabs tab, int dmg) {
        super(mod, tab, dmg);
    }
}
