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
            ViaFabricPlusBedrock.impl().logger().info("BEDROCK_NATIVE_MENU_SMOKE_PASSED");
            client.stop();
        }
    }
}
