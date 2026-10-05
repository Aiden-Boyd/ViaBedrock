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

import com.viaversion.viaversion.api.minecraft.BlockPosition;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockConnectionsTest {

    private static final BlockPosition ORIGIN = new BlockPosition(-16, 16, -1);
    private static final BlockState WIRE = BlockState.fromString("minecraft:redstone_wire[east=none,north=none,power=11,south=none,west=none]");
    private final Map<BlockPosition, BlockState> blocks = new HashMap<>();

    private void put(final int x, final int y, final int z, final String state) {
        this.blocks.put(new BlockPosition(ORIGIN.x() + x, ORIGIN.y() + y, ORIGIN.z() + z), BlockState.fromString("minecraft:" + state));
    }

    private BlockState connect(final BlockState state) {
        return BlockConnections.connect(state, ORIGIN, this.blocks::get, BlockConnectionProperties::flags);
    }

    @Test
    void linesCornersAndPower() {
        assertEquals(WIRE, this.connect(WIRE));
        this.put(1, 0, 0, "redstone_wire[power=0]");
        BlockState result = this.connect(WIRE);
        assertTrue(result.hasProperty("east", "side"));
        assertTrue(result.hasProperty("west", "side"));
        assertTrue(result.hasProperty("north", "none"));
        assertTrue(result.hasProperty("power", "11"));
        this.put(0, 0, -1, "redstone_wire[power=15]");
        result = this.connect(WIRE);
        assertTrue(result.hasProperty("north", "side"));
        assertTrue(result.hasProperty("east", "side"));
        assertTrue(result.hasProperty("west", "none"));
        assertTrue(result.hasProperty("south", "none"));
        this.blocks.clear();
        assertEquals(WIRE, this.connect(WIRE));
    }

    @Test
    void stepsSupportsAndBlockedHeadroom() {
        this.put(1, 0, 0, "stone");
        this.put(1, 1, 0, "redstone_wire[power=0]");
        assertTrue(this.connect(WIRE).hasProperty("east", "up"));
        this.put(0, 1, 0, "stone");
        assertTrue(this.connect(WIRE).hasProperty("east", "none"));
        this.put(0, 1, 0, "glass");
        this.put(1, 0, 0, "glass");
        assertTrue(this.connect(WIRE).hasProperty("east", "up"));
        this.put(1, 0, 0, "oak_slab[type=bottom,waterlogged=false]");
        assertTrue(this.connect(WIRE).hasProperty("east", "none"));
        this.put(1, 0, 0, "oak_slab[type=top,waterlogged=false]");
        assertTrue(this.connect(WIRE).hasProperty("east", "side"));
        this.blocks.clear();
        this.put(1, -1, 0, "redstone_wire[power=0]");
        assertTrue(this.connect(WIRE).hasProperty("east", "side"));
        this.put(1, 0, 0, "stone");
        assertTrue(this.connect(WIRE).hasProperty("east", "none"));
    }

    @Test
    void repeaterAndObserverDirection() {
        this.put(1, 0, 0, "repeater[facing=north]");
        assertTrue(this.connect(WIRE).hasProperty("east", "none"));
        this.put(1, 0, 0, "repeater[facing=west]");
        assertTrue(this.connect(WIRE).hasProperty("east", "side"));
        this.put(1, 0, 0, "observer[facing=west]");
        assertTrue(this.connect(WIRE).hasProperty("east", "none"));
        this.put(1, 0, 0, "observer[facing=east]");
        assertTrue(this.connect(WIRE).hasProperty("east", "side"));
        this.put(1, 0, 0, "lever[face=floor,facing=north,powered=false]");
        assertTrue(this.connect(WIRE).hasProperty("east", "side"));
    }

    @Test
    void hopperAndTrapdoorAllowClimbingWithoutInventingConnections() {
        this.put(1, 1, 0, "redstone_wire[power=0]");
        this.put(1, 0, 0, "hopper[enabled=true,facing=down]");
        assertTrue(this.connect(WIRE).hasProperty("east", "side"));
        this.put(1, 0, 0, "oak_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]");
        assertTrue(this.connect(WIRE).hasProperty("east", "side"));
        this.put(1, 1, 0, "lever[face=floor,facing=north,powered=true]");
        assertTrue(this.connect(WIRE).hasProperty("east", "none"));
    }

    @Test
    void vineBodyAndTipFollowGrowthAndRemoval() {
        final BlockState weeping = BlockState.fromString("minecraft:weeping_vines[age=17]");
        assertEquals(weeping, this.connect(weeping));
        this.put(0, -1, 0, "weeping_vines[age=3]");
        assertEquals(BlockState.fromString("minecraft:weeping_vines_plant"), this.connect(weeping));
        this.blocks.clear();
        assertEquals(weeping, this.connect(weeping));
        final BlockState twisting = BlockState.fromString("minecraft:twisting_vines[age=5]");
        this.put(0, 1, 0, "twisting_vines_plant");
        assertEquals(BlockState.fromString("minecraft:twisting_vines_plant"), this.connect(twisting));
        this.put(0, 1, 0, "weeping_vines[age=0]");
        assertEquals(twisting, this.connect(twisting));
    }

    @Test
    void nativePropertiesDistinguishTransparencyFromConduction() {
        assertEquals(0, BlockConnectionProperties.flags(BlockState.fromString("minecraft:air")));
        assertEquals(1, BlockConnectionProperties.flags(BlockState.fromString("minecraft:stone")) & 1);
        assertEquals(0, BlockConnectionProperties.flags(BlockState.fromString("minecraft:glass")) & 1);
        assertEquals(4, BlockConnectionProperties.flags(BlockState.fromString("minecraft:glass")) & 4);
    }

}
