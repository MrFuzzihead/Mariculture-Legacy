package mariculture.plugins;

import net.minecraftforge.fluids.FluidRegistry;

import mariculture.core.util.Fluids;
import mariculture.core.util.XPRegistry;
import mariculture.plugins.Plugins.Plugin;

public class PluginOpenBlocks extends Plugin {

    public PluginOpenBlocks(String name) {
        super(name);
    }

    @Override
    public void preInit() {
        return;
    }

    @Override
    public void init() {
        Fluids.add("xp", FluidRegistry.getFluid("xpjuice"), 20, true);
        XPRegistry.register("xpjuice", 20);
    }

    @Override
    public void postInit() {
        return;
    }
}
