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

package com.viaversion.viafabricplus.bedrock.injection.mixin.core.integration;

import com.viaversion.viafabricplus.bedrock.integration.BedrockFriendEntry;
import com.viaversion.viafabricplus.bedrock.integration.BedrockServerList;
import com.viaversion.viafabricplus.bedrock.friends.BedrockFriendsService.FriendWorld;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.class)
public abstract class MixinBedrockServerSelectionList extends ObjectSelectionList<ServerSelectionList.Entry> implements BedrockServerList {

    @Shadow @Final private JoinMultiplayerScreen screen;
    @Shadow protected abstract void refreshEntries();
    @Unique private List<FriendWorld> viaBedrock$worlds = List.of();
    @Unique private Component viaBedrock$status;
    @Unique private String viaBedrock$selection;
    @Unique private Object viaBedrock$account;

    public MixinBedrockServerSelectionList(final Minecraft client, final int width, final int height, final int y, final int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    @Override
    public void viaBedrock$friends(final List<FriendWorld> worlds, final Component status) {
        final Object account = com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock.impl().account().get();
        if (this.viaBedrock$account == account && this.viaBedrock$worlds.equals(worlds) && java.util.Objects.equals(this.viaBedrock$status, status)) {
            return;
        }
        this.viaBedrock$account = account;
        this.viaBedrock$worlds = List.copyOf(worlds);
        this.viaBedrock$status = status;
        this.refreshEntries();
    }

    @Inject(method = "refreshEntries", at = @At("HEAD"))
    private void rememberFriendSelection(final CallbackInfo ci) {
        this.viaBedrock$selection = this.getSelected() instanceof BedrockFriendEntry friend ? friend.key() : null;
    }

    @Inject(method = "refreshEntries", at = @At("TAIL"))
    private void appendFriends(final CallbackInfo ci) {
        if (this.viaBedrock$status == null) {
            return;
        }
        boolean restoredSelection = false;
        for (FriendWorld world : this.viaBedrock$worlds) {
            this.viaBedrock$add(new BedrockFriendEntry(this.screen, (ServerSelectionList) (Object) this, world, Component.empty()));
            restoredSelection |= world.handleId().equals(this.viaBedrock$selection);
        }
        if (this.viaBedrock$selection != null && !restoredSelection) {
            this.setSelected(null);
        }
    }

    @Unique
    private void viaBedrock$add(final BedrockFriendEntry entry) {
        this.addEntry(entry);
        if (entry.key().equals(this.viaBedrock$selection)) {
            this.setSelected(entry);
        }
    }
}

