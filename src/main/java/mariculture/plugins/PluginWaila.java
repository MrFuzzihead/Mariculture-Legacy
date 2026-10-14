package mariculture.plugins;

import cpw.mods.fml.common.event.FMLInterModComms;
import mariculture.plugins.Plugins.Plugin;

public class PluginWaila extends Plugin {

    public PluginWaila(String name) {
        super(name);
    }

    @Override
    public void preInit() {}

    @Override
    public void init() {
        FMLInterModComms.sendMessage("Waila", "register", "mariculture.plugins.waila.WailaRegistrar.register");
    }

    @Override
    public void postInit() {}
}
