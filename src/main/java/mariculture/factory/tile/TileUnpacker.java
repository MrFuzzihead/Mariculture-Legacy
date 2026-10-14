package mariculture.factory.tile;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import mariculture.core.helpers.cofh.InventoryHelper;
import mariculture.core.tile.base.TileStorage;
import mariculture.factory.UnpackerHelper;

public class TileUnpacker extends TileStorage implements ISidedInventory {

    public static final int INPUT = 0;
    public static final int[] SLOTS = new int[] { INPUT };

    /** How many items are unpacked per tick. */
    private static final int ITEMS_PER_TICK = 1;

    public TileUnpacker() {
        inventory = new ItemStack[1];
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || inventory[INPUT] == null) return;

        TileEntity tile = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
        if (!(tile instanceof IInventory)) return;
        IInventory target = (IInventory) tile;
        int side = ForgeDirection.UP.ordinal();

        // Unpack one item at a time and stop as soon as the output cannot take another result. The previous
        // implementation unpacked the whole incoming stack with a single recipe lookup and never stored it, so all
        // but one item of a stack were silently destroyed.
        for (int i = 0; i < ITEMS_PER_TICK && inventory[INPUT] != null; i++) {
            ItemStack single = inventory[INPUT].copy();
            single.stackSize = 1;
            ItemStack result = UnpackerHelper.unpack(worldObj, single);
            if (result == null) return;

            if (!InventoryHelper.canInsertItemStackIntoInventory(target, result, side)) return;
            if (InventoryHelper.insertItemStackIntoInventory(target, result, side) != null) return;

            decrStackSize(INPUT, 1);
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int slot) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        if (slot == INPUT && side == ForgeDirection.UP.ordinal()) {
            ItemStack single = stack.copy();
            single.stackSize = 1;
            ItemStack ret = UnpackerHelper.unpack(worldObj, single);
            if (ret == null) return false;
            else {
                TileEntity tile = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
                if (tile instanceof IInventory) {
                    return InventoryHelper
                        .canInsertItemStackIntoInventory((IInventory) tile, ret, ForgeDirection.UP.ordinal());
                } else return false;
            }
        } else return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return false;
    }
}
