package org.yanix.yx_samp_core.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CoreRolesApi {

    // --- JOB ---
    CompletableFuture<Boolean> getHasJob(UUID uuid);
    CompletableFuture<String>  getNameJob(UUID uuid);   // <профессия> (например "mechanic")
    CompletableFuture<Integer> getRankJob(UUID uuid);   // 1..10, 0 если нет

    // --- FIRE / RANK ---
    CompletableFuture<Boolean> doFireFrom(UUID uuid, String type, String dept);      // type: "org"|"job"
    CompletableFuture<Boolean> doRankUp(UUID uuid, String type, String dept);        // +1, максимум 10
    CompletableFuture<Boolean> doRankDown(UUID uuid, String type, String dept);      // -1, минимум 1

    // --- GROUP / PERMISSION ---
    CompletableFuture<Boolean> getHasGroup(UUID uuid, String groupName);
    CompletableFuture<Boolean> getHasPermission(UUID uuid, String permission);
    CompletableFuture<Boolean> setGiveGroup(UUID uuid, String groupName);            // добавляет, соблюдая инварианты
    CompletableFuture<Boolean> setGivePermission(UUID uuid, String permission);      // true

    // --- STAFF ---
    CompletableFuture<Boolean> getIsStaff(UUID uuid);
}
