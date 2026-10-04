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
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavaSkinDataTest {
    @Test
    void sendsRgbaAndSlimGeometryWithoutChangingSessionClaims() {
        final var image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x12345678);
        image.setRGB(40, 8, 0x80332211);
        final var claims = new HashMap<String, Object>();
        claims.put("Nonce", "host-session");
        claims.put("ServerAddress", "example:19132");
        claims.put("TrustedSkin", false);
        final var skin = JavaSkinData.from(image, true);
        skin.apply(claims);
        final byte[] pixels = Base64.getDecoder().decode((String) claims.get("SkinData"));
        assertArrayEquals(new byte[]{0x34, 0x56, 0x78, (byte) 0xFF}, java.util.Arrays.copyOf(pixels, 4));
        final int overlay = (8 * 64 + 40) * 4;
        assertArrayEquals(new byte[]{0x33, 0x22, 0x11, (byte) 0x80}, java.util.Arrays.copyOfRange(pixels, overlay, overlay + 4));
        assertEquals(64 * 64 * 4, pixels.length);
        assertEquals("slim", claims.get("ArmSize"));
        assertTrue(new String(Base64.getDecoder().decode((String) claims.get("SkinResourcePatch")),
            StandardCharsets.UTF_8).contains("geometry.humanoid.customSlim"));
        assertEquals("host-session", claims.get("Nonce"));
        assertEquals("example:19132", claims.get("ServerAddress"));
        assertEquals(false, claims.get("TrustedSkin"));
    }

    @Test
    void legacySkinsMirrorLimbFacesAndClearOpaqueLegacyHat() {
        final var old = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                old.setRGB(x, y, 0xFF000000 | x << 16 | y);
            }
        }
        final var converted = JavaSkinData.normalize(old);
        assertEquals(64, converted.getHeight());
        assertEquals(old.getRGB(7, 16), converted.getRGB(20, 48));
        assertEquals(old.getRGB(4, 16), converted.getRGB(23, 48));
        assertEquals(old.getRGB(47, 16), converted.getRGB(36, 48));
        assertEquals(0, converted.getRGB(40, 8) >>> 24);
        assertEquals(0, converted.getRGB(0, 40) >>> 24);
        assertEquals(32, old.getHeight());
    }

    @Test
    void modernOverlaysAndWideModelArePreserved() {
        final var image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(40, 8, 0xFF998877);
        image.setRGB(0, 40, 0x44332211);
        final var normalized = JavaSkinData.normalize(image);
        assertEquals(0xFF998877, normalized.getRGB(40, 8));
        assertEquals(0x44332211, normalized.getRGB(0, 40));
        final var claims = new HashMap<String, Object>();
        JavaSkinData.from(image, false).apply(claims);
        assertEquals("wide", claims.get("ArmSize"));
        assertEquals("{\"geometry\":{\"default\":\"geometry.humanoid.custom\"}}",
            new String(Base64.getDecoder().decode((String) claims.get("SkinResourcePatch")), StandardCharsets.UTF_8));
        assertEquals(JavaSkinData.from(image, false).skinId(), JavaSkinData.from(image, false).skinId());
        assertNotEquals(JavaSkinData.from(image, false).skinId(), JavaSkinData.from(image, true).skinId());
    }

    @Test
    void rejectsMalformedSkinDimensions() {
        assertThrows(IllegalArgumentException.class, () -> JavaSkinData.from(new BufferedImage(32, 32, 2), false));
        assertThrows(IllegalArgumentException.class, () -> JavaSkinData.from(new BufferedImage(64, 48, 2), false));
    }
}
