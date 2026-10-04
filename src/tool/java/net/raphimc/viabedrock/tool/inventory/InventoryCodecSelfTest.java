package net.raphimc.viabedrock.tool.inventory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlotInfo;
import net.raphimc.viabedrock.protocol.types.InventoryTypes;
import net.raphimc.viabedrock.protocol.types.inventory.ItemStackSlotResponseType;

import java.util.HexFormat;

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
        System.out.println("Inventory protocol 2193 fixtures passed");
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
