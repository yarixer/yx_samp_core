package org.yanix.yx_samp_core.impl;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.yanix.yx_samp_core.api.CoreRolesApi;
import org.yanix.yx_samp_core.util.GroupParsing;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class LpCoreRolesService implements CoreRolesApi {

    private final LuckPerms lp;

    // паттерны групп
    private static final Pattern ORG_PATTERN = GroupParsing.compileEmployment("org"); // org.<org>.<dept>.rank.<01-10>
    private static final Pattern JOB_PATTERN = GroupParsing.compileEmployment("job"); // job.<job>.<dept>.rank.<01-10>
    private static final Pattern STAFF_PATTERN = Pattern.compile("^staff\\.[a-z0-9_]+$", Pattern.CASE_INSENSITIVE);

    public LpCoreRolesService(LuckPerms lp) {
        this.lp = lp;
    }

    // -------------------- Helpers --------------------

    private CompletableFuture<User> loadUser(UUID uuid) {
        var cached = lp.getUserManager().getUser(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return lp.getUserManager().loadUser(uuid);
    }

    private CompletableFuture<Boolean> saveUser(User user) {
        return lp.getUserManager().saveUser(user).thenApply(res -> true);
    }

    private QueryOptions queryOptions(User user) {
        // контекст игрока, если доступен; иначе статический
        return lp.getContextManager().getQueryOptions(user)
                .orElse(lp.getContextManager().getStaticQueryOptions());
    }

    private static Optional<InheritanceNode> findFirstMatchingGroup(User user, Pattern pattern) {
        return user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .filter(n -> pattern.matcher(n.getGroupName()).matches())
                .findFirst();
    }

    private static List<InheritanceNode> findAllMatchingGroups(User user, Pattern pattern) {
        return user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .filter(n -> pattern.matcher(n.getGroupName()).matches())
                .toList();
    }

    private static Optional<InheritanceNode> findEmploymentGroup(User user, String type /*org|job*/, String dept) {
        Pattern p = GroupParsing.compileEmployment(type, dept);
        return findFirstMatchingGroup(user, p);
    }

    private static Optional<InheritanceNode> findAnyJob(User user) {
        return findFirstMatchingGroup(user, JOB_PATTERN);
    }

    private static int parseRank(String groupName) {
        return GroupParsing.parseRank(groupName).orElse(0);
    }

    private static String buildEmploymentGroup(String type, String name, String dept, int rank) {
        return "%s.%s.%s.rank.%s".formatted(
                type,
                name.toLowerCase(Locale.ROOT),
                dept.toLowerCase(Locale.ROOT),
                String.format("%02d", rank)
        );
    }

    // -------------------- API --------------------

    @Override
    public CompletableFuture<Boolean> getHasJob(UUID uuid) {
        return loadUser(uuid).thenApply(user -> findAnyJob(user).isPresent());
    }

    @Override
    public CompletableFuture<String> getNameJob(UUID uuid) {
        return loadUser(uuid).thenApply(user ->
                findAnyJob(user)
                        .map(n -> GroupParsing.extractEmploymentName(n.getGroupName()).orElse(""))
                        .orElse("")
        );
    }

    @Override
    public CompletableFuture<Integer> getRankJob(UUID uuid) {
        return loadUser(uuid).thenApply(user ->
                findAnyJob(user).map(n -> parseRank(n.getGroupName())).orElse(0));
    }

    @Override
    public CompletableFuture<Boolean> doFireFrom(UUID uuid, String type, String dept) {
        String normalizedType = GroupParsing.normalizeType(type);
        if (!normalizedType.equals("org") && !normalizedType.equals("job")) {
            return CompletableFuture.completedFuture(false);
        }
        return loadUser(uuid).thenCompose(user -> {
            var toRemove = findAllMatchingGroups(user, GroupParsing.compileEmployment(normalizedType, dept));
            if (toRemove.isEmpty()) return CompletableFuture.completedFuture(false);
            toRemove.forEach(node -> user.data().remove(node));
            return saveUser(user);
        });
    }

    @Override
    public CompletableFuture<Boolean> doRankUp(UUID uuid, String type, String dept) {
        return adjustRank(uuid, type, dept, +1);
    }

    @Override
    public CompletableFuture<Boolean> doRankDown(UUID uuid, String type, String dept) {
        return adjustRank(uuid, type, dept, -1);
    }

    private CompletableFuture<Boolean> adjustRank(UUID uuid, String type, String dept, int delta) {
        String normalizedType = GroupParsing.normalizeType(type);
        if (!normalizedType.equals("org") && !normalizedType.equals("job")) {
            return CompletableFuture.completedFuture(false);
        }
        return loadUser(uuid).thenCompose(user -> {
            var currentOpt = findEmploymentGroup(user, normalizedType, dept);
            if (currentOpt.isEmpty()) return CompletableFuture.completedFuture(false);

            var current = currentOpt.get();
            int rank = parseRank(current.getGroupName());
            if (rank == 0) return CompletableFuture.completedFuture(false);

            int next = rank + delta;
            if (next < 1 || next > 10) return CompletableFuture.completedFuture(false);

            // извлечь имя (orgName|jobName) из текущей группы
            String name = GroupParsing.extractEmploymentName(current.getGroupName()).orElseThrow();
            String nextGroup = buildEmploymentGroup(normalizedType, name, dept, next);

            // заменить ноду
            user.data().remove(current);
            user.data().add(InheritanceNode.builder(nextGroup).build());
            return saveUser(user);
        });
    }

    @Override
    public CompletableFuture<Boolean> getHasGroup(UUID uuid, String groupName) {
        String g = groupName.toLowerCase(Locale.ROOT);
        return loadUser(uuid).thenApply(user ->
                user.getNodes().stream()
                        .filter(NodeType.INHERITANCE::matches)
                        .map(NodeType.INHERITANCE::cast)
                        .anyMatch(n -> n.getGroupName().equalsIgnoreCase(g))
        );
    }

    @Override
    public CompletableFuture<Boolean> getHasPermission(UUID uuid, String permission) {
        // если игрок онлайн — используем Bukkit (пермишены уже проксированы LuckPerms’ом)
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return CompletableFuture.completedFuture(online.hasPermission(permission));
        }
        // оффлайн — проверяем через LuckPerms cache
        return loadUser(uuid).thenApply(user ->
                user.getCachedData()
                        .getPermissionData(queryOptions(user))
                        .checkPermission(permission)
                        .asBoolean()
        );
    }

    @Override
    public CompletableFuture<Boolean> setGiveGroup(UUID uuid, String groupName) {
        String g = groupName.toLowerCase(Locale.ROOT);

        return loadUser(uuid).thenCompose(user -> {
            // Сохранение инвариантов:
            // - одновременно только один job.*.rank.* и один org.*.rank.* (по вашим правилам)
            if (JOB_PATTERN.matcher(g).matches()) {
                // удалить ВСЕ прочие job.* у игрока
                findAllMatchingGroups(user, JOB_PATTERN).forEach(user.data()::remove);
            } else if (ORG_PATTERN.matcher(g).matches()) {
                // удалить ВСЕ прочие org.* у игрока
                findAllMatchingGroups(user, ORG_PATTERN).forEach(user.data()::remove);
            }
            user.data().add(InheritanceNode.builder(g).build());
            return saveUser(user);
        });
    }

    @Override
    public CompletableFuture<Boolean> setGivePermission(UUID uuid, String permission) {
        return loadUser(uuid).thenCompose(user -> {
            Node node = PermissionNode.builder(permission).value(true).build();
            user.data().add(node);
            return saveUser(user);
        });
    }

    @Override
    public CompletableFuture<Boolean> getIsStaff(UUID uuid) {
        return loadUser(uuid).thenApply(user ->
                user.getNodes().stream()
                        .filter(NodeType.INHERITANCE::matches)
                        .map(NodeType.INHERITANCE::cast)
                        .anyMatch(n -> STAFF_PATTERN.matcher(n.getGroupName()).matches())
        );
    }
}
