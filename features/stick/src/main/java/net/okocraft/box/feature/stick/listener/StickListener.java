package net.okocraft.box.feature.stick.listener;

import com.github.siroshun09.configapi.api.value.ConfigValue;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.feature.stick.item.BoxStickItem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ClassCanBeRecord")
public class StickListener implements Listener {

    private static final ConfigValue<String> MENU_COMMAND_SETTING =
            config -> config.getString("stick.menu-command", "box gui");

    private final BoxStickItem boxStickItem;

    public StickListener(@NotNull BoxStickItem boxStickItem) {
        this.boxStickItem = boxStickItem;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        var player = event.getPlayer();

        if (event.getAction() == Action.PHYSICAL || event.getAction().isLeftClick() ||
                BoxProvider.get().isDisabledWorld(player) || !player.hasPermission("box.stick.menu")) {
            return;
        }

        var mainHand = player.getInventory().getItemInMainHand();
        var offHand = player.getInventory().getItemInOffHand();

        if ((event.getHand() == EquipmentSlot.HAND && boxStickItem.check(mainHand)) ||
                (event.getHand() == EquipmentSlot.OFF_HAND && mainHand.getType().isAir() && boxStickItem.check(offHand))) {
            var command = BoxProvider.get().getConfiguration().get(MENU_COMMAND_SETTING);

            if (!command.isEmpty()) {
                Bukkit.dispatchCommand(player, command);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        var player = event.getPlayer();

        if (!checkPlayerCondition(player, "box.stick.block")) {
            return;
        }

        var inHand = event.getItemInHand();
        var mainHandItem = player.getInventory().getItemInMainHand();

        if (inHand.equals(mainHandItem) && tryConsumingStock(player, mainHandItem)) {
            player.getInventory().setItemInMainHand(mainHandItem.clone());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemConsume(@NotNull PlayerItemConsumeEvent event) {
        var player = event.getPlayer();

        if (!checkPlayerCondition(player, "box.stick.food")) {
            return;
        }

        var mainHandItem = player.getInventory().getItemInMainHand();

        if (event.getItem().equals(mainHandItem) && tryConsumingStock(player, mainHandItem)) {
            event.setReplacement(mainHandItem.clone());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemBreak(@NotNull PlayerItemBreakEvent event) {
        var player = event.getPlayer();

        if (!checkPlayerCondition(player, "box.stick.tool")) {
            return;
        }

        var original = event.getBrokenItem();
        var copied = original.clone();

        copied.editMeta(meta -> {
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(0);
            }
        });

        if (tryConsumingStock(player, copied)) {
            original.setAmount(2);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(@NotNull ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        var permissionNodeSuffix =
                switch (event.getEntity().getType()) {
                    case EGG -> "egg";
                    case ENDER_PEARL -> "enderpearl";
                    case FIREWORK -> "firework";
                    case SNOWBALL -> "snowball";
                    case SPLASH_POTION -> "potion";
                    case THROWN_EXP_BOTTLE -> "expbottle";
                    default -> null;
                };

        if (permissionNodeSuffix == null || !checkPlayerCondition(player, "box.stick." + permissionNodeSuffix)) {
            return;
        }

        var mainHand = player.getInventory().getItemInMainHand();

        if (tryConsumingStock(player, mainHand)) {
            mainHand.setAmount(mainHand.getAmount() + 1);
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(@NotNull EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) ||
                !checkPlayerCondition(player, "box.stick.arrow") ||
                !(event.getProjectile() instanceof Arrow arrow)) {
            return;
        }

        var bow = event.getBow();

        if (bow == null || bow.getType() != Material.BOW || !event.shouldConsumeItem()) {
            return;
        }

        var arrowItem = event.getConsumable();

        if (arrowItem != null && tryConsumingStock(player, arrowItem)) {
            event.setConsumeItem(false);
            player.updateInventory();

            // If setConsumeItem is set to false, the arrow will not be picked up.
            // This task overwrites it after 1 tick.
            Bukkit.getScheduler().runTask(
                    BoxProvider.get().getPluginInstance(),
                    () -> arrow.setPickupStatus(AbstractArrow.PickupStatus.ALLOWED)
            );
        }
    }

    private boolean tryConsumingStock(@NotNull Player player, @NotNull ItemStack item) {
        var boxItem = BoxProvider.get().getItemManager().getBoxItem(item);

        if (boxItem.isEmpty()) {
            return false;
        }

        var stockHolder = BoxProvider.get().getBoxPlayerMap().get(player).getCurrentStockHolder();

        if (0 < stockHolder.getAmount(boxItem.get())) {
            stockHolder.decrease(boxItem.get());
            return true;
        } else {
            return false;
        }
    }

    private boolean checkPlayerCondition(@NotNull Player player, @NotNull String permissionNode) {
        if (player.getGameMode() != GameMode.ADVENTURE &&
                player.getGameMode() != GameMode.SURVIVAL) {
            return false;
        }

        if (BoxProvider.get().isDisabledWorld(player)) {
            return false;
        }

        if (!player.hasPermission(permissionNode)) {
            return false;
        }

        return boxStickItem.check(player.getInventory().getItemInOffHand());
    }
}