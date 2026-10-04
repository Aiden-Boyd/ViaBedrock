/*
 * This file is part of ViaFabricPlus - https://github.com/ViaVersion/ViaFabricPlus
 * Copyright (C) 2021-2026 the original authors
 *                         - Florian Reuth <git@florianreuth.de>
 *                         - RK_01/RaphiMC
 * Copyright (C) 2023-2026 ViaVersion and contributors
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

package com.viaversion.viafabricplus.bedrock.settings;

import com.google.gson.JsonObject;
import com.viaversion.viafabricplus.bedrock.client.BedrockClient;
import com.viaversion.viafabricplus.bedrock.client.JsonSave;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;

public final class BedrockSettings {
    private boolean replaceDefaultPort = true;
    public BedrockSettings() {
        JsonSave.load(BedrockClient.get().path().resolve("settings.json"), object -> {
            if (object.has("replace_default_port")) this.replaceDefaultPort = object.get("replace_default_port").getAsBoolean();
        }, this::serialize);
    }
    private JsonObject serialize() {
        final var object = new JsonObject(); object.addProperty("replace_default_port", this.replaceDefaultPort); return object;
    }
    public boolean defaultBedrockPort() { return this.replaceDefaultPort; }
    public void defaultBedrockPort(final boolean enabled) {
        this.replaceDefaultPort = enabled;
        JsonSave.write(BedrockClient.get().path().resolve("settings.json"), this::serialize);
    }
    public String replaceDefaultPort(final String address, final ProtocolVersion version) {
        return this.replaceDefaultPort && BedrockProtocolVersion.BEDROCK_LATEST.equals(version) && !address.contains(":")
            ? address + ":" + ProtocolConstants.BEDROCK_DEFAULT_PORT : address;
    }
}
