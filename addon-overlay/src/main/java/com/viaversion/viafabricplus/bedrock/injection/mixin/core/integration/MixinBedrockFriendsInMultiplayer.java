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

import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import com.viaversion.viafabricplus.bedrock.friends.BedrockFriendsService;
import com.viaversion.viafabricplus.bedrock.friends.BedrockFriendsService.FriendWorld;
import com.viaversion.viafabricplus.bedrock.integration.AccountWorldList;
import com.viaversion.viafabricplus.bedrock.integration.BedrockMenuJoiner;
import com.viaversion.viafabricplus.bedrock.integration.BedrockServerList;
import com.viaversion.viafabricplus.bedrock.integration.BedrockMenuSmokeAccess;
import com.viaversion.viafabricplus.bedrock.integration.BedrockFriendEntry;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MixinBedrockFriendsInMultiplayer implements BedrockMenuSmokeAccess {

    @Shadow protected ServerSelectionList serverSelectionList;
    @Shadow private Button joinButton;
    @Shadow private Button editButton;
    @Shadow private Button deleteButton;
    @Unique private final AccountWorldList<FriendWorld> viaBedrock$friends = new AccountWorldList<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void refreshFriends(final CallbackInfo ci) {
        final BedrockAuthManager account = ViaFabricPlusBedrock.impl().account().get();
        this.viaBedrock$friends.refresh(account, System.nanoTime(), TimeUnit.SECONDS.toNanos(30),
            () -> BedrockFriendsService.worlds(account).thenApply(worlds -> {
                final var unique = new LinkedHashMap<String, FriendWorld>();
                worlds.stream().forEach(world -> unique.putIfAbsent(world.handleId(), world));
                return unique.values().stream().sorted(Comparator.comparing(FriendWorld::hostName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(FriendWorld::worldName, String.CASE_INSENSITIVE_ORDER)).toList();
            }), Minecraft.getInstance(), this::viaBedrock$updateList);
        this.useBedrockFriendButtons(ci);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void initializeFriendRows(final CallbackInfo ci) {
        this.refreshFriends(ci);
        this.viaBedrock$updateList();
    }

    @Inject(method = "onSelectedChange", at = @At("TAIL"))
    private void useBedrockFriendButtons(final CallbackInfo ci) {
        if (this.serverSelectionList.getSelected() instanceof BedrockFriendEntry friend) {
            this.joinButton.active = friend.available();
            this.editButton.active = false;
            this.deleteButton.active = false;
        }
    }

    @Override
    public boolean viaBedrock$hasMenuRows() {
        if (this.serverSelectionList.children().stream().anyMatch(BedrockFriendEntry.class::isInstance)) {
            throw new AssertionError("Empty Xbox friends must not add a placeholder row");
        }
        final var fixture = new FriendWorld("menu-qa", "0", "Menu QA", "Menu QA World", "26.3", 1, 8, 0, null);
        ((BedrockServerList) this.serverSelectionList).viaBedrock$friends(java.util.List.of(fixture), Component.empty());
        final var row = this.serverSelectionList.children().stream().filter(BedrockFriendEntry.class::isInstance).findFirst();
        if (row.isEmpty()) {
            return false;
        }
        final var clickable = com.viaversion.viafabricplus.bedrock.integration.BedrockFriendEntrySmoke.verify(
            (JoinMultiplayerScreen) (Object) this, this.serverSelectionList, fixture, row.get());
        this.serverSelectionList.setSelected(clickable);
        if (!(clickable instanceof ServerSelectionList.OnlineServerEntry) || !this.joinButton.active || this.editButton.active || this.deleteButton.active) {
            return false;
        }
        final var updated = new FriendWorld("menu-qa", "0", "Menu QA", "Menu QA World", "26.3", 2, 8, 0, null);
        ((BedrockServerList) this.serverSelectionList).viaBedrock$friends(java.util.List.of(updated), Component.empty());
        if (!(this.serverSelectionList.getSelected() instanceof BedrockFriendEntry selected) || !selected.key().equals("menu-qa")) {
            throw new AssertionError("Refreshing a friend row lost its selection");
        }
        final var full = new FriendWorld("menu-qa", "0", "Menu QA", "Menu QA World", "26.3", 8, 8, 0, null);
        ((BedrockServerList) this.serverSelectionList).viaBedrock$friends(java.util.List.of(full), Component.empty());
        if (this.joinButton.active) throw new AssertionError("A full friend world left Join enabled");
        ((BedrockServerList) this.serverSelectionList).viaBedrock$friends(java.util.List.of(), Component.empty());
        if (this.serverSelectionList.getSelected() instanceof BedrockFriendEntry || this.joinButton.active) {
            throw new AssertionError("An offline friend left a stale selection or Join button");
        }
        ((BedrockServerList) this.serverSelectionList).viaBedrock$friends(java.util.List.of(fixture), Component.empty());
        this.serverSelectionList.setSelected(this.serverSelectionList.children().stream().filter(BedrockFriendEntry.class::isInstance).findFirst().orElseThrow());
        return true;
    }

    @Unique
    private void viaBedrock$updateList() {
        if (this.serverSelectionList == null || Minecraft.getInstance().gui.screen() != (Object) this) {
            return;
        }
        final String key = ViaFabricPlusBedrock.impl().account().get() == null ? "bedrock_menu.viafabricplus.sign_in"
            : this.viaBedrock$friends.loading() ? "bedrock_menu.viafabricplus.loading_friends"
            : this.viaBedrock$friends.error() != null ? "bedrock_menu.viafabricplus.friends_error"
            : this.viaBedrock$friends.worlds().isEmpty() ? "bedrock_menu.viafabricplus.no_friends"
            : "bedrock_menu.viafabricplus.manage_friends";
        ((BedrockServerList) this.serverSelectionList).viaBedrock$friends(this.viaBedrock$friends.worlds(), Component.translatable(key));
    }
}

