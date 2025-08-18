package org.yanix.yx_samp_core;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.yanix.yx_samp_core.api.CoreRolesApi;
import org.yanix.yx_samp_core.impl.LpCoreRolesService;

public final class Yx_samp_core extends JavaPlugin {

    private CoreRolesApi api;

    @Override
    public void onEnable() {
        // Получаем LuckPerms через Services API
        LuckPerms lp = Bukkit.getServicesManager().load(LuckPerms.class);
        if (lp == null) {
            getLogger().severe("LuckPerms не найден. Останов.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.api = new LpCoreRolesService(lp);

        // Экспортируем API как сервис, чтобы другие плагины могли получить его через ServicesManager
        Bukkit.getServicesManager().register(CoreRolesApi.class, api, this, ServicePriority.Normal);

        getLogger().info("CorePlugin загружен. API зарегистрирован.");
    }

    @Override
    public void onDisable() {
        // отписка от сервиса не обязательна — Bukkit сам при выгрузке снимет регистрацию
    }

    public CoreRolesApi getApi() {
        return api;
    }
}
