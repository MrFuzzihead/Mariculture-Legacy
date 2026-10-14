package maritech.handlers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import mariculture.core.lib.ArmorSlot;

public class ScubaTank {

    public static void activate(EntityPlayer player) {
        ItemStack scuba = player.inventory.armorItemInSlot(ArmorSlot.TOP);
        if (scuba.getItemDamage() < scuba.getMaxDamage()) {
            player.setAir(300);
        }
    }

    public static void damage(ItemStack stack, EntityPlayer player) {
        if (stack.getItemDamage() < stack.getMaxDamage() && player.worldObj.getTotalWorldTime() % 150 == 0) {
            stack.damageItem(1, player);
        }
    }
}
