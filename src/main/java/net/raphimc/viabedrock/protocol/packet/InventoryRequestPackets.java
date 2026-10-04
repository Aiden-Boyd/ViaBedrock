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
package net.raphimc.viabedrock.protocol.packet;

import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPackets26_3;

import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.block.*;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.inventory.*;
import net.raphimc.viabedrock.protocol.model.recipe.EnchantData;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import net.raphimc.viabedrock.protocol.types.InventoryTypes;
import net.raphimc.viabedrock.protocol.types.recipe.CraftingRecipesType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

public final class InventoryRequestPackets {

    public static void register(final BedrockProtocol protocol) {
        protocol.registerServerbound(ServerboundPackets26_3.CONTAINER_BUTTON_CLICK, null, wrapper -> {
            wrapper.cancel();
            final int containerId = wrapper.read(Types.VAR_INT); // container id
            final int button = wrapper.read(Types.VAR_INT); // button

            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            if (inventoryTracker.getPendingCloseContainer() != null) {
                wrapper.cancel();
                return;
            }
            final Container container = inventoryTracker.getContainerServerbound((byte) containerId);
            if (container == null) {
                wrapper.cancel();
                return;
            }
            final UserConnection user = wrapper.user();
            user.get(InventoryRequestTracker.class).runInventoryAction(() -> {
                if (!inventoryTracker.enforceContainerPermissions() || inventoryTracker.getPendingCloseContainer() != null
                        || inventoryTracker.getContainerServerbound((byte) containerId) != container) {
                    return;
                }
                if (!container.handleButtonClick(button)) {
                    if (container.type() != ContainerType.INVENTORY) {
                        PacketFactory.sendJavaContainerSetContent(user, inventoryTracker.getInventoryContainer());
                    }
                    PacketFactory.sendJavaContainerSetContent(user, container);
                }
            });
        });
        protocol.registerServerbound(ServerboundPackets26_3.SET_BEACON, null, wrapper -> {
            wrapper.cancel();

            int primaryPower = -1;
            int secondaryPower = -1;

            final boolean hasPrimary = wrapper.read(Types.BOOLEAN);
            if (hasPrimary) {
                primaryPower = wrapper.read(Types.VAR_INT);
            }
            final boolean hasSecondary = wrapper.read(Types.BOOLEAN);
            if (hasSecondary) {
                secondaryPower = wrapper.read(Types.VAR_INT);
            }

            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            if (inventoryTracker.isContainerOpen() && inventoryTracker.getCurrentContainer() instanceof BeaconContainer beaconContainer) {
                final int selectedPrimary = primaryPower;
                final int selectedSecondary = secondaryPower;
                wrapper.user().get(InventoryRequestTracker.class).runInventoryAction(() -> {
                    if (inventoryTracker.enforceContainerPermissions() && inventoryTracker.getCurrentContainer() == beaconContainer
                            && inventoryTracker.getPendingCloseContainer() == null) {
                        beaconContainer.updateEffects(selectedPrimary, selectedSecondary);
                    }
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets26_3.RENAME_ITEM, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final String newName = wrapper.read(Types.STRING);

            if (inventoryTracker.isContainerOpen() && inventoryTracker.getCurrentContainer() instanceof AnvilContainer anvilContainer) {
                anvilContainer.setRenameText(newName);
            }
        });
        protocol.registerServerbound(ServerboundPackets26_3.CONTAINER_SLOT_STATE_CHANGED, ServerboundBedrockPackets.TOGGLE_CRAFTER_SLOT_REQUEST, wrapper -> {
            final int slotId = wrapper.read(Types.VAR_INT);
            final int windowId = wrapper.read(Types.VAR_INT);
            final boolean state = wrapper.read(Types.BOOLEAN);

            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final Container container = inventoryTracker.getContainerServerbound((byte) windowId);
            if (!inventoryTracker.enforceContainerPermissions() || inventoryTracker.getPendingCloseContainer() != null
                    || !(container instanceof CrafterContainer crafter) || container.position() == null
                    || !crafter.setSlotEnabled(slotId, state)) {
                wrapper.cancel();
                return;
            }

            wrapper.write(BedrockTypes.INT_LE, container.position().x());
            wrapper.write(BedrockTypes.INT_LE, container.position().y());
            wrapper.write(BedrockTypes.INT_LE, container.position().z());
            wrapper.write(Types.UNSIGNED_BYTE, (short) slotId);
            wrapper.write(Types.BOOLEAN, !state);
        });

        protocol.registerClientbound(ClientboundBedrockPackets.CRAFTING_DATA, null, wrapper -> {
            wrapper.cancel();
            final CraftingDataTracker craftingDataTracker = wrapper.user().get(CraftingDataTracker.class);
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);

            try {
                final CraftingDataStorage[] recipes = wrapper.read(new CraftingRecipesType(itemRewriter.itemInstanceType()));
                craftingDataTracker.updateCraftingDataList(List.of(recipes));
            } catch (final Exception exception) {
                wrapper.clearPacket();
                ViaBedrock.getPlatform().getLogger().log(java.util.logging.Level.WARNING,
                        "Unable to decode crafting recipes; manual crafting is unavailable", exception);
                return;
            }
            wrapper.clearPacket();
            craftingDataTracker.sendJavaUpdateRecipes(wrapper.user());
            final var hud = wrapper.user().get(InventoryTracker.class).getHudContainer();
            hud.updateCraftingResult();
            hud.sendCraftingResult();
        });
        protocol.registerClientbound(ClientboundBedrockPackets.ITEM_STACK_RESPONSE, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final InventoryRequestTracker inventoryRequestTracker = wrapper.user().get(InventoryRequestTracker.class);
            final ItemStackResponseInfo[] infoList = wrapper.read(InventoryTypes.ITEM_STACK_RESPONSES);

            final Set<Container> changedContainers = new LinkedHashSet<>();
            // Apply authoritative stack IDs before running the next queued click.
            for (ItemStackResponseInfo info : infoList) {

                final InventoryRequestStorage requestInfo = inventoryRequestTracker.getRequest(info.requestId());
                if (requestInfo == null) {
                    ViaBedrock.getPlatform().getLogger().warning("Received item stack response for unknown request ID: " + info.requestId());
                    continue;
                }
                inventoryRequestTracker.removeRequest(info.requestId());

                if (info.result() != ItemStackNetResult.Success) {
                    ViaBedrock.getPlatform().getLogger().warning(
                            "Received unsuccessful item stack response: result=" + info.result()
                                    + ", requestId=" + info.requestId()
                                    + ", javaRevision=" + requestInfo.javaRevision()
                                    + ", actions=" + requestInfo.requestInfo().actions()
                    );
                    inventoryRequestTracker.clearInventoryActions();
                    inventoryTracker.getHudContainer().setItem(0, requestInfo.prevCursorContainer().getItem(0).copy());
                    changedContainers.add(inventoryTracker.getInventoryContainer());
                    for (Container container : requestInfo.prevContainers()) {
                        final Container newContainer = inventoryTracker.getContainerClientbound(container.containerId(), null, null);
                        if (newContainer == null) {
                            continue;
                        }
                        newContainer.setItems(container.getItems().clone());
                        changedContainers.add(newContainer);
                    }
                    continue;
                }

                for (ItemStackResponseContainerInfo containerInfo : info.containers()) {
                    for (ItemStackResponseSlotInfo slotInfo : containerInfo.slots()) {
                        final int slot = Byte.toUnsignedInt(slotInfo.slot());
                        final Container container = inventoryTracker.getContainerFromName(containerInfo.containerName(), slot);
                        if (container == null) {
                            ViaBedrock.getPlatform().getLogger().warning("Received item stack response for unknown container: " + containerInfo.containerName());
                            continue;
                        }
                        final BedrockItem expectedItem = container.getItem(slot);
                        final int amount = Byte.toUnsignedInt(slotInfo.amount());
                        if (amount == 0) {
                            if (!expectedItem.isEmpty()) {
                                container.setItem(slot, BedrockItem.empty());
                                changedContainers.add(container);
                            }
                            continue;
                        }
                        if (expectedItem.isEmpty()) {
                            continue;
                        }
                        if (expectedItem.amount() != amount) {
                            final BedrockItem updated = expectedItem.copy();
                            if (slotInfo.itemNetId() > 0) {
                                updated.setNetId(slotInfo.itemNetId());
                            }
                            updated.setAmount(amount);
                            container.setItem(slot, updated);
                            changedContainers.add(container);
                        } else {
                            // Stack network IDs are invisible to Java. Updating only the ID must not rewind its prediction.
                            if (slotInfo.itemNetId() > 0) {
                                expectedItem.setNetId(slotInfo.itemNetId());
                            }
                        }
                    }
                }
            }
            inventoryRequestTracker.flushInventoryActions();
            for (Container container : changedContainers) {
                if (container == inventoryTracker.getHudContainer()) {
                    PacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
                } else {
                    PacketFactory.sendJavaContainerSetContent(wrapper.user(), container);
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_SET_DATA, ClientboundPackets26_3.CONTAINER_SET_DATA, wrapper -> {
            final byte containerId = wrapper.read(Types.BYTE);
            final int id = wrapper.read(BedrockTypes.VAR_INT);
            final int value = wrapper.read(BedrockTypes.VAR_INT);

            final Container container = wrapper.user().get(InventoryTracker.class).getContainerClientbound(containerId, null, null);
            if (container == null) {
                // TODO: This throws every time we open a container
                // Unknown container, ignore
                wrapper.cancel();
                ViaBedrock.getPlatform().getLogger().warning("Received ContainerSetData packet for unknown container: containerId=" + containerId + ", id=" + id + ", value=" + value);
                return;
            }
            final int windowId = container.javaContainerId();
            if (windowId == -1) {
                // Unknown container, ignore
                wrapper.cancel();
                ViaBedrock.getPlatform().getLogger().warning("Received ContainerSetData packet for unknown container: containerId=" + containerId + ", id=" + id + ", value=" + value);
                return;
            }

            final short javaId = container.translateContainerData(id);
            if (javaId == -1) {
                ViaBedrock.getPlatform().getLogger().warning("Received ContainerSetData packet with unknown id: containerId=" + containerId + ", id=" + id + ", value=" + value);
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, windowId);
            wrapper.write(Types.SHORT, javaId);
            wrapper.write(Types.SHORT, (short) value);
        });
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_ENCHANT_OPTIONS, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            if (!(inventoryTracker.getCurrentContainer() instanceof EnchantmentContainer)) {
                return;
            }
            final EnchantmentContainer enchantmentContainer = (EnchantmentContainer) inventoryTracker.getCurrentContainer();

            final int size = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size

            final List<EnchantData> data = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final byte cost = wrapper.read(Types.BYTE); // cost
                wrapper.read(BedrockTypes.INT_LE); // slot

                // TODO: How does bedrock decide on what to show
                Enchant_Type selected = null;
                int level = -1;

                final int l1 = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size
                for (int j = 0; j < l1; j++) {
                    final int id = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // enchant id
                    final byte lvl = wrapper.read(Types.BYTE); // enchant level

                    if (selected == null) {
                        selected = Enchant_Type.getByValue(id);
                        level = lvl;
                    }
                }

                final int l2 = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size
                for (int j = 0; j < l2; j++) {
                    final int id = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // enchant id
                    final byte lvl = wrapper.read(Types.BYTE); // enchant level

                    if (selected == null) {
                        selected = Enchant_Type.getByValue(id);
                        level = lvl;
                    }
                }

                final int l3 = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size
                for (int j = 0; j < l3; j++) {
                    final int id = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // enchant id
                    final byte lvl = wrapper.read(Types.BYTE); // enchant level

                    if (selected == null) {
                        selected = Enchant_Type.getByValue(id);
                        level = lvl;
                    }
                }

                wrapper.read(BedrockTypes.STRING); // Name
                final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // Enchant Net Id

                data.add(new EnchantData(cost, selected, level, netId));
            }

            enchantmentContainer.setEnchantData(data);
        });
    }

    private InventoryRequestPackets() {
    }

}
