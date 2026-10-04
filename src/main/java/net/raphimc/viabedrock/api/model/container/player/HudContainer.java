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
package net.raphimc.viabedrock.api.model.container.player;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.block.CraftingTableContainer;
import net.raphimc.viabedrock.api.model.container.block.StonecutterContainer;
import net.raphimc.viabedrock.api.model.container.block.EnchantmentContainer;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.PlayerActionPacketFactory;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.TextProcessingEventOrigin;
import net.raphimc.viabedrock.protocol.model.inventory.*;
import net.raphimc.viabedrock.protocol.model.recipe.ShapedRecipe;
import net.raphimc.viabedrock.protocol.model.recipe.ShapelessRecipe;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.*;

import java.util.ArrayList;
import java.util.List;

import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerID;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;

public class HudContainer extends InventoryRedirectContainer {

    public HudContainer(final UserConnection user) {
        super(user, (byte) ContainerID.CONTAINER_ID_PLAYER_ONLY_UI.getValue(), ContainerType.HUD, 54);
    }

    @Override
    public FullContainerName getFullContainerName(final int slot) {
        if (slot == 0) {
            return new FullContainerName(ContainerEnumName.CursorContainer, null);
        } else if (slot >= 28 && slot <= 31) {
            return new FullContainerName(ContainerEnumName.CraftingInputContainer, null);
        } else if (slot == 50) {
            return new FullContainerName(ContainerEnumName.CreatedOutputContainer, null);
        } else {
            return new FullContainerName(ContainerEnumName.CursorContainer, null); // TODO: This should not happen
        }
    }

    @Override
    protected void onSlotChanged(final int slot, final BedrockItem oldItem, final BedrockItem newItem) {
        final Container open = this.user.get(InventoryTracker.class).getCurrentContainer();
        if ((open instanceof CraftingTableContainer && (slot >= 32 && slot <= 40 || slot == 50))
                || (open instanceof StonecutterContainer && (slot == 3 || slot == 50))
                || (open instanceof EnchantmentContainer && (slot == 14 || slot == 15))) {
            open.setItem(slot, newItem);
            PacketFactory.sendJavaContainerSetContent(this.user, open);
        }
        if (slot >= 28 && slot <= 31) {
            this.updateCraftingResult();
        }
    }

    public void updateCraftingResult() {
        final CraftingDataStorage recipe = this.user.get(CraftingDataTracker.class).getRecipeData(this, "crafting_table");
        final List<BedrockItem> results = recipe == null ? List.of()
                : recipe.recipe() instanceof ShapedRecipe shaped ? shaped.getResults()
                : recipe.recipe() instanceof ShapelessRecipe shapeless ? shapeless.getResults() : List.of();
        super.setItem(50, results.size() == 1 ? results.get(0).copy() : BedrockItem.empty());
    }

    public boolean craft(final int revision) {
        return this.craft(revision, false);
    }

    public boolean craft(final int revision, final boolean quickMove) {
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
        final java.util.Map<Integer, Integer> consumed = new java.util.LinkedHashMap<>();
        for (int slot = 0; slot < consumption.length; slot++) {
            if (consumption[slot] > 0) {
                consumed.put(slot + 28, consumption[slot]);
            }
        }
        return this.craftOutput(revision, recipe.networkId(), this.getItem(50), consumed, quickMove);
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        if (super.setItem(bedrockSlot, item)) {
            return bedrockSlot == 0 || (bedrockSlot >= 28 && bedrockSlot <= 31) || bedrockSlot == 50;
        } else {
            return false;
        }
    }

    @Override
    public int javaSlot(final int slot) {
        if (slot >= 28 && slot <= 31) {
            return slot - 27;
        } else if (slot == 50) {
            return 0;
        } else {
            return super.javaSlot(slot);
        }
    }

    @Override
    public int bedrockSlot(final int slot) {
        if (slot >= 1 && slot <= 4) {
            return slot + 27;
        } else if (slot == 0) {
            return 50;
        } else {
            return super.bedrockSlot(slot);
        }
    }

}
