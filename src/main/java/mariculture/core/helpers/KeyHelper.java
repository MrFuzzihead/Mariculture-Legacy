package mariculture.core.helpers;

import net.minecraft.client.settings.GameSettings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mariculture.MCClientProxy;

@SideOnly(Side.CLIENT)
public class KeyHelper {

    public static boolean isToggleKeyPressed() {
        return GameSettings.isKeyDown(MCClientProxy.key_toggle);
    }

    public static boolean isActivateKeyPressed() {
        return GameSettings.isKeyDown(MCClientProxy.key_activate);
    }

}
