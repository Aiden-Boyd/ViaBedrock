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

package com.viaversion.viafabricplus.bedrock.client;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import java.io.File;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

public final class BedrockViaVersionPlatform extends UserConnectionViaVersionPlatform {
    public BedrockViaVersionPlatform(final File path) { super(path); }
    @Override public Logger createLogger(final String name) { return new JLoggerToSLF4J(LoggerFactory.getLogger(name)); }
    @Override public String getPlatformName() { return "Bedrock Multiplayer"; }
    @Override public String getPlatformVersion() { return "1.1.1-SNAPSHOT"; }
    @Override protected AbstractViaConfig createConfig() { return new BedrockViaVersionConfig(new File(this.getDataFolder(), "viaversion.yml"), this.getLogger()); }
    @Override public void sendCustomPayload(final UserConnection user, final String channel, final byte[] data) {
        throw new UnsupportedOperationException("Java plugin messages are not supported on Bedrock connections: " + channel);
    }
    @Override public void sendCustomPayloadToClient(final UserConnection user, final String channel, final byte[] data) {
        throw new UnsupportedOperationException("Java plugin messages are not supported on Bedrock connections: " + channel);
    }
    @Override public JsonObject getDump() {
        final var result = new JsonObject();
        result.addProperty("platform", this.getPlatformName());
        result.addProperty("native_version", BedrockClient.NATIVE_VERSION.getName());
        return result;
    }
}
