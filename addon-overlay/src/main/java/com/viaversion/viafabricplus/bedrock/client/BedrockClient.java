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

import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import com.viaversion.viafabricplus.bedrock.client.access.IConnection;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.commands.ViaCommandHandler;
import com.viaversion.viaversion.platform.NoopInjector;
import com.viaversion.viaversion.protocol.version.BaseVersionProvider;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import io.netty.util.AttributeKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Client transport and protocol engine only. Java servers keep their native pipeline. */
public final class BedrockClient {
    private static final BedrockClient INSTANCE = new BedrockClient();
    public static final ProtocolVersion NATIVE_VERSION = ProtocolVersion.v26_3;
    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("viabedrock-connection");
    public static final AttributeKey<ProtocolVersion> TARGET_VERSION = AttributeKey.valueOf("viabedrock-target-version");
    private final Path path = FabricLoader.getInstance().getConfigDir().resolve("viabedrock");
    private final Logger logger = LogManager.getLogger("Bedrock Multiplayer");
    private volatile CompletableFuture<Void> loading;
    private volatile Connection pendingConnection;
    private volatile ProtocolVersion connectingTarget = NATIVE_VERSION;

    public static BedrockClient get() { return INSTANCE; }
    public Path path() { return this.path; }
    public Logger logger() { return this.logger; }
    public ProtocolVersion targetVersion() {
        final var minecraft = Minecraft.getInstance();
        final var handler = minecraft == null ? null : minecraft.getConnection();
        return handler == null ? this.connectingTarget : ((IConnection) handler.getConnection()).viaFabricPlus$getTargetVersion();
    }
    public UserConnection userConnection() {
        final var minecraft = Minecraft.getInstance();
        final var handler = minecraft == null ? null : minecraft.getConnection();
        return handler == null ? null : ((IConnection) handler.getConnection()).viaFabricPlus$getUserConnection();
    }
    public void connecting(final ProtocolVersion version) { this.connectingTarget = version == null ? NATIVE_VERSION : version; }
    public void pending(final Connection connection) { this.pendingConnection = connection; }
    public void disconnected(final Connection connection) {
        if (this.pendingConnection == connection) { this.pendingConnection = null; this.connectingTarget = NATIVE_VERSION; }
    }

    public synchronized void initialize() {
        if (this.loading != null) return;
        try {
            Files.createDirectories(this.path);
            final Path previous = FabricLoader.getInstance().getConfigDir().resolve("viafabricplus/bedrock.json");
            if (Files.exists(previous) && !Files.exists(this.path.resolve("bedrock.json"))) Files.copy(previous, this.path.resolve("bedrock.json"));
        } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot initialize Bedrock configuration", exception); }
        final var addon = new ViaFabricPlusBedrock();
        addon.onPreSettingsLoading();
        this.loading = CompletableFuture.runAsync(() -> {
            ViaManagerImpl.initAndLoad(new BedrockViaVersionPlatform(this.path.toFile()), new NoopInjector(), new ViaCommandHandler(), new ViaPlatformLoader() {
                @Override public void load() {
                    Via.getManager().getProviders().use(VersionProvider.class, new BaseVersionProvider() {
                        @Override public ProtocolVersion getClosestServerProtocol(final UserConnection user) throws Exception {
                            if (user.isClientSide()) {
                                final ProtocolVersion target = user.getChannel().attr(TARGET_VERSION).get();
                                return target == null ? NATIVE_VERSION : target;
                            }
                            return super.getClosestServerProtocol(user);
                        }
                    });
                    addon.onPostProtocolTranslationLoading();
                }
                @Override public void unload() {}
            }, () -> {});
            if (Via.getManager().getProtocolManager().getProtocolPath(NATIVE_VERSION, BedrockProtocolVersion.BEDROCK_LATEST) == null)
                throw new IllegalStateException("No native Java to Bedrock protocol path");
            this.logger.info("Bedrock client components initialized");
        });
    }
    public void awaitReady() { this.initialize(); this.loading.join(); }
}
