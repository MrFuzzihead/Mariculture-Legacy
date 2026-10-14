package mariculture.diving;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;

import mariculture.core.helpers.PlayerHelper;
import mariculture.core.lib.ArmorSlot;

public class Snorkel {

    public static void init(EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            if (PlayerHelper.hasArmor(player, ArmorSlot.HAT, Diving.snorkel)) {
                if (player.isInsideOfMaterial(Material.water)) {
                    if (player.worldObj.getTotalWorldTime() % 2 == 0) {
                        player.setAir(player.getAir() + 1);
                    }
                }
            }
        }
    }
}
