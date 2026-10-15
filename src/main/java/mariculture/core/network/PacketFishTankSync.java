package mariculture.core.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import mariculture.fishery.tile.TileFishTank;
import mariculture.lib.helpers.ClientHelper;

/**
 * Synchronises a fish tank's full inventory (and facing) to the client.
 *
 * <p>
 * This used to only carry slots 0-2, which left the GUI's remaining 51 slots permanently desynchronised, and it
 * wrote a boolean <em>and</em> a full item stack for every slot while reading the stack only when the boolean was
 * true, which shifted the payload by two bytes for each null slot. The codec below is symmetric: a single boolean
 * marks a null stack, and the stack size is written as an {@code int} because fish tank slots accept very large
 * stacks ({@code Integer.MAX_VALUE}) that do not fit in the byte {@link ByteBufUtils#writeItemStack} uses.
 */
public class PacketFishTankSync extends PacketCoords implements IMessageHandler<PacketFishTankSync, IMessage> {

    /** Upper bound to reject a corrupt/hostile payload before allocating. */
    private static final int MAX_INVENTORY_SIZE = 4096;

    private ForgeDirection dir;
    private ItemStack[] inventory;

    public PacketFishTankSync() {}

    public PacketFishTankSync(ItemStack[] inventory, int x, int y, int z, ForgeDirection dir) {
        super(x, y, z);
        this.dir = dir;
        this.inventory = inventory;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeInt(dir == null ? ForgeDirection.UNKNOWN.ordinal() : dir.ordinal());

        int size = inventory == null ? 0 : inventory.length;
        buffer.writeInt(size);
        for (int i = 0; i < size; i++) {
            writeItemStack(buffer, inventory[i]);
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        dir = ForgeDirection.getOrientation(buffer.readInt());

        int size = buffer.readInt();
        if (size < 0 || size > MAX_INVENTORY_SIZE) {
            size = 0;
        }

        inventory = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            inventory[i] = readItemStack(buffer);
        }
    }

    private static void writeItemStack(ByteBuf buffer, ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            buffer.writeBoolean(false);
            return;
        }

        buffer.writeBoolean(true);
        buffer.writeInt(Item.getIdFromItem(stack.getItem()));
        buffer.writeInt(stack.stackSize);
        buffer.writeShort(stack.getItemDamage());

        NBTTagCompound tag = null;
        if (stack.getItem()
            .isDamageable()
            || stack.getItem()
                .getShareTag()) {
            tag = stack.stackTagCompound;
        }

        ByteBufUtils.writeTag(buffer, tag);
    }

    private static ItemStack readItemStack(ByteBuf buffer) {
        if (!buffer.readBoolean()) return null;

        Item item = Item.getItemById(buffer.readInt());
        int count = buffer.readInt();
        int damage = buffer.readShort();
        NBTTagCompound tag = ByteBufUtils.readTag(buffer);

        // Unknown/removed item id: consume the payload but do not create a null-item stack.
        if (item == null) return null;

        ItemStack stack = new ItemStack(item, count, damage);
        if (tag != null) {
            stack.stackTagCompound = tag;
        }

        return stack;
    }

    @Override
    public IMessage onMessage(PacketFishTankSync message, MessageContext ctx) {
        EntityPlayer player = ClientHelper.getPlayer();
        if (player.worldObj.getTileEntity(message.x, message.y, message.z) instanceof TileFishTank) {
            TileFishTank tank = (TileFishTank) player.worldObj.getTileEntity(message.x, message.y, message.z);
            tank.setFacing(message.dir);

            ItemStack[] target = tank.getInventory();
            if (message.inventory != null) {
                int size = Math.min(target.length, message.inventory.length);
                for (int i = 0; i < size; i++) {
                    tank.setInventorySlotContents(i, message.inventory[i]);
                }
            }

            ClientHelper.updateRender(message.x, message.y, message.z);
        }

        return null;
    }
}
