/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.viabedrock.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisconnectHandlerTest {

    @Test
    void disconnectNotificationIsFollowedByCloseEvenWhenTransportRemainsActive() {
        final int[] notifications = new int[1];
        final EmbeddedChannel channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void disconnect(final ChannelHandlerContext ctx, final ChannelPromise promise) {
                notifications[0]++;
                promise.setSuccess();
            }
        }, new DisconnectHandler());
        assertTrue(channel.isActive());
        assertTrue(channel.close().isSuccess());
        channel.runPendingTasks();
        assertFalse(channel.isOpen());
        assertEquals(1, notifications[0]);
        channel.close();
        assertEquals(1, notifications[0]);
        channel.finishAndReleaseAll();
    }

    @Test
    void failedNotificationStillReleasesTheChannel() {
        final EmbeddedChannel channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void disconnect(final ChannelHandlerContext ctx, final ChannelPromise promise) {
                promise.setFailure(new IllegalStateException("Notification failed"));
            }
        }, new DisconnectHandler());
        assertTrue(channel.close().isSuccess());
        channel.runPendingTasks();
        assertFalse(channel.isOpen());
        channel.finishAndReleaseAll();
    }

}
