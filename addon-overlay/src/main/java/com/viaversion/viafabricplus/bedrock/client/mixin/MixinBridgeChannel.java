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

package com.viaversion.viafabricplus.bedrock.client.mixin;

import com.viaversion.viafabricplus.bedrock.client.BedrockClient;
import com.viaversion.viafabricplus.bedrock.client.NoReadFlowControlHandler;
import com.viaversion.viafabricplus.bedrock.client.access.IConnection;
import com.viaversion.viaversion.platform.ViaChannelInitializer;
import com.viaversion.viaversion.platform.ViaDecodeHandler;
import com.viaversion.viaversion.platform.ViaEncodeHandler;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.HandlerNames;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.Connection$1")
public abstract class MixinBridgeChannel {
    @Shadow @Final Connection val$connection;
    @Inject(method = "initChannel", at = @At("RETURN"))
    private void initializePipeline(final Channel channel, final CallbackInfo ci) {
        final IConnection bridge = (IConnection) this.val$connection;
        final var target = bridge.viaFabricPlus$getTargetVersion();
        channel.attr(BedrockClient.CONNECTION).set(this.val$connection);
        channel.attr(BedrockClient.TARGET_VERSION).set(target);
        channel.closeFuture().addListener(future -> BedrockClient.get().disconnected(this.val$connection));
        if (!BedrockProtocolVersion.BEDROCK_LATEST.equals(target)) return;
        BedrockClient.get().awaitReady();
        final var user = ViaChannelInitializer.createUserConnection(channel, true);
        bridge.viaFabricPlus$setUserConnection(user);
        final var pipeline = channel.pipeline();
        pipeline.addBefore(HandlerNames.INBOUND_CONFIG, ViaDecodeHandler.NAME, new ViaDecodeHandler(user));
        pipeline.addBefore(HandlerNames.ENCODER, ViaEncodeHandler.NAME, new ViaEncodeHandler(user));
        pipeline.addAfter(ViaDecodeHandler.NAME, NoReadFlowControlHandler.NAME, new NoReadFlowControlHandler());
    }
}
