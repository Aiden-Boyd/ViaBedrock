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

package com.viaversion.viafabricplus.bedrock.screen;

import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BedrockSettingsScreen extends Screen {
    private final Screen parent;
    public BedrockSettingsScreen(final Screen parent) { super(Component.literal("Bedrock Multiplayer")); this.parent = parent; }
    private Component portLabel() {
        return Component.literal("Default Bedrock port: " + (ViaFabricPlusBedrock.impl().settings().defaultBedrockPort() ? "ON" : "OFF"));
    }
    @Override protected void init() {
        final var account = ViaFabricPlusBedrock.impl().account();
        this.addRenderableWidget(Button.builder(Component.literal(account.get() == null ? "Sign in to Microsoft" : "Switch Microsoft account"), button -> account.login())
            .bounds(this.width / 2 - 100, 70, 200, 20).build());
        this.addRenderableWidget(Button.builder(this.portLabel(), button -> {
            final var settings = ViaFabricPlusBedrock.impl().settings();
            settings.defaultBedrockPort(!settings.defaultBedrockPort()); button.setMessage(this.portLabel());
        }).bounds(this.width / 2 - 100, 100, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
            .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }
    @Override public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, -1);
        final var name = ViaFabricPlusBedrock.impl().account().displayName();
        graphics.centeredText(this.font, name == null ? "No Microsoft account connected" : name, this.width / 2, 45, 0xFFAAAAAA);
    }
    @Override public void onClose() { this.minecraft.gui.setScreen(this.parent); }
}
