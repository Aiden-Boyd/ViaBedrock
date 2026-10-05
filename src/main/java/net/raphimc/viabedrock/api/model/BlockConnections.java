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
package net.raphimc.viabedrock.api.model;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class BlockConnections {

    private static final String[] DIRECTIONS = {"north", "east", "south", "west"};
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DZ = {-1, 0, 1, 0};

    private static final Set<String> CONTAINER_TAGS = Set.of("chest", "trapped_chest", "barrel", "shulker_box", "ender_chest",
            "workbench", "furnace", "blast_furnace", "smoker", "brewing_stand", "enchanting_table", "stonecutter",
            "anvil", "smithing_table", "grindstone", "loom", "cartography_table", "beacon", "dispenser", "dropper", "hopper", "crafter");

    public static boolean needsNeighbours(final BlockState state) {
        return state != null && (state.identifier().equals("redstone_wire") || vineFamily(state) != null);
    }

    public static BlockState connect(final BlockState state, final BlockPosition position,
                                     final Function<BlockPosition, BlockState> neighbour, final ToIntFunction<BlockState> properties) {
        final String vine = vineFamily(state);
        if (vine != null) {
            final int dy = vine.equals("weeping_vines") ? -1 : 1;
            final BlockState next = neighbour.apply(offset(position, 0, dy, 0));
            return vine.equals(vineFamily(next)) ? state.withIdentifier(vine + "_plant").withoutProperties("age") : state;
        }
        if (state == null || !state.identifier().equals("redstone_wire")) {
            return state;
        }
        final String[] sides = new String[4];
        final boolean aboveBlocked = (properties.applyAsInt(neighbour.apply(offset(position, 0, 1, 0))) & 1) != 0;
        for (int i = 0; i < 4; i++) {
            final BlockPosition sidePosition = offset(position, DX[i], 0, DZ[i]);
            final BlockState side = neighbour.apply(sidePosition);
            final int flags = properties.applyAsInt(side);
            final boolean support = (flags & 4) != 0 || named(side, "hopper")
                    || (side != null && side.identifier().endsWith("trapdoor"));
            if (!aboveBlocked && support && wireConnects(neighbour.apply(offset(sidePosition, 0, 1, 0)), -1, properties)) {
                sides[i] = (flags & (8 << ((i + 2) % 4))) != 0 ? "up" : "side";
            } else if (wireConnects(side, i, properties)
                    || ((flags & 1) == 0 && wireConnects(neighbour.apply(offset(sidePosition, 0, -1, 0)), -1, properties))) {
                sides[i] = "side";
            } else {
                sides[i] = "none";
            }
        }
        final boolean northSouth = !sides[0].equals("none") || !sides[2].equals("none");
        final boolean eastWest = !sides[1].equals("none") || !sides[3].equals("none");
        if (northSouth || eastWest) {
            for (int i = 0; i < 4; i++) {
                if (sides[i].equals("none") && (i % 2 == 0 ? !eastWest : !northSouth)) {
                    sides[i] = "side";
                }
            }
        }
        BlockState result = state;
        for (int i = 0; i < 4; i++) {
            result = result.withProperty(DIRECTIONS[i], sides[i]);
        }
        return result;
    }

    private static boolean wireConnects(final BlockState state, final int direction, final ToIntFunction<BlockState> properties) {
        if (named(state, "redstone_wire")) {
            return true;
        }
        if (state == null || direction < 0) {
            return false;
        }
        final String facing = state.properties().get("facing");
        if (named(state, "repeater")) {
            return DIRECTIONS[direction].equals(facing) || DIRECTIONS[(direction + 2) % 4].equals(facing);
        }
        if (named(state, "observer")) {
            return DIRECTIONS[direction].equals(facing);
        }
        return (properties.applyAsInt(state) & 2) != 0;
    }

    private static boolean named(final BlockState state, final String name) {
        return state != null && state.namespace().equals("minecraft") && state.identifier().equals(name);
    }

    private static String vineFamily(final BlockState state) {
        if (named(state, "weeping_vines") || named(state, "weeping_vines_plant")) {
            return "weeping_vines";
        }
        if (named(state, "twisting_vines") || named(state, "twisting_vines_plant")) {
            return "twisting_vines";
        }
        return null;
    }

    private static BlockPosition offset(final BlockPosition position, final int dx, final int dy, final int dz) {
        return new BlockPosition(position.x() + dx, position.y() + dy, position.z() + dz);
    }

    public static boolean isContainerInteraction(final String tag) {
        return tag != null && CONTAINER_TAGS.contains(tag);
    }

    public static boolean usesBlockInteraction(final BlockState state, final String tag) {
        if (tag != null && CONTAINER_TAGS.contains(tag)) {
            return true;
        }
        if (state == null) {
            return false;
        }
        final String name = state.identifier();
        return (!name.startsWith("iron_") && (name.endsWith("_door") || name.endsWith("trapdoor")))
                || name.endsWith("fence_gate") || name.endsWith("button") || name.equals("lever");
    }

    public static BlockState door(final BlockState state, final BlockState otherHalf) {
        if (state == null || otherHalf == null || !state.identifier().endsWith("_door")
                || !state.namespacedIdentifier().equals(otherHalf.namespacedIdentifier())
                || !state.properties().containsKey("half") || !otherHalf.properties().containsKey("half")
                || state.properties().get("half").equals(otherHalf.properties().get("half"))) {
            return state;
        }
        final BlockState lower = state.hasProperty("half", "lower") ? state : otherHalf;
        final BlockState upper = state.hasProperty("half", "upper") ? state : otherHalf;
        BlockState result = state;
        for (String property : new String[]{"facing", "open", "powered"}) {
            if (lower.properties().containsKey(property)) {
                result = result.replaceProperty(property, lower.properties().get(property));
            }
        }
        if (upper.properties().containsKey("hinge")) {
            result = result.replaceProperty("hinge", upper.properties().get("hinge"));
        }
        return result;
    }

    public static BlockPosition chestPartner(final BlockPosition position, final CompoundTag tag) {
        if (tag == null || !(tag.get("pairx") instanceof NumberTag x) || !(tag.get("pairz") instanceof NumberTag z)) {
            return null;
        }
        final long dx = (long) x.asInt() - position.x();
        final long dz = (long) z.asInt() - position.z();
        if (Math.abs(dx) + Math.abs(dz) != 1) {
            return null;
        }
        return new BlockPosition(x.asInt(), position.y(), z.asInt());
    }

    public static BlockState chest(final BlockState state, final BlockPosition position, final CompoundTag tag) {
        if (state == null || !state.properties().containsKey("type") || !state.identifier().endsWith("chest")
                || state.identifier().equals("ender_chest")) {
            return state;
        }
        final BlockPosition partner = chestPartner(position, tag);
        if (partner == null) {
            return state.withProperty("type", "single");
        }
        int dx = 0;
        int dz = 0;
        switch (state.properties().getOrDefault("facing", "")) {
            case "north" -> dx = 1;
            case "south" -> dx = -1;
            case "east" -> dz = 1;
            case "west" -> dz = -1;
            default -> {
                return state;
            }
        }
        final int partnerX = partner.x() - position.x();
        final int partnerZ = partner.z() - position.z();
        final String type = partnerX == dx && partnerZ == dz ? "left"
                : partnerX == -dx && partnerZ == -dz ? "right" : "single";
        return state.withProperty("type", type);
    }

    private BlockConnections() {
    }

}

