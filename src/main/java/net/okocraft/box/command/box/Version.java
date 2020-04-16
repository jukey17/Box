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

package net.okocraft.box.command.box;

import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;

import net.okocraft.box.Box;

class Version extends BoxSubCommand {

    Version() {
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        messages.sendMessage(sender, "command.box.version.info.format", Map.of("%version%", Box.getVersion()));
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    @Override
    public int getLeastArgLength() {
        return 1;
    }

    @Override
    public String getUsage() {
        return "/box version";
    }
}