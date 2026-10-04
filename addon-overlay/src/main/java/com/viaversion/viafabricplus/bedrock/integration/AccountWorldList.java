/*
 * This file is part of ViaFabricPlus Bedrock - https://github.com/florianreuth/viafabricplus-bedrock
 * Copyright (C) 2021-2026 the original authors
 *                         - Florian Reuth <git@florianreuth.de>
 *                         - RK_01/RaphiMC
 * Copyright (C) 2023-2026 ViaVersion and contributors
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

package com.viaversion.viafabricplus.bedrock.integration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/** A menu-owned discovery snapshot. All access runs on the client executor. */
public final class AccountWorldList<T> {

    private Object account;
    private long generation;
    private long nextRefresh = Long.MIN_VALUE;
    private boolean loading;
    private List<T> worlds = List.of();
    private Throwable error;

    public void refresh(final Object account, final long now, final long interval,
                        final Supplier<CompletableFuture<List<T>>> request, final Executor client,
                        final Runnable changed) {
        if (this.account != account) {
            this.account = account;
            this.generation++;
            this.loading = false;
            this.worlds = List.of();
            this.error = null;
            this.nextRefresh = Long.MIN_VALUE;
            changed.run();
        }
        if (account == null || this.loading || now < this.nextRefresh) {
            return;
        }
        this.loading = true;
        this.nextRefresh = now + interval;
        final long generation = this.generation;
        changed.run();
        final CompletableFuture<List<T>> future;
        try {
            future = request.get();
        } catch (Exception error) {
            this.loading = false;
            this.error = error;
            changed.run();
            return;
        }
        future.whenCompleteAsync((worlds, error) -> {
            if (this.generation != generation || this.account != account) {
                return;
            }
            this.loading = false;
            this.error = error;
            if (error == null) {
                this.worlds = List.copyOf(worlds);
            }
            changed.run();
        }, client);
    }

    public List<T> worlds() {
        return this.worlds;
    }

    public boolean loading() {
        return this.loading;
    }

    public Throwable error() {
        return this.error;
    }
}
