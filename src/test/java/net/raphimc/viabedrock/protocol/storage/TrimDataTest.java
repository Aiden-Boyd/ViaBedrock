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
package net.raphimc.viabedrock.protocol.storage;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.rewriter.ItemDataRewriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class TrimDataTest {

    @BeforeAll
    static void initializeTypes() throws ClassNotFoundException {
        Class.forName("com.viaversion.viaversion.api.minecraft.data.StructuredDataKey");
    }

    @Test
    void serverTrimDefinitionsDecodeAndProduceMetadataWithoutChangingBase() {
        // One pattern (template -> coast), one material (iron, #fff, ingredient), then sentinel.
        final ByteBuf bytes = Unpooled.wrappedBuffer(HexFormat.of().parseHex("010874656d706c61746505636f617374010469726f6e04236666660a696e6772656469656e7466"));
        try {
            final PacketWrapper packet = new PacketWrapperImpl(ClientboundBedrockPackets.TRIM_DATA, bytes, null);
            final TrimDataStorage trims = TrimDataStorage.read(packet);
            assertEquals(0x66, bytes.readUnsignedByte());
            assertEquals(0, bytes.readableBytes());
            final BedrockItem base = new BedrockItem(1);
            base.setNetId(45);
            final CompoundTag tag = new CompoundTag();
            tag.putInt("Damage", 25);
            base.setTag(tag);
            final BedrockItem result = trims.result(base, "template", "ingredient");
            assertEquals(1, result.identifier());
            assertEquals(25, result.tag().getInt("Damage"));
            assertEquals("coast", result.tag().getCompoundTag("Trim").getString("Pattern"));
            assertEquals("iron", result.tag().getCompoundTag("Trim").getString("Material"));
            assertNull(result.netId());
            assertFalse(base.tag().contains("Trim"));
            assertTrue(trims.result(result, "template", "ingredient").isEmpty());
            assertTrue(trims.result(base, "unknown", "ingredient").isEmpty());
        } finally {
            bytes.release();
        }
    }

    @Test
    void trimsUseJavaRegistryOrderAndUnknownTrimsDoNotInventIds() {
        final CompoundTag registries = new CompoundTag();
        final CompoundTag materials = new CompoundTag();
        final CompoundTag iron = new CompoundTag();
        iron.putString("asset_name", "iron");
        materials.put("minecraft:iron", iron);
        registries.put("minecraft:trim_material", materials);
        final CompoundTag patterns = new CompoundTag();
        final CompoundTag coast = new CompoundTag();
        coast.putString("asset_id", "minecraft:coast");
        patterns.put("minecraft:coast", coast);
        registries.put("minecraft:trim_pattern", patterns);
        final CompoundTag trim = new CompoundTag();
        trim.putString("Material", "iron");
        trim.putString("Pattern", "minecraft:coast");
        final var converted = ItemDataRewriter.translateTrim(trim, registries);
        assertNotNull(converted);
        assertEquals(0, converted.material().id());
        assertEquals(0, converted.pattern().id());
        trim.putString("Pattern", "custom:unmapped");
        assertNull(ItemDataRewriter.translateTrim(trim, registries));
        assertNull(ItemDataRewriter.translateTrim(new CompoundTag(), registries));
    }

}
