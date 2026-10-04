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

import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Crops the face and hat together, so translucent hats do not reveal the old icon. */
public final class RealmOwnerImage {
    private RealmOwnerImage() {}

    public static BufferedImage head(final BufferedImage skin) {
        if (skin == null || skin.getWidth() < 64 || skin.getWidth() % 64 != 0 ||
                skin.getHeight() != skin.getWidth() && skin.getHeight() * 2 != skin.getWidth()) {
            throw new IllegalArgumentException("Unsupported owner skin dimensions");
        }
        final int scale = skin.getWidth() / 64;
        final BufferedImage head = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        final var graphics = head.createGraphics();
        try {
            graphics.setColor(java.awt.Color.BLACK);
            graphics.fillRect(0, 0, 32, 32);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(skin, 0, 0, 32, 32, 8 * scale, 8 * scale, 16 * scale, 16 * scale, null);
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.drawImage(skin, 0, 0, 32, 32, 40 * scale, 8 * scale, 48 * scale, 16 * scale, null);
        } finally {
            graphics.dispose();
        }
        return head;
    }

    public static BufferedImage avatar(final BufferedImage source) {
        if (source == null) {
            throw new IllegalArgumentException("Missing owner avatar");
        }
        final BufferedImage avatar = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        final var graphics = avatar.createGraphics();
        try {
            graphics.setColor(java.awt.Color.BLACK);
            graphics.fillRect(0, 0, 32, 32);
            final int size = Math.min(source.getWidth(), source.getHeight());
            final int x = (source.getWidth() - size) / 2;
            final int y = (source.getHeight() - size) / 2;
            graphics.drawImage(source, 0, 0, 32, 32, x, y, x + size, y + size, null);
        } finally {
            graphics.dispose();
        }
        return avatar;
    }
}
