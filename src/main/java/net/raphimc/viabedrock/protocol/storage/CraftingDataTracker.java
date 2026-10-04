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
package net.raphimc.viabedrock.protocol.storage;

import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;

import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.player.HudContainer;
import net.raphimc.viabedrock.protocol.model.recipe.CraftingGridMatcher;
import net.raphimc.viabedrock.protocol.model.recipe.Recipe;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.recipe.ItemDescriptor;
import net.raphimc.viabedrock.protocol.model.recipe.ShapelessRecipe;
import net.raphimc.viabedrock.protocol.model.recipe.SmithingRecipe;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;

import java.util.ArrayList;
import java.util.List;

public class CraftingDataTracker extends StoredObject {

    private List<CraftingDataStorage> craftingDataList = new ArrayList<>();

    public CraftingDataTracker(final UserConnection user) {
        super(user);
    }

    public List<CraftingDataStorage> getCraftingDataList() {
        return this.craftingDataList;
    }

    public void updateCraftingDataList(final List<CraftingDataStorage> craftingDataList) {
        this.craftingDataList = craftingDataList;
    }

    public CraftingDataStorage getRecipeData(final Container container, final String tag) {
        for (CraftingDataStorage craftingData : this.getCraftingDataList()) {
            if (craftingData.recipe() == null || !craftingData.recipe().getRecipeTag().equals(tag)) {
                continue;
            }

            switch (craftingData.type()) {
                case SHAPELESS, SHAPED, USER_DATA_SHAPELESS -> {
                    if (this.getIngredientConsumption(container, craftingData.recipe()) != null) {
                        return craftingData;
                    }
                }
                case SMITHING_TRIM, SMITHING_TRANSFORM -> {
                    // TODO: Hard coded slots for Smithing Container
                    final SmithingRecipe smithingRecipe = (SmithingRecipe) craftingData.recipe();
                    if (smithingRecipe.getTemplate().matchesItem(this.user(), container.getItem(53))
                            && smithingRecipe.getBaseIngredient().matchesItem(this.user(), container.getItem(51))
                            && smithingRecipe.getAdditionIngredient().matchesItem(this.user(), container.getItem(52))) {
                        return craftingData;
                    }
                }
                default -> ViaBedrock.getPlatform().getLogger().warning(
                        "Unknown recipe type: " + craftingData.type() + " in recipe " + craftingData.recipe().getUniqueId()
                );
            }
        }
        return null;
    }

    public int[] getIngredientConsumption(final Container container, final Recipe recipe) {
        final int width = container instanceof HudContainer ? 2 : 3;
        final BedrockItem[] grid = new BedrockItem[width * width];
        for (int slot = 0; slot < grid.length; slot++) {
            grid[slot] = container.getItem(container.bedrockSlot(slot + 1));
        }
        return CraftingGridMatcher.match(recipe, grid, width, (descriptor, item) -> descriptor.matchesItem(this.user(), item));
    }

    public void sendJavaUpdateRecipes(final UserConnection user) {
        //TODO: Fix up this mess
        if (this.craftingDataList.isEmpty()) {
            ViaBedrock.getPlatform().getLogger().warning("No crafting data available to update.");
            return;
        }
        final ItemRewriter itemRewriter = user.get(ItemRewriter.class);

        final PacketWrapper packet = PacketWrapper.create(ClientboundPackets26_3.UPDATE_RECIPES, user);
        packet.write(Types.VAR_INT, 0); // Property Sets (Prefixed array) TODO: Sends registries e.g. furnace fuel, smithing template
        final List<CraftingDataStorage> stonecutterList = this.craftingDataList.stream()
                .filter(c -> c.recipe().getRecipeTag().equals("stonecutter"))
                .filter(c -> c.recipe() instanceof ShapelessRecipe recipe && !recipe.getIngredients().isEmpty() && !recipe.getResults().isEmpty()
                        && (recipe.getIngredients().get(0) instanceof ItemDescriptor.DefaultDescriptor
                        || recipe.getIngredients().get(0) instanceof ItemDescriptor.DeferredDescriptor named && itemRewriter.getItems().containsKey(named.fullName())))
                .toList();
        packet.write(Types.VAR_INT, stonecutterList.size()); // Number of recipes
        for (CraftingDataStorage craftingData : stonecutterList) {
            //IDs
            packet.write(Types.VAR_INT, 2); // Type (Size + 1)
            final ItemDescriptor ingredient = ((ShapelessRecipe) craftingData.recipe()).getIngredients().get(0);
            final int bedrockId = ingredient instanceof ItemDescriptor.DeferredDescriptor named
                    ? itemRewriter.getItems().get(named.fullName()) : ((ItemDescriptor.DefaultDescriptor) ingredient).itemId();
            final int javaId = itemRewriter.javaItem(new BedrockItem(bedrockId)).identifier();
            packet.write(Types.VAR_INT, javaId);

            //Slot Display
            final Item javaOutput = itemRewriter.javaItem(((ShapelessRecipe) craftingData.recipe()).getResults().get(0));
            packet.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaSlotDisplayId("minecraft:item_stack")); // Type
            packet.write(VersionedTypes.V26_3.itemTemplate, javaOutput);
        }

        packet.send(BedrockProtocol.class);
    }

    public void sendJavaRecipeBook(final UserConnection user) {
        if (this.craftingDataList.isEmpty()) {
            ViaBedrock.getPlatform().getLogger().warning("No crafting data available to send Java recipe book.");
            return;
        }

        final PacketWrapper packet = PacketWrapper.create(ClientboundPackets26_3.RECIPE_BOOK_ADD, user);
        packet.write(Types.VAR_INT, this.craftingDataList.size()); // Number of recipes
        for (CraftingDataStorage craftingData : this.craftingDataList) {
            packet.write(Types.VAR_INT, craftingData.networkId()); // Recipe ID
            craftingData.recipe().writeJavaRecipeData(packet, user);
            packet.write(Types.OPTIONAL_VAR_INT, craftingData.networkId()); //TODO: Group Id
            packet.write(Types.VAR_INT, 1); // TODO: Category ID
            packet.write(Types.BOOLEAN, false); // Optional Ingredients list
            packet.write(Types.BYTE, (byte) 0x00); // Recipe Flags (0x01: show notification; 0x02: highlight as new)
        }
        packet.write(Types.BOOLEAN, false); //  Replace or Add
        packet.send(BedrockProtocol.class);
    }

}
