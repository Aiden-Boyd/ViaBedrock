package com.viaversion.viafabricplus.bedrock.injection.mixin.features.movement;

import com.viaversion.viafabricplus.bedrock.client.BedrockInputPermissions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.raphimc.viabedrock.protocol.data.enums.java.InputFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class MixinBedrockInputKeys {
    @Inject(method = "isDown", at = @At("RETURN"), cancellable = true)
    private void enforceServerInputLocks(final CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        final var player = BedrockInputPermissions.player();
        if (player == null) return;
        final var options = Minecraft.getInstance().options;
        final Object key = this;
        final InputFlag input;
        if (key == options.keyUp) input = InputFlag.FORWARD;
        else if (key == options.keyDown) input = InputFlag.BACKWARD;
        else if (key == options.keyLeft) input = InputFlag.LEFT;
        else if (key == options.keyRight) input = InputFlag.RIGHT;
        else if (key == options.keyJump) input = InputFlag.JUMP;
        else if (key == options.keyShift) input = InputFlag.SHIFT;
        else if (key == options.keySprint) input = InputFlag.SPRINT;
        else return;
        if (!player.inputLocks().allows(input, player.mountEntityRuntimeId() != -1)) cir.setReturnValue(false);
    }
}
