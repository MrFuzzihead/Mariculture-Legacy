# Mariculture Legacy — Codebase Analysis

**Repository:** `Mariculture-Legacy` (Minecraft 1.7.10 / Forge 10.13.4.1614, GTNH buildscript)
**Branch analyzed:** `feature-Refactors` (identical to `main` at `ea3dcbc7`)
**Date:** 2026-10-14
**Scope:** 774 Java source files (~35,300 LOC) across `mariculture`, `maritech`, mixins, plugins and APIs.
**Method:** manual review of the hot paths (module startup, recipes, Vat, Fishery/Fish Tank, networking, machine bases), plus a runtime capture of the startup log and a standalone round-trip test of the suspect packet (see §7).

---

## TL;DR — Findings table

| #  | Severity | Area                | Finding                                                                                                                                                                                                                                                                                     | Location                                                                              |
|----|----------|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| 1  | **High** | Startup / Vat       | One Vat alloy recipe is built with a null output fluid and rejected at startup (error in log every launch). Cause: `"magnesium"` is looked up in `FluidRegistry` but registered as `"magnesium.molten"`.                                                                                    | `RecipesSmelting.java:401-405`, `Core.java:213`                                       |
| 2  | **High** | Fish Tank           | `PacketFishTankSync` writes a boolean **and** a full item-stack for every slot, but only reads the item-stack when the boolean is true. The writer/reader stream desynchronises; stacks that follow a null stack are silently dropped (or mis-read) on the client. Verified experimentally. | `PacketFishTankSync.java:37-57`                                                       |
| 3  | **High** | Fish Tank           | Only 3 of the tank's 54 slots are ever synchronised to the client. The GUI exposes all 54, so slots 3+ are permanently client/server-desynced (ghost or invisible fish; shift-click can move items to the server that the client never sees).                                               | `TileFishTank.java:140-152`, `ContainerFishTank.java:24-31`                           |
| 4  | **High** | Fish Tank           | Fish stack sizes > 127 are corrupted by `PacketBuffer.writeItemStackToBuffer` (writes `Count` as a byte). The tank deliberately allows `Integer.MAX_VALUE` per slot.                                                                                                                        | `TileFishTank.java:97-99`, `PacketFishTankSync.java:40`                               |
| 5  | **High** | Fish Tank           | `ContainerMariculture.slotClick` has an off-by-one (`slotID > size` instead of `>=`), and the fish-tank slot handler calls the client-only `ClientHelper.isShiftPressed()` from common code, which runs for every GUI click on a dedicated server.                                          | `ContainerMariculture.java:57`, `ContainerFishTank.java:161-180`                      |
| 6  | **High** | Vat                 | `TileVat.hasRoom` compares `FluidStack` objects with `!=` instead of comparing fluids. Once the output tank contains anything, every fluid-output recipe is permanently blocked.                                                                                                            | `TileVat.java:245-253`                                                                |
| 7  | Medium   | Fishery             | `TileFeeder.fixFish` checks `Fish.temperature` twice (copy/paste) and never repairs salinity; it can also NPE on broken fish because `species`/`secondary` are not null-checked. Runs every tick when `FIX_FISH` is on.                                                                     | `TileFeeder.java:216-231`, `456-457`                                                  |
| 8  | Medium   | Environmental logic | `EnvironmentHandler.matches` computes `maxSalt` with `Math.max(2, ...)`, which can never constrain salinity (only 3 salinity values exist). Fish salinity requirements are effectively ignored.                                                                                             | `EnvironmentHandler.java:118-124`                                                     |
| 9  | Medium   | Fishery             | `ItemFishy.getEntityLifespan` dereferences `fishy` without a null check; a fish item with unknown species on the ground → NPE in entity tick (crash loop).                                                                                                                                  | `ItemFishy.java:148-152`                                                              |
| 10 | Medium   | Fishery             | `FishDNASpecies.getDominant` calls `FishSpecies.species.get(id).isDominant()` with no null check → NPE on unknown/legacy species IDs.                                                                                                                                                       | `FishDNASpecies.java:33-46`                                                           |
| 11 | Medium   | Machines            | `TileCrucible.canMelt` computes the melting speed modifier from `2000 - recipe.temp` instead of the *difference* between crucible temperature and recipe temperature; melting speed is constant per recipe and can even shrink.                                                             | `TileCrucible.java:280-281`                                                           |
| 12 | Medium   | GUI                 | `GuiItemToolTip.addToolTip` constructs a brand-new `Gui`/`Container` **for every item tooltip of every frame** just to call `addItemToolTip`. Expensive, and the pattern caused re-entrant GUI construction crashes with other mods (upstream issue #358).                                  | `GuiItemToolTip.java:24-36`                                                           |
| 13 | Low-Med  | Helpers             | `InventoryHelper.canInsertItemStackIntoInventory` claims to simulate but mutates the passed stack; the made `original` copy is unused.                                                                                                                                                      | `InventoryHelper.java:184-231`                                                        |
| 14 | Low-Med  | Helpers             | `BlockTransferHelper` shuffles a **static** shared `sides` list on every transfer, and dereferences `master.slaves` which can be `null` after a multiblock is broken.                                                                                                                       | `BlockTransferHelper.java:31-38, 150`                                                 |
| 15 | Low-Med  | Helpers             | `NBTHelper.getItemStackFromNBT` builds an `ItemStack` with a null item if the stored registry name is unknown (removed mod / renamed item) → later NPEs while rendering, saving or interacting.                                                                                             | `NBTHelper.java:35-55`                                                                |
| 16 | Low      | GUI                 | `OreDicHandler.getNextValidEntry` calls `lore.getStringTagAt(0)` without checking `tagCount()`; an empty `Lore` list throws `IndexOutOfBoundsException`.                                                                                                                                    | `OreDicHandler.java:194`                                                              |
| 17 | Low      | Recipes             | `ShapelessFishRecipe.getRecipeOutput` uses integer division inside `Math.ceil(eggs / 650)` and mutates the shared `output` stack.                                                                                                                                                           | `ShapelessFishRecipe.java:76-82`                                                      |
| 18 | Low      | Factory             | `TileUnpacker.setInventorySlotContents` is a pass-through that never stores its input and unpacks whole stacks with a 1-item recipe (`UnpackerHelper.unpack`) — input can be destroyed.                                                                                                     | `TileUnpacker.java:20-33`, `UnpackerHelper.java:23-41`                                |
| 19 | Low      | Startup             | `Mariculture.preInit` gates **all** config + module + GUI registration behind `EnchiridionManager.isPresent()`. Without Enchiridion the mod loads as an empty shell on the client (and halts on a dedicated server).                                                                        | `Mariculture.java:41-53`                                                              |
| 20 | Low      | Config              | While iterating `Plugins.plugins` with an index, disabled plugins are removed from the same list — the element after a removal is skipped.                                                                                                                                                  | `config/Modules.java:50-64`                                                           |
| 21 | Low      | Fish Tank           | `BlockTank.getPickBlock` casts the tile entity with no null check; `getAccessibleSlotsFromSide` returns a shared mutable static array; `transferStackInSlot` computes an unused `lastClick/difference`.                                                                                     | `BlockTank.java:378-400`, `TileFishTank.java:224-230`, `ContainerFishTank.java:55-67` |
| 22 | Info     | Build               | Mixin migration to GTNH mixins is wired correctly (`FMLCorePlugin`, refmap, `IMixins`); the base `mixins.Mariculture.json` intentionally has no mixin list.                                                                                                                                 | `gradle.properties`, `mixins/*`                                                       |

I also explicitly checked and **cleared** several suspicious patterns — see §6 "False positives examined" — so that future readers don't re-investigate them.

### Fix status (working tree, `feature-Refactors`)

| Batch | Findings | Status | Notes |
|-------|----------|--------|-------|
| 1 | #2, #4 | **Fixed** | `PacketFishTankSync` rewritten with a symmetric codec and `int` stack counts. Regression test `PacketFishTankSyncTest` (3 tests) fails on the old code and passes now. |
| 2 | #1, #6, §3.2, §3.5 | **Fixed** | Vat magnesium recipe, `hasRoom` fluid comparison, `readFromNBT` tank index/guard, `drain(null)` guard. |
| 3 | #7, #8, #9, #10 | **Fixed** | Feeder salinity copy/paste + null guard, salinity range clamp, `getEntityLifespan` guard, `getDominant` guard. `EnvironmentHandlerTest` (4 tests) fails on the old code and passes now. |
| 4 | #3, #12 | **Fixed** | `PacketFishTankSync` now carries the whole 54-slot inventory (with an `int` count and a symmetric null marker); `GuiItemToolTip` dispatches tooltips through `ContainerMariculture` instead of constructing a GUI per tooltip frame, so the `addItemToolTip` logic moved onto the containers. |
| 5 | #5 | **Fixed** | `slotClick` bounds guard corrected (and now returns early for bogus ids); `SlotFake.handle` receives the vanilla click mode, so `ClientHelper.isShiftPressed()` is gone from common slot code. `ContainerMaricultureTest` (1 test) fails on the old guard. |
| 6 | #11, #15, #16, #17 | **Fixed** | Crucible melt-speed now scales with actual overheat (`TileCrucibleTest`, 4 tests, fails on the old formula); `NBTHelper` drops entries for removed items; `OreDicHandler` guards an empty lore list; `ShapelessFishRecipe` uses `650.0D` and no longer mutates the cached output (`ShapelessFishRecipeTest`, 2 tests). |
| 7 | #13, #14, #18, #21 | **Fixed** | `InventoryHelper` simulates on a copy; `BlockTransferHelper` returns a shuffled copy and guards null slaves; `TileUnpacker` now stores its input and drains one item per tick instead of destroying a stack; fish-tank `getPickBlock` null guard, `all_slots` copy, dead `lastClick` removed. |
| 8 | #19, #20 | **Fixed** | `Mariculture` lifecycle events are gated on the `loaded` flag so a missing Enchiridion leaves the mod dormant rather than half-initialised; the plugin config loop iterates backwards; `PluginEE3` now logs a swallowed exception. |
| 9 | §4.16 (temperature) | **Fixed** | `EnvironmentHandler.getTemperature` now uses the intended flat 60..68 surface band with cooling above/below (the old code had the `60`/`68` constants swapped), keeps the calculation in `double` and rounds once with `Math.round`, and applies the rain term instead of truncating it to 0. `EnvironmentHandlerTemperatureTest` (5 tests incl. fish placability) fails on the old swapped/truncating behaviour. |
| — | — | Done | All numbered findings addressed. Deliberately deferred: live in-world tank rendering for server-side automation (GUI is authoritative), rendering more than 3 display fish, and tuning the small explicit rain coefficient (0.15; sun brightness already reacts to weather). |

Remaining known limitation after #3: automation that inserts into a tank server-side (hoppers/pipes, `BlockTransferHelper.ejectToSides`) does not push an update to clients until the next description packet; player clicks stay in sync through the symmetric `handle` path. The only remaining `ClientHelper.isShiftPressed()` caller is `ItemArmorFishingHat.addInformation` (client-only tooltip code), which is not a dedicated-server hazard.

Build/test command: `./gradlew test --offline` (uses `testImplementation("junit:junit:4.13.2")`).

---

## 1. Confirmed startup bug — Vat recipe dropped during registration

**Symptom (every launch, client and server):**

```
[ERROR] [Mariculture]: A mod attempted to add an invalid Vat recipe, with both a null output item and fluid
[ERROR] [Mariculture]: mariculture.api.core.RecipeVat@...
```

**Root cause.** `RecipesSmelting.addMetalRecipes()` registers the "quicklime + water → magnesium" alloy like this:

```java
// RecipesSmelting.java:401-405
RecipeHelper.addFluidAlloy(
    get("water", 1000),                                  // FluidRegistry.getFluidStack("water", 1000)  -> OK
    get(getFluidName("quicklime"), 3000),                // "quicklime"                                  -> OK
    get("magnesium", MetalRates.INGOT),                  // FluidRegistry.getFluidStack("magnesium",..)  -> NULL
    9);
```

`get(...)` is the local helper:

```java
// RecipesSmelting.java:450-455
public static FluidStack get(String name, int vol) {
    return FluidRegistry.getFluidStack(name, vol);     // returns null for unregistered names
}
```

but the magnesium fluid is registered under its *display* name, not the field key:

```java
// Core.java:213
addFluid("magnesium", "magnesium.molten", MetalRates.ORE, BottleMeta.MAGNESIUM, 144, BucketMeta.MAGNESIUM);
//          ^ key        ^ registry name (Fluid.getName() == "magnesium.molten")
```

`FluidRegistry.getFluidStack("magnesium", …)` therefore returns `null`, so `RecipeVat.outputFluid` is null while `outputItem` is also null. `VatHandler.addRecipe` correctly rejects the recipe — but the recipe is silently lost.

**Verification.** A temporary stack-trace probe in `VatHandler.addRecipe` reproduced the exact origin during `Mariculture.init`:

```
at mariculture.core.handlers.VatHandler.addRecipe(VatHandler.java:27)
at mariculture.core.helpers.RecipeHelper.addFluidAlloy(RecipeHelper.java:151)
at mariculture.core.RecipesSmelting.addMetalRecipes(RecipesSmelting.java:401)
at mariculture.core.RecipesSmelting.add(RecipesSmelting.java:99)
at mariculture.core.Recipes.add(Recipes.java:154)
at mariculture.core.Core.registerRecipes(Core.java:428)
```

**Impact.** Content bug + log noise: the quicklime→magnesium Vat step is missing. (The other magnesium alloy at `RecipesSmelting.java:331-332` uses `Fluids.getFluidStack` and works, and titanium's own alloy also works.) It is the first thing visible in every log and will confuse players and pack makers.

**Fix.** Use the registered name or the mod helper:

```java
get(getFluidName("magnesium"), MetalRates.INGOT)   // or Fluids.getFluidStack("magnesium", MetalRates.INGOT)
```

The same class of mistake exists in `RecipeHelper.addVatItemRecipe`/`addMelting(stack, temp, String fluid, vol)` which both use `FluidRegistry.getFluidStack(fluid, vol)`; callers currently pass correct names (`"lava"`, `"water"`, `getFluidName(...)`), but the API is a trap. A debug assertion/warning in `Fluids.getFluidStack`/`RecipeHelper` for null fluids would prevent recurrence.

---

## 2. Fish Tank — the network path is broken

The fish tank (`Core.tanks`, meta `TankMeta.FISH` → `TileFishTank`) is by far the most fragile block in the mod. The sections below are independent confirmed defects.

### 2.1 `PacketFishTankSync` stream desynchronization (the core sync bug)

```java
// PacketFishTankSync.java:36-57
public void toBytes(ByteBuf buffer) {
    super.toBytes(buffer);
    buffer.writeInt(dir.ordinal());

    if (stack0 == null) buffer.writeBoolean(false);
    else                buffer.writeBoolean(true);
    ByteBufUtils.writeItemStack(buffer, stack0);   // ALWAYS writes at least 2 bytes
    ... // same for stack1, stack2
}

public void fromBytes(ByteBuf buffer) {
    super.fromBytes(buffer);
    dir = ForgeDirection.getOrientation(buffer.readInt());
    if (buffer.readBoolean()) stack0 = ByteBufUtils.readItemStack(buffer);   // reads 0 bytes when false
    if (buffer.readBoolean()) stack1 = ByteBufUtils.readItemStack(buffer);
    if (buffer.readBoolean()) stack2 = ByteBufUtils.readItemStack(buffer);
}
```

In 1.7.10 `ByteBufUtils.writeItemStack` delegates to `PacketBuffer.writeItemStackToBuffer`, which for `null` writes `writeShort(-1)` (2 bytes) and nothing else. The reader, however, deliberately *skips* `readItemStack` when the preceding boolean is `false`. So for every null slot the writer emits 3 bytes while the reader consumes only 1 — a 2-byte desync that shifts every subsequent field.

I verified the real byte stream and decode behavior with a standalone harness against the compiled classes + Forge/Netty from the dev classpath. Example for an empty tank (coords 1/2/3, direction 6):

```
written = 25 bytes
hex: 00 00 00 01 00 00 00 02 00 00 00 03 00 00 00 06 00 FF FF 00 FF FF 00 FF FF
decode: no exception, remainingBytes=2
```

Because `readItemStackFromBuffer` returns early (`if (short < 0) return null`) and the bogus short always starts at an `0xFF` byte, the misreads happen to return `null` instead of reading deeper — **but the whole 3-slot payload is now shifted**. Consequences (confirmed by simulating all 8 null/non-null combinations):

| Sent (slot0, slot1, slot2) | Decoded on client                         |
|----------------------------|-------------------------------------------|
| `null, null, ITEM`         | `null, null, null` — slot 2 silently lost |
| `null, ITEM, null`         | `null, null, null`                        |
| `null, ITEM, ITEM`         | `null, null, null`                        |
| `ITEM, null, ITEM`         | `ITEM, null, null`                        |
| `ITEM, ITEM, ITEM`         | correct (no desync)                       |

Everything before the first null decodes correctly; everything after it is dropped. With large registered item IDs / NBT-bearing fish a deeper misread becomes possible (the boolean can land on an arbitrary NBT byte), which is where “random” client disconnects/crashes come from in bigger packs. This alone is enough to explain persistent “fish tank is broken” reports.

**Fix.** Drop the boolean entirely (`writeItemStack`/`readItemStack` already encode null with the `-1` short):

```java
ByteBufUtils.writeItemStack(buffer, stack0); ...
stack0 = ByteBufUtils.readItemStack(buffer); ...
```

or keep the boolean and skip the `writeItemStack` call when the stack is null.

### 2.2 Only 3 of 54 slots are ever sent to the client

`TileFishTank` has a 54-slot inventory and `ContainerFishTank` adds slots for all 54, but the only client synchronization is:

```java
// TileFishTank.java:140-152
public Packet getDescriptionPacket() {
    return PacketHandler.getPacket(
        new PacketFishTankSync(getStackInSlot(0), getStackInSlot(1), getStackInSlot(2), x, y, z, orientation));
}
```

`TileFishTank` does **not** override `onDataPacket`, and 1.7.10 chunk packets carry no tile-entity NBT (`S21PacketChunkData`). The vanilla path for tile entities on chunk-watch is `PlayerManager.PlayerInstance.sendTileToAllPlayersWatchingChunk(...)` → `TileEntity.getDescriptionPacket()`, i.e. exactly this custom packet. So slots 3-53 exist only on the server; the client-side GUI shows them as empty, and local edits to them cannot round-trip. Combined with §2.1, even slots 0-2 are unreliable at chunk load.

**Impact:** ghost items, invisible fish, shift-click desyncs. The special renderer purposely only draws slots 0-2 (`FishTankSpecialRenderer`), which hides the problem visually while the GUI still exposes the other 51 slots.

**Fix:** either sync the full inventory (e.g. a `PacketInventorySync`-style NBT payload, as `TileHatchery`/`TileFeeder` already do) or reduce the exposed container to the 3 synced slots. Do not expose 54 slots with a 3-slot sync.

### 2.3 Stack sizes above 127 are corrupted by the item-stack codec

`TileFishTank.getInventoryStackLimit()` returns `Integer.MAX_VALUE`, and `GuiFishTank`/`RenderTankItem` deliberately support 4+ digit stacks (`stack.stackSize > 999` → “k” suffix). But `PacketBuffer.writeItemStackToBuffer` serialises the count with `writeByte`. A stack of 128 becomes −128 on the wire; the client stores a negative stack size, which then propagates through `ContainerFishTank.handle()` (`held.stackSize + inSlot.stackSize`) and slot-click handling.

**Fix:** cap tank slots at 127 (or 64) or use a custom payload that writes counts as ints.

### 2.4 Client-only helper used from common code

`ContainerFishTank$SlotFishTank.handle` (common code) starts with:

```java
// ContainerFishTank.java:161-180
public ItemStack handle(EntityPlayer player, int mouseButton, Slot slot) {
    if (ClientHelper.isShiftPressed()) return slot.getStack();
    ...
```

`ClientHelper` is `@SideOnly(Side.CLIENT)` and touches LWJGL `Keyboard`. The server-side container also runs this method for **every** GUI click (`ContainerMariculture.slotClick` → `handle`), so on a dedicated server this is at best wrong (reads the server’s keyboard / always false, breaking shift-clicks) and at worst a `NoClassDefFoundError`/NPE on the first click. Use the click modifier (`mode` / `isShiftKeyDown`) that `slotClick` already receives.

Related: `ContainerMariculture.slotClick`:

```java
// ContainerMariculture.java:56-60
Slot slot = slotID < 0 || slotID > inventorySlots.size() ? null : (Slot) inventorySlots.get(slotID);
return slot instanceof SlotFake && ((SlotFake) slot).handle(...) == null ? null : super.slotClick(...);
```

`slotID == inventorySlots.size()` would throw `IndexOutOfBoundsException`; the guard should be `>=`. (Vanilla normally never sends that id, so it is latent. Note that `windowClick` still forwards the click packet even though `slotClick` returns `null`, which is what makes the symmetric `handle` implementation work at all.)

### 2.5 Smaller fish-tank issues

* `BlockTank.getPickBlock` (line 378) casts `world.getTileEntity(...)` and immediately calls `getInventory()`; a missing TE (mid-generation, broken save) → NPE on middle-click.
* `TileFishTank.getAccessibleSlotsFromSide` returns the shared static `all_slots` array, including/excluding slot 3 asymmetrically relative to `ContainerFishTank` (slot 3 is rendered but not automation-accessible). The array is mutable and world-visible.
* `ContainerFishTank.transferStackInSlot` computes `lastClick`/`difference` and never uses them; the shift-click merge caps at 64 while the slot handler merges without a cap (inconsistent stack sizes).

---

## 3. Vat logic bugs

### 3.1 `hasRoom` compares FluidStack object identity

```java
// TileVat.java:245-253
private boolean hasRoom(ItemStack stack, FluidStack newFluid) {
    if (tank3.getFluid() != null && newFluid != null)
        if (newFluid != tank3.getFluid() ||                                   // <-- identity compare
            newFluid.amount + tank3.getFluidAmount() > tank3.getCapacity())
            return false;
    ...
}
```

`createResult` always fills `recipe.outputFluid.copy()`, so `tank3.getFluid()` can never be the same object as a recipe’s `outputFluid`. The first condition is effectively always true, so **once tank3 holds any fluid, every recipe with a fluid output is rejected** (`canWork()` becomes false). It should compare fluids:

```java
if (!newFluid.isFluidEqual(tank3.getFluid()) || ...) return false;
```

### 3.2 `readFromNBT` ignores the stored tank index and can NPE

```java
// TileVat.java:463-471
NBTTagList tagList = nbt.getTagList("Tanks", 10);
for (int i = 0; i < 3; i++) {
    NBTTagCompound tag = tagList.getCompoundTagAt(i);   // null if the list is short
    byte tank = tag.getByte("Tank");                    // unused local
    getTank(i).readFromNBT(tag);
}
```

The saved `"Tank"` byte is ignored (positional ordering is assumed), and a truncated/corrupt list NPEs. Read `tagList.tagCount()` and use the stored index.

### 3.3 Stubbed `ITank` members

`TileVat` implements `ITank` and ends with `getTankScaled → 0`, `getFluidName → null`, `getFluidQty → null`. Any `FeatureTank` fed with a `TileVat` would add `null` to a tooltip and crash the font renderer. There is currently no Vat GUI, so it is latent, but the stubs make the class lie about its contract.

### 3.4 Burst crafting loop / timing

```java
// TileVat.java:145-160
if (timeRemaining >= timeNeeded) {
    for (int i = 0; i < 64; i++) { ... createResult(...); }
    timeRemaining = 0; timeNeeded = 0; canWork = canWork();
}
```

After one timer completes, the Vat instantly crafts up to 64 times. With `getResult()` scanning the full recipe list once per iteration (64 × N per activation) this can be a noticeable server spike. Also, `timeRemaining/timeNeeded` units are seconds (multi-block: +1 per 20 ticks; single block: +1 per 30 ticks), so single and multi-block Vats run at different speeds.

### 3.5 `drain(from, resource, doDrain)` NPEs on a null resource

```java
// TileVat.java
public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
    return drain(from, resource.amount, doDrain);   // NPE if resource == null
}
```

`IFluidHandler` implementations should tolerate a null `FluidStack` (Forge callers do pass null in some versions).

---

## 4. Other correctness bugs

### 4.1 `TileFeeder.fixFish` — copy/paste and NPE (Medium)

```java
// TileFeeder.java:216-231
FishSpecies species = Fishing.fishHelper.getSpecies(fish);
FishSpecies secondary = Fishing.fishHelper.getSpecies(Fishing.fishHelper.getLowerDNA(Fish.species.getName(), fish));
if (!fish.stackTagCompound.hasKey(Fish.temperature.getName())) {
    Fish.temperature.addDNA(fish, species.getTemperatureTolerance());
    Fish.temperature.addLowerDNA(fish, secondary.getTemperatureTolerance());
}
if (!fish.stackTagCompound.hasKey(Fish.temperature.getName())) {     // should be Fish.salinity
    Fish.temperature.addDNA(fish, species.getSalinityTolerance());   // and Fish.salinity.add...
    Fish.temperature.addLowerDNA(fish, secondary.getSalinityTolerance());
}
```

* The second guard repeats the temperature key, so the salinity repair never runs (and, after the first block adds the key, it would be skipped even if it were correct).
* `species`/`secondary` may be `null` for exactly the “broken fish” this method exists to fix (`canWork()` runs `fixFish` every tick when `FIX_FISH=true`, the default), so a broken fish in a Feeder can NPE the server tick and produce a restart crash-loop.

### 4.2 `EnvironmentHandler.matches` clamps salinity the wrong way (Medium)

```java
// EnvironmentHandler.java:118-124
int maxSalt = Math.max(2, checkSaltBase.ordinal() + checkSaltVariation);
```

`Salinity` has only 3 values (`FRESH`, `BRACKISH`, `SALINE` → ordinals 0-2), so `maxSalt >= 2` always, making the upper salinity bound a no-op. It should be `Math.min(2, ...)`. Fish salinity tolerances are therefore not enforced at all through this API.

### 4.3 `ItemFishy.getEntityLifespan` NPE (Medium)

```java
// ItemFishy.java:148-152
if (stack.getItem() == this && stack.hasTagCompound()) {
    FishSpecies fishy = Fishing.fishHelper.getSpecies(stack);
    return fishy.getOnLandLifespan(fishy);          // fishy can be null
}
```

An item entity holding a fish whose species id is unknown (fish config changed, mod removed, corrupted NBT) NPEs during `EntityItem` tick → server crash, repeating on every restart while the item is loaded. `getItemStackDisplayName` and `getIcon` already handle the null species case — this one was missed.

### 4.4 `FishDNASpecies.getDominant` NPE (Medium)

```java
// FishDNASpecies.java:33-46
boolean species1 = FishSpecies.species.get(option1).isDominant();   // NPE if absent
boolean species2 = FishSpecies.species.get(option2).isDominant();
```

Called from `FishyHelper.makeBredFish` for every egg, so a legacy/corrupt egg with an unregistered species id crashes hatching. Guard with a null check fallback.

### 4.5 `TileCrucible` melting-speed formula (Medium)

```java
// TileCrucible.java:280-281
int max_temp = 2000 - recipe.temp;
melting_modifier = 1.0D + Math.min(realTemp, max_temp) * 3.333333D / 2000;
```

`realTemp` is the scaled current temperature; `max_temp` is derived only from the recipe. The intent (compare crucible heat to the required temp) is presumably `Math.min(realTemp - recipe.temp, …)`. As written, once a recipe is meltable its speed is fixed (1.0-1.67×) regardless of how hot the crucible is, and if a future recipe has `temp > 2000` the term can even become negative.

### 4.6 `InventoryHelper.canInsertItemStackIntoInventory` mutates its input (Low-Med)

```java
// InventoryHelper.java:184-231
ItemStack original = stack.copy();          // never used
int stackSize = stack.stackSize;
...
stack = simulateAddToOccupiedInventorySlot(sidedInv, slots[i], stack);  // mutates `stack`
```

The method name promises a simulation but the `simulate*` helpers decrement/increment `stack.stackSize`. `TileUnpacker.canInsertItem` passes its real `ret` stack. Use `original` (or a copy) for the simulation.

### 4.7 `BlockTransferHelper` shared static state (Low-Med)

```java
// BlockTransferHelper.java:31-38
private static List<Integer> sides;
static { sides = new ArrayList<Integer>(); ... }

public static List<Integer> getSides() { Collections.shuffle(sides); return sides; }
```

All machines share and shuffle the same list; nested ejection (`insertStack` → `ejectToSides` → `Collections.shuffle(sides)`) can throw `ConcurrentModificationException` if a caller iterates `getSides()` while recursing, and the actions of one tile change the transfer order of another tile in the same tick. Use a local copy per call. Also `insertStack` dereferences `master.slaves` (line 150) without guarding, but `TileMultiBlock.onBlockBreak` sets it to `null`.

### 4.8 `NBTHelper.getItemStackFromNBT` can create a null-item stack (Low-Med)

```java
// NBTHelper.java:35-55
Item item = (Item) Item.itemRegistry.getObject(tag.getString("Name"));  // may be null
ItemStack stack = new ItemStack(item, count, damage);                   // no null check
```

On a world that contained items from a now-removed mod, the Fish Tank (and any `TileMultiStorage`) load/save chain can produce a stack whose `getItem()` is null, which NPEs later in rendering/saving/interaction. Return `null` when the registry name no longer resolves.

### 4.9 `OreDicHandler` empty-Lore crash (Low)

```java
// OreDicHandler.java:194
String old = lore.getStringTagAt(0) != null ? lore.getStringTagAt(0) : "";
```

`NBTTagList.getStringTagAt(0)` throws `IndexOutOfBoundsException` on an empty list; the null check is useless. Check `lore.tagCount() > 0`.

### 4.10 `ShapelessFishRecipe` integer division (Low)

```java
// ShapelessFishRecipe.java:79
output.stackSize = (int) Math.max(1, Math.ceil(eggs / 650));   // eggs / 650 is int division
```

For 651-1299 eggs the result is 1 instead of 2; also `output` is the recipe’s cached result stack and is mutated in place.

### 4.11 `TileUnpacker` pass-through can destroy input (Low)

`TileUnpacker.setInventorySlotContents` never stores the incoming stack; it calls `UnpackerHelper.unpack(worldObj, stack)` on the whole stack and inserts the (single) crafting result below. A stack of N > 1 is used with a 1-item `DummyCrafting` recipe, so N-1 inputs vanish. If the design is a pass-through, it should split one item off and keep the rest (or reject N > 1).

### 4.12 `GuiItemToolTip` re-instantiates GUIs (Medium; perf + crash risk)

```java
// GuiItemToolTip.java:24-36
Container container = player.openContainer;
if (container instanceof ContainerMachine) {
    TileEntity tile = (TileEntity) ((ContainerMachine) container).tile;
    Gui gui = (Gui) Mariculture.proxy.getClientGuiElement(-1, player, tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
    if (gui instanceof GuiMariculture) ((GuiMariculture) gui).addItemToolTip(stack, list);
}
```

This runs for every tooltip render (every frame the tooltip is visible) and builds a complete `Gui` + `Container` + 54 slots for fish tanks. Upstream issue #358 (`adding fish food in fish tank crash`) shows the resulting recursion: `Gui construction → ItemTooltipEvent → GuiItemToolTip → getClientGuiElement → Gui construction…`. Cache the `GuiMariculture` per open container, or move the per-machine tooltip logic into a method/interface on the container instead of constructing the GUI.

### 4.13 Startup gating on Enchiridion (Low/informational)

```java
// Mariculture.java:41-53
public void preInit(FMLPreInitializationEvent event) {
    if (EnchiridionManager.isPresent()) {
        root = event.getModConfigurationDirectory();
        Config.setup();
        for (Module module : Modules.modules) module.preInit();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
    }
}
```

If Enchiridion is absent, `Config.setup()` and every module `preInit` are skipped: no config files, no blocks/items/tiles and no GUI handler are registered, while `init()` still runs `PacketHandler.init()` and (on `FMLLoadCompleteEvent`) plugins may reference uninitialised statics. On a dedicated server `isPresent()` halts the game, so this is only visible client-side, but it is an all-or-nothing coupling that makes the mod appear installed but empty. Consider failing fast on first launch instead of silently skipping registration.

### 4.14 Plugin config loop skips entries (Low)

```java
// config/Modules.java:50-64
for (int i = 0; i < Plugins.plugins.size(); i++) {
    Plugin plugin = Plugins.plugins.get(i);
    if (config.get("Plugins", plugin.getClass().getSimpleName().substring(6), true).getBoolean(true) == false) {
        Plugins.plugins.remove(i);
    }
}
```

Removing while iterating with an index skips the element that shifts into the removed slot: if two adjacent plugins are disabled, the second is kept. Iterate backwards or use an `Iterator`.

### 4.15 Static-init fragility in `MCLib` (Info)

`MCLib` holds dozens of `public static final ItemStack` fields built from `Core.crafting`, `Fishery.*`, `Modules.isActive(...)` and `MaricultureHandlers.HIGH_TECH_ENABLED`. Loading the class before `Core.registerItems()` / `MariTech.preInit()` silently captures null items/`false` and caches them forever (e.g. `rotor` would permanently be a titanium sheet). The current order happens to work, but any new early reference (NEI, Waila, a plugin `preInit`) will produce very confusing bugs. Lazy getters or moving these into registration methods would remove the landmine.

### 4.16 Job-lot noise

* `OreDicHandler` prints `new Exception().printStackTrace()` for *other mods’* bad ore registrations — noisy and slow at startup; log once per offender.
* `PluginEE3.load()` wraps its whole body in `catch (Exception e) {}` so any EE3 integration failure is invisible.
* `EnvironmentHandler.getTemperature` uses a peaky `if (y > 60) … else if (y < 68)` pair where the first branch also applies to 61-67; the temperature curve in that band is probably not what was intended.

---

## 5. Optimisations

1. **Vat recipe lookup is O(n) with allocations per tick.** `TileVat.getResult()` is called at least every 20 ticks (and 64× on completion); each call builds an `Object[2]`, calls `tank.getFluid()` twice and linearly scans every registered `RecipeVat`. Build an index keyed by the primary input fluid name (and optional item/ore) at `VatHandler.addRecipe` time, and return a small record instead of `Object[]` casts (`(RecipeVat) result[0]`, `(Byte) result[1]`). The same pattern applies to `CrucibleHandler`/`CastingHandler`/`AnvilHandler` if they scan lists too.
2. **`GuiItemToolTip` creates a GUI per tooltip render** (see 4.12). Cache the `GuiMariculture` instance or expose `addItemToolTip` through `ContainerMariculture`.
3. **`FishyHelper.registerFishies()`** opens, loads and saves `fish-mappings.cfg` **once per fish in three separate loops** (and each save rewrites the file). Read once into a `Map`, write once at the end.
4. **`BlockConnected.getIcon`** allocates a `boolean[8]` and performs 8 neighbor lookups on every face of every block render. The 256-state icon table is already cached; the bit matrix could be built with a small `int` bitfield without allocations.
5. **`BlockTransferHelper`** shuffles shared state per call (4.7) — use a per-invocation copy. The `sides` list itself could be a static `ForgeDirection[]` to avoid boxing.
6. **`FishTankSpecialRenderer`** constructs a new `EntityFakeItem` per fish per frame. A single reusable fake entity (updating its stack) would cut per-frame allocation.
7. **`PacketFishTankSync`** could carry the full inventory as one NBT payload instead of per-stack booleans/shorts (see 2.2), reducing packet count and eliminating the byte-size limitation.
8. **`TileVat.updateAll`** completes up to 64 crafts in one server tick; consider a bounded origin/burst cap or distribute over ticks to avoid a spike.
9. **`Tank.drain`** allocates a new `FluidStack` on every simulated drain; it is called frequently by ejectors. Draining by amount only would help in hot loops.
10. **`BlockTransferHelper.getSides()`** returns a `List<Integer>` (boxed) and shuffles it for every side iteration.

---

## 6. False positives examined (not bugs)

These looked suspicious but are actually fine — documented so they don’t get “fixed” twice:

* **`VatHandler.getResult` NPEs on null recipes.** `FluidHelper.areEqual(fluid1, null)` returns `false`, and `ItemHelper.areEqual(null, stack)` returns `false`, so all the `vat.inputFluid1.amount` / `input.stackSize` accesses are short-circuited. Invalid recipes are rejected at `addRecipe`, and a null `inputItem` simply skips the item check.
* **NEI Vat recipes with no item input.** `NEIVatRecipeHandler.CachedVatRecipe` leaves `input` null for fluid-only recipes, but NEI’s `CachedRecipe.getIngredients()` explicitly null-checks `getIngredient()` before adding it to the list, so no NEI crash.
* **`ItemFishy.isSame` early return.** If the dominant species match, the method returns the recessive comparison; the skipped cross-orientation check is logically redundant for that case. Behavior is correct (the `.equals` on boxed ints is correct too, if inelegant).
* **`Tank.fill(resource, doFill, other)`.** The `resource.getFluid() == other.getFluid().getFluid()` identity comparison is safe: Forge fluids are singletons returned from `FluidRegistry`.
* **`BlockConnected` fish-tank texture index.** `textureRefByID` has 256 entries with maximum value 46 and `fishTank[47]` is allocated/registered (indices 0-46), so no `ArrayIndexOutOfBoundsException`.
* **`Mariculture.preInit` skip without Enchiridion** is deliberate in intent (the manager prints a warning and downloads/ halts), but is still listed as #19 for the silent client-side no-op.

---

## 7. Verification log

Commands/results used while producing this report (all on the current working tree, `ea3dcbc7`):

1. `./gradlew compileJava --offline` — clean compile (Jabel, mixin AP writes refmap).
2. Temporary probe added to `VatHandler.addRecipe` and `./gradlew runServer --offline` — produced the startup stack trace quoted in §1; the probe was reverted afterward (`git status` clean).
3. Decompiled the shipped FML `PacketBuffer.writeItemStackToBuffer` / `readItemStackFromBuffer` from the 1.7.10 Forge binary:
   * null → only `writeShort(-1)`;
   * reader returns `null` for a negative short without consuming anything else.
   This is the exact code path `ByteBufUtils` delegates to.
4. Standalone harness against `build/classes/java/main` + the dev classpath: serialised `PacketFishTankSync(null, null, null, 1, 2, 3, UNKNOWN)` produced 25 bytes; `fromBytes` returned “no exception” with **2 bytes left unread**. Simulating all eight null/non-null combinations showed the “stacks after the first null are lost” table in §2.1.
5. Confirmed 1.7.10 chunk packets carry no tile-entity NBT (`S21PacketChunkData`), so `getDescriptionPacket()` really is the only sync path for `TileFishTank` (`PlayerManager.PlayerInstance.sendTileToAllPlayersWatchingChunk`).
6. Confirmed the existing launch logs already contain the Vat error on every start (`fml-client-1.log:910`, `fml-client-2.log:901`, `fml-client-latest.log:1337`, `fml-server-latest.log:1137`).

---

## 8. Suggested fix order

1. **§2.1** `PacketFishTankSync` codec (one-line removal of the boolean per stack) — fixes the visible fish-tank corruption.
2. **§2.2 + §2.3** decide on a single sync strategy for the tank and make the payload authoritative (or shrink the container to 3 slots and cap stack sizes).
3. **§1** Vat magnesium recipe (`get(getFluidName("magnesium"), …)`).
4. **§3.1** `TileVat.hasRoom` fluid comparison — restores Vat crafting after the first fluid output.
5. **§4.1-4.4** Feeder/salinity/species NPE fixes — these are crash-loop candidates.
6. **§4.12** cache the GUI instance for machine tooltips (also removes the known re-entrancy crash).
7. The remaining Low/Info items as time permits; §4.13/4.14 are cheap wins for clean startup logs.
