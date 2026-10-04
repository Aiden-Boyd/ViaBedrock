/*
 * This file is part of ViaFabricPlus Bedrock - https://github.com/florianreuth/viafabricplus-bedrock
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

package com.viaversion.viafabricplus.bedrock.integration;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer;
import net.raphimc.minecraftauth.extra.realms.service.impl.BedrockRealmsService;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;

public final class BedrockRealmDiscovery {

    private BedrockRealmDiscovery() {}

    public record World(BedrockAuthManager account, RealmsServer realm, BedrockRealmsService service) {}

    public static CompletableFuture<List<World>> worlds(final BedrockAuthManager account) {
        final BedrockRealmsService service = new BedrockRealmsService(MinecraftAuth.createHttpClient(),
            ProtocolConstants.BEDROCK_VERSION_NAME, account.getRealmsXstsToken());
        return service.isCompatibleAsync().thenCompose(compatible -> {
            if (!compatible) {
                return CompletableFuture.failedFuture(new IllegalStateException("Bedrock Realms is unavailable for this client version"));
            }
            return service.getWorldsAsync().thenApply(worlds -> {
                final var unique = new LinkedHashMap<String, World>();
                for (RealmsServer realm : worlds) {
                    unique.putIfAbsent(String.valueOf(realm.getId()), new World(account, realm, service));
                }
                return List.copyOf(unique.values());
            });
        });
    }
}
