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

package com.viaversion.viafabricplus.bedrock.client.mixin;

import com.viaversion.viafabricplus.bedrock.client.access.IServerData;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ManageServerScreen.class)
public abstract class MixinEditionManageServerScreen extends Screen {
    @Shadow @Final private ServerData serverData;
    protected MixinEditionManageServerScreen(final Component title) { super(title); }
    @Unique private Component viaBedrock$editionLabel() {
        return Component.literal(BedrockProtocolVersion.BEDROCK_LATEST.equals(((IServerData) this.serverData).viaFabricPlus$forcedVersion()) ? "Bedrock" : "Java");
    }
    @Inject(method = "init", at = @At("RETURN"))
    private void addEditionChoice(final CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(this.viaBedrock$editionLabel(), button -> {
            final var data = (IServerData) this.serverData;
            data.viaFabricPlus$forceVersion(BedrockProtocolVersion.BEDROCK_LATEST.equals(data.viaFabricPlus$forcedVersion()) ? null : BedrockProtocolVersion.BEDROCK_LATEST);
            button.setMessage(this.viaBedrock$editionLabel());
        }).bounds(this.width - 85, 5, 80, 20).build());
    }
}
