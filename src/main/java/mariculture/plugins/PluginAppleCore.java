package mariculture.plugins;

import mariculture.core.lib.Extra;
import mariculture.plugins.Plugins.Plugin;

public class PluginAppleCore extends Plugin {

    public PluginAppleCore(String name) {
        super(name);
    }

    @Override
    public void preInit() {
        Extra.NERF_FOOD = true;
    }

    @Override
    public void init() {

    }

    @Override
    public void postInit() {

    }
}
