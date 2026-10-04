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

import java.net.URI;

public final class RealmInviteCode {
    private RealmInviteCode() {}

    public static String normalize(final String input) {
        String code = input.trim();
        if (code.startsWith("https://") || code.startsWith("http://")) {
            final URI uri = URI.create(code);
            if (!"realms.gg".equalsIgnoreCase(uri.getHost()) || uri.getUserInfo() != null || uri.getPort() != -1) {
                throw new IllegalArgumentException("Use a Realm code or a realms.gg invite link");
            }
            code = uri.getPath().replaceFirst("^/", "");
        } else if (code.startsWith("realms.gg/")) {
            code = code.substring(10);
        }
        if (!code.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid Realm invite code");
        }
        return code;
    }
}
