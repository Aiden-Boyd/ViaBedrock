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
import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import com.viaversion.viafabricplus.bedrock.integration.BedrockMenuSmokeAccess;
import net.minecraft.client.Minecraft;
import com.viaversion.viafabricplus.bedrock.skin.JavaSkinService;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Enabled only by the explicit CI JVM property; never requests Xbox credentials. */
@Mixin(Minecraft.class)
public abstract class MixinBedrockMenuSmokeTest {

    @Unique private static final boolean VIA_BEDROCK_MENU_SMOKE = Boolean.getBoolean("viaBedrock.menuSmoke");
    @Unique private int viaBedrock$smokeTicks;
    @Unique private int viaBedrock$smokePhase;
    @Unique private int viaBedrock$smokeWait;
    @Unique private Screen viaBedrock$smokeScreen;
    @Unique private Screen viaBedrock$smokeParent;

    @Inject(method = "tick", at = @At("TAIL"))
    private void verifyNativeMenus(final CallbackInfo ci) {
        if (!VIA_BEDROCK_MENU_SMOKE || ++this.viaBedrock$smokeTicks < 40) {
            return;
        }
        final Minecraft client = Minecraft.getInstance();
        if (this.viaBedrock$smokeTicks % 100 == 0) {
            ViaFabricPlusBedrock.impl().logger().info("Bedrock menu smoke phase {} screen {}",
                this.viaBedrock$smokePhase, client.gui.screen() == null ? "null" : client.gui.screen().getClass().getName());
        }
        if (this.viaBedrock$smokePhase == 0) {
            if (!(client.gui.screen() instanceof TitleScreen) || ViaFabricPlusBedrock.impl().account() == null) {
                return;
            }
            if (!JavaSkinService.prepare().isDone()) {
                return;
            }
            if (JavaSkinService.current() == null) {
                throw new AssertionError("Java default skin did not load in the development client");
            }
            if (JavaSkinService.visibleSkin() == null ||
                new net.minecraft.client.multiplayer.PlayerInfo(client.getGameProfile(), false).getSkin() == null) {
                throw new AssertionError("Java account skin rendering or PlayerInfo mixin did not initialize");
            }
            // Exercise saved-account restoration and JWT providers without signing in or making requests.
            final var httpClient = net.raphimc.minecraftauth.MinecraftAuth.createHttpClient();
            final var auth = net.raphimc.minecraftauth.bedrock.BedrockAuthManager.create(
                httpClient, net.raphimc.viabedrock.protocol.data.ProtocolConstants.BEDROCK_VERSION_NAME);
            final var restored = net.raphimc.minecraftauth.bedrock.BedrockAuthManager.fromJson(
                httpClient, net.raphimc.viabedrock.protocol.data.ProtocolConstants.BEDROCK_VERSION_NAME,
                net.raphimc.minecraftauth.bedrock.BedrockAuthManager.toJson(auth));
            if (restored == null) throw new AssertionError("Bedrock account restoration failed");
            final var key = io.jsonwebtoken.Jwts.SIG.HS256.key().build();
            final String jwt = io.jsonwebtoken.Jwts.builder().subject("offline-smoke").signWith(key).compact();
            final String subject = io.jsonwebtoken.Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(jwt).getPayload().getSubject();
            if (!"offline-smoke".equals(subject)) throw new AssertionError("JWT provider round trip failed");
            ViaFabricPlusBedrock.impl().logger().info("BEDROCK_AUTH_RUNTIME_SMOKE_PASSED");
            ViaFabricPlusBedrock.impl().logger().info("BEDROCK_JAVA_SKIN_SMOKE_PASSED");
            this.viaBedrock$smokeParent = client.gui.screen();
            this.viaBedrock$smokeScreen = new JoinMultiplayerScreen(this.viaBedrock$smokeParent);
            client.gui.setScreen(this.viaBedrock$smokeScreen);
            this.viaBedrock$smokePhase = 1;
        } else if (this.viaBedrock$smokePhase == 1) {
            if (!((BedrockMenuSmokeAccess) this.viaBedrock$smokeScreen).viaBedrock$hasMenuRows()) {
                throw new AssertionError("Native Multiplayer empty state or friend selection failed");
            }
            this.viaBedrock$smokePhase = 2;
            this.viaBedrock$smokeWait = 0;
        } else if (this.viaBedrock$smokePhase == 2) {
            if (++this.viaBedrock$smokeWait < 10) {
                return;
            }
            this.viaBedrock$smokeScreen = new RealmsMainScreen(this.viaBedrock$smokeParent);
            client.gui.setScreen(this.viaBedrock$smokeScreen);
            this.viaBedrock$smokePhase = 3;
        } else if (this.viaBedrock$smokePhase == 3) {
            if (!((BedrockMenuSmokeAccess) this.viaBedrock$smokeScreen).viaBedrock$hasMenuRows()) {
                throw new AssertionError("Native Realms empty state or Realm selection failed");
            }
            this.viaBedrock$smokePhase = 4;
            this.viaBedrock$smokeWait = 0;
        } else if (this.viaBedrock$smokePhase == 4) {
            if (++this.viaBedrock$smokeWait < 10) {
                return;
            }
            this.viaBedrock$smokeScreen = new com.mojang.realmsclient.gui.screens.RealmsJoinRealmWithCodeScreen(
                this.viaBedrock$smokeScreen, () -> { throw new AssertionError("Smoke must not redeem an invite"); });
            client.gui.setScreen(this.viaBedrock$smokeScreen);
            this.viaBedrock$smokePhase = 5;
            this.viaBedrock$smokeWait = 0;
        } else if (this.viaBedrock$smokePhase == 5) {
            if (++this.viaBedrock$smokeWait < 10) return;
            final var field = this.viaBedrock$smokeScreen.children().stream()
                .filter(net.minecraft.client.gui.components.EditBox.class::isInstance)
                .map(net.minecraft.client.gui.components.EditBox.class::cast).findFirst().orElseThrow();
            final String link = "https://realms.gg/AbCdEfGhIjKlMnOp";
            field.setValue(link);
            if (!link.equals(field.getValue())) throw new AssertionError("Realm invite link was truncated");
            ViaFabricPlusBedrock.impl().logger().info("BEDROCK_REALM_INVITE_SCREEN_SMOKE_PASSED");
            ViaFabricPlusBedrock.impl().logger().info("BEDROCK_NATIVE_MENU_SMOKE_PASSED");
            client.stop();
        }
    }
}
