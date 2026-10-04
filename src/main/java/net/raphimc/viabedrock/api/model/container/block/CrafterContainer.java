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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;

import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.BedrockProtocol;

import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;

public class CrafterContainer extends Container {

    private final boolean[] disabledSlots;

    public CrafterContainer(final UserConnection user, final byte containerId, final TextComponent title, final BlockPosition position) {
        super(user, containerId, ContainerType.CRAFTER, title, position, 9, CustomBlockTags.CRAFTER);

        this.disabledSlots = this.getCrafterMetadata();
        for (short i = 0; i < 9; i++) {
            final boolean disabled = this.disabledSlots[i];

            final PacketWrapper setData = PacketWrapper.create(ClientboundPackets26_3.CONTAINER_SET_DATA, user);
            setData.write(Types.VAR_INT, (int) this.javaContainerId());
            setData.write(Types.SHORT, i);
            setData.write(Types.SHORT, (short) (disabled ? 1 : 0));
            setData.scheduleSend(BedrockProtocol.class);
        }
    }

    @Override
    public FullContainerName getFullContainerName(final int slot) {
        return new FullContainerName(ContainerEnumName.AnvilInputContainer, null); // Bedrock moment, from testing they send AnvilInput
    }

    @Override
    protected boolean canPlaceItem(final int slot, final BedrockItem item) {
        return slot >= 0 && slot < 9 && !this.disabledSlots[slot] && super.canPlaceItem(slot, item);
    }

    public boolean setSlotEnabled(final int slot, final boolean enabled) {
        if (slot < 0 || slot >= 9 || !this.getItem(slot).isEmpty()) {
            return false;
        }
        this.disabledSlots[slot] = !enabled;
        return true;
    }

    public static boolean[] decodeDisabledSlots(final CompoundTag tag) {
        final boolean[] disabledSlots = new boolean[9];
        if (tag != null && tag.get("disabled_slots") instanceof NumberTag mask) {
            for (int i = 0; i < disabledSlots.length; i++) {
                disabledSlots[i] = (mask.asInt() & (1 << i)) != 0;
            }
        }
        return disabledSlots;
    }

    private boolean[] getCrafterMetadata() {
        final BedrockBlockEntity blockEntity = this.user.get(ChunkTracker.class).getBlockEntity(this.position);
        return decodeDisabledSlots(blockEntity != null ? blockEntity.tag() : null);
    }

}
