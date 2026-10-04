package com.viaversion.viafabricplus.bedrock.client;

import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.api.model.entity.ClientPlayerEntity;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;

public final class BedrockInputPermissions {
    public static ClientPlayerEntity player() {
        if (!BedrockProtocolVersion.BEDROCK_LATEST.equals(BedrockClient.get().targetVersion())) return null;
        final var user = BedrockClient.get().userConnection();
        final var tracker = user == null ? null : user.get(EntityTracker.class);
        return tracker == null ? null : tracker.getClientPlayer();
    }
    private BedrockInputPermissions() {}
}
