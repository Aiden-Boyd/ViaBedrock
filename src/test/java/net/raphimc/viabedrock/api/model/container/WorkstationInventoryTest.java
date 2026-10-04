/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viabedrock.api.model.container;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.api.model.container.block.AnvilContainer;
import net.raphimc.viabedrock.api.model.container.block.BeaconContainer;
import net.raphimc.viabedrock.api.model.container.block.CrafterContainer;
import net.raphimc.viabedrock.api.model.container.block.GrindstoneContainer;
import net.raphimc.viabedrock.api.model.container.block.SmithingContainer;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.inventory.LegacySetItemSlotData;
import net.raphimc.viabedrock.protocol.storage.InventoryTracker;
import net.raphimc.viabedrock.protocol.types.inventory.InventoryTransactionPacketType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkstationInventoryTest {

    @BeforeAll
    static void initializeVersionedTypes() throws ClassNotFoundException {
        Class.forName("com.viaversion.viaversion.api.minecraft.data.StructuredDataKey");
    }

    @Test
    void virtualWorkstationsAcceptCompactAndFullUpdatesAndRollbackSnapshots() {
        for (Container container : List.of(new AnvilContainer(null, (byte) 1, null, null),
                new SmithingContainer(null, (byte) 2, null, null), new GrindstoneContainer(null, (byte) 3, null, null))) {
            final BedrockItem[] compact = BedrockItem.emptyArray(container.size());
            final BedrockItem[] full = BedrockItem.emptyArray(54);
            for (int slot = 0; slot < compact.length; slot++) {
                compact[slot] = new BedrockItem(slot + 1);
                full[container.bedrockSlot(slot)] = new BedrockItem(slot + 11);
            }
            assertTrue(container.setItems(compact));
            for (int slot = 0; slot < compact.length; slot++) {
                assertEquals(slot + 1, container.getItem(container.bedrockSlot(slot)).identifier());
            }
            final Container snapshot = container.copy();
            assertTrue(container.setItems(full));
            for (int slot = 0; slot < compact.length; slot++) {
                assertEquals(slot + 11, container.getItem(container.bedrockSlot(slot)).identifier());
            }
            assertTrue(container.setItems(snapshot.getItems()));
            assertEquals(1, container.getItem(container.bedrockSlot(0)).identifier());
            assertFalse(container.setItems(BedrockItem.emptyArray(2)));
            assertEquals(1, container.getItem(container.bedrockSlot(0)).identifier());
        }
    }

    @Test
    void anvilDelegatesInputDragAndRejectsInvalidOutputClicks() {
        final InventoryTracker[] tracker = new InventoryTracker[1];
        final UserConnection user = (UserConnection) Proxy.newProxyInstance(UserConnection.class.getClassLoader(),
                new Class<?>[]{UserConnection.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("get") && arguments[0] == InventoryTracker.class) {
                        return tracker[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        tracker[0] = new InventoryTracker(user);
        final BedrockItem cursor = new BedrockItem(1);
        cursor.setNetId(10);
        tracker[0].getHudContainer().setItem(0, cursor);
        final AnvilContainer anvil = new AnvilContainer(user, (byte) 1, null, null);
        assertTrue(anvil.handleClick(0, (short) -999, (byte) 0, ContainerInput.QUICK_CRAFT));
        assertFalse(anvil.handleClick(0, (short) 2, (byte) 0, ContainerInput.PICKUP));
        assertFalse(anvil.handleClick(0, (short) 2, (byte) 0, ContainerInput.QUICK_MOVE));
        assertFalse(anvil.handleClick(0, (short) 2, (byte) 0, ContainerInput.SWAP));
        assertEquals(Integer.valueOf(10), tracker[0].getHudContainer().getItem(0).netId());
    }

    @Test
    void smithingTransformPreservesMetadataWithoutMutatingRecipeOrBase() {
        final BedrockItem base = new BedrockItem(1);
        final CompoundTag tag = new CompoundTag();
        tag.putInt("Damage", 120);
        tag.putString("CustomName", "My sword");
        base.setTag(tag);
        base.setNetId(15);
        base.setCanBreak(new String[]{"minecraft:stone"});
        final BedrockItem recipe = new BedrockItem(2);
        final BedrockItem result = SmithingContainer.transformResult(base, recipe);
        assertEquals(2, result.identifier());
        assertEquals(120, result.tag().getInt("Damage"));
        assertEquals("My sword", result.tag().getString("CustomName"));
        assertArrayEquals(base.canBreak(), result.canBreak());
        assertNull(result.netId());
        result.tag().putInt("Damage", 5);
        result.canBreak()[0] = "minecraft:dirt";
        assertEquals(120, base.tag().getInt("Damage"));
        assertEquals("minecraft:stone", base.canBreak()[0]);
        assertNull(recipe.tag());
        assertTrue(SmithingContainer.transformResult(base, BedrockItem.empty()).isEmpty());
    }

    @Test
    void crafterMetadataHandlesAbsentAndNumericMasks() {
        assertArrayEquals(new boolean[9], CrafterContainer.decodeDisabledSlots(null));
        final CompoundTag tag = new CompoundTag();
        assertArrayEquals(new boolean[9], CrafterContainer.decodeDisabledSlots(tag));
        tag.putString("disabled_slots", "invalid");
        assertArrayEquals(new boolean[9], CrafterContainer.decodeDisabledSlots(tag));
        tag.putInt("disabled_slots", 257);
        final boolean[] expected = new boolean[9];
        expected[0] = true;
        expected[8] = true;
        assertArrayEquals(expected, CrafterContainer.decodeDisabledSlots(tag));
        tag.putShort("disabled_slots", (short) 257);
        assertArrayEquals(expected, CrafterContainer.decodeDisabledSlots(tag));
    }

    @Test
    void beaconOnlyAcceptsPaymentMaterials() {
        for (String material : List.of("emerald", "diamond", "gold_ingot", "iron_ingot", "netherite_ingot")) {
            assertTrue(BeaconContainer.acceptsPayment("minecraft:" + material));
        }
        assertFalse(BeaconContainer.acceptsPayment("minecraft:wheat"));
        assertFalse(BeaconContainer.acceptsPayment("minecraft:gold_nugget"));
        assertFalse(BeaconContainer.acceptsPayment(null));
    }

    @Test
    void legacySlotPrefixMatchesProtocol2193GoldenBytes() {
        final List<LegacySetItemSlotData> slots = List.of(new LegacySetItemSlotData(ContainerEnumName.InventoryContainer, new byte[]{1, 2}));
        assertLegacyPrefix(0, slots, "0000");
        assertLegacyPrefix(-1, slots, "0100");
        assertLegacyPrefix(-3, slots, "0500");
        assertLegacyPrefix(2, slots, "0400");
        assertLegacyPrefix(-2, slots, "0301011d020102");
        assertLegacyPrefix(-4, slots, "0701011d020102");
        assertLegacyPrefix(-2, List.of(), "030100");
    }

    private static void assertLegacyPrefix(final int id, final List<LegacySetItemSlotData> slots, final String golden) {
        final ByteBuf buffer = Unpooled.buffer();
        try {
            InventoryTransactionPacketType.writeLegacyRequest(buffer, id, slots);
            final byte[] encoded = new byte[buffer.readableBytes()];
            buffer.readBytes(encoded);
            assertArrayEquals(HexFormat.of().parseHex(golden), encoded);
        } finally {
            buffer.release();
        }
    }

}
