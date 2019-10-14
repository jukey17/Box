/*
 * Box
 * Copyright (C) 2019 OKOCRAFT
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.okocraft.box;

import net.okocraft.box.util.CraftRecipes;
import net.okocraft.box.worldedit.WorldEditEventListener;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import net.okocraft.box.command.boxadmin.BoxAdmin;
import net.okocraft.box.config.Config;
import net.okocraft.box.database.PlayerData;
import net.okocraft.box.database.Sqlite;
import net.okocraft.box.gui.CategorySelectorGUI;
import net.okocraft.box.listeners.BoxStick;
import net.okocraft.box.listeners.EntityPickupItem;
import net.okocraft.box.listeners.Replant;
import org.jetbrains.annotations.Nullable;

/**
 * @author OKOCRAFT
 */
public class Box extends JavaPlugin {
    /**
     * プラグイン Box のインスタンス。
     */
    @Nullable
    private static Box instance;

    /**
     * 経済
     */
    private Economy economy;

    @Override
    public void onEnable() {
        Config.reloadAllConfigs();

        if (!setupEconomy()) {
            getLogger().severe("Box failed to setup economy.");
        }

        registerEvents();

        // Load static class.
        net.okocraft.box.command.box.Box.load();
        BoxAdmin.load();

        PlayerData.loadOnlinePlayersData();

        // Load static class CraftRecipes
        CraftRecipes.load();

        // GO GO GO
        getLogger().info(String.format("Box v%s has been enabled!", getVersion()));
    }

    @Override
    public void onDisable() {

        PlayerData.saveOnlinePlayersData();
        Sqlite.disconnect();

        unregisterEvents();
        Bukkit.getScheduler().cancelTasks(this);

        getLogger().info(String.format("Box v%s has been disabled!", getVersion()));
    }

    /**
     * このクラスのインスタンスを返す。
     *
     * @return インスタンス
     */
    @Nullable
    public static Box getInstance() {
        if (instance == null) {
            instance = (Box) Bukkit.getPluginManager().getPlugin("Box");
        }

        return instance;
    }

    public static String getVersion() {
        return Box.class.getPackage().getImplementationVersion();
    }

    /**
     * イベントを Bukkit サーバに登録する。
     */
    public void registerEvents() {
        unregisterEvents();

        // Events should be registered in its own initializer
        new PlayerData(this);
        if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            WorldEditEventListener.register();
        }
        new EntityPickupItem(this);
        new BoxStick();
        new Replant();

        // GUI
        CategorySelectorGUI.restartListener();

        getLogger().info("Events have been registered.");
    }

    /**
     * 登録したイベントを Bukkit サーバから削除する。
     */
    private void unregisterEvents() {
        HandlerList.unregisterAll(this);
        if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            WorldEditEventListener.unregister();
        }
    }

    /**
     * economyをセットする。
     *
     * @return 成功したらtrue 失敗したらfalse
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault was not found.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    /**
     * @return the economy
     */
    public Economy getEconomy() {
        return economy;
    }
}
