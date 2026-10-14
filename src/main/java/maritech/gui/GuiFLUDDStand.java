package maritech.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import mariculture.core.gui.GuiMariculture;
import mariculture.core.gui.feature.FeatureRedstone;
import mariculture.core.gui.feature.FeatureTank;
import mariculture.core.gui.feature.FeatureTank.TankSize;
import mariculture.core.gui.feature.FeatureUpgrades;
import maritech.lib.MTModInfo;
import maritech.tile.TileFLUDDStand;

public class GuiFLUDDStand extends GuiMariculture {

    public static final ResourceLocation texture = new ResourceLocation(
        MTModInfo.MODPATH,
        "textures/gui/fluddstand.png");

    public GuiFLUDDStand(InventoryPlayer player, TileFLUDDStand tile) {
        super(new ContainerFLUDDStand(tile, player), texture, 10);
        features.add(new FeatureTank(tile, 61, 19, TankSize.DOUBLE));
        features.add(new FeatureRedstone(tile));
        features.add(new FeatureUpgrades());
    }

    @Override
    public int getX() {
        return 60;
    }
}
