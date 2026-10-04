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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RealmInviteCodeTest {
    @Test
    void acceptsCodesAndOfficialInviteLinks() {
        assertEquals("Abc_-123", RealmInviteCode.normalize("  Abc_-123  "));
        assertEquals("Abc_-123", RealmInviteCode.normalize("https://realms.gg/Abc_-123?source=share"));
        assertEquals("Abc_-123", RealmInviteCode.normalize("realms.gg/Abc_-123"));
    }

    @Test
    void rejectsForeignLinksAndExtraPaths() {
        assertThrows(IllegalArgumentException.class, () -> RealmInviteCode.normalize("https://example.com/code"));
        assertThrows(IllegalArgumentException.class, () -> RealmInviteCode.normalize("https://realms.gg/a/b"));
        assertThrows(IllegalArgumentException.class, () -> RealmInviteCode.normalize(""));
    }
}
