package mariculture.core.gui;

import net.minecraft.entity.player.InventoryPlayer;

import mariculture.core.gui.feature.FeatureArrow;
import mariculture.core.gui.feature.FeatureEject;
import mariculture.core.gui.feature.FeatureRedstone;
import mariculture.core.gui.feature.FeatureTank;
import mariculture.core.gui.feature.FeatureTank.TankSize;
import mariculture.core.gui.feature.FeatureUpgrades;
import mariculture.core.tile.TileCrucible;

public class GuiCrucible extends GuiMariculture {

    private final TileCrucible tile;

    public GuiCrucible(InventoryPlayer player, TileCrucible tile) {
        super(new ContainerCrucible(tile, player), "liquifier", 10);
        this.tile = tile;
        features.add(new FeatureUpgrades());
        features.add(new FeatureTank(tile, 98, 19, TankSize.DOUBLE));
        features.add(new FeatureArrow(tile, 65, 41));
        features.add(new FeatureRedstone(tile));
        features.add(new FeatureEject(tile));
    }

    @Override
    public void addToolTip() {
        if (mouseX >= 12 && mouseX <= 16 && mouseY >= 18 && mouseY <= 77) {
            tooltip.add(tile.getRealTemperature() + "\u00B0" + "C");
        }
    }

    @Override
    public void drawBackground(int x, int y) {
        int temp = tile.getTemperatureScaled(60);
        drawTexturedModalRect(x + 12, y + 18 + 60 - temp, 251, 60 - temp, 5, temp);

        int burn = tile.getBurnTimeRemainingScaled();
        drawTexturedModalRect(x + 38, y + 43 + 12 - burn, 242, 60 + 12 - burn, 14, burn);
    }

    @Override
    public int getX() {
        return 16;
    }
}
