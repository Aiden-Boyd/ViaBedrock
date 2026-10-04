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
package net.raphimc.viabedrock.api.model.entity;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PlayerAuthInputData;
import net.raphimc.viabedrock.protocol.data.enums.java.Relative;
import net.raphimc.viabedrock.protocol.model.PlayerAbilities;
import net.raphimc.viabedrock.protocol.model.Position3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovementSyncTest {

    @BeforeAll
    static void initializeTypes() throws ClassNotFoundException {
        Class.forName("com.viaversion.viaversion.api.minecraft.data.StructuredDataKey");
    }

    @Test
    void onlyLatestTeleportWithCorrectSignIsAcknowledged() {
        final ClientPlayerEntity player = player();
        player.setInitiallySpawned();
        final int first = teleport(player, false);
        final int fake = teleport(player, true);
        assertTrue(first > 0);
        assertTrue(fake < 0);
        player.confirmTeleport(first);
        player.confirmTeleport(-fake);
        assertFalse(player.authInputData().contains(PlayerAuthInputData.HandledTeleport));
        player.confirmTeleport(fake);
        final int real = teleport(player, false);
        player.confirmTeleport(real);
        assertTrue(player.authInputData().contains(PlayerAuthInputData.HandledTeleport));
        player.authInputData().clear();
        player.confirmTeleport(real);
        player.confirmTeleport(0);
        assertTrue(player.authInputData().isEmpty());
    }

    @Test
    void nonfiniteMovementCannotBypassChecksAfterTeleport() {
        final ClientPlayerEntity player = player();
        player.setPosition(new Position3f(1, 65, 2));
        player.setInitiallySpawned();
        player.confirmTeleport(teleport(player, false));
        player.updatePlayerPosition(Double.NaN, 64, 2, (short) 0);
        player.updatePlayerPosition(Double.MAX_VALUE, 64, 2, (short) 0);
        player.updatePlayerPosition(Float.POSITIVE_INFINITY, 0F, (short) 0);
        assertEquals(new Position3f(1, 65, 2), player.position());
        // Invalid packets do not consume the one permitted movement after a valid teleport.
        player.updatePlayerPosition(2D, 64D, 3D, (short) 0);
        assertEquals(2F, player.position().x());
        assertEquals(3F, player.position().z());
    }

    @Test
    void rotationOnlyBeforeSpawnDoesNotSpamPositionResyncs() {
        final int[] resyncs = new int[1];
        final ClientPlayerEntity player = new ClientPlayerEntity(user(), 1, UUID.randomUUID(), abilities()) {
            @Override
            public void sendPlayerPositionPacketToClient(final Set<Relative> relatives) {
                resyncs[0]++;
            }
        };
        player.updatePlayerPosition(45F, 0F, (short) 0);
        player.updatePlayerPosition((short) 0);
        assertEquals(0, resyncs[0]);
        player.updatePlayerPosition(4D, 64D, 2D, (short) 0);
        assertEquals(1, resyncs[0]);
    }

    private static int teleport(final ClientPlayerEntity player, final boolean fake) {
        final PacketWrapper packet = new PacketWrapperImpl(ClientboundPackets26_3.PLAYER_POSITION, null, null);
        player.writePlayerPositionPacketToClient(packet, Relative.NONE, fake);
        return packet.get(Types.VAR_INT, 0);
    }

    private static ClientPlayerEntity player() {
        return new ClientPlayerEntity(user(), 1, UUID.randomUUID(), abilities());
    }

    private static PlayerAbilities abilities() {
        return new PlayerAbilities(1, (byte) 1, (byte) 0);
    }

    private static UserConnection user() {
        return (UserConnection) Proxy.newProxyInstance(UserConnection.class.getClassLoader(), new Class<?>[]{UserConnection.class},
                (proxy, method, arguments) -> null);
    }

}
