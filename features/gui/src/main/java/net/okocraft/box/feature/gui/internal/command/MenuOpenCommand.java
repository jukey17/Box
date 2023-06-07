package net.okocraft.box.feature.gui.internal.command;

import net.kyori.adventure.text.Component;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.api.command.AbstractCommand;
import net.okocraft.box.api.message.Components;
import net.okocraft.box.api.message.GeneralMessage;
import net.okocraft.box.api.message.argument.SingleArgument;
import net.okocraft.box.api.model.stock.StockHolder;
import net.okocraft.box.api.util.TabCompleter;
import net.okocraft.box.api.util.UserStockHolderOperator;
import net.okocraft.box.feature.category.api.registry.CategoryRegistry;
import net.okocraft.box.feature.gui.api.event.MenuOpenEvent;
import net.okocraft.box.feature.gui.api.menu.Menu;
import net.okocraft.box.feature.gui.api.mode.ClickModeRegistry;
import net.okocraft.box.feature.gui.api.session.PlayerSession;
import net.okocraft.box.feature.gui.api.util.MenuOpener;
import net.okocraft.box.feature.gui.internal.menu.CategoryMenu;
import net.okocraft.box.feature.gui.internal.menu.CategorySelectorMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class MenuOpenCommand extends AbstractCommand {

    private static final String OTHER_PLAYERS_GUI_PERMISSION = "box.admin.command.gui.other";
    private static final Component CANNOT_OPEN_MENU = Components.redTranslatable("box.gui.cannot-open-menu");
    private static final Component COMMAND_HELP = Components.commandHelp("box.gui.command-help", false);
    private static final SingleArgument<String> CATEGORY_NOT_FOUND = arg -> Components.redTranslatable("box.gui.category-not-found", Components.aquaText(arg));

    public MenuOpenCommand() {
        super("gui", "box.command.gui", Set.of("g", "menu", "m"));
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GeneralMessage.ERROR_COMMAND_ONLY_PLAYER);
            return;
        }

        if (args.length < 2) {
            openMenu(player, getCurrentStockHolder(player), new CategorySelectorMenu());
            return;
        }

        if (!args[1].startsWith("-")) {
            legacyBehavior(player, args);
            return;
        }

        StockHolder source = null;
        Menu menu = null;

        for (int i = 1; i + 1 < args.length; i = i + 2) {
            var arg = args[i].toLowerCase(Locale.ENGLISH);

            if (source == null && (arg.equalsIgnoreCase("-p") || arg.equalsIgnoreCase("--player"))) {
                if (!player.hasPermission(OTHER_PLAYERS_GUI_PERMISSION)) {
                    player.sendMessage(GeneralMessage.ERROR_NO_PERMISSION.apply(OTHER_PLAYERS_GUI_PERMISSION));
                    return;
                }

                source = UserStockHolderOperator.create(args[i + 1]).supportOffline(true).getUserStockHolder();

                if (source == null) {
                    player.sendMessage(GeneralMessage.ERROR_COMMAND_PLAYER_NOT_FOUND.apply(args[i + 1]));
                    return;
                }
            } else if (menu == null && (arg.equalsIgnoreCase("-c") || arg.equalsIgnoreCase("--category"))) {
                var category = CategoryRegistry.get().getByName(args[i + 1]);

                if (category.isEmpty()) {
                    player.sendMessage(CATEGORY_NOT_FOUND.apply(args[i + 1]));
                    return;
                }

                menu = new CategoryMenu(category.get());
            }
        }

        openMenu(
                player,
                Objects.requireNonNullElseGet(source, () -> getCurrentStockHolder(player)),
                Objects.requireNonNullElseGet(menu, CategorySelectorMenu::new)
        );
    }

    @Override
    public @NotNull Component getHelp() {
        return COMMAND_HELP;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2 || !(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (2 < args.length) {
            var arg = args[args.length - 2].toLowerCase(Locale.ENGLISH);

            if (arg.equalsIgnoreCase("-p") || arg.equalsIgnoreCase("--player")) {
                return sender.hasPermission(OTHER_PLAYERS_GUI_PERMISSION) ?
                        TabCompleter.players(args[args.length - 1]) :
                        Collections.emptyList();
            }

            if (arg.equalsIgnoreCase("-c") || arg.equalsIgnoreCase("--category")) {
                return CategoryRegistry.get().names()
                        .stream()
                        .filter(name -> name.startsWith(args[args.length - 1]))
                        .toList();
            }
        }

        return sender.hasPermission(OTHER_PLAYERS_GUI_PERMISSION) ?
                List.of("--player", "--category", "-p", "-c") :
                List.of("--category", "-c");
    }

    private void legacyBehavior(@NotNull Player player, @NotNull String[] args) {
        if (!player.hasPermission(OTHER_PLAYERS_GUI_PERMISSION)) {
            player.sendMessage(GeneralMessage.ERROR_NO_PERMISSION.apply(OTHER_PLAYERS_GUI_PERMISSION));
            return;
        }

        var stockHolder = UserStockHolderOperator.create(args[1]).supportOffline(true).getUserStockHolder();

        if (stockHolder == null) {
            player.sendMessage(GeneralMessage.ERROR_COMMAND_PLAYER_NOT_FOUND.apply(args[1]));
            return;
        }

        openMenu(player, stockHolder, new CategorySelectorMenu());
    }

    private void openMenu(@NotNull Player player, @NotNull StockHolder source, @NotNull Menu menu) {
        var session = PlayerSession.get(player);

        session.setBoxItemClickMode(null);
        session.resetCustomNumbers();
        session.setStockHolder(source);

        var modes = ClickModeRegistry.getModes().stream().filter(mode -> mode.canUse(player)).toList();
        session.setAvailableClickModes(modes);

        var event = new MenuOpenEvent(player, menu);

        if (BoxProvider.get().getEventBus().callEvent(event).isCancelled()) {
            player.sendMessage(CANNOT_OPEN_MENU);
            return;
        }

        MenuOpener.open(menu, player);
    }

    private @NotNull StockHolder getCurrentStockHolder(@NotNull Player player) { // Helper method
        return BoxProvider.get().getBoxPlayerMap().get(player).getCurrentStockHolder();
    }
}
