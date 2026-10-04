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
package net.raphimc.viabedrock.api.model.entity;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.PlayerActionPacketFactory;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ActorDataIds;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ActorFlags;
import net.raphimc.viabedrock.protocol.data.generated.java.EntityDataFields;
import net.raphimc.viabedrock.protocol.model.Position3f;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import net.raphimc.viabedrock.protocol.types.entitydata.EntityDataTypesBedrock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityTranslationTest {

    private Object previousDataTypes;
    private Object previousJavaFields;

    @BeforeAll
    static void initializeVersionedTypes() throws ClassNotFoundException {
        // Match ViaVersion's initialization order: the legacy keys initialize VersionedTypes.
        Class.forName("com.viaversion.viaversion.api.minecraft.data.StructuredDataKey");
    }

    @BeforeEach
    void setUp() throws Exception {
        this.previousDataTypes = field("bedrockEntityDataTypes").get(BedrockProtocol.MAPPINGS);
        this.previousJavaFields = field("javaEntityDataFields").get(BedrockProtocol.MAPPINGS);
        field("bedrockEntityDataTypes").set(BedrockProtocol.MAPPINGS, new EnumMap<>(ActorDataIds.class));
        final EnumMap<EntityTypes26_3, List<String>> fields = new EnumMap<>(EntityTypes26_3.class);
        fields.put(EntityTypes26_3.SKELETON, List.of(EntityDataFields.SHARED_FLAGS, EntityDataFields.SILENT,
            EntityDataFields.NO_GRAVITY, EntityDataFields.LIVING_ENTITY_FLAGS, EntityDataFields.MOB_FLAGS));
        fields.put(EntityTypes26_3.OAK_BOAT, List.of(EntityDataFields.PADDLE_LEFT, EntityDataFields.PADDLE_RIGHT));
        field("javaEntityDataFields").set(BedrockProtocol.MAPPINGS, fields);
    }

    @AfterEach
    void tearDown() throws Exception {
        field("bedrockEntityDataTypes").set(BedrockProtocol.MAPPINGS, this.previousDataTypes);
        field("javaEntityDataFields").set(BedrockProtocol.MAPPINGS, this.previousJavaFields);
    }

    @Test
    void signedFlagWordsDoNotInventFlagsInTheOtherWord() {
        final Entity entity = skeleton();
        entity.entityData().put(ActorDataIds.RESERVED_0, data(ActorDataIds.RESERVED_0, Long.MIN_VALUE));
        assertEquals(java.util.Set.of(ActorFlags.EATING), entity.entityFlags());
        entity.entityData().put(ActorDataIds.RESERVED_092, data(ActorDataIds.RESERVED_092, 1L << 24));
        assertTrue(entity.entityFlags().contains(ActorFlags.FACING_TARGET_TO_RANGE_ATTACK));
        assertEquals(2, entity.entityFlags().size());
        entity.entityData().put(ActorDataIds.RESERVED_092, data(ActorDataIds.RESERVED_092, Long.MIN_VALUE));
        assertEquals(java.util.Set.of(ActorFlags.EATING, ActorFlags.USES_LEGACY_FRICTION), entity.entityFlags());
    }

    @Test
    void skeletonRaisesBowAndClearsPoseWithoutDuplicateMetadataIds() {
        final Entity entity = skeleton();
        final List<EntityData> output = new ArrayList<>();
        entity.updateEntityData(new EntityData[]{data(ActorDataIds.RESERVED_092, 1L << 24),
            data(ActorDataIds.RESERVED_0, (1L << 16) | (1L << 4))}, output);
        assertEquals((byte) 5, value(output, 4));
        assertEquals((byte) 1, value(output, 3));
        assertEquals(output.size(), output.stream().map(EntityData::id).distinct().count());
        output.clear();
        entity.updateEntityData(new EntityData[]{data(ActorDataIds.RESERVED_0, 0L), data(ActorDataIds.RESERVED_092, 0L)}, output);
        assertEquals((byte) 0, value(output, 4));
        assertEquals((byte) 0, value(output, 3));
        output.clear();
        entity.updateEntityData(new EntityData[]{data(ActorDataIds.TARGET, 123L)}, output);
        assertEquals((byte) 4, value(output, 4));
        output.clear();
        entity.updateEntityData(new EntityData[]{data(ActorDataIds.TARGET, 0L)}, output);
        assertEquals((byte) 0, value(output, 4));
    }

    @Test
    void driverIsFirstEvenWhenPassengerLinkArrivesFirst() {
        final BoatEntity boat = boat();
        boat.addPassenger(200, false);
        boat.addPassenger(100, true);
        assertEquals(List.of(100L, 200L), boat.passengers());
        boat.addPassenger(100, true);
        assertEquals(List.of(100L, 200L), boat.passengers());
        boat.addPassenger(300, true);
        assertEquals(List.of(300L, 100L, 200L), boat.passengers());
        boat.removePassenger(300);
        assertFalse(boat.isDriver(300));
        assertFalse(boat.isDriver(100));
    }

    @Test
    void boatPositionRotationAndPredictionDeltaUseBoatCoordinates() throws Exception {
        final BoatEntity boat = boat();
        boat.setPosition(new Position3f(1F, 64.375F, 3F));
        boat.setRotation(new Position3f(2F, 90F, 90F));
        assertEquals(Position3f.ZERO, boat.inputDelta());
        boat.setPosition(new Position3f(2F, 64.375F, 4F));
        assertEquals(new Position3f(1F, 0F, 1F), boat.inputDelta());
        assertEquals(Position3f.ZERO, boat.inputDelta());
        boat.setPosition(new Position3f(10F, 64.375F, 10F));
        boat.resetInputPrediction();
        assertEquals(Position3f.ZERO, boat.inputDelta());
        final PacketWrapper wrapper = wrapper();
        PlayerActionPacketFactory.writeJavaVehicleMove(wrapper, boat);
        final ByteBuf buffer = Unpooled.buffer();
        try {
            wrapper.writeToBuffer(buffer);
            BedrockTypes.UNSIGNED_VAR_INT.read(buffer);
            assertEquals(10D, buffer.readDouble());
            assertEquals(64D, buffer.readDouble());
            assertEquals(10D, buffer.readDouble());
            assertEquals(0F, buffer.readFloat());
            assertEquals(2F, buffer.readFloat());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void predictedVehicleUsesTwoPresenceFlagsAndSignedUniqueId() throws Exception {
        final BoatEntity boat = boat();
        boat.setRotation(new Position3f(2F, 90F, 90F));
        for (BoatEntity vehicle : new BoatEntity[]{null, boat}) {
            final PacketWrapper wrapper = wrapper();
            PlayerActionPacketFactory.writePredictedVehicle(wrapper, vehicle);
            wrapper.write(Types.INT, 0x12345678);
            final ByteBuf buffer = Unpooled.buffer();
            try {
                wrapper.writeToBuffer(buffer);
                BedrockTypes.UNSIGNED_VAR_INT.read(buffer);
                assertEquals(vehicle != null, buffer.readBoolean());
                if (vehicle != null) {
                    assertEquals(2F, buffer.readFloatLE());
                    assertEquals(90F, buffer.readFloatLE());
                }
                assertEquals(vehicle != null, buffer.readBoolean());
                if (vehicle != null) {
                    assertEquals(81, buffer.readUnsignedByte()); // -41 as signed zigzag VarLong, not the runtime id
                }
                assertEquals(0x12345678, buffer.readInt());
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void remoteBoatPaddlesStartAndStop() {
        final BoatEntity boat = boat();
        final List<EntityData> output = new ArrayList<>();
        boat.updateEntityData(new EntityData[]{new EntityData(ActorDataIds.ROW_TIME_LEFT.getValue(), EntityDataTypesBedrock.FLOAT, 0.4F)}, output);
        assertEquals(true, value(output, 0));
        output.clear();
        boat.updateEntityData(new EntityData[]{new EntityData(ActorDataIds.ROW_TIME_LEFT.getValue(), EntityDataTypesBedrock.FLOAT, 0F)}, output);
        assertEquals(false, value(output, 0));
    }

    private static PacketWrapper wrapper() {
        return new PacketWrapperImpl(ServerboundBedrockPackets.PLAYER_AUTH_INPUT, null, null);
    }

    private static EntityData data(final ActorDataIds id, final long value) {
        return new EntityData(id.getValue(), EntityDataTypesBedrock.LONG, value);
    }

    private static Object value(final List<EntityData> output, final int id) {
        return output.stream().filter(data -> data.id() == id).findFirst().orElseThrow().getValue();
    }

    private static Entity skeleton() {
        return new Entity(user(), 123, 456, "minecraft:skeleton", 2, UUID.randomUUID(), EntityTypes26_3.SKELETON);
    }

    private static BoatEntity boat() {
        return new BoatEntity(user(), -41, 999, "minecraft:boat", 3, UUID.randomUUID(), EntityTypes26_3.OAK_BOAT);
    }

    private static UserConnection user() {
        return (UserConnection) Proxy.newProxyInstance(UserConnection.class.getClassLoader(), new Class<?>[]{UserConnection.class},
            (proxy, method, args) -> null);
    }

    private static Field field(final String name) throws Exception {
        final Field field = BedrockProtocol.MAPPINGS.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

}
