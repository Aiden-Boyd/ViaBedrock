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

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RealmOwnerImageTest {
    @Test
    void usesOwnersFaceAndCompositesTheHat() {
        final var skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 8; y < 16; y++) {
            for (int x = 8; x < 16; x++) skin.setRGB(x, y, 0xFF123456);
        }
        skin.setRGB(40, 8, 0xFFFF0000);
        skin.setRGB(41, 8, 0x8000FF00);
        final var head = RealmOwnerImage.head(skin);
        assertEquals(32, head.getWidth());
        assertEquals(0xFFFF0000, head.getRGB(0, 0));
        assertEquals(0xFF123456, head.getRGB(31, 31));
        assertEquals(255, head.getRGB(4, 0) >>> 24);
        assertNotEquals(0xFF123456, head.getRGB(4, 0));
    }

    @Test
    void supportsHigherResolutionAndRejectsNonSkins() {
        final var skin = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        skin.setRGB(16, 16, 0xFFABCDEF);
        assertEquals(0xFFABCDEF, RealmOwnerImage.head(skin).getRGB(0, 0));
        assertThrows(IllegalArgumentException.class, () -> RealmOwnerImage.head(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)));
    }
}
