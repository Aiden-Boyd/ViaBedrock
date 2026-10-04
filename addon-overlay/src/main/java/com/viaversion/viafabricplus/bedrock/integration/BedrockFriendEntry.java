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
import com.viaversion.viafabricplus.bedrock.screen.BedrockFriendsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.minecraft.network.chat.Component;

public final class BedrockFriendEntry extends ServerSelectionList.Entry {

    private final JoinMultiplayerScreen screen;
    private final ServerSelectionList list;
    private final FriendWorld world;
    private final Component status;
    private final BedrockAuthManager account;

    public BedrockFriendEntry(final JoinMultiplayerScreen screen, final ServerSelectionList list,
                              final FriendWorld world, final Component status) {
        this.screen = screen;
        this.list = list;
        this.world = world;
        this.status = status;
        this.account = ViaFabricPlusBedrock.impl().account().get();
    }

    public String key() {
        return this.world == null ? "bedrock-friends" : this.world.handleId();
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
        final int x = this.getContentX() + 4;
        final int y = this.getContentY() + 5;
        final int width = this.getContentWidth() - 8;
        final String count = this.world == null ? "" : this.world.players() + "/" + this.world.maxPlayers();
        final String name = this.world == null ? "Xbox friends" : this.world.worldName();
        final String detail = this.world == null ? this.status.getString() : this.world.hostName() + " • Bedrock " + this.world.version();
        graphics.text(font, font.plainSubstrByWidth(name, Math.max(0, width - font.width(count) - 12)), x, y, 0xFF58A6FF);
        graphics.text(font, count, x + width - font.width(count), y, -1);
        graphics.text(font, font.plainSubstrByWidth(detail, width), x, y + font.lineHeight + 3, 0xFFB8B8B8);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != 0) {
            return false;
        }
        this.list.setSelected(this);
        if (doubleClick) {
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
        return super.keyPressed(event);
    }

    @Override
    public void join() {
        if (this.world != null) {
            if (this.account != ViaFabricPlusBedrock.impl().account().get()) {
                return;
            }
            BedrockMenuJoiner.joinFriend(this.screen, this.world);
        } else if (ViaFabricPlusBedrock.impl().account().get() == null) {
            ViaFabricPlusBedrock.impl().account().login();
        } else {
            new BedrockFriendsScreen().open(this.screen);
        }
    }
}
