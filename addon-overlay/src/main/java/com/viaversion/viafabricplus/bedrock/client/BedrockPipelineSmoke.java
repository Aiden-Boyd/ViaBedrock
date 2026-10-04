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

package com.viaversion.viafabricplus.bedrock.client;

import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.platform.ViaChannelInitializer;
import com.viaversion.viaversion.platform.ViaEncodeHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;

/** Exercises the actual protocol pipeline with an offline Java handshake and login, without opening a socket. */
public final class BedrockPipelineSmoke {
    public static void run() {
        final var channel = new EmbeddedChannel();
        channel.attr(BedrockClient.TARGET_VERSION).set(BedrockProtocolVersion.BEDROCK_LATEST);
        final var user = ViaChannelInitializer.createUserConnection(channel, true);
        channel.pipeline().addLast(ViaEncodeHandler.NAME, new ViaEncodeHandler(user));
        try {
            final var handshake = Unpooled.buffer();
            Types.VAR_INT.write(handshake, 0);
            Types.VAR_INT.write(handshake, BedrockClient.NATIVE_VERSION.getOriginalVersion());
            Types.STRING.write(handshake, "localhost");
            Types.UNSIGNED_SHORT.write(handshake, 19132);
            Types.VAR_INT.write(handshake, 2);
            channel.writeOutbound(handshake);
            if (!user.getProtocolInfo().getPipeline().contains(BedrockProtocol.class))
                throw new AssertionError("Java handshake did not activate the Bedrock protocol");
            final var hello = Unpooled.buffer();
            Types.VAR_INT.write(hello, 0);
            Types.STRING.write(hello, "AdapterQA");
            Types.UUID.write(hello, java.util.UUID.randomUUID());
            channel.writeOutbound(hello);
            final ByteBuf settings = channel.readOutbound();
            if (settings == null) throw new AssertionError("Java login did not send Bedrock network settings");
            try {
                if (Types.VAR_INT.read(settings) != ServerboundBedrockPackets.REQUEST_NETWORK_SETTINGS.getId()
                    || settings.readInt() != ProtocolConstants.BEDROCK_PROTOCOL_VERSION || settings.isReadable())
                    throw new AssertionError("Bedrock network-settings request had the wrong wire format");
            } finally { settings.release(); }
        } finally { channel.finishAndReleaseAll(); }
    }
}
