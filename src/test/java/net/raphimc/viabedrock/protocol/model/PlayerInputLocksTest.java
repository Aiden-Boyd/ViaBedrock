/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.viabedrock.protocol.model;

import net.raphimc.viabedrock.protocol.data.enums.java.InputFlag;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInputLocksTest {

    @Test
    void wireBitsFilterDirectionalAndGroupedInputsWithoutMutatingCaller() {
        final PlayerInputLocks locks = new PlayerInputLocks();
        final var inputs = EnumSet.allOf(InputFlag.class);
        locks.setMask(1 << 9);
        final var filtered = locks.filter(inputs, false);
        assertFalse(filtered.contains(InputFlag.FORWARD));
        assertTrue(filtered.contains(InputFlag.BACKWARD));
        assertTrue(inputs.contains(InputFlag.FORWARD));
        locks.setMask(1 << 4);
        assertEquals(EnumSet.of(InputFlag.JUMP, InputFlag.SHIFT), locks.filter(inputs, false));
        locks.setMask(1 << 2);
        assertEquals(EnumSet.of(InputFlag.SHIFT), locks.filter(inputs, false));
        locks.setMask(0);
        assertEquals(inputs, locks.filter(inputs, false));
    }

    @Test
    void mountDismountCameraAndSneakAreSeparatePermissions() {
        final PlayerInputLocks locks = new PlayerInputLocks();
        locks.setMask((1 << 8) | (1 << 1));
        assertTrue(locks.allows(InputFlag.SHIFT, false));
        assertFalse(locks.allows(InputFlag.SHIFT, true));
        assertTrue(locks.locked(PlayerInputLocks.CAMERA));
        assertFalse(locks.locked(PlayerInputLocks.MOUNT));
        locks.setMask(1 << 5);
        assertFalse(locks.allows(InputFlag.SHIFT, false));
        assertTrue(locks.allows(InputFlag.JUMP, false));
        assertTrue(PlayerInputLocks.mountTarget("minecraft:chest_boat"));
        assertTrue(PlayerInputLocks.mountTarget("minecraft:camel"));
        assertFalse(PlayerInputLocks.mountTarget("minecraft:villager"));
    }

}
