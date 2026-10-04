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
package net.raphimc.viabedrock.api.model.container;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.viabedrock.api.model.container.block.BrewingStandContainer;
import net.raphimc.viabedrock.api.model.container.block.FurnaceContainer;
import net.raphimc.viabedrock.api.model.container.block.SmithingContainer;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.storage.InventoryTracker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class InventoryPolicyTest {

    @Test
    void brewingSlotsUseIngredientAndFuelNamesWithoutChangingJavaOrder() {
        final BrewingStandContainer brewing = new BrewingStandContainer(null, (byte) 1, null, null);
        assertEquals(ContainerEnumName.BrewingStandInputContainer, brewing.getFullContainerName(0).name());
        assertEquals(ContainerEnumName.BrewingStandFuelContainer, brewing.getFullContainerName(4).name());
        assertEquals(3, brewing.javaSlot(0));
        assertEquals(4, brewing.javaSlot(4));
        for (int slot = 0; slot < 5; slot++) {
            assertEquals(slot, brewing.bedrockSlot(brewing.javaSlot(slot)));
        }
    }

    @Test
    void brewingRejectsNonBottlesAndNonFuelAndLimitsBottleStacks() {
        final var brewing = new BrewingStandContainer(null, (byte) 1, null, null) {
            int bottleLimit(final int slot) {
                return this.slotStackLimit(slot, new BedrockItem(1));
            }
        };
        for (int slot = 1; slot <= 3; slot++) {
            assertFalse(BrewingStandContainer.acceptsItem(slot, "minecraft:wheat"));
            assertTrue(BrewingStandContainer.acceptsItem(slot, "minecraft:potion"));
            assertTrue(BrewingStandContainer.acceptsItem(slot, "minecraft:glass_bottle"));
            assertEquals(1, brewing.bottleLimit(slot));
        }
        assertFalse(BrewingStandContainer.acceptsItem(4, "minecraft:wheat"));
        assertTrue(BrewingStandContainer.acceptsItem(4, "minecraft:blaze_powder"));
        assertFalse(BrewingStandContainer.acceptsItem(0, "minecraft:potion"));
        assertTrue(BrewingStandContainer.acceptsItem(0, "custom:brewing_ingredient"));
    }

    @Test
    void furnaceAndSmithingResultsRejectPlacement() {
        final var furnace = new FurnaceContainer(null, (byte) 1, null, null) {
            boolean acceptsOutput() {
                return this.canPlaceItem(2, new BedrockItem(1));
            }
        };
        final var smithing = new SmithingContainer(null, (byte) 2, null, null) {
            boolean acceptsOutput() {
                return this.canPlaceItem(RESULT_SLOT, new BedrockItem(1));
            }
        };
        assertFalse(furnace.acceptsOutput());
        assertFalse(smithing.acceptsOutput());
    }

    @Test
    void doubleClickSkipsSmithingPreviewButIncludesRealFurnaceResult() {
        final InventoryTracker[] tracker = new InventoryTracker[1];
        final UserConnection user = (UserConnection) Proxy.newProxyInstance(UserConnection.class.getClassLoader(),
                new Class<?>[]{UserConnection.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("get") && arguments[0] == InventoryTracker.class) {
                        return tracker[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        tracker[0] = new InventoryTracker(user);
        final var smithing = new SmithingContainer(user, (byte) 2, null, null) {
            boolean collectsPreview() {
                return this.pickupAllSources().stream().anyMatch(slot -> slot.container() == this && slot.bedrockSlot() == RESULT_SLOT);
            }
        };
        final var furnace = new FurnaceContainer(user, (byte) 1, null, null) {
            boolean collectsResult() {
                return this.pickupAllSources().stream().anyMatch(slot -> slot.container() == this && slot.bedrockSlot() == 2);
            }
        };
        assertFalse(smithing.collectsPreview());
        assertTrue(furnace.collectsResult());
    }

    @Test
    void unsignedStackCountsRemainNonemptyAndCopyCorrectly() {
        final BedrockItem item = new BedrockItem(1);
        for (int amount : new int[]{1, 127, 128, 255}) {
            item.setAmount(amount);
            assertFalse(item.isEmpty());
            assertEquals(amount, item.copy().amount());
        }
        item.setAmount(0);
        assertTrue(item.isEmpty());
        assertTrue(BedrockItem.empty().isEmpty());
    }

    @Test
    void adventureRestrictionsArePartOfStackIdentity() {
        final BedrockItem item = new BedrockItem(1);
        final BedrockItem other = item.copy();
        other.setAmount(12);
        other.setNetId(25);
        assertFalse(item.isDifferent(other));
        other.setCanPlace(new String[]{"minecraft:stone"});
        assertTrue(item.isDifferent(other));
        item.setCanPlace(other.canPlace().clone());
        assertFalse(item.isDifferent(other));
        other.setCanBreak(new String[]{"minecraft:stone"});
        assertTrue(item.isDifferent(other));
        assertFalse(other.isDifferent(other.copy()));
    }

}
