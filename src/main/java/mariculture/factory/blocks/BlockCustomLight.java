package mariculture.factory.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import mariculture.core.lib.PlansMeta;

public class BlockCustomLight extends BlockCustomBase {

    public BlockCustomLight() {
        super(Material.rock);
    }

    @Override
    public int getID() {
        return PlansMeta.LIGHT;
    }

    @Override
    public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
        return true;
    }
}
