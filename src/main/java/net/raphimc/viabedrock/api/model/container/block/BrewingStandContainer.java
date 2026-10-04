/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.viabedrock.api.model.container.block;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;

import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;

public class BrewingStandContainer extends Container {

    public BrewingStandContainer(final UserConnection user, final byte containerId, final TextComponent title, final BlockPosition position) {
        super(user, containerId, ContainerType.BREWING_STAND, title, position, 5, CustomBlockTags.BREWING_STAND);
    }

    @Override
    public FullContainerName getFullContainerName(final int slot) {
        return switch (slot) {
            case 0 -> new FullContainerName(ContainerEnumName.BrewingStandInputContainer, null);
            case 1, 2, 3 -> new FullContainerName(ContainerEnumName.BrewingStandResultContainer, null);
            case 4 -> new FullContainerName(ContainerEnumName.BrewingStandFuelContainer, null);
            default -> throw new IllegalArgumentException("Invalid slot for Brewing Container: " + slot);
        };
    }

    @Override
    public int javaSlot(final int slot) {
        return switch (slot) {
            case 0 -> 3;
            case 1, 2, 3 -> slot - 1;
            case 4 -> 4;
            default -> super.javaSlot(slot);
        };
    }

    @Override
    public int bedrockSlot(final int slot) {
        return switch (slot) {
            case 3 -> 0;
            case 0, 1, 2 -> slot + 1;
            case 4 -> 4;
            default -> super.bedrockSlot(slot);
        };
    }

    public static boolean acceptsItem(final int slot, final String identifier) {
        final boolean bottle = "minecraft:potion".equals(identifier) || "minecraft:splash_potion".equals(identifier)
                || "minecraft:lingering_potion".equals(identifier) || "minecraft:glass_bottle".equals(identifier);
        return switch (slot) {
            case 0 -> !bottle; // Ingredient validity is determined by the server recipes.
            case 1, 2, 3 -> bottle;
            case 4 -> "minecraft:blaze_powder".equals(identifier);
            default -> false;
        };
    }

    @Override
    protected boolean canPlaceItem(final int slot, final BedrockItem item) {
        final String identifier = this.user.get(ItemRewriter.class).getItems().inverse().get(item.identifier());
        return acceptsItem(slot, identifier) && super.canPlaceItem(slot, item);
    }

    @Override
    protected int slotStackLimit(final int slot, final BedrockItem item) {
        return slot >= 1 && slot <= 3 ? 1 : super.slotStackLimit(slot, item);
    }

    @Override
    public short translateContainerData(final int containerData) {
        return switch (containerData) {
            case 0 -> 0; // Progress arrow
            case 1 -> 1; // Fuel progress
            default -> -1; // Unknown
        };
    }

}

