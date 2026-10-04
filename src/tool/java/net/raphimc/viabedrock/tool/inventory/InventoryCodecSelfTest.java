package net.raphimc.viabedrock.tool.inventory;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.viabedrock.api.model.container.block.CraftingTableContainer;
import net.raphimc.viabedrock.api.model.container.block.StonecutterContainer;
import net.raphimc.viabedrock.protocol.storage.CraftingDataTracker;
import net.raphimc.viabedrock.protocol.storage.CraftingDataStorage;
import java.lang.reflect.Proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.recipe.*;
import net.raphimc.viabedrock.protocol.types.recipe.CraftingRecipesType;
import net.raphimc.viabedrock.protocol.types.item.BedrockItemType;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlotInfo;
import net.raphimc.viabedrock.protocol.types.InventoryTypes;
import net.raphimc.viabedrock.protocol.types.inventory.ItemStackSlotResponseType;

import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import net.raphimc.viabedrock.protocol.storage.InventoryTracker;
import net.raphimc.viabedrock.protocol.storage.InventoryRequestTracker;
import net.raphimc.viabedrock.protocol.storage.InventoryRequestStorage;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestInfo;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.TextProcessingEventOrigin;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.player.InventoryContainer;
import net.raphimc.viabedrock.api.model.container.player.HudContainer;
import net.raphimc.viabedrock.api.model.container.block.EnchantmentContainer;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.api.model.BlockConnections;
import net.raphimc.viabedrock.api.model.BlockPredictionQueue;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;

/**
 * Fixed protocol-2193 fixtures, checked independently of the corresponding writers.
 * Layout reference: Cloudburst BedrockCodecHelper_v2168/v2193 and ItemStackResponseSerializer_v2193.
 */
public final class InventoryCodecSelfTest {

    public static void main(final String[] args) {
        final ItemStackRequestSlotInfo source = new ItemStackRequestSlotInfo(new FullContainerName(ContainerEnumName.HotbarContainer, null), (byte) 0, 42);
        final ItemStackRequestSlotInfo cursor = new ItemStackRequestSlotInfo(new FullContainerName(ContainerEnumName.CursorContainer, null), (byte) 0, 0);
        checkAction(new ItemStackRequestAction.TakeAction(1, source, cursor), "0000011c00002a0000003b000000000000");
        checkAction(new ItemStackRequestAction.PlaceAction(1, source, cursor), "0101011c00002a0000003b000000000000");
        checkAction(new ItemStackRequestAction.SwapAction(source, cursor), "02021c00002a0000003b000000000000");
        checkAction(new ItemStackRequestAction.BeaconPaymentAction(1, 2), "080a0204");
        checkAction(new ItemStackRequestAction.MineBlockAction(0, 0, 42), "090b00002a000000");
        checkAction(new ItemStackRequestAction.CraftRecipeAction(7, 1), "0a0c0701");
        checkAction(new ItemStackRequestAction.ConsumeAction(1, new ItemStackRequestSlotInfo(
                new FullContainerName(ContainerEnumName.CraftingInputContainer, null), (byte) 28, 42)), "0505010d001c2a000000");
        checkAction(new ItemStackRequestAction.TakeAction(4, new ItemStackRequestSlotInfo(
                new FullContainerName(ContainerEnumName.CreatedOutputContainer, null), (byte) 50, -1), cursor),
                "0000043c0032ffffffff3b000000000000");

        final ByteBuf slots = fixture("000001015400000000000000000000");
        try {
            final var first = new ItemStackSlotResponseType().read(slots);
            final var second = new ItemStackSlotResponseType().read(slots);
            if (first.itemNetId() != 42 || first.amount() != 1 || first.filteredCustomName() != null || second.itemNetId() != 0 || slots.isReadable()) {
                throw new AssertionError("Optional slot fields lost alignment");
            }
        } finally {
            slots.release();
        }

        final ByteBuf responses = fixture("02000100000301011c00010000010154000000");
        try {
            final var decoded = InventoryTypes.ITEM_STACK_RESPONSES.read(responses);
            if (decoded.length != 2 || decoded[0].requestId() != -1 || !decoded[0].containers().isEmpty()
                    || decoded[1].requestId() != -2 || decoded[1].containers().get(0).slots().get(0).itemNetId() != 42 || responses.isReadable()) {
                throw new AssertionError("Response entries lost alignment");
            }
        } finally {
            responses.release();
        }
        checkCrafting();
        checkVirtualContainers();
        checkInventorySequencing();
        checkBlockConnections();
        checkBlockPredictions();
        System.out.println("Inventory protocol 2193 and crafting fixtures passed");
    }

    private static void checkBlockConnections() {
        if (!BlockConnections.usesBlockInteraction(BlockState.fromString("minecraft:oak_door"), null)
                || !BlockConnections.usesBlockInteraction(BlockState.fromString("minecraft:stonecutter"), "stonecutter")
                || BlockConnections.usesBlockInteraction(BlockState.fromString("minecraft:iron_door"), null)
                || BlockConnections.usesBlockInteraction(BlockState.fromString("minecraft:stone"), null)) {
            throw new AssertionError("Block interaction was mistaken for item placement");
        }
        final BlockPosition position = new BlockPosition(0, 64, -1);
        final String[] facings = {"north", "east", "south", "west"};
        final int[][] partners = {{1, -1}, {0, 0}, {-1, -1}, {0, -2}};
        for (int i = 0; i < facings.length; i++) {
            final BlockState single = BlockState.fromString("minecraft:chest[facing=" + facings[i] + ",type=single,waterlogged=false]");
            final CompoundTag tag = new CompoundTag();
            tag.putInt("pairx", partners[i][0]);
            tag.putInt("pairz", partners[i][1]);
            final BlockPosition partner = BlockConnections.chestPartner(position, tag);
            if (partner == null || !BlockConnections.chest(single, position, tag).hasProperty("type", "left")) {
                throw new AssertionError("Chest pairing failed for " + facings[i]);
            }
            tag.putInt("pairx", position.x());
            tag.putInt("pairz", position.z());
            if (!BlockConnections.chest(single, partner, tag).hasProperty("type", "right")) {
                throw new AssertionError("Chest halves did not complement each other");
            }
            if (!BlockConnections.chest(single, position, new CompoundTag()).hasProperty("type", "single")) {
                throw new AssertionError("Unpaired chest became double");
            }
        }
        for (boolean open : new boolean[]{false, true}) {
            for (String hinge : new String[]{"left", "right"}) {
                final BlockState lower = BlockState.fromString("minecraft:oak_door[facing=east,half=lower,hinge=left,open=" + open + ",powered=false]");
                final BlockState upper = BlockState.fromString("minecraft:oak_door[facing=north,half=upper,hinge=" + hinge + ",open=false,powered=false]");
                final BlockState mergedLower = BlockConnections.door(lower, upper);
                final BlockState mergedUpper = BlockConnections.door(upper, lower);
                if (!mergedLower.hasProperty("hinge", hinge) || !mergedUpper.hasProperty("hinge", hinge)
                        || !mergedUpper.hasProperty("facing", "east") || !mergedUpper.hasProperty("open", Boolean.toString(open))
                        || !mergedLower.hasProperty("half", "lower") || !mergedUpper.hasProperty("half", "upper")) {
                    throw new AssertionError("Door properties were not combined across halves");
                }
            }
        }
    }

    private static void checkBlockPredictions() {
        final BlockPosition clicked = new BlockPosition(0, 64, 0);
        final BlockPosition placed = new BlockPosition(1, 64, 0);
        final BlockPredictionQueue queue = new BlockPredictionQueue();
        queue.add(1, Set.of(clicked, placed), 0);
        queue.addConfirmed(2, 1);
        if (!queue.pollReady(10_000_000L).isEmpty()) {
            throw new AssertionError("A later acknowledgement released an unconfirmed prediction");
        }
        queue.blockUpdated(new BlockPosition(9, 64, 9), 20_000_000L);
        if (!queue.pollReady(80_000_000L).isEmpty()) {
            throw new AssertionError("Unrelated block update confirmed a placement");
        }
        queue.blockUpdated(placed, 100_000_000L);
        if (!queue.pollReady(149_000_000L).isEmpty()) {
            throw new AssertionError("Acknowledgement preceded settled block updates");
        }
        final var ready = queue.pollReady(150_000_000L);
        if (ready.size() != 2 || ready.get(0).sequence() != 1 || ready.get(1).sequence() != 2 || !queue.isEmpty()) {
            throw new AssertionError("Block acknowledgements lost sequence order");
        }
        queue.add(3, Set.of(placed), 200_000_000L);
        if (!queue.pollReady(1_199_000_000L).isEmpty() || queue.pollReady(1_200_000_000L).size() != 1) {
            throw new AssertionError("Rejected placement did not reach correction timeout");
        }
    }

    private static void checkInventorySequencing() {
        final UserConnection user = (UserConnection) Proxy.newProxyInstance(UserConnection.class.getClassLoader(),
                new Class<?>[]{UserConnection.class}, (proxy, method, arguments) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
        final InventoryRequestTracker requests = new InventoryRequestTracker(user);
        final List<Integer> executed = new ArrayList<>();
        final ItemStackRequestInfo first = new ItemStackRequestInfo(-3, List.of(), List.of(), TextProcessingEventOrigin.unknown);
        final ItemStackRequestInfo second = new ItemStackRequestInfo(-5, List.of(), List.of(), TextProcessingEventOrigin.unknown);
        requests.addRequest(new InventoryRequestStorage(first, 0, null, List.of()));
        requests.runInventoryAction(() -> {
            executed.add(1);
            requests.addRequest(new InventoryRequestStorage(second, 0, null, List.of()));
        });
        requests.runInventoryAction(() -> executed.add(2));
        requests.flushInventoryActions();
        if (!executed.isEmpty()) {
            throw new AssertionError("Click ran before stack IDs were confirmed");
        }
        requests.removeRequest(-3);
        requests.flushInventoryActions();
        if (!executed.equals(List.of(1))) {
            throw new AssertionError("Queued clicks overlapped the next request");
        }
        requests.removeRequest(-5);
        requests.flushInventoryActions();
        if (!executed.equals(List.of(1, 2))) {
            throw new AssertionError("Queued click order changed");
        }
        requests.addRequest(new InventoryRequestStorage(first, 0, null, List.of()));
        requests.runInventoryAction(() -> executed.add(3));
        requests.clearInventoryActions();
        requests.removeRequest(-3);
        requests.flushInventoryActions();
        if (!executed.equals(List.of(1, 2))) {
            throw new AssertionError("Rejected request retained dependent clicks");
        }
    }

    private static void checkVirtualContainers() {
        final CraftingDataTracker[] tracker = new CraftingDataTracker[1];
        final Map<Class<?>, Object> storages = new HashMap<>();
        final UserConnection user = (UserConnection) Proxy.newProxyInstance(UserConnection.class.getClassLoader(),
                new Class<?>[]{UserConnection.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("get")) {
                        return storages.get(arguments[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        tracker[0] = new CraftingDataTracker(user);
        storages.put(CraftingDataTracker.class, tracker[0]);
        final InventoryTracker inventory = new InventoryTracker(user);
        storages.put(InventoryTracker.class, inventory);
        final EnchantmentContainer enchanting = new EnchantmentContainer(user, (byte) 3, null, null);
        final BedrockItem book = new BedrockItem(1);
        final BedrockItem lapis = new BedrockItem(2);
        if (!enchanting.setItems(new BedrockItem[]{book, lapis}) || enchanting.getItem(14).identifier() != 1
                || enchanting.getItem(15).identifier() != 2 || !enchanting.setItems(BedrockItem.emptyArray(54))
                || !enchanting.getItem(14).isEmpty() || !enchanting.getItem(15).isEmpty()
                || EnchantmentContainer.acceptsItem(14, "minecraft:wheat", 0, false)
                || EnchantmentContainer.acceptsItem(15, "minecraft:wheat", 0, false)
                || !EnchantmentContainer.acceptsItem(15, "minecraft:lapis_lazuli", 0, false)
                || !EnchantmentContainer.acceptsItem(14, "minecraft:book", 1, false)
                || EnchantmentContainer.acceptsItem(14, "minecraft:diamond_sword", 10, true)) {
            throw new AssertionError("Enchanting slot updates or item restrictions failed");
        }
        final StonecutterContainer stonecutter = new StonecutterContainer(user, (byte) 1, null, null);
        if (!stonecutter.setItems(BedrockItem.emptyArray(2)) || !stonecutter.setItems(BedrockItem.emptyArray(54))
                || stonecutter.bedrockSlot(0) != 3 || stonecutter.bedrockSlot(1) != 50
                || stonecutter.handleButtonClick(-1) || stonecutter.handleButtonClick(0)) {
            throw new AssertionError("Stonecutter compact/UI updates or recipe bounds failed");
        }
        final var table = new CraftingTableContainer(user, (byte) 2, null, null) {
            public void checkPlayerSlots() {
                for (short slot = 10; slot < 46; slot++) {
                    final SlotRef mapped = this.resolveJavaSlot(slot);
                    final int expected = inventory.getInventoryContainer().bedrockSlot(slot - 10 + 9);
                    if (mapped == null || mapped.container() != inventory.getInventoryContainer() || mapped.bedrockSlot() != expected) {
                        throw new AssertionError("Open crafting table redirected a player slot to equipment: " + slot);
                    }
                }
                if (this.resolveJavaSlot((short) 1).bedrockSlot() != 32 || this.resolveJavaSlot((short) 46) != null) {
                    throw new AssertionError("Virtual input or invalid slot mapping failed");
                }
            }
        };
        table.checkPlayerSlots();
        final var player = new InventoryContainer(user) {
            public void checkCollection() {
                final var sources = this.pickupAllSources();
                if (sources.size() != 45) {
                    throw new AssertionError("Player collection omitted visible inventory slots");
                }
                for (int index = 0; index < 4; index++) {
                    if (sources.get(index).container() != inventory.getHudContainer()
                            || sources.get(index).bedrockSlot() != index + 28) {
                        throw new AssertionError("Double-click collection omitted the 2x2 grid");
                    }
                }
                if (sources.stream().anyMatch(source -> source.container() == inventory.getHudContainer() && source.bedrockSlot() == 50)) {
                    throw new AssertionError("Double-click collected a virtual recipe preview");
                }
            }
        };
        player.checkCollection();
        if (!table.setItems(BedrockItem.emptyArray(10)) || !table.setItems(BedrockItem.emptyArray(54))) {
            throw new AssertionError("Crafting table compact/UI updates failed");
        }
        for (int slot = 0; slot < table.size(); slot++) {
            if (table.javaSlot(table.bedrockSlot(slot)) != slot) {
                throw new AssertionError("Crafting grid slot mapping failed");
            }
        }
        final BedrockItem input = new BedrockItem(1);
        input.setAmount(3);
        final BedrockItem output = new BedrockItem(2);
        output.setAmount(4);
        final ShapedRecipe recipe = new ShapedRecipe("table", new UUID(0, 0), "crafting_table", 0,
                new ItemDescriptor[][]{{new ItemDescriptor.DefaultDescriptor(1, 0)}}, List.of(output), false);
        tracker[0].updateCraftingDataList(List.of(new CraftingDataStorage(RecipeType.SHAPED, 7, recipe)));
        final var hud = new HudContainer(user) {
            @Override
            protected boolean craftOutput(final int revision, final int recipeId, final BedrockItem result,
                                          final Map<Integer, Integer> consumed, final boolean quickMove) {
                if (revision != 19 || recipeId != 7 || result.amount() != 4 || !consumed.equals(Map.of(31, 1))) {
                    throw new AssertionError("2x2 crafting did not use the shared output transaction");
                }
                return quickMove;
            }
        };
        hud.setItem(31, input.copy());
        if (hud.craft(19, false) || !hud.craft(19, true) || hud.getItem(31).amount() != 3) {
            throw new AssertionError("2x2 shift-click mode or recipe planning failed");
        }
        table.setItem(40, input);
        if (table.getItem(50).amount() != 4 || output.amount() != 4) {
            throw new AssertionError("Crafting table preview did not update");
        }
        final Container snapshot = table.copy();
        table.getItem(40).setAmount(2);
        if (snapshot.getItems()[9].amount() != 3) {
            throw new AssertionError("Rollback snapshot shared mutable stacks");
        }
        table.setItem(40, BedrockItem.empty());
        if (!table.getItem(50).isEmpty()) {
            throw new AssertionError("Crafting table retained a stale output");
        }
    }

    private static void checkCrafting() {
        final ByteBuf recipeData = fixture("01047465737402020101046e616d65116d696e6563726166743a6f616b5f6c6f67feff0302015404000000020000000000000000000000000000000000000e6372616674696e675f7461626c6500010007010574657374320101086974656d5f746167106d696e6563726166743a706c616e6b73feff0302015604000000020000000000000000000000000000000000000e6372616674696e675f7461626c6500000800000000000000000001");
        try {
            final var recipes = new CraftingRecipesType(new BedrockItemType(0, new Int2ObjectOpenHashMap<>(), false, false)).read(recipeData);
            final ShapedRecipe first = (ShapedRecipe) recipes[0].recipe();
            final ShapelessRecipe second = (ShapelessRecipe) recipes[1].recipe();
            if (recipes.length != 2 || recipes[0].networkId() != 7 || recipes[1].networkId() != 8 || first.getResults().get(0).amount() != 4
                    || !(first.getPattern()[0][0] instanceof ItemDescriptor.DeferredDescriptor name && name.fullName().equals("minecraft:oak_log"))
                    || !(second.getIngredients().get(0) instanceof ItemDescriptor.ItemTagDescriptor tag && tag.itemTag().equals("minecraft:planks"))
                    || recipeData.readableBytes() != 4) {
                throw new AssertionError("Crafting arrays or named ingredients lost alignment");
            }
        } finally {
            recipeData.release();
        }
        final BedrockItem one = new BedrockItem(1);
        one.setAmount(1);
        final BedrockItem two = new BedrockItem(2);
        two.setAmount(1);
        final UUID uuid = new UUID(0, 0);
        final ShapedRecipe single = new ShapedRecipe("single", uuid, "crafting_table", 0,
                new ItemDescriptor[][]{{new ItemDescriptor.DefaultDescriptor(1, 0)}}, List.of(two), false);
        final BedrockItem[] grid = BedrockItem.emptyArray(4);
        grid[3] = one;
        final int[] consumed = CraftingGridMatcher.match(single, grid, 2, (d, i) -> d.matchesItem(null, i));
        if (!Arrays.equals(consumed, new int[]{0, 0, 0, 1})) {
            throw new AssertionError("Offset 2x2 recipe did not match");
        }
        grid[0] = two;
        if (CraftingGridMatcher.match(single, grid, 2, (d, i) -> d.matchesItem(null, i)) != null) {
            throw new AssertionError("Extra ingredient accepted");
        }
        final ShapedRecipe large = new ShapedRecipe("large", uuid, "crafting_table", 0,
                new ItemDescriptor[][]{{new ItemDescriptor.DefaultDescriptor(1, 0), new ItemDescriptor.DefaultDescriptor(1, 0), new ItemDescriptor.DefaultDescriptor(1, 0)}},
                List.of(two), false);
        if (CraftingGridMatcher.match(large, grid, 2, (d, i) -> d.matchesItem(null, i)) != null) {
            throw new AssertionError("3-wide recipe accepted in 2x2 grid");
        }
        final ShapelessRecipe ambiguous = new ShapelessRecipe("ambiguous", uuid, "crafting_table", 0,
                List.of(new ItemDescriptor.ComplexAliasDescriptor("either"), new ItemDescriptor.DefaultDescriptor(1, 0)), List.of(two));
        grid[0] = one;
        grid[1] = two;
        grid[3] = BedrockItem.empty();
        if (CraftingGridMatcher.match(ambiguous, grid, 2,
                (d, i) -> d instanceof ItemDescriptor.ComplexAliasDescriptor || d.matchesItem(null, i)) == null) {
            throw new AssertionError("Shapeless matching did not backtrack");
        }
    }

    private static void checkAction(final ItemStackRequestAction action, final String expectedHex) {
        final ByteBuf buffer = Unpooled.buffer();
        try {
            InventoryTypes.ITEM_STACK_REQUEST_ACTIONS.write(buffer, new ItemStackRequestAction[]{action});
            if (buffer.readUnsignedByte() != 1) {
                throw new AssertionError("Wrong action count");
            }
            final byte[] actual = new byte[buffer.readableBytes()];
            buffer.readBytes(actual);
            if (!HexFormat.of().formatHex(actual).equals(expectedHex)) {
                throw new AssertionError(action.getType() + ": " + HexFormat.of().formatHex(actual));
            }
        } finally {
            buffer.release();
        }
    }

    private static ByteBuf fixture(final String hex) {
        return Unpooled.wrappedBuffer(HexFormat.of().parseHex(hex));
    }

    private InventoryCodecSelfTest() {
    }

}
