package mariculture.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Regression tests for the fish tank sync payload.
 *
 * <p>
 * The original implementation wrote a boolean <em>and</em> a full item stack for every slot, but only read the
 * item stack when the boolean was true. For a null slot that made the writer emit three bytes and the reader
 * consume one, shifting the rest of the payload. {@link #emptyTankRoundTripConsumesWholePayload()} fails against
 * that implementation (two bytes are left unread) and passes against the fixed one.
 *
 * <p>
 * The payload now carries the whole inventory, so these tests use a full-size tank. Non-null stacks need the
 * Minecraft item registry, which is bootstrapped by the game rather than by the test JVM; that half of the codec
 * is validated in-game.
 */
public class PacketFishTankSyncTest {

    private static ItemStack[] inventoryField(PacketFishTankSync packet) throws Exception {
        Field field = PacketFishTankSync.class.getDeclaredField("inventory");
        field.setAccessible(true);
        return (ItemStack[]) field.get(packet);
    }

    private static ForgeDirection dirField(PacketFishTankSync packet) throws Exception {
        Field field = PacketFishTankSync.class.getDeclaredField("dir");
        field.setAccessible(true);
        return (ForgeDirection) field.get(packet);
    }

    @Test
    public void emptyTankRoundTripConsumesWholePayload() throws Exception {
        ItemStack[] inventory = new ItemStack[54];

        ByteBuf buffer = Unpooled.buffer();
        new PacketFishTankSync(inventory, 1, 2, 3, ForgeDirection.UNKNOWN).toBytes(buffer);

        PacketFishTankSync decoded = new PacketFishTankSync();
        decoded.fromBytes(buffer);

        assertEquals("decoder left bytes unread (writer/reader stream desync)", 0, buffer.readableBytes());

        ItemStack[] decodedInventory = inventoryField(decoded);
        assertNotNull(decodedInventory);
        assertEquals("full inventory size is not preserved", 54, decodedInventory.length);
        for (int i = 0; i < decodedInventory.length; i++) {
            assertNull("slot " + i + " should be empty", decodedInventory[i]);
        }
    }

    @Test
    public void coordinatesAndDirectionSurviveRoundTrip() throws Exception {
        ByteBuf buffer = Unpooled.buffer();
        new PacketFishTankSync(new ItemStack[54], -5, 64, 1234, ForgeDirection.EAST).toBytes(buffer);

        PacketFishTankSync decoded = new PacketFishTankSync();
        decoded.fromBytes(buffer);

        assertEquals(-5, decoded.x);
        assertEquals(64, decoded.y);
        assertEquals(1234, decoded.z);
        assertEquals(ForgeDirection.EAST, dirField(decoded));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void nullDirectionDoesNotThrow() throws Exception {
        ByteBuf buffer = Unpooled.buffer();
        new PacketFishTankSync(new ItemStack[54], 0, 0, 0, null).toBytes(buffer);

        PacketFishTankSync decoded = new PacketFishTankSync();
        decoded.fromBytes(buffer);

        assertEquals(ForgeDirection.UNKNOWN, dirField(decoded));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void nullInventoryIsSerialisedAsEmpty() throws Exception {
        ByteBuf buffer = Unpooled.buffer();
        new PacketFishTankSync(null, 0, 0, 0, ForgeDirection.UNKNOWN).toBytes(buffer);

        PacketFishTankSync decoded = new PacketFishTankSync();
        decoded.fromBytes(buffer);

        assertEquals(0, buffer.readableBytes());
        assertNotNull(inventoryField(decoded));
        assertEquals(0, inventoryField(decoded).length);
    }
}
