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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountWorldListTest {

    @Test
    void switchingAccountsDiscardsOldWorldsAndLateRequests() {
        final AccountWorldList<String> list = new AccountWorldList<>();
        final Object first = new Object();
        final Object second = new Object();
        final CompletableFuture<List<String>> stale = new CompletableFuture<>();
        final CompletableFuture<List<String>> current = new CompletableFuture<>();
        list.refresh(first, 1, 30, () -> stale, Runnable::run, () -> {});
        list.refresh(second, 2, 30, () -> current, Runnable::run, () -> {});
        stale.complete(List.of("first account's private world"));
        assertTrue(list.worlds().isEmpty());
        assertTrue(list.loading());
        current.complete(List.of("second account's world"));
        assertEquals(List.of("second account's world"), list.worlds());
        assertFalse(list.loading());
    }

    @Test
    void logoutInvalidatesPendingDiscovery() {
        final AccountWorldList<String> list = new AccountWorldList<>();
        final CompletableFuture<List<String>> pending = new CompletableFuture<>();
        list.refresh(new Object(), 1, 30, () -> pending, Runnable::run, () -> {});
        list.refresh(null, 2, 30, () -> { throw new AssertionError("logged out"); }, Runnable::run, () -> {});
        pending.complete(List.of("private world"));
        assertTrue(list.worlds().isEmpty());
        assertFalse(list.loading());
    }

    @Test
    void failedRefreshPreservesSnapshotAndDoesNotRetryEveryFrame() {
        final AccountWorldList<String> list = new AccountWorldList<>();
        final Object account = new Object();
        list.refresh(account, 1, 30, () -> CompletableFuture.completedFuture(List.of("world")), Runnable::run, () -> {});
        list.refresh(account, 31, 30, () -> CompletableFuture.failedFuture(new IllegalStateException("offline")), Runnable::run, () -> {});
        assertEquals(List.of("world"), list.worlds());
        assertNotNull(list.error());
        list.refresh(account, 32, 30, () -> { throw new AssertionError("rate limit"); }, Runnable::run, () -> {});
        list.refresh(account, 61, 30, () -> CompletableFuture.completedFuture(List.of()), Runnable::run, () -> {});
        assertTrue(list.worlds().isEmpty());
        assertNull(list.error());
    }
}
