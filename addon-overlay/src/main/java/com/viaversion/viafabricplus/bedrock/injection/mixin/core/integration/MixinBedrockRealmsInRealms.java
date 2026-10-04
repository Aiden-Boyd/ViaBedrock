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

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import com.viaversion.viafabricplus.bedrock.integration.*;
import com.viaversion.viafabricplus.bedrock.integration.BedrockRealmDiscovery.World;
import com.viaversion.viafabricplus.bedrock.screen.BedrockRealmsScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsMainScreen.class)
public abstract class MixinBedrockRealmsInRealms implements BedrockRealmRows, BedrockMenuSmokeAccess {

    @Shadow private RealmsMainScreen.RealmSelectionList realmSelectionList;

    @Shadow private Button playButton;
    @Shadow private Button configureButton;
    @Shadow private Button renewButton;
    @Shadow private Button leaveButton;
    @Shadow protected abstract RealmsServer getSelectedServer();
    @Shadow protected abstract void refreshListAndLayout();
    @Shadow protected abstract void updateLayout(RealmsMainScreen.LayoutState state);
    @Unique private final AccountWorldList<World> viaBedrock$worlds = new AccountWorldList<>();

    @Inject(method = "init", at = @At("TAIL"))
    private void initializeBedrockRealms(final CallbackInfo ci) {
        this.viaBedrock$refresh(ci);
        this.refreshListAndLayout();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void viaBedrock$refresh(final CallbackInfo ci) {
        final BedrockAuthManager account = ViaFabricPlusBedrock.impl().account().get();
        this.viaBedrock$worlds.refresh(account, System.nanoTime(), TimeUnit.SECONDS.toNanos(30),
            () -> BedrockRealmDiscovery.worlds(account), Minecraft.getInstance(), () -> {
                if (Minecraft.getInstance().gui.screen() == (Object) this) {
                    this.refreshListAndLayout();
                }
            });
    }

    @Override
    public boolean viaBedrock$hasMenuRows() {
        final var row = this.realmSelectionList.children().stream().filter(BedrockRealmEntry.class::isInstance).findFirst();
        if (row.isEmpty()) {
            return false;
        }
        this.realmSelectionList.setSelected(row.get());
        return this.playButton.active && !this.configureButton.active && !this.renewButton.active && !this.leaveButton.active;
    }

    @Override
    public List<BedrockRealmData> viaBedrock$realms() {
        final BedrockAuthManager account = ViaFabricPlusBedrock.impl().account().get();
        final String key = account == null ? "bedrock_menu.viafabricplus.sign_in_realms"
            : this.viaBedrock$worlds.loading() ? "bedrock_menu.viafabricplus.loading_realms"
            : this.viaBedrock$worlds.error() != null ? "bedrock_menu.viafabricplus.realms_error"
            : this.viaBedrock$worlds.worlds().isEmpty() ? "bedrock_menu.viafabricplus.no_realms"
            : "bedrock_menu.viafabricplus.manage_realms";
        final List<BedrockRealmData> rows = new ArrayList<>();
        long id = Long.MIN_VALUE;
        rows.add(new BedrockRealmData(id++, account, null, null, Component.translatable(key)));
        for (World world : this.viaBedrock$worlds.worlds()) {
            if (world.account() == account) {
                rows.add(new BedrockRealmData(id++, world.account(), world.realm(), world.service(), Component.empty()));
            }
        }
        return rows;
    }

    @Inject(method = "updateLayout()V", at = @At("HEAD"), cancellable = true)
    private void showCombinedRealmList(final CallbackInfo ci) {
        this.updateLayout(RealmsMainScreen.LayoutState.LIST);
        ci.cancel();
    }

    @Inject(method = "openPdpIfNoRealms", at = @At("HEAD"), cancellable = true)
    private void retainBedrockRealmList(final CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "updateButtonStates", at = @At("TAIL"))
    private void useBedrockButtons(final CallbackInfo ci) {
        if (this.getSelectedServer() instanceof BedrockRealmData realm) {
            this.playButton.active = realm.bedrock == null || realm.shouldPlayButtonBeActive();
            this.configureButton.active = false;
            this.renewButton.active = false;
            this.leaveButton.active = false;
        }
    }

    @Inject(method = "play(Lcom/mojang/realmsclient/dto/RealmsServer;Lnet/minecraft/client/gui/screens/Screen;)V",
        at = @At("HEAD"), cancellable = true)
    private static void joinBedrockRealm(final RealmsServer server, final Screen parent, final CallbackInfo ci) {
        if (server instanceof BedrockRealmData realm) {
            ci.cancel();
            BedrockMenuJoiner.joinRealm(parent, realm);
        }
    }

    @Inject(method = "play(Lcom/mojang/realmsclient/dto/RealmsServer;Lnet/minecraft/client/gui/screens/Screen;Z)V",
        at = @At("HEAD"), cancellable = true)
    private static void joinBedrockRealmChecked(final RealmsServer server, final Screen parent,
                                               final boolean skipCompatibility, final CallbackInfo ci) {
        joinBedrockRealm(server, parent, ci);
    }

    @Inject(method = {"configureClicked", "onRenew", "leaveClicked"}, at = @At("HEAD"), cancellable = true)
    private void preventJavaRealmActions(final RealmsServer server, final CallbackInfo ci) {
        if (server instanceof BedrockRealmData) {
            ci.cancel();
            new BedrockRealmsScreen().open((Screen) (Object) this);
        }
    }

    @Inject(method = "leaveServer", at = @At("HEAD"), cancellable = true)
    private void preventJavaRealmLeave(final RealmsServer server, final CallbackInfo ci) {
        if (server instanceof BedrockRealmData) {
            ci.cancel();
        }
    }
}
