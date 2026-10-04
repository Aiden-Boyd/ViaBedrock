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
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.HashMap;
import java.util.Map;

public final class TrimDataStorage implements StorableObject {

    private final Map<String, String> patterns = new HashMap<>();
    private final Map<String, String> materials = new HashMap<>();

    public static TrimDataStorage read(final PacketWrapper packet) {
        final TrimDataStorage data = new TrimDataStorage();
        final int patterns = packet.read(BedrockTypes.UNSIGNED_VAR_INT);
        for (int i = 0; i < patterns; i++) {
            final String item = packet.read(BedrockTypes.STRING);
            data.patterns.put(item, packet.read(BedrockTypes.STRING));
        }
        final int materials = packet.read(BedrockTypes.UNSIGNED_VAR_INT);
        for (int i = 0; i < materials; i++) {
            final String material = packet.read(BedrockTypes.STRING);
            packet.read(BedrockTypes.STRING); // Display color
            data.materials.put(packet.read(BedrockTypes.STRING), material);
        }
        return data;
    }

    public BedrockItem result(final BedrockItem base, final String template, final String ingredient) {
        final String pattern = this.patterns.get(template);
        final String material = this.materials.get(ingredient);
        if (base.isEmpty() || pattern == null || material == null) {
            return BedrockItem.empty();
        }
        final CompoundTag trim = new CompoundTag();
        trim.putString("Pattern", pattern);
        trim.putString("Material", material);
        if (base.tag() != null && trim.equals(base.tag().get("Trim"))) {
            return BedrockItem.empty();
        }
        final BedrockItem result = base.copy();
        final CompoundTag tag = result.tag() != null ? result.tag() : new CompoundTag();
        tag.put("Trim", trim);
        result.setTag(tag);
        result.setAmount(1);
        result.setNetId(null);
        return result;
    }

}
