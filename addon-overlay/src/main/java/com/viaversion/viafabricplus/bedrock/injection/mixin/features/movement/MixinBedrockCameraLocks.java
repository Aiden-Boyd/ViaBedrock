package com.viaversion.viafabricplus.bedrock.injection.mixin.features.movement;

import com.viaversion.viafabricplus.bedrock.client.BedrockInputPermissions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.raphimc.viabedrock.protocol.model.PlayerInputLocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinBedrockCameraLocks {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void enforceServerCameraLock(final double yaw, final double pitch, final CallbackInfo ci) {
        if ((Object) this != Minecraft.getInstance().player) return;
        final var player = BedrockInputPermissions.player();
        if (player != null && player.inputLocks().locked(PlayerInputLocks.CAMERA)) ci.cancel();
    }
}
