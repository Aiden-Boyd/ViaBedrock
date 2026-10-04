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

package com.viaversion.viafabricplus.bedrock.skin;

import com.mojang.authlib.SignatureState;
import com.viaversion.viafabricplus.bedrock.ViaFabricPlusBedrock;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;

/** Preloads the Java account's texture without fetching it on the render thread. */
public final class JavaSkinService {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private static UUID profileId;
    private static CompletableFuture<JavaSkinData> pending;
    private static volatile JavaSkinData fallback;
    private static volatile net.minecraft.world.entity.player.PlayerSkin visibleSkin;

    private JavaSkinService() {}

    public static synchronized CompletableFuture<JavaSkinData> prepare() {
        final Minecraft client = Minecraft.getInstance();
        final UUID id = client.getUser().getProfileId();
        if (id.equals(profileId) && pending != null) {
            return pending;
        }
        profileId = id;
        fallback = null;
        visibleSkin = DefaultPlayerSkin.get(id);
        pending = CompletableFuture.supplyAsync(() -> load(client, id));
        return pending;
    }

    public static JavaSkinData current() {
        final CompletableFuture<JavaSkinData> future = prepare();
        if (!future.isDone() && !Minecraft.getInstance().isSameThread()) {
            try {
                return future.get(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // An unavailable skin must not fail or indefinitely delay joining.
            }
        }
        return future.getNow(fallback);
    }

    public static net.minecraft.world.entity.player.PlayerSkin visibleSkin() {
        prepare();
        return visibleSkin;
    }

    private static JavaSkinData load(final Minecraft client, final UUID id) {
        JavaSkinData defaultSkin = null;
        try {
            final var skin = DefaultPlayerSkin.get(id);
            try (var input = client.getResourceManager().getResourceOrThrow(skin.body().texturePath()).open()) {
                defaultSkin = JavaSkinData.from(ImageIO.read(input), skin.model() == PlayerModelType.SLIM);
            }
            synchronized (JavaSkinService.class) {
                if (id.equals(profileId)) {
                    fallback = defaultSkin;
                }
            }
            var profile = client.getGameProfile();
            if (!id.equals(profile.id())) {
                return defaultSkin;
            }
            final var session = client.services().sessionService();
            var property = session.getPackedTextures(profile);
            if (property == null) {
                final var fetched = session.fetchProfile(id, true);
                if (fetched == null || !id.equals(fetched.profile().id())) {
                    return defaultSkin;
                }
                profile = fetched.profile();
                property = session.getPackedTextures(profile);
            }
            if (property == null) {
                return defaultSkin;
            }
            final var textures = session.unpackTextures(property);
            if (textures.signatureState() == SignatureState.INVALID || textures.skin() == null) {
                return defaultSkin;
            }
            final var resolvedProfile = profile;
            client.execute(() -> {
                synchronized (JavaSkinService.class) {
                    if (!id.equals(profileId)) return;
                }
                client.getSkinManager().get(resolvedProfile).thenAcceptAsync(loaded -> {
                    synchronized (JavaSkinService.class) {
                        if (id.equals(profileId)) {
                            loaded.ifPresent(skin -> visibleSkin = skin);
                        }
                    }
                }, client);
            });
            final var texture = textures.skin();
            final URI original = URI.create(texture.getUrl());
            if (!"textures.minecraft.net".equalsIgnoreCase(original.getHost()) ||
                (!"https".equalsIgnoreCase(original.getScheme()) && !"http".equalsIgnoreCase(original.getScheme()))) {
                return defaultSkin;
            }
            final URI url = new URI("https", null, "textures.minecraft.net", -1, original.getPath(), null, null);
            final var request = HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(5)).GET().build();
            final var response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var input = response.body()) {
                if (response.statusCode() != 200) {
                    return defaultSkin;
                }
                return JavaSkinData.from(ImageIO.read(input), "slim".equals(texture.getMetadata("model")));
            }
        } catch (Exception e) {
            ViaFabricPlusBedrock.impl().logger().warn("Could not load the Java account skin; using its default skin", e);
            return defaultSkin;
        }
    }
}
