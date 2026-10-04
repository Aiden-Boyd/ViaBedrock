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
package net.raphimc.viabedrock.protocol.packet;

import com.viaversion.nbt.tag.CompoundTag;
import net.raphimc.viabedrock.api.model.container.block.AnvilContainer;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackResponseSlotInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StackResponseMetadataTest {

    @BeforeAll
    static void initializeTypes() throws ClassNotFoundException {
        Class.forName("com.viaversion.viaversion.api.minecraft.data.StructuredDataKey");
    }

    @Test
    void equalCountResponseStillAppliesFilteredNameAndDurabilityWithoutMutatingPrediction() {
        final BedrockItem expected = named("Old");
        expected.tag().putInt("Damage", 120);
        final BedrockItem updated = InventoryRequestPackets.applyStackResponse(expected,
                new ItemStackResponseSlotInfo((byte) 0, (byte) 0, (byte) 1, 40, "Raw", "Filtered", 15));
        assertEquals("Filtered", updated.tag().getCompoundTag("display").getString("Name"));
        assertEquals(15, updated.tag().getInt("Damage"));
        assertEquals(Integer.valueOf(40), updated.netId());
        assertEquals("Old", expected.tag().getCompoundTag("display").getString("Name"));
        assertEquals(120, expected.tag().getInt("Damage"));
        assertTrue(expected.isDifferent(updated));
    }

    @Test
    void serverCanClearNameAndFullyRepairAnItem() {
        final BedrockItem expected = named("Old");
        expected.tag().putInt("Damage", 20);
        final BedrockItem updated = InventoryRequestPackets.applyStackResponse(expected,
                new ItemStackResponseSlotInfo((byte) 0, (byte) 0, (byte) 1, 30, "", null, 0));
        assertFalse(updated.tag().getCompoundTag("display").contains("Name"));
        assertEquals(0, updated.tag().getInt("Damage"));
        final BedrockItem ordinary = InventoryRequestPackets.applyStackResponse(new BedrockItem(1),
                new ItemStackResponseSlotInfo((byte) 0, (byte) 0, (byte) 255, 2, "", null, 0));
        assertNull(ordinary.tag());
        assertEquals(255, ordinary.amount());
    }

    @Test
    void anvilRenamePreservesFullStackAndOtherMetadata() {
        final BedrockItem input = named("Old");
        input.setAmount(64);
        input.tag().putString("custom", "kept");
        final BedrockItem result = AnvilContainer.renameResult(input, "New");
        assertEquals(64, result.amount());
        assertEquals("New", result.tag().getCompoundTag("display").getString("Name"));
        assertEquals("kept", result.tag().getString("custom"));
        assertEquals("Old", input.tag().getCompoundTag("display").getString("Name"));
        assertNull(result.netId());
        assertTrue(AnvilContainer.renameResult(input, "Old").isEmpty());
        assertFalse(AnvilContainer.renameResult(input, "").tag().getCompoundTag("display").contains("Name"));
    }

    private static BedrockItem named(final String name) {
        final BedrockItem item = new BedrockItem(1);
        final CompoundTag tag = new CompoundTag();
        final CompoundTag display = new CompoundTag();
        display.putString("Name", name);
        tag.put("display", display);
        item.setTag(tag);
        return item;
    }
}
