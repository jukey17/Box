package net.okocraft.box.feature.command.box;

import net.kyori.adventure.text.Component;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.api.command.AbstractCommand;
import net.okocraft.box.api.message.GeneralMessage;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.feature.command.message.BoxMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ItemInfoCommand extends AbstractCommand {

    public ItemInfoCommand() {
        super("iteminfo", "box.command.iteminfo", Set.of("i"));
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GeneralMessage.ERROR_COMMAND_ONLY_PLAYER);
            return;
        }

        BoxItem boxItem;

        if (1 < args.length) {
            var optionalBoxItem = BoxProvider.get().getItemManager().getBoxItem(args[1]);

            if (optionalBoxItem.isPresent()) {
                boxItem = optionalBoxItem.get();
            } else {
                sender.sendMessage(GeneralMessage.ERROR_COMMAND_ITEM_NOT_FOUND.apply(args[1]));
                return;
            }
        } else {
            var itemInMainHand = player.getInventory().getItemInMainHand();

            if (itemInMainHand.getType().isAir()) {
                player.sendMessage(BoxMessage.ITEM_INFO_IS_AIR);
                return;
            }

            var optionalBoxItem = BoxProvider.get().getItemManager().getBoxItem(itemInMainHand);

            if (optionalBoxItem.isPresent()) {
                boxItem = optionalBoxItem.get();
            } else {
                player.sendMessage(BoxMessage.ITEM_INFO_NOT_REGISTERED);
                return;
            }
        }

        int stock = BoxProvider.get().getBoxPlayerMap().get(player).getCurrentStockHolder().getAmount(boxItem);

        player.sendMessage(BoxMessage.ITEM_INFO_NAME.apply(boxItem));
        player.sendMessage(BoxMessage.ITEM_INFO_STOCK.apply(stock));
    }

    @Override
    public @NotNull Component getHelp() {
        return BoxMessage.ITEM_INFO_HELP;
    }
}