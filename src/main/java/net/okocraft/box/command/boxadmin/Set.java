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

package net.okocraft.box.command.boxadmin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import net.okocraft.box.database.Items;
import net.okocraft.box.database.PlayerData;
import net.okocraft.box.util.OtherUtil;
import net.okocraft.box.util.PlayerUtil;
import org.jetbrains.annotations.NotNull;

class Set extends BaseSubAdminCommand {

    private static final String COMMAND_NAME = "set";
    private static final int LEAST_ARG_LENGTH = 3;
    private static final String USAGE = "/boxadmin set <player> <ITEM> <amount>";

    @Override
    public boolean runCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!validate(sender, args)) {
            return false;
        }
        OfflinePlayer player = PlayerUtil.getOfflinePlayer(args[1]);
        String itemName = args[2].toUpperCase();
        ItemStack item = Items.getItemStack(itemName);

        long amount = args.length < 4 ? 0 : OtherUtil.parseLongOrDefault(args[3], 0);

        PlayerData.setItemAmount(player, item, amount);

        sender.sendMessage(
                MESSAGE_CONFIG.getSuccessSet()
                        .replaceAll("%player%", player.getName())
                        .replaceAll("%item%", itemName)
                        .replaceAll("%amount%", String.valueOf(amount))
        );

        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, @NotNull String[] args) {
        List<String> result = new ArrayList<>();

        List<String> players = new ArrayList<>(PlayerData.getPlayers().values());

        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], players, result);
        }

        if (!players.contains(args[1].toLowerCase())) {
            return List.of();
        }

        List<String> items = new ArrayList<>(CONFIG.getAllItems());

        if (args.length == 3) {
            return StringUtil.copyPartialMatches(args[2], items, result);
        }

        if (!items.contains(args[2].toUpperCase())) {
            return List.of();
        }

        if (args.length == 4) {
            return StringUtil.copyPartialMatches(args[3], List.of("1", "10", "100", "1000", "10000"), result);
        }

        return result;
    }

    @NotNull
    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public int getLeastArgLength() {
        return LEAST_ARG_LENGTH;
    }

    @NotNull
    @Override
    public String getUsage() {
        return USAGE;
    }

    @Override
    public String getDescription() {
        return MESSAGE_CONFIG.getSetDesc();
    }

    @Override
    protected boolean validate(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!super.validate(sender, args)) {
            return false;
        }

        if (!PlayerUtil.existPlayer(sender, args[1].toLowerCase())) {
            sender.sendMessage(MESSAGE_CONFIG.getNoPlayerFound());
            return false;
        }

        if (!CONFIG.getAllItems().contains(args[2].toUpperCase())) {
            sender.sendMessage(MESSAGE_CONFIG.getNoItemFound());
            return false;
        }

        return true;
    }
}