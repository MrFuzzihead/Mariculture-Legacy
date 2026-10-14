package maritech.extensions.blocks;

import net.minecraft.tileentity.TileEntity;

import mariculture.core.lib.MachineMultiMeta;
import mariculture.core.lib.Modules;
import maritech.tile.TileIncubator;

public class ExtensionMachineMulti extends ExtensionBlocksBase {

    @Override
    public String getName(int meta, String name) {
        switch (meta) {
            case MachineMultiMeta.INCUBATOR_BASE:
                return "incubatorBase";
            case MachineMultiMeta.INCUBATOR_TOP:
                return "incubatorTop";
        }

        return name;
    }

    @Override
    public String getMod(int meta, String mod) {
        switch (meta) {
            case MachineMultiMeta.INCUBATOR_BASE:
            case MachineMultiMeta.INCUBATOR_TOP:
                return "maritech";
        }

        return mod;
    }

    @Override
    public boolean isActive(int meta, boolean isActive) {
        switch (meta) {
            case MachineMultiMeta.INCUBATOR_BASE:
            case MachineMultiMeta.INCUBATOR_TOP:
                return Modules.isActive(Modules.fishery);
        }

        return isActive;
    }

    @Override
    public TileEntity getTileEntity(int meta, TileEntity tile) {
        switch (meta) {
            case MachineMultiMeta.INCUBATOR_BASE:
            case MachineMultiMeta.INCUBATOR_TOP:
                return new TileIncubator();
        }

        return tile;
    }
}
