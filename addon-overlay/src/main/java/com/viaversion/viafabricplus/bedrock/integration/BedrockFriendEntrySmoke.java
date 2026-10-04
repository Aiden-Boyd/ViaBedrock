package com.viaversion.viafabricplus.bedrock.integration;

import com.viaversion.viafabricplus.bedrock.friends.BedrockFriendsService.FriendWorld;
import java.util.ArrayList;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

public final class BedrockFriendEntrySmoke {
    private BedrockFriendEntrySmoke() {}

    public static BedrockFriendEntry verify(final JoinMultiplayerScreen screen, final ServerSelectionList list,
                                            final FriendWorld fixture, final ServerSelectionList.Entry row) {
        final var rows = list.children();
        final int lanIndex = java.util.stream.IntStream.range(0, rows.size())
            .filter(index -> rows.get(index) instanceof ServerSelectionList.LANHeader).findFirst().orElseThrow();
        if (rows.indexOf(row) >= lanIndex) {
            throw new AssertionError("Bedrock worlds must be grouped with saved servers above LAN discovery");
        }
        final int[] joins = {0};
        final var clickable = new BedrockFriendEntry(screen, list, fixture, Component.empty()) {
            @Override public void join() { joins[0]++; }
        };
        final var clickRows = new ArrayList<>(rows);
        clickRows.set(clickRows.indexOf(row.get()), clickable);
        list.replaceEntries(clickRows);
        final double textX = clickable.getContentX() + 40;
        final double rowY = clickable.getContentY() + 8;
        list.setSelected(null);
        list.mouseClicked(new MouseButtonEvent(textX, rowY, new MouseButtonInfo(1, 0)), false);
        if (list.getSelected() != clickable || joins[0] != 0) {
            throw new AssertionError("A primary click must select the Bedrock row without joining");
        }
        list.mouseClicked(new MouseButtonEvent(clickable.getContentX() + 24, rowY, new MouseButtonInfo(1, 0)), false);
        if (joins[0] != 1) throw new AssertionError("The Bedrock play icon did not dispatch Join");
        list.mouseClicked(new MouseButtonEvent(textX, rowY, new MouseButtonInfo(1, 0)), true);
        if (joins[0] != 2) throw new AssertionError("Double clicking a Bedrock row did not dispatch Join");
        clickable.mouseClicked(new MouseButtonEvent(textX, rowY, new MouseButtonInfo(2, 0)), true);
        if (joins[0] != 2) throw new AssertionError("A secondary click must not join a Bedrock world");

        return clickable;
    }
}
