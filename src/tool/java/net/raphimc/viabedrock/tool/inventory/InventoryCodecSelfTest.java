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
        System.out.println("Inventory protocol 2193 and crafting fixtures passed");
    }

    private static void checkVirtualContainers() {
        final CraftingDataTracker[] tracker = new CraftingDataTracker[1];
        final UserConnection user = (UserConnection) Proxy.newProxyInstance(UserConnection.class.getClassLoader(),
                new Class<?>[]{UserConnection.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("get") && arguments[0] == CraftingDataTracker.class) {
                        return tracker[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        tracker[0] = new CraftingDataTracker(user);
        final StonecutterContainer stonecutter = new StonecutterContainer(user, (byte) 1, null, null);
        if (!stonecutter.setItems(BedrockItem.emptyArray(2)) || !stonecutter.setItems(BedrockItem.emptyArray(54))
                || stonecutter.bedrockSlot(0) != 3 || stonecutter.bedrockSlot(1) != 50
                || stonecutter.handleButtonClick(-1) || stonecutter.handleButtonClick(0)) {
            throw new AssertionError("Stonecutter compact/UI updates or recipe bounds failed");
        }
        final CraftingTableContainer table = new CraftingTableContainer(user, (byte) 2, null, null);
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
        table.setItem(40, input);
        if (table.getItem(50).amount() != 4 || output.amount() != 4) {
            throw new AssertionError("Crafting table preview did not update");
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
