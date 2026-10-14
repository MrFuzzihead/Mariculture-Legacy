package mariculture.core.blocks.base;

import net.minecraft.block.material.Material;

import mariculture.api.core.MaricultureTab;
import mariculture.core.lib.MCModInfo;
import mariculture.lib.base.BlockBaseMeta;

public abstract class BlockMCBaseMeta extends BlockBaseMeta {

    public BlockMCBaseMeta(Material material) {
        super(material, MCModInfo.MODPATH, MaricultureTab.tabCore);
    }
}
