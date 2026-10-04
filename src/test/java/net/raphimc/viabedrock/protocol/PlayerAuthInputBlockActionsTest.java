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
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.api.model.entity.ClientPlayerEntity;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PlayerActionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PlayerAuthInputBlockActionsTest {

    @Test
    void mixedStopAndStartMatchesProtocol2193WireFormat() throws Exception {
        final PacketWrapper wrapper = new PacketWrapperImpl(ServerboundBedrockPackets.PLAYER_AUTH_INPUT, null, null);
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
            // v2193: unsigned count=2; stop=2 with default position/face; start=0 at (1,2,3), face=5.
            assertArrayEquals(new byte[]{2, 4, 0, 0, 0, 0, 0, 2, 4, 6, 10}, payload);
        } finally {
            buffer.release();
        }
    }

    @Test
    void allMiningActionsLeaveTheFollowingAuthInputFieldsAligned() throws Exception {
        final var actions = List.of(
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.StartDestroyBlock, new BlockPosition(-1, 64, 127), 5),
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.CrackBlock, new BlockPosition(-1, 64, 127), 5),
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.ContinueDestroyBlock, new BlockPosition(-1, 64, 127), 5),
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.PredictDestroyBlock, new BlockPosition(-1, 64, 127), 5),
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.AbortDestroyBlock, new BlockPosition(-1, 64, 127), 5),
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.StopDestroyBlock)
        );
        final PacketWrapper wrapper = new PacketWrapperImpl(ServerboundBedrockPackets.PLAYER_AUTH_INPUT, null, null);
        PlayerActionPacketFactory.writeAuthInputBlockActions(wrapper, actions);
        wrapper.write(Types.BOOLEAN, false); // vehicle rotation absent
        wrapper.write(Types.BOOLEAN, false); // vehicle ID absent
        wrapper.write(net.raphimc.viabedrock.protocol.types.BedrockTypes.FLOAT_LE, 0.25F); // next movement field
        final var buffer = Unpooled.buffer();
        try {
            wrapper.writeToBuffer(buffer);
            Types.VAR_INT.read(buffer);
            // Read fields individually like the v2193 decoder, independently of our writer.
            assertEquals(6, readUnsignedVarInt(buffer));
            for (var action : actions) {
                assertEquals(action.action().getValue(), readSignedVarInt(buffer));
                assertEquals(action.position().x(), readSignedVarInt(buffer));
                assertEquals(action.position().y(), readSignedVarInt(buffer));
                assertEquals(action.position().z(), readSignedVarInt(buffer));
                assertEquals(action.direction(), readSignedVarInt(buffer));
            }
            assertEquals(false, buffer.readBoolean());
            assertEquals(false, buffer.readBoolean());
            assertEquals(0.25F, buffer.readFloatLE());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    private static int readUnsignedVarInt(final ByteBuf buffer) {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            final int next = buffer.readUnsignedByte();
            value |= (next & 0x7F) << shift;
            if ((next & 0x80) == 0) {
                return value;
            }
        }
        throw new AssertionError("Invalid varint");
    }

    private static int readSignedVarInt(final ByteBuf buffer) {
        final int value = readUnsignedVarInt(buffer);
        return (value >>> 1) ^ -(value & 1);
    }

    @Test
    void creativeDestroyCannotBeEncodedAsAnAuthInputBlockAction() {
        final PacketWrapper wrapper = new PacketWrapperImpl(ServerboundBedrockPackets.PLAYER_AUTH_INPUT, null, null);
        assertThrows(IllegalArgumentException.class, () -> PlayerActionPacketFactory.writeAuthInputBlockActions(wrapper, List.of(
            new ClientPlayerEntity.AuthInputBlockAction(PlayerActionType.CreativeDestroyBlock, new BlockPosition(0, 0, 0), 0)
        )));
    }

}


