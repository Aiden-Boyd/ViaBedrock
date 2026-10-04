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
package net.raphimc.viabedrock.protocol;

import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.api.model.entity.ClientPlayerEntity;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PlayerActionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PlayerAuthInputBlockActionsTest {

    @Test
    void mixedStopAndStartMatchesBedrockWireFormat() throws Exception {
        final PacketWrapper wrapper = PacketWrapper.create(ServerboundBedrockPackets.PLAYER_AUTH_INPUT, null);
        PlayerActionPacketFactory.writeAuthInputBlockActions(wrapper, List.of(
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.StopDestroyBlock),
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.StartDestroyBlock, new BlockPosition(1, 2, 3), 5)
        ));
        final var buffer = Unpooled.buffer();
        try {
            wrapper.writeToBuffer(buffer);
            Types.VAR_INT.read(buffer); // packet id
            final byte[] payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
            // Signed count=2, stop=2 without payload, start=0, x=1, signed y=2, z=3, face=5.
            assertArrayEquals(new byte[]{4, 4, 0, 2, 4, 6, 10}, payload);
        } finally {
            buffer.release();
        }
    }

    @Test
    void creativeDestroyCannotBeEncodedAsAnAuthInputBlockAction() {
        final PacketWrapper wrapper = PacketWrapper.create(ServerboundBedrockPackets.PLAYER_AUTH_INPUT, null);
        assertThrows(IllegalArgumentException.class, () -> PlayerActionPacketFactory.writeAuthInputBlockActions(wrapper, List.of(
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.CreativeDestroyBlock, new BlockPosition(0, 0, 0), 0)
        )));
    }
}
