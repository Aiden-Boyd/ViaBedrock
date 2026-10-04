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
import com.viaversion.viafabricplus.bedrock.integration.BedrockRealmData;
import com.viaversion.viafabricplus.bedrock.integration.BedrockRealmEntry;
import com.viaversion.viafabricplus.bedrock.integration.BedrockRealmRows;
import com.viaversion.viafabricplus.bedrock.integration.BedrockRealmList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsMainScreen.RealmSelectionList.class)
public abstract class MixinBedrockRealmSelectionList extends ObjectSelectionList<RealmsMainScreen.Entry> implements BedrockRealmList {

    @Unique private String viaBedrock$selected;

    public MixinBedrockRealmSelectionList(final Minecraft client, final int width, final int height, final int y, final int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    @Inject(method = "refreshEntries", at = @At("HEAD"))
    private void rememberBedrockRealm(final RealmsMainScreen screen, final CallbackInfo ci) {
        this.viaBedrock$selected = this.getSelected() instanceof BedrockRealmEntry entry ? entry.key() : null;
    }

    @Inject(method = "refreshEntries", at = @At("TAIL"))
    private void appendBedrockRealms(final RealmsMainScreen screen, final CallbackInfo ci) {
        for (BedrockRealmData data : ((BedrockRealmRows) screen).viaBedrock$realms()) {
            this.viaBedrock$addRealm(screen, data);
        }
    }

    @Override
    public BedrockRealmEntry viaBedrock$addRealm(final RealmsMainScreen screen, final BedrockRealmData data) {
        final BedrockRealmEntry entry = new BedrockRealmEntry(screen, data);
        this.addEntry(entry);
        if (entry.key().equals(this.viaBedrock$selected)) {
            this.setSelected(entry);
        }
        return entry;
    }
}
