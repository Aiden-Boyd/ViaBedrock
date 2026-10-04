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

import net.raphimc.viabedrock.protocol.data.enums.bedrock.AbilitiesIndex;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.SerializedAbilitiesData_SerializedAbilitiesLayer;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PlayerPermissionLevel;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerPermissionsTest {

    @Test
    void visitorCannotInteractEvenWhenFallbackAbilitiesArePermissive() {
        final var visitor = player(PlayerPermissionLevel.Visitor);
        for (var ability : new AbilitiesIndex[]{AbilitiesIndex.Build, AbilitiesIndex.Mine, AbilitiesIndex.OpenContainers,
            AbilitiesIndex.DoorsAndSwitches, AbilitiesIndex.AttackPlayers, AbilitiesIndex.AttackMobs}) {
            assertFalse(visitor.mayInteract(ability), ability.name());
        }
    }

    @Test
    void customPermissionsKeepBuildingContainersAndDoorsIndependent() {
        final var custom = player(PlayerPermissionLevel.Custom);
        custom.abilityLayers().get(SerializedAbilitiesData_SerializedAbilitiesLayer.Base).setAbility(AbilitiesIndex.Build, false);
        custom.abilityLayers().get(SerializedAbilitiesData_SerializedAbilitiesLayer.Base).setAbility(AbilitiesIndex.AttackPlayers, false);
        assertFalse(custom.mayUseBlock(false, false, false));
        assertTrue(custom.mayUseBlock(true, false, false));
        assertTrue(custom.mayUseBlock(false, true, false));
        assertFalse(custom.mayInteract(AbilitiesIndex.AttackPlayers));
        assertTrue(custom.mayInteract(AbilitiesIndex.AttackMobs));
        assertTrue(custom.mayInteract(AbilitiesIndex.Mine));
    }

    @Test
    void operatorStatusDoesNotOverrideRevokedAbilitiesAndUpdatesRoundTrip() {
        final var operator = player(PlayerPermissionLevel.Operator);
        operator.abilityLayers().get(SerializedAbilitiesData_SerializedAbilitiesLayer.Base).setAbility(AbilitiesIndex.Mine, false);
        final var buffer = Unpooled.buffer();
        try {
            BedrockTypes.PLAYER_ABILITIES.write(buffer, operator);
            final var received = BedrockTypes.PLAYER_ABILITIES.read(buffer);
            assertFalse(received.mayInteract(AbilitiesIndex.Mine));
            assertTrue(received.mayInteract(AbilitiesIndex.Build));
            received.abilityLayers().get(SerializedAbilitiesData_SerializedAbilitiesLayer.Base).setAbility(AbilitiesIndex.Mine, true);
            assertTrue(received.mayInteract(AbilitiesIndex.Mine));
        } finally {
            buffer.release();
        }
    }

    @Test
    void immutableWorldStopsPlacementButAllowsGrantedDoorsAndContainers() {
        final var member = player(PlayerPermissionLevel.Member);
        assertFalse(member.mayUseBlock(false, false, true));
        assertTrue(member.mayUseBlock(true, false, true));
        assertTrue(member.mayUseBlock(false, true, true));
        assertTrue(member.mayUseBlock(false, false, false));
    }

    private static PlayerAbilities player(final PlayerPermissionLevel role) {
        return new PlayerAbilities(1, (byte) role.getValue(), (byte) 0);
    }

}
