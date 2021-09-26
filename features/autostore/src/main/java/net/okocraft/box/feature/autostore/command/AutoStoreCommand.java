package net.okocraft.box.feature.autostore.command;

import net.kyori.adventure.text.Component;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.api.command.AbstractCommand;
import net.okocraft.box.api.message.GeneralMessage;
import net.okocraft.box.feature.autostore.event.AutoStoreSettingChangeEvent;
import net.okocraft.box.feature.autostore.message.AutoStoreMessage;
import net.okocraft.box.feature.autostore.model.AutoStoreSettingContainer;
import net.okocraft.box.feature.autostore.model.setting.AutoStoreSetting;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoStoreCommand extends AbstractCommand {

    public AutoStoreCommand() {
        super("autostore", "box.command.autostore", Set.of("a", "as"));
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GeneralMessage.ERROR_COMMAND_ONLY_PLAYER);
            return;
        }

        var setting = AutoStoreSettingContainer.INSTANCE.get(player);

        Boolean toggleAutoStore;

        if (args.length < 2) {
            toggleAutoStore = !setting.isEnabled();
        } else {
            toggleAutoStore = getBoolean(args[1]);
        }

        if (toggleAutoStore != null) {
            setting.setEnabled(toggleAutoStore);
            player.sendMessage(AutoStoreMessage.COMMAND_AUTOSTORE_TOGGLED.apply(toggleAutoStore));
            return;
        }

        if (isAll(args[1])) {
            enableAutoStore(setting, player);
            changeCurrentMode(setting, true, player);
            return;
        }

        if (!isPerItem(args[1])) {
            player.sendMessage(AutoStoreMessage.COMMAND_MODE_NOT_FOUND.apply(args[1]));
            return;
        }

        if (args.length < 3) {
            enableAutoStore(setting, player);
            setting.setAllMode(false);

            player.sendMessage(AutoStoreMessage.COMMAND_MODE_CHANGED.apply(setting.isAllMode()));

            callEvent(setting);
        } else {
            processPerItemMode(player, args, setting);
        }
    }

    private void processPerItemMode(@NotNull Player player, @NotNull String[] args, @NotNull AutoStoreSetting setting) {
        if (args[2].isEmpty()) {
            return;
        }

        var perItemModeSetting = setting.getPerItemModeSetting();

        var itemManager = BoxProvider.get().getItemManager();

        var optionalBoxItem = itemManager.getBoxItem(args[2]);

        if (optionalBoxItem.isEmpty()) {
            if (4 < args[2].length() || args.length < 4) {
                player.sendMessage(GeneralMessage.ERROR_COMMAND_ITEM_NOT_FOUND.apply(args[2]));
                return;
            }

            if (isAll(args[2])) {
                Boolean bool = getBoolean(args[3]);

                if (bool != null) {
                    enableAutoStore(setting, player);
                    changeCurrentMode(setting, false, player);

                    perItemModeSetting.setEnabledItems(bool ? itemManager.getBoxItemSet() : Collections.emptyList());
                    player.sendMessage(AutoStoreMessage.COMMAND_PER_ITEM_ALL_TOGGLED.apply(bool));

                    callEvent(setting);
                } else {
                    player.sendMessage(AutoStoreMessage.COMMAND_NOT_BOOLEAN.apply(args[3]));
                }
            } else {
                player.sendMessage(GeneralMessage.ERROR_COMMAND_ITEM_NOT_FOUND.apply(args[2]));
            }

            return;
        }

        var boxItem = optionalBoxItem.get();
        Boolean bool = 3 < args.length ? getBoolean(args[3]) : null;

        enableAutoStore(setting, player);
        changeCurrentMode(setting, false, player);

        if (bool != null) {
            perItemModeSetting.setEnabled(boxItem, bool);
        } else {
            bool = perItemModeSetting.toggleEnabled(boxItem);
        }

        player.sendMessage(AutoStoreMessage.COMMAND_PER_ITEM_ITEM_TOGGLED.apply(boxItem, bool));
        callEvent(setting);
    }

    private @Nullable Boolean getBoolean(@NotNull String arg) {
        // for aliases: t = true, f = false, of = off
        if ((arg.length() < 5 && arg.charAt(0) == 't') || arg.equalsIgnoreCase("on")) {
            return true;
        } else if ((arg.length() < 6 && arg.charAt(0) == 'f') || (arg.length() < 4 && arg.startsWith("of"))) {
            return false;
        } else {
            return null;
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            return Stream.of("all", "item", "on", "off")
                    .filter(mode -> mode.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (!isPerItem(args[1])) {
            return Collections.emptyList();
        }

        if (args.length == 3) {
            var itemNameFilter = args[2].toUpperCase(Locale.ROOT);

            var result =
                    BoxProvider.get()
                            .getItemManager()
                            .getItemNameSet()
                            .stream()
                            .filter(itemName -> itemName.startsWith(itemNameFilter))
                            .sorted()
                            .collect(Collectors.toList());

            if ("all".startsWith(args[2].toLowerCase(Locale.ROOT))) {
                result.add("all");
            }

            return result;
        }

        if (args.length == 4) {
            return Stream.of("on", "off")
                    .filter(bool -> bool.startsWith(args[3].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private boolean isAll(@NotNull String arg) {
        return !arg.isEmpty() && arg.length() < 4 && (arg.charAt(0) == 'a' || arg.charAt(0) == 'A');
    }

    private boolean isPerItem(@NotNull String arg) {
        return !arg.isEmpty() && arg.length() < 5 && (arg.charAt(0) == 'i' || arg.charAt(0) == 'I');
    }

    private void callEvent(@NotNull AutoStoreSetting setting) {
        BoxProvider.get().getEventBus().callEvent(new AutoStoreSettingChangeEvent(setting));
    }

    private void enableAutoStore(@NotNull AutoStoreSetting setting, @NotNull Player player) {
        if (!setting.isEnabled()) {
            setting.setEnabled(true);
            player.sendMessage(AutoStoreMessage.COMMAND_AUTOSTORE_TOGGLED.apply(true));
        }
    }

    private void changeCurrentMode(@NotNull AutoStoreSetting setting,
                                   boolean allMode, @NotNull Player player) {
        if (setting.isAllMode() != allMode) {
            setting.setAllMode(allMode);
            player.sendMessage(AutoStoreMessage.COMMAND_MODE_CHANGED.apply(setting.isAllMode()));
        }
    }

    @Override
    public @NotNull Component getHelp() {
        return Component.text()
                .append(AutoStoreMessage.COMMAND_HELP_1).append(Component.newline())
                .append(AutoStoreMessage.COMMAND_HELP_2).append(Component.newline())
                .append(AutoStoreMessage.COMMAND_HELP_3)
                .build();
    }
}