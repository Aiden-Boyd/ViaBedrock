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

import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import com.viaversion.viafabricplus.bedrock.friends.BedrockFriendsService;
import com.viaversion.viafabricplus.bedrock.friends.BedrockFriendsService.FriendWorld;
import com.viaversion.viafabricplus.bedrock.protocoltranslator.network.BedrockConnectionUtil;
import com.viaversion.viafabricplus.screen.base.VFPScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;

public final class BedrockMenuJoiner {

    private static boolean joining;

    private BedrockMenuJoiner() {}

    public static boolean joinable(final FriendWorld world) {
        return (world.protocol() == 0 || world.protocol() == ProtocolConstants.BEDROCK_PROTOCOL_VERSION)
            && (world.maxPlayers() <= 0 || world.players() < world.maxPlayers());
    }

    public static void joinFriend(final Screen parent, final FriendWorld world) {
        final Minecraft client = Minecraft.getInstance();
        final BedrockAuthManager account = ViaFabricPlusBedrock.impl().account().get();
        if (joining || account == null || client.getConnection() != null) {
            return;
        }
        if (!joinable(world)) {
            VFPScreen.showToast(Component.translatable("bedrock_friends.viafabricplus.incompatible"));
            return;
        }
        joining = true;
        BedrockFriendsService.join(account, world).whenCompleteAsync((joined, error) -> {
            joining = false;
            if (error != null) {
                ViaFabricPlusBedrock.impl().logger().error("Failed to join a Bedrock friend's world", error);
                if (client.gui.screen() == parent) {
                    VFPScreen.showToast(Component.translatable("base.viafabricplus.something_went_wrong"));
                }
            } else if (client.gui.screen() != parent || ViaFabricPlusBedrock.impl().account().get() != account) {
                BedrockFriendsService.leaveCurrent();
            } else {
                BedrockConnectionUtil.connectNetherNet(joined.address());
            }
        }, client);
    }
}
