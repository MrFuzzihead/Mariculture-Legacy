package mariculture.mixins.early;

import net.minecraft.item.Item;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemFishingRod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import mariculture.fishery.items.ItemVanillaFish;
import mariculture.fishery.items.ItemVanillaRod;

/**
 * Replaces the vanilla fishing rod and raw fish items with Mariculture's versions.
 *
 * <p>
 * This replaces the old {@code mariculture.fishery.VanillaOverride} ASM coremod, which walked the instruction list of
 * {@code Item.registerItems()} and rewrote the {@code NEW}/{@code INVOKESPECIAL} pair after the "fishing_rod" and
 * "fish"
 * string constants. Redirecting the constructors is the mixin equivalent and keeps the chained
 * {@code setUnlocalizedName(...).setTextureName(...)} calls on the Mariculture instances.
 */
@Mixin(Item.class)
public class MixinItem {

    @Redirect(
        method = "registerItems",
        at = @At(value = "NEW", target = "()Lnet/minecraft/item/ItemFishingRod;"),
        require = 1)
    private static ItemFishingRod mariculture$replaceFishingRod() {
        return new ItemVanillaRod();
    }

    @Redirect(
        method = "registerItems",
        at = @At(value = "NEW", target = "(Z)Lnet/minecraft/item/ItemFishFood;", ordinal = 0),
        require = 1)
    private static ItemFishFood mariculture$replaceRawFish(boolean cooked) {
        return new ItemVanillaFish(cooked);
    }
}
