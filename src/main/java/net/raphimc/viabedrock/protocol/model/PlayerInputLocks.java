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

import java.util.EnumSet;
import java.util.Set;

/** Server-controlled input permissions, independent of world interaction abilities. */
public final class PlayerInputLocks {

    public static final int CAMERA = 1 << 1;
    public static final int MOVEMENT = 1 << 2;
    public static final int LATERAL_MOVEMENT = 1 << 4;
    public static final int SNEAK = 1 << 5;
    public static final int JUMP = 1 << 6;
    public static final int MOUNT = 1 << 7;
    public static final int DISMOUNT = 1 << 8;
    public static final int FORWARD = 1 << 9;
    public static final int BACKWARD = 1 << 10;
    public static final int LEFT = 1 << 11;
    public static final int RIGHT = 1 << 12;

    private volatile int mask;

    public void setMask(final int mask) {
        this.mask = mask;
    }

    public boolean locked(final int flags) {
        return (this.mask & flags) != 0;
    }

    public boolean allows(final InputFlag input, final boolean mounted) {
        return !this.locked(switch (input) {
            case FORWARD -> MOVEMENT | LATERAL_MOVEMENT | FORWARD;
            case BACKWARD -> MOVEMENT | LATERAL_MOVEMENT | BACKWARD;
            case LEFT -> MOVEMENT | LATERAL_MOVEMENT | LEFT;
            case RIGHT -> MOVEMENT | LATERAL_MOVEMENT | RIGHT;
            case JUMP -> MOVEMENT | JUMP;
            case SHIFT -> SNEAK | (mounted ? DISMOUNT : 0);
            case SPRINT -> MOVEMENT | LATERAL_MOVEMENT;
        });
    }

    public Set<InputFlag> filter(final Set<InputFlag> inputs, final boolean mounted) {
        final Set<InputFlag> filtered = EnumSet.noneOf(InputFlag.class);
        for (InputFlag input : inputs) {
            if (this.allows(input, mounted)) {
                filtered.add(input);
            }
        }
        return filtered;
    }

    public static boolean mountTarget(final String type) {
        return type != null && (type.endsWith("_boat") || type.endsWith("_raft")
                || type.equals("minecraft:boat") || type.equals("minecraft:chest_boat")
                || type.equals("minecraft:minecart") || switch (type) {
            case "minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:skeleton_horse",
                    "minecraft:zombie_horse", "minecraft:camel", "minecraft:camel_husk",
                    "minecraft:pig", "minecraft:strider", "minecraft:happy_ghast" -> true;
            default -> false;
        });
    }

}
