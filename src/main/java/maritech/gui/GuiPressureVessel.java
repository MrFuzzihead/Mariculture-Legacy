package maritech.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import mariculture.core.gui.GuiMariculture;
import mariculture.core.gui.feature.FeatureTank;
import mariculture.core.gui.feature.FeatureTank.TankSize;
import mariculture.core.gui.feature.FeatureUpgrades;
import mariculture.core.helpers.BlockHelper;
import maritech.lib.MTModInfo;
import maritech.tile.TilePressureVessel;

public class GuiPressureVessel extends GuiMariculture {

    public static final ResourceLocation texture = new ResourceLocation(
        MTModInfo.MODPATH,
        "textures/gui/pressurevessel.png");
    private TilePressureVessel tile;

    public GuiPressureVessel(InventoryPlayer player, TilePressureVessel tile) {
        super(new ContainerPressureVessel(tile, player), texture, 10);
        this.tile = tile;
        features.add(new FeatureTank(tile, 87, 19, TankSize.DOUBLE));
        features.add(new FeatureUpgrades());
    }

    @Override
    public String getName() {
        return BlockHelper.getName(tile);
    }

    @Override
    public int getX() {
        return 46;
    }
}
