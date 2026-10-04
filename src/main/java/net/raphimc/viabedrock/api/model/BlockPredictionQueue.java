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
package net.raphimc.viabedrock.api.model;

import com.viaversion.viaversion.api.minecraft.BlockPosition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

public final class BlockPredictionQueue {

    private static final long TIMEOUT_NANOS = 1_000_000_000L;
    private static final long SETTLE_NANOS = 50_000_000L;
    private final Deque<Prediction> pending = new ArrayDeque<>();

    public void add(final int sequence, final Set<BlockPosition> positions, final long now) {
        this.pending.addLast(new Prediction(sequence, Set.copyOf(positions), now + TIMEOUT_NANOS, now + TIMEOUT_NANOS));
    }

    public void addConfirmed(final int sequence, final long now) {
        this.pending.addLast(new Prediction(sequence, Set.of(), now, now));
    }

    public boolean isEmpty() {
        return this.pending.isEmpty();
    }

    public void blockUpdated(final BlockPosition position, final long now) {
        for (Prediction prediction : this.pending) {
            if (prediction.positions.contains(position)) {
                prediction.readyAt = Math.min(prediction.deadline, now + SETTLE_NANOS);
                return;
            }
        }
    }

    public List<Prediction> pollReady(final long now) {
        final List<Prediction> ready = new ArrayList<>();
        while (!this.pending.isEmpty() && now >= this.pending.peekFirst().readyAt) {
            ready.add(this.pending.removeFirst());
        }
        return ready;
    }

    public static final class Prediction {
        private final int sequence;
        private final Set<BlockPosition> positions;
        private final long deadline;
        private long readyAt;

        private Prediction(final int sequence, final Set<BlockPosition> positions, final long deadline, final long readyAt) {
            this.sequence = sequence;
            this.positions = positions;
            this.deadline = deadline;
            this.readyAt = readyAt;
        }

        public int sequence() {
            return this.sequence;
        }

        public Set<BlockPosition> positions() {
            return this.positions;
        }
    }

}
