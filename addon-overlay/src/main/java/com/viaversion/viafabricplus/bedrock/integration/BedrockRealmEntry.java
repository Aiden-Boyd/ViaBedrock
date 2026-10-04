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

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class BedrockRealmEntry extends RealmsMainScreen.ServerEntry {

    private final BedrockRealmData data;

    public BedrockRealmEntry(final RealmsMainScreen screen, final BedrockRealmData data) {
        screen.super(data);
        this.data = data;
    }

    public String key() {
        return this.data.key();
    }

    @Override
    public Component getNarration() {
        return Component.literal(this.data.name + ", Bedrock Realms, " + this.data.motd);
    }

    @Override
    public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                               final boolean hovered, final float delta) {
        final var font = Minecraft.getInstance().font;
        final int x = this.getContentX() + 4;
        final int y = this.getContentY() + 2;
        final int width = this.getContentWidth() - 8;
        final String label = this.data.bedrock == null ? "" : this.data.expired ? "Expired"
            : this.data.state == com.mojang.realmsclient.dto.RealmsServer.State.CLOSED ? "Closed"
            : !this.data.isCompatible() ? "Incompatible" : "Bedrock";
        graphics.text(font, font.plainSubstrByWidth(this.data.name == null ? "Bedrock Realm" : this.data.name,
            Math.max(0, width - font.width(label) - 12)), x, y, 0xFF58A6FF);
        graphics.text(font, label, x + width - font.width(label), y, 0xFFB8B8B8);
        final String detail = this.data.bedrock == null ? this.data.motd : this.data.owner + " • " + this.data.activeVersion;
        graphics.text(font, font.plainSubstrByWidth(detail == null ? "" : detail, width), x, y + font.lineHeight + 2, 0xFFB8B8B8);
        if (this.data.bedrock != null && this.data.motd != null) {
            graphics.text(font, font.plainSubstrByWidth(this.data.motd, width), x, y + (font.lineHeight + 2) * 2, -1);
        }
    }
}
