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

import com.mojang.realmsclient.gui.screens.RealmsJoinRealmWithCodeScreen;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import com.viaversion.viafabricplus.bedrock.integration.*;
import com.viaversion.viafabricplus.bedrock.realms.BedrockRealmsError;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsJoinRealmWithCodeScreen.class)
public abstract class MixinBedrockRealmInvite {
    @Shadow @Final private Screen lastScreen;
    @Shadow @Final private Runnable onJoin;
    @Shadow private EditBox joinCode;
    @Shadow private Button joinButton;
    @Shadow protected abstract void showMessage(Component message);

    @Inject(method = "init", at = @At("TAIL"))
    private void allowInviteLinks(final CallbackInfo ci) {
        this.joinCode.setMaxLength(256);
    }

    @Inject(method = "joinRealm", at = @At("HEAD"))
    private void normalizeInvite(final CallbackInfo ci) {
        try {
            this.joinCode.setValue(RealmInviteCode.normalize(this.joinCode.getValue()));
        } catch (IllegalArgumentException ignored) {
            // The native validation retains responsibility for malformed input.
        }
    }

    @Inject(method = "onJoinFailure", at = @At("HEAD"), cancellable = true)
    private void tryBedrockInvite(final RealmsServiceException failure, final CallbackInfo ci) {
        final var account = ViaFabricPlusBedrock.impl().account().get();
        if (account == null) {
            return;
        }
        ci.cancel();
        final Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.gui.screen() != (Object) this) {
                return;
            }
            final String code;
            try {
                code = RealmInviteCode.normalize(this.joinCode.getValue());
            } catch (IllegalArgumentException error) {
                this.viaBedrock$failed(Component.literal(error.getMessage()));
                return;
            }
            this.joinCode.setEditable(false);
            this.joinButton.active = false;
            this.showMessage(Component.literal("Checking Bedrock Realm…"));
            final var service = BedrockRealmDiscovery.service(account);
            service.acceptInviteAsync(code).whenCompleteAsync((realm, error) -> {
                if (client.gui.screen() != (Object) this) {
                    return;
                }
                if (ViaFabricPlusBedrock.impl().account().get() != account) {
                    this.viaBedrock$failed(Component.literal("Your Bedrock account changed. Try again."));
                } else if (error != null) {
                    this.viaBedrock$failed(BedrockRealmsError.describe(error));
                } else {
                    this.onJoin.run();
                    if (client.gui.screen() instanceof BedrockRealmRows rows) {
                        rows.viaBedrock$acceptedRealm(new BedrockRealmDiscovery.World(account, realm, service));
                    }
                }
            }, client);
        });
    }

    @Unique
    private void viaBedrock$failed(final Component message) {
        this.joinCode.setEditable(true);
        this.joinButton.active = !this.joinCode.getValue().isBlank();
        this.showMessage(message);
    }
}
