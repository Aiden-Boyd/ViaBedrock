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
package net.raphimc.viabedrock.protocol.types.inventory;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackResponseContainerInfo;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackResponseInfo;
import net.raphimc.viabedrock.protocol.types.InventoryTypes;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ItemStackNetResult;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.List;

public class ItemStackResponseType extends Type<ItemStackResponseInfo> {

    public ItemStackResponseType() {
        super(ItemStackResponseInfo.class);
    }

    @Override
    public ItemStackResponseInfo read(final ByteBuf buffer) {
        final ItemStackNetResult result = ItemStackNetResult.getByValue(buffer.readByte());
        final int requestId = BedrockTypes.VAR_INT.read(buffer);

        final List<ItemStackResponseContainerInfo> containers = buffer.readBoolean()
                ? List.of(InventoryTypes.ITEM_STACK_RESPONSE_CONTAINERS.read(buffer)) : List.of();

        return new ItemStackResponseInfo(result, requestId, containers);
    }

    @Override
    public void write(final ByteBuf buffer, final ItemStackResponseInfo value) {
        buffer.writeByte(value.result().getValue());
        BedrockTypes.VAR_INT.write(buffer, value.requestId());

        final boolean hasContainers = value.containers() != null && !value.containers().isEmpty();
        buffer.writeBoolean(hasContainers);
        if (hasContainers) {
            InventoryTypes.ITEM_STACK_RESPONSE_CONTAINERS.write(buffer, value.containers().toArray(new ItemStackResponseContainerInfo[0]));
        }
    }

}
