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
package net.raphimc.viabedrock.protocol.types.recipe;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

import net.raphimc.viabedrock.protocol.model.recipe.ItemDescriptor;
import net.raphimc.viabedrock.protocol.model.recipe.ItemDescriptorType;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class NetworkItemDescriptorType extends Type<ItemDescriptor> {

    public NetworkItemDescriptorType() {
        super(ItemDescriptor.class);
    }

    @Override
    public ItemDescriptor read(final ByteBuf buffer) {
        final int variant = BedrockTypes.UNSIGNED_VAR_INT.read(buffer);
        final String kind = variant == 0 ? "empty" : BedrockTypes.STRING.read(buffer);
        final ItemDescriptor result = switch (kind) {
            case "empty" -> {
                BedrockTypes.VAR_INT.read(buffer); // Empty descriptor aux value
                yield new ItemDescriptor.InvalidDescriptor();
            }
            case "name" -> new ItemDescriptor.DeferredDescriptor(BedrockTypes.STRING.read(buffer), BedrockTypes.VAR_INT.read(buffer));
            case "item_tag" -> {
                final String tag = BedrockTypes.STRING.read(buffer);
                BedrockTypes.VAR_INT.read(buffer); // Tag aux value
                yield new ItemDescriptor.ItemTagDescriptor(tag);
            }
            case "molang" -> new ItemDescriptor.MolangDescriptor(BedrockTypes.STRING.read(buffer), buffer.readUnsignedShortLE());
            default -> throw new IllegalArgumentException("Unknown ingredient descriptor: " + kind);
        };

        final int amount = BedrockTypes.VAR_INT.read(buffer);

        return result.withAmount(amount);
    }

    @Override
    public void write(final ByteBuf buffer, final ItemDescriptor value) {
        BedrockTypes.UNSIGNED_VAR_INT.write(buffer, value.getType() == ItemDescriptorType.INVALID ? 0 : 1);
        switch (value.getType()) {
            case INVALID -> BedrockTypes.VAR_INT.write(buffer, (int) Short.MAX_VALUE);
            case DEFERRED -> {
                final ItemDescriptor.DeferredDescriptor descriptor = (ItemDescriptor.DeferredDescriptor) value;
                BedrockTypes.STRING.write(buffer, "name");
                BedrockTypes.STRING.write(buffer, descriptor.fullName());
                BedrockTypes.VAR_INT.write(buffer, descriptor.auxValue());
            }
            case ITEM_TAG -> {
                final ItemDescriptor.ItemTagDescriptor descriptor = (ItemDescriptor.ItemTagDescriptor) value;
                BedrockTypes.STRING.write(buffer, "item_tag");
                BedrockTypes.STRING.write(buffer, descriptor.itemTag());
                BedrockTypes.VAR_INT.write(buffer, (int) Short.MAX_VALUE);
            }
            case MOLANG -> {
                final ItemDescriptor.MolangDescriptor descriptor = (ItemDescriptor.MolangDescriptor) value;
                BedrockTypes.STRING.write(buffer, "molang");
                BedrockTypes.STRING.write(buffer, descriptor.tagExpression());
                buffer.writeShortLE(descriptor.molangVersion());
            }
            default -> throw new UnsupportedOperationException("Ingredient requires a name: " + value.getType());
        }
        BedrockTypes.VAR_INT.write(buffer, value.amount());
    }

}
