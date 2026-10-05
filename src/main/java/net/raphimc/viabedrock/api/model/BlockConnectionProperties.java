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
package net.raphimc.viabedrock.api.model;

import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.GsonUtil;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/** Minecraft 26.3 conductor, signal source and sturdy face flags from the native registry. */
public final class BlockConnectionProperties {

    private static final Map<String, Integer> DEFAULTS = new HashMap<>();
    private static final Map<BlockState, Integer> STATES = new HashMap<>();

    static {
        try (InputStreamReader reader = new InputStreamReader(new GZIPInputStream(Objects.requireNonNull(BlockConnectionProperties.class.getResourceAsStream(
                "/assets/viabedrock/data/custom/block_connection_properties.json.gz"))), StandardCharsets.UTF_8)) {
            final JsonObject data = GsonUtil.getGson().fromJson(reader, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    DEFAULTS.put(entry.getKey(), entry.getValue().getAsInt());
                } else {
                    for (Map.Entry<String, JsonElement> state : entry.getValue().getAsJsonObject().entrySet()) {
                        STATES.put(BlockState.fromString(state.getKey()), state.getValue().getAsInt());
                    }
                }
            }
        } catch (final Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static int flags(final BlockState state) {
        return state != null ? STATES.getOrDefault(state, DEFAULTS.getOrDefault(state.namespacedIdentifier(), 0)) : 0;
    }

    private BlockConnectionProperties() {
    }

}
