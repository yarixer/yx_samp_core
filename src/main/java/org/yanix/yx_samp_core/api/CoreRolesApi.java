package org.yanix.yx_samp_core.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

public interface CoreRolesApi {
    record DepartmentInfo(String dept, int rank, String groupName) {}

    // --- JOB ---
    CompletableFuture<Boolean> getHasJob(UUID uuid);    // return true/false
    CompletableFuture<String>  getNameJob(UUID uuid);   // <профессия> ( return например "mechanic")
    CompletableFuture<Integer> getRankJob(UUID uuid);   // return 1..10, 0 если нет

    // --- FIRE / RANK ---
    CompletableFuture<Boolean> doFireFrom(UUID uuid, String type, String dept);      // type: "org"|"job"
    CompletableFuture<Boolean> doRankUp(UUID uuid, String type, String dept);        // api.doRankUp(uuid, "org", "lspd") +1, максимум 10
    CompletableFuture<Boolean> doRankDown(UUID uuid, String type, String dept);      // api.doRankUp(uuid, "org", "lspd") -1, минимум 1

    // --- GROUP / PERMISSION ---
    CompletableFuture<Boolean> getHasGroup(UUID uuid, String groupName);
    CompletableFuture<Boolean> getHasPermission(UUID uuid, String permission);
    CompletableFuture<Boolean> setGiveGroup(UUID uuid, String groupName);            // setGiveGroup(uuid, "org.police.lspd.rank.03")
    CompletableFuture<Boolean> setGivePermission(UUID uuid, String permission);      // setGivePermission(uuid, "yx.perm.work")

    // --- STAFF ---
    CompletableFuture<Boolean> getIsStaff(UUID uuid);

    CompletableFuture<Optional<DepartmentInfo>> getDepartment(UUID uuid, String orgname);
    CompletableFuture<Boolean> joinDepartment(UUID uuid, String orgname, String dept, int rank);
}
