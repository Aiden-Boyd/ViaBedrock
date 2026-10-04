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

import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import com.viaversion.viafabricplus.bedrock.friends.BedrockFriendsService.FriendWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class BedrockFriendEntry extends ServerSelectionList.OnlineServerEntry {

    private final JoinMultiplayerScreen screen;
    private final ServerSelectionList list;
    private final FriendWorld world;
    private final Component status;
    private final BedrockAuthManager account;

    public BedrockFriendEntry(final JoinMultiplayerScreen screen, final ServerSelectionList list,
                              final FriendWorld world, final Component status) {
        list.super(screen, new net.minecraft.client.multiplayer.ServerData(world.worldName(),
            "Xbox: " + world.hostName(), net.minecraft.client.multiplayer.ServerData.Type.OTHER));
        this.screen = screen;
        this.list = list;
        this.world = world;
        this.status = status;
        this.account = ViaFabricPlusBedrock.impl().account().get();
    }

    public String key() {
        return this.world == null ? "bedrock-friends" : this.world.handleId();
    }

    public boolean available() {
        return this.account == ViaFabricPlusBedrock.impl().account().get()
            && !BedrockMenuJoiner.isJoining() && BedrockMenuJoiner.joinable(this.world);
    }

    @Override
    protected boolean matches(final ServerSelectionList.Entry entry) {
        return entry instanceof BedrockFriendEntry friend && this.key().equals(friend.key());
    }

    @Override
    public Component getNarration() {
        return this.world == null ? Component.literal("Xbox friends").append(". ").append(this.status)
            : Component.literal(this.world.worldName() + ", " + this.world.hostName() + ", Bedrock");
    }

    @Override
    public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                               final boolean hovered, final float delta) {
        final var font = Minecraft.getInstance().font;
        final int left = this.getContentX();
        final int y = this.getContentY();
        final int x = left + 35;
        final int width = Math.max(0, this.getContentWidth() - 35);
        final String count = this.world.maxPlayers() > 0 ? this.world.players() + "/" + this.world.maxPlayers()
            : Integer.toString(this.world.players());
        graphics.blit(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("textures/misc/unknown_server.png"),
            left, y, 0, 0, 32, 32, 32, 32);
        graphics.text(font, font.plainSubstrByWidth(this.world.worldName(),
            Math.max(0, width - font.width(count) - 8)), x, y + 1, -1);
        graphics.text(font, count, x + width - font.width(count), y + 1,
            BedrockMenuJoiner.joinable(this.world) ? 0xFFAAAAAA : 0xFFFF5555);
        graphics.text(font, font.plainSubstrByWidth(this.world.hostName(), width), x, y + 12, 0xFF808080);
        graphics.text(font, font.plainSubstrByWidth("Bedrock " + this.world.version(), width), x, y + 23, 0xFF808080);
        if (hovered && this.available()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("server_list/join"),
                left, y, 32, 32);
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != 0) {
            return false;
        }
        this.list.setSelected(this);
        if (doubleClick || (event.x() >= this.getContentX() && event.x() - this.getContentX() < 32)) {
            this.join();
        }
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.isSelection()) {
            this.join();
            return true;
        }
        return false; // Live friend rows cannot reorder saved servers with Shift+arrow.
    }

    @Override
    public void join() {
        if (this.available()) {
            BedrockMenuJoiner.joinFriend(this.screen, this.world);
        }
    }
}
