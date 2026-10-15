package mariculture.core.gui;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

import mariculture.api.core.FuelInfo;
import mariculture.api.core.IItemUpgrade;
import mariculture.api.core.MaricultureHandlers;
import mariculture.api.core.RecipeSmelter;
import mariculture.core.helpers.FluidHelper;
import mariculture.core.tile.TileCrucible;
import mariculture.core.util.MCTranslate;
import mariculture.lib.util.Text;

public class ContainerCrucible extends ContainerMachine {

    private final TileCrucible crucible;

    public ContainerCrucible(TileCrucible tile, InventoryPlayer inventory) {
        super(tile);
        this.crucible = tile;

        addUpgradeSlots(tile);

        addSlotToContainer(new SlotFluidContainer(tile, 3, 145, 36));
        addSlotToContainer(new SlotOutput(tile, 4, 145, 67));

        for (int i = 5; i <= 6; i++) {
            addSlotToContainer(new Slot(tile, i, 29 + (i - 5) * 18, 21));
        }

        addSlotToContainer(new SlotFuel(tile, 7, 38, 59));
        addSlotToContainer(new SlotOutput(tile, 8, 145, 13));

        bindPlayerInventory(inventory, 10);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotID) {
        int size = getSizeInventory();
        int low = size + 27;
        int high = low + 9;
        ItemStack itemstack = null;
        Slot slot = (Slot) inventorySlots.get(slotID);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            itemstack = stack.copy();

            if (slotID < size) {
                if (!mergeItemStack(stack, size, high, true)) return null;

                slot.onSlotChange(stack, itemstack);
            } else if (slotID >= size) {
                if (MaricultureHandlers.crucible.getFuelInfo(stack) != null) {
                    if (!mergeItemStack(stack, 7, 8, false)) return null;
                } else if (MaricultureHandlers.crucible.getResult(stack, null, -1, true) != null) {
                    if (!mergeItemStack(stack, 5, 7, false)) return null;
                } else if (FluidHelper.isFluidOrEmpty(stack)) {
                    if (!mergeItemStack(stack, 3, 4, false)) return null;
                } else if (stack.getItem() instanceof IItemUpgrade) {
                    if (!mergeItemStack(stack, 0, 3, false)) return null;
                } else if (slotID >= size && slotID < low) {
                    if (!mergeItemStack(stack, low, high, false)) return null;
                } else if (slotID >= low && slotID < high && !mergeItemStack(stack, high, low, false)) return null;
            } else if (!mergeItemStack(stack, size, high, false)) return null;

            if (stack.stackSize == 0) {
                slot.putStack((ItemStack) null);
            } else {
                slot.onSlotChanged();
            }

            if (stack.stackSize == itemstack.stackSize) return null;

            slot.onPickupFromSlot(player, stack);
        }

        return itemstack;
    }

    @Override
    public void addItemToolTip(ItemStack stack, List<String> currenttip) {
        boolean ethereal = MaricultureHandlers.upgrades.hasUpgrade("ethereal", crucible);
        if (stack != null) {
            int meltingPoint = MaricultureHandlers.crucible.getMeltingPoint(stack, ethereal);
            FuelInfo info = MaricultureHandlers.crucible.getFuelInfo(stack);
            if (FluidContainerRegistry.isFilledContainer(stack)) {
                FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
                info = MaricultureHandlers.crucible.getFuelInfo(fluid);
            }

            if (meltingPoint > 0) {
                currenttip.add(Text.ORANGE + MCTranslate.translate("melting") + ": " + meltingPoint + "\u00B0" + "C");
            }

            if (MaricultureHandlers.crucible.getResult(stack, null, -1, ethereal) != null) {
                RecipeSmelter result = MaricultureHandlers.crucible.getResult(stack, null, -1, ethereal);
                if (result.fluid.amount > 0) {
                    if (result.rands != null) {
                        currenttip.add(Text.INDIGO + MCTranslate.translate("randomMetal"));
                    } else {
                        currenttip
                            .add(Text.INDIGO + FluidHelper.getName(result.fluid) + ": " + result.fluid.amount + "mB");
                    }
                }

                if (result.output != null && result.chance > 0) {
                    int chance = (int) ((float) 1 / result.chance * 100);
                    currenttip
                        .add(Text.GREY + chance + MCTranslate.translate("percent") + result.output.getDisplayName());
                }
            }

            if (info != null) {
                if (FluidContainerRegistry.isFilledContainer(stack)) {
                    currenttip.add(Text.DARK_AQUA + MCTranslate.translate("asFluid"));
                    currenttip.add(
                        Text.WHITE + MCTranslate.translate("perTempFluid") + ": " + info.maxTempPer + "\u00B0" + "C");
                } else {
                    currenttip.add(Text.DARK_GREEN + MCTranslate.translate("asSolid"));
                    currenttip.add(
                        Text.WHITE + MCTranslate.translate("perTempSolid") + ": " + info.maxTempPer + "\u00B0" + "C");
                }

                currenttip.add(Text.GREY + MCTranslate.translate("maxTemp") + ": " + info.maxTemp + "\u00B0" + "C");
            }
        }
    }
}
