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

package com.viaversion.viafabricplus.bedrock.injection.mixin.features.misc;

import com.viaversion.viafabricplus.bedrock.client.BedrockClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.api.model.BlockConnections;
import net.raphimc.viabedrock.api.model.entity.ClientPlayerEntity;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.AbilitiesIndex;
import net.raphimc.viabedrock.protocol.rewriter.BlockStateRewriter;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.storage.GameSessionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinBedrockPermissions {

    @Unique
    private static ClientPlayerEntity viaBedrock$player() {
        if (!BedrockProtocolVersion.BEDROCK_LATEST.equals(BedrockClient.get().targetVersion())) return null;
        final var connection = BedrockClient.get().userConnection();
        if (connection == null || connection.get(EntityTracker.class) == null) return null;
        return connection.get(EntityTracker.class).getClientPlayer();
    }

    @Inject(method = {"startDestroyBlock", "continueDestroyBlock"}, at = @At("HEAD"), cancellable = true)
    private void checkMining(final CallbackInfoReturnable<Boolean> cir) {
        final var player = viaBedrock$player();
        if (player != null && (!player.abilities().mayInteract(AbilitiesIndex.Mine)
                || BedrockClient.get().userConnection().get(GameSessionStorage.class).isImmutableWorld())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {"useItem", "interact", "interactAt"}, at = @At("HEAD"), cancellable = true)
    private void checkVisitorInteraction(final CallbackInfoReturnable<InteractionResult> cir) {
        final var player = viaBedrock$player();
        if (player != null && player.abilities().playerPermission()
                == net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PlayerPermissionLevel.Visitor.getValue()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void checkBlockUse(final LocalPlayer player, final InteractionHand hand, final BlockHitResult hit,
                               final CallbackInfoReturnable<InteractionResult> cir) {
        final var bedrockPlayer = viaBedrock$player();
        if (bedrockPlayer == null) return;
        final var connection = BedrockClient.get().userConnection();
        final var chunks = connection.get(ChunkTracker.class);
        final var position = new com.viaversion.viaversion.api.minecraft.BlockPosition(hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
        final var state = BedrockProtocol.MAPPINGS.getJavaBlockStates().inverse().get(chunks.getJavaBlockState(position));
        final String tag = connection.get(BlockStateRewriter.class).tag(chunks.getBlockState(position));
        final boolean usesBlock = !bedrockPlayer.isSneaking() && BlockConnections.usesBlockInteraction(state, tag);
        if (!bedrockPlayer.abilities().mayUseBlock(usesBlock && BlockConnections.isContainerInteraction(tag),
                usesBlock && !BlockConnections.isContainerInteraction(tag), connection.get(GameSessionStorage.class).isImmutableWorld())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
