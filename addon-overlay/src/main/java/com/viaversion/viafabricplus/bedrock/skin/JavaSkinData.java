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

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import net.raphimc.viabedrock.protocol.types.primitive.ImageType;

/** Java texture pixels and model, converted to Bedrock's classic skin format. */
public record JavaSkinData(String pixels, boolean slim, String skinId) {

    public static JavaSkinData from(final BufferedImage source, final boolean slim) {
        final byte[] rgba = ImageType.getImageData(normalize(source));
        return new JavaSkinData(Base64.getEncoder().encodeToString(rgba), slim,
            "java-" + UUID.nameUUIDFromBytes(rgba) + (slim ? "-slim" : "-wide"));
    }

    public void apply(final Map<String, Object> claims) {
        claims.put("SkinId", this.skinId);
        claims.put("SkinData", this.pixels);
        claims.put("SkinImageWidth", 64);
        claims.put("SkinImageHeight", 64);
        claims.put("ArmSize", this.slim ? "slim" : "wide");
        final String geometry = this.slim ? "geometry.humanoid.customSlim" : "geometry.humanoid.custom";
        claims.put("SkinResourcePatch", Base64.getEncoder().encodeToString(
            ("{\"geometry\":{\"default\":\"" + geometry + "\"}}").getBytes(StandardCharsets.UTF_8)));
        claims.put("PersonaSkin", false);
        claims.put("PremiumSkin", false);
    }

    public static BufferedImage normalize(final BufferedImage source) {
        if (source == null || source.getWidth() != 64 || (source.getHeight() != 64 && source.getHeight() != 32)) {
            throw new IllegalArgumentException("Java skins must be 64x64 or legacy 64x32");
        }
        final BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 64, source.getHeight(), source.getRGB(0, 0, 64, source.getHeight(), null, 0, 64), 0, 64);
        if (source.getHeight() == 32) {
            // Legacy skins share right limb textures; the modern left limbs are mirrored.
            final int[][] faces = {
                {4,16,20,48,4,4}, {8,16,24,48,4,4}, {0,20,24,52,4,12},
                {4,20,20,52,4,12}, {8,20,16,52,4,12}, {12,20,28,52,4,12},
                {44,16,36,48,4,4}, {48,16,40,48,4,4}, {40,20,40,52,4,12},
                {44,20,36,52,4,12}, {48,20,32,52,4,12}, {52,20,44,52,4,12}
            };
            for (int[] face : faces) {
                for (int y = 0; y < face[5]; y++) {
                    for (int x = 0; x < face[4]; x++) {
                        image.setRGB(face[2] + x, face[3] + y, source.getRGB(face[0] + face[4] - 1 - x, face[1] + y));
                    }
                }
            }
            boolean opaqueHat = true;
            for (int y = 0; y < 32; y++) {
                for (int x = 32; x < 64; x++) {
                    opaqueHat &= (image.getRGB(x, y) >>> 24) >= 128;
                }
            }
            if (opaqueHat) {
                alpha(image, 32, 0, 64, 32, false);
            }
        }
        // Match Java's opaque base-layer rules while keeping modern overlays transparent.
        alpha(image, 0, 0, 32, 16, true);
        alpha(image, 0, 16, 64, 32, true);
        alpha(image, 16, 48, 48, 64, true);
        return image;
    }

    private static void alpha(final BufferedImage image, final int x0, final int y0, final int x1, final int y1, final boolean opaque) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                final int rgb = image.getRGB(x, y) & 0xFFFFFF;
                image.setRGB(x, y, opaque ? rgb | 0xFF000000 : rgb);
            }
        }
    }
}
