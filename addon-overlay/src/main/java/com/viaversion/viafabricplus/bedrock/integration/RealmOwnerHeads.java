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


package com.viaversion.viafabricplus.bedrock.integration;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.viaversion.viafabricplus.bedrock.skin.JavaSkinService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;

/** Account-scoped, bounded texture cache; network and image work never run in rendering. */
public final class RealmOwnerHeads {
    private static final Identifier UNKNOWN = Identifier.withDefaultNamespace("textures/misc/unknown_server.png");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private static final ExecutorService WORKER = Executors.newFixedThreadPool(2, runnable -> {
        final Thread thread = new Thread(runnable, "Bedrock Realm owner heads");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, 0.75F, true);
    private static BedrockAuthManager account;
    private static long generation;
    private static long textureId;

    private RealmOwnerHeads() {}
    private static final class Entry {
        Identifier texture = UNKNOWN;
        long retryAfter = Long.MAX_VALUE;
    }

    public static Identifier get(final BedrockRealmData realm) {
        final Minecraft client = Minecraft.getInstance();
        if (account != realm.account) {
            CACHE.values().forEach(entry -> {
                if (entry.texture != UNKNOWN) client.getTextureManager().release(entry.texture);
            });
            CACHE.clear();
            account = realm.account;
            generation++;
        }
        if (realm.bedrock == null || realm.account == null) return UNKNOWN;
        final String xuid = realm.bedrock.getOwnerUid();
        if (xuid == null || !xuid.matches("[0-9]{1,20}")) return UNKNOWN;
        final Entry cached = CACHE.get(xuid);
        if (cached != null && System.nanoTime() < cached.retryAfter) return cached.texture;
        if (cached != null && cached.texture != UNKNOWN) client.getTextureManager().release(cached.texture);
        if (CACHE.size() >= 128) {
            final var oldest = CACHE.entrySet().iterator();
            final var removed = oldest.next().getValue();
            oldest.remove();
            if (removed.texture != UNKNOWN) client.getTextureManager().release(removed.texture);
        }
        final Entry entry = new Entry();
        CACHE.put(xuid, entry);
        final long requestGeneration = generation;
        CompletableFuture.supplyAsync(() -> load(realm.account, xuid), WORKER).whenCompleteAsync((png, error) -> {
            if (requestGeneration != generation || CACHE.get(xuid) != entry) return;
            if (error == null && png != null) {
                try {
                    final NativeImage image = NativeImage.read(png);
                    final Identifier id = Identifier.fromNamespaceAndPath("viafabricplus-bedrock", "realm-owner/" + textureId++);
                    client.getTextureManager().register(id, new DynamicTexture(() -> "Bedrock Realm owner", image));
                    entry.texture = id;
                    entry.retryAfter = System.nanoTime() + TimeUnit.MINUTES.toNanos(10);
                } catch (Exception ignored) {
                    entry.retryAfter = System.nanoTime() + TimeUnit.MINUTES.toNanos(1);
                }
            } else {
                entry.retryAfter = System.nanoTime() + TimeUnit.MINUTES.toNanos(1);
            }
        }, client);
        return UNKNOWN;
    }

    private static byte[] load(final BedrockAuthManager account, final String xuid) {
        BufferedImage image = null;
        try {
            if (xuid.equals(account.getXboxUserProfile().getUpToDate().getId())) {
                final var skin = JavaSkinService.prepare().get(6, TimeUnit.SECONDS);
                if (skin != null) {
                    final byte[] rgba = Base64.getDecoder().decode(skin.pixels());
                    final BufferedImage texture = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                    for (int index = 0; index < 4096; index++) {
                        final int offset = index * 4;
                        texture.setRGB(index % 64, index / 64, (rgba[offset + 3] & 255) << 24 |
                            (rgba[offset] & 255) << 16 | (rgba[offset + 1] & 255) << 8 | rgba[offset + 2] & 255);
                    }
                    image = RealmOwnerImage.head(texture);
                }
            }
        } catch (Exception ignored) {}
        if (image == null) {
            try {
                final var json = JsonParser.parseString(new String(fetch(URI.create("https://api.geysermc.org/v2/skin/" + xuid), null),
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if (json.has("texture_id")) {
                    final String hash = json.get("texture_id").getAsString();
                    if (hash.matches("[a-fA-F0-9]{32,128}")) {
                        image = RealmOwnerImage.head(ImageIO.read(new ByteArrayInputStream(
                            fetch(URI.create("https://textures.minecraft.net/texture/" + hash), null))));
                    }
                }
            } catch (Exception ignored) {}
        }
        if (image == null) {
            try {
                final String auth = account.getXboxLiveXstsToken().getUpToDate().getAuthorizationHeader();
                final var json = JsonParser.parseString(new String(fetch(URI.create(
                    "https://profile.xboxlive.com/users/xuid(" + xuid + ")/profile/settings?settings=GameDisplayPicRaw"), auth),
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for (var setting : json.getAsJsonArray("profileUsers").get(0).getAsJsonObject().getAsJsonArray("settings")) {
                    final var data = setting.getAsJsonObject();
                    if (!"GameDisplayPicRaw".equals(data.get("id").getAsString())) continue;
                    final URI uri = URI.create(data.get("value").getAsString());
                    final String host = uri.getHost();
                    if ("https".equalsIgnoreCase(uri.getScheme()) && host != null &&
                        (host.equalsIgnoreCase("xboxlive.com") || host.toLowerCase(Locale.ROOT).endsWith(".xboxlive.com"))) {
                        image = RealmOwnerImage.avatar(ImageIO.read(new ByteArrayInputStream(fetch(uri, null))));
                    }
                }
            } catch (Exception ignored) {}
        }
        if (image == null) return null;
        try {
            final var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] fetch(final URI uri, final String authorization) throws Exception {
        final var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET();
        if (authorization != null) builder.header("Authorization", authorization).header("x-xbl-contract-version", "3");
        final var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200 || response.body().length > 1024 * 1024) {
            throw new java.io.IOException("Owner picture unavailable");
        }
        return response.body();
    }
}
