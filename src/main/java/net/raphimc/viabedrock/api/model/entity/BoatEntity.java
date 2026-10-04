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
import net.raphimc.viabedrock.protocol.model.Position3f;

import java.util.UUID;

public final class BoatEntity extends Entity {

    private Position3f previousInputPosition;
    private boolean leftPaddle;
    private boolean rightPaddle;

    public BoatEntity(final UserConnection user, final long uniqueId, final long runtimeId, final String type, final int javaId, final UUID javaUuid, final EntityTypes26_3 javaType) {
        super(user, uniqueId, runtimeId, type, javaId, javaUuid, javaType);
    }

    @Override
    public float eyeOffset() {
        return 0.375F;
    }

    @Override
    public float javaYaw() {
        return this.rotation.y() - 90F;
    }

    @Override
    public void setPosition(final Position3f position) {
        super.setPosition(position);
        if (this.previousInputPosition == null) {
            this.previousInputPosition = position;
        }
    }

    public Position3f inputDelta() {
        final Position3f delta = this.position.subtract(this.previousInputPosition);
        this.previousInputPosition = this.position;
        return delta;
    }

    public void resetInputPrediction() {
        this.previousInputPosition = this.position;
        this.leftPaddle = false;
        this.rightPaddle = false;
    }

    public void setPaddles(final boolean left, final boolean right) {
        this.leftPaddle = left;
        this.rightPaddle = right;
    }

    public boolean leftPaddle() {
        return this.leftPaddle;
    }

    public boolean rightPaddle() {
        return this.rightPaddle;
    }

}
