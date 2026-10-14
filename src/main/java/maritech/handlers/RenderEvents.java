package maritech.handlers;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mariculture.core.Core;
import mariculture.core.events.IconEvent.IconRegisterEvent;
import mariculture.core.events.RenderEvent;
import mariculture.core.lib.MachineRenderedMeta;
import maritech.extensions.modules.ExtensionFactory;
import maritech.lib.SpecialIcons;

@SideOnly(Side.CLIENT)
public class RenderEvents {

    @SubscribeEvent
    public void onRender(RenderEvent event) {
        if (event.stack.getItem() == Item.getItemFromBlock(Core.renderedMachines)
            && event.stack.getItemDamage() == MachineRenderedMeta.FLUDD_STAND) {
            ItemStack fludd = new ItemStack(ExtensionFactory.fludd);
            IItemRenderer customRenderer = MinecraftForgeClient
                .getItemRenderer(new ItemStack(ExtensionFactory.fludd), INVENTORY);
            customRenderer.renderItem(INVENTORY, fludd);
        }
    }

    @SubscribeEvent
    public void registerIcons(IconRegisterEvent event) {
        SpecialIcons.registerIcons(event.register);
    }
}
