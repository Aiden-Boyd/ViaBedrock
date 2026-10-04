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
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.recipe.ShapedRecipe;
import net.raphimc.viabedrock.protocol.model.recipe.ShapelessRecipe;
import net.raphimc.viabedrock.protocol.storage.CraftingDataStorage;
import net.raphimc.viabedrock.protocol.storage.CraftingDataTracker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CraftingTableContainer extends Container {

    public CraftingTableContainer(final UserConnection user, final byte containerId, final TextComponent title, final BlockPosition position) {
        super(user, containerId, ContainerType.WORKBENCH, title, position, 10, CustomBlockTags.WORKBENCH);
    }

    @Override
    public FullContainerName getFullContainerName(final int slot) {
        return switch (slot) {
            case 32, 33, 34, 35, 36, 37, 38, 39, 40 -> new FullContainerName(ContainerEnumName.CraftingInputContainer, null);
            case 50 -> new FullContainerName(ContainerEnumName.CreatedOutputContainer, null);
            default -> throw new IllegalArgumentException("Invalid slot for Crafting Container: " + slot);
        };
    }

    @Override
    public int javaSlot(final int bedrockSlot) {
        return switch (bedrockSlot) {
            case 32, 33, 34, 35, 36, 37, 38, 39, 40 -> bedrockSlot - 31;
            case 50 -> 0;
            default -> super.javaSlot(bedrockSlot);
        };
    }

    @Override
    public int bedrockSlot(final int javaSlot) {
        return switch (javaSlot) {
            case 1, 2, 3, 4, 5, 6, 7, 8, 9 -> javaSlot + 31;
            case 0 -> 50;
            default -> super.bedrockSlot(javaSlot);
        };
    }

    @Override
    public BedrockItem getItem(final int bedrockSlot) {
        return switch (bedrockSlot) {
            case 50 -> this.items[0];
            case 32, 33, 34, 35, 36, 37, 38, 39, 40 -> this.items[bedrockSlot - 31];
            default -> throw new IllegalArgumentException("Invalid slot for Crafting Container: " + bedrockSlot);
        };
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        return switch (bedrockSlot) {
            case 50 -> super.setItem(0, item);
            case 32, 33, 34, 35, 36, 37, 38, 39, 40 -> super.setItem(bedrockSlot - 31, item);
            default -> throw new IllegalArgumentException("Invalid slot for Crafting Container: " + bedrockSlot);
        };
    }

    @Override
    public boolean setItems(final BedrockItem[] items) {
        if (items.length != this.size() && items.length != 54) {
            return false;
        }
        for (int slot = 0; slot < this.size(); slot++) {
            this.setItem(this.bedrockSlot(slot), items.length == 54 ? items[this.bedrockSlot(slot)] : items[slot]);
        }
        this.updateCraftingResult();
        return true;
    }

    @Override
    protected void onSlotChanged(final int slot, final BedrockItem previous, final BedrockItem item) {
        if (slot >= 1 && slot <= 9) {
            this.updateCraftingResult();
        }
    }

    public void updateCraftingResult() {
        final CraftingDataStorage recipe = this.user.get(CraftingDataTracker.class).getRecipeData(this, "crafting_table");
        final List<BedrockItem> results = recipe == null ? List.of()
                : recipe.recipe() instanceof ShapedRecipe shaped ? shaped.getResults()
                : recipe.recipe() instanceof ShapelessRecipe shapeless ? shapeless.getResults() : List.of();
        super.setItem(0, results.size() == 1 ? results.get(0).copy() : BedrockItem.empty());
    }

    @Override
    public boolean handleClick(final int revision, final short javaSlot, final byte button, final ContainerInput action) {
        if (javaSlot != 0 || action == ContainerInput.QUICK_CRAFT || action == ContainerInput.PICKUP_ALL) {
            final boolean handled = super.handleClick(revision, javaSlot, button, action);
            this.updateCraftingResult();
            if (action != ContainerInput.QUICK_CRAFT || (button & 3) == 2) {
                PacketFactory.sendJavaContainerSetContent(this.user, this);
            }
            return handled;
        }
        if ((action != ContainerInput.PICKUP && action != ContainerInput.QUICK_MOVE) || (button != 0 && button != 1)) {
            return false;
        }
        final CraftingDataTracker recipes = this.user.get(CraftingDataTracker.class);
        final CraftingDataStorage recipe = recipes.getRecipeData(this, "crafting_table");
        if (recipe == null) {
            return false;
        }
        this.updateCraftingResult();
        final int[] consumption = recipes.getIngredientConsumption(this, recipe.recipe());
        if (consumption == null) {
            return false;
        }
        final Map<Integer, Integer> consumed = new LinkedHashMap<>();
        for (int slot = 0; slot < consumption.length; slot++) {
            if (consumption[slot] > 0) {
                consumed.put(slot + 32, consumption[slot]);
            }
        }
        return this.craftOutput(revision, recipe.networkId(), this.getItem(50), consumed, action == ContainerInput.QUICK_MOVE);
    }

}
