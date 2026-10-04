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

import com.mojang.realmsclient.dto.RealmsServer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.extra.realms.service.impl.BedrockRealmsService;

/** A display-only Java row. Its ID must never be sent to Java Realms. */
public final class BedrockRealmData extends RealmsServer {

    public final BedrockAuthManager account;
    public final net.raphimc.minecraftauth.extra.realms.model.RealmsServer bedrock;
    public final BedrockRealmsService service;

    public BedrockRealmData(final long displayId, final BedrockAuthManager account,
                            final net.raphimc.minecraftauth.extra.realms.model.RealmsServer bedrock,
                            final BedrockRealmsService service, final Component status) {
        this.account = account;
        this.bedrock = bedrock;
        this.service = service;
        this.id = displayId;
        this.name = bedrock == null ? "Bedrock Realms" : bedrock.getName();
        this.motd = bedrock == null ? status.getString() : bedrock.getMotd();
        this.owner = bedrock == null ? "" : bedrock.getOwnerName();
        this.ownerUUID = new UUID(0, 0);
        this.players = List.of();
        this.slots = Map.of();
        this.worldType = WorldType.NORMAL;
        this.activeVersion = bedrock == null ? "" : bedrock.getActiveVersion();
        this.state = bedrock == null || !"CLOSED".equalsIgnoreCase(bedrock.getState()) ? State.OPEN : State.CLOSED;
        this.expired = bedrock != null && bedrock.isExpired();
        this.compatibility = bedrock == null || bedrock.isCompatible() ? Compatibility.COMPATIBLE : Compatibility.INCOMPATIBLE;
    }

    public String key() {
        return this.bedrock == null ? "bedrock-realms" : String.valueOf(this.bedrock.getId());
    }
}
