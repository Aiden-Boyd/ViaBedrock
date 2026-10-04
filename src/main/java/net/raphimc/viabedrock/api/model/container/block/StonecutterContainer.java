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
package net.raphimc.viabedrock.api.model.container.block;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;

import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.recipe.ShapelessRecipe;
import net.raphimc.viabedrock.protocol.storage.CraftingDataStorage;
import net.raphimc.viabedrock.protocol.storage.CraftingDataTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StonecutterContainer extends Container {

    private final List<CraftingDataStorage> currentRecipes = new ArrayList<>();
    private int selectedRecipe = 0;

    public StonecutterContainer(final UserConnection user, final byte containerId, final TextComponent title, final BlockPosition position) {
        super(user, containerId, ContainerType.STONECUTTER, title, position, 2, "stonecutter_block", "stonecutter");
    }

    @Override
    public FullContainerName getFullContainerName(final int slot) {
        return switch (slot) {
            case 3 -> new FullContainerName(ContainerEnumName.StonecutterInputContainer, null);
            case 50 -> new FullContainerName(ContainerEnumName.CreatedOutputContainer, null); //TODO: CreatedOutputContainer?
            default -> throw new IllegalArgumentException("Invalid slot for Stonecutter Container: " + slot);
        };
    }

    @Override
    public int javaSlot(final int slot) {
        return switch (slot) {
            case 3 -> 0;
            case 50 -> 1;
            default -> super.javaSlot(slot);
        };
    }

    @Override
    public int bedrockSlot(final int slot) {
        return switch (slot) {
            case 0 -> 3;
            case 1 -> 50;
            default -> super.bedrockSlot(slot);
        };
    }

    @Override
    public BedrockItem getItem(final int bedrockSlot) {
        if (bedrockSlot == 3) {
            return this.items[0];
        } else if (bedrockSlot == 50) {
            return this.items[1];
        } else {
            throw new IllegalArgumentException("Bedrock Slot out of bounds for stonecutter (getItem): " + bedrockSlot);
        }
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        if (bedrockSlot == 3) {
            return super.setItem(0, item);
        } else if (bedrockSlot == 50) {
            return super.setItem(1, item);
        } else {
            throw new IllegalArgumentException("Bedrock Slot out of bounds for stonecutter (setItem): " + bedrockSlot);
        }
    }

    @Override
    public boolean setItems(final BedrockItem[] items) {
        if (items.length != this.size() && items.length != 54) {
            return false;
        }
        for (int slot = 0; slot < this.size(); slot++) {
            this.setItem(this.bedrockSlot(slot), items.length == 54 ? items[this.bedrockSlot(slot)] : items[slot]);
        }
        this.updateRecipeData(this.getItem(3));
        return true;
    }

    @Override
    protected void onSlotChanged(final int slot, final BedrockItem previous, final BedrockItem item) {
        if (slot == 0) {
            this.updateRecipeData(item);
        }
    }

    @Override
    public boolean handleClick(final int revision, final short javaSlot, final byte button, final ContainerInput action) {
        if (javaSlot != 1 || action == ContainerInput.QUICK_CRAFT) {
            final boolean handled = super.handleClick(revision, javaSlot, button, action);
            if (action != ContainerInput.QUICK_CRAFT || (button & 3) == 2) {
                PacketFactory.sendJavaContainerSetContent(this.user, this);
            }
            return handled;
        }
        if ((action != ContainerInput.PICKUP && action != ContainerInput.QUICK_MOVE) || (button != 0 && button != 1)) {
            return false;
        }
        this.updateRecipeData(this.getItem(3));
        if (this.currentRecipes.isEmpty()) {
            return false;
        }
        final CraftingDataStorage recipe = this.currentRecipes.get(this.selectedRecipe);
        final ShapelessRecipe data = (ShapelessRecipe) recipe.recipe();
        return this.craftOutput(revision, recipe.networkId(), this.getItem(50),
                Map.of(3, data.getIngredients().get(0).amount()), action == ContainerInput.QUICK_MOVE);
    }

    @Override
    public boolean handleButtonClick(final int button) {
        this.updateRecipeData(this.getItem(3));
        if (button < 0 || button >= this.currentRecipes.size()) {
            return false;
        }
        this.selectedRecipe = button;
        this.updateOutput();
        PacketFactory.sendJavaContainerSetContent(this.user, this);
        return true;
    }

    private void updateRecipeData(final BedrockItem item) {
        final CraftingDataStorage previous = this.currentRecipes.isEmpty() ? null : this.currentRecipes.get(this.selectedRecipe);
        this.currentRecipes.clear();
        if (!item.isEmpty()) {
            for (CraftingDataStorage craftingData : this.user.get(CraftingDataTracker.class).getCraftingDataList()) {
                if (!(craftingData.recipe() instanceof ShapelessRecipe recipe) || !"stonecutter".equals(recipe.getRecipeTag())
                        || recipe.getIngredients().size() != 1 || recipe.getResults().size() != 1) {
                    continue;
                }
                if (recipe.getIngredients().get(0).matchesItem(this.user, item)) {
                    this.currentRecipes.add(craftingData);
                }
            }
        }
        this.selectedRecipe = Math.max(0, this.currentRecipes.indexOf(previous));
        this.updateOutput();
    }

    private void updateOutput() {
        final BedrockItem output = this.currentRecipes.isEmpty() ? BedrockItem.empty()
                : ((ShapelessRecipe) this.currentRecipes.get(this.selectedRecipe).recipe()).getResults().get(0).copy();
        super.setItem(1, output);
    }

}
