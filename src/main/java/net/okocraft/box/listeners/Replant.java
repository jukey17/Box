package net.okocraft.box.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import net.okocraft.box.Box;
import net.okocraft.box.database.Database;
import net.okocraft.box.util.GeneralConfig;

public class Replant implements Listener {
    private static final Box INSTANCE = Box.getInstance();
    private static final GeneralConfig CONFIG = INSTANCE.getGeneralConfig();
    private final Database DATABASE = INSTANCE.getDatabase();

    private final Map<Material, Material> plants = new HashMap<>() {
        private static final long serialVersionUID = 1L;

        {
            put(Material.WHEAT, Material.WHEAT_SEEDS);
            put(Material.POTATOES, Material.POTATO);
            put(Material.CARROTS, Material.CARROT);
            put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        }
    };

    private final Map<Material, Material> trees = new HashMap<>() {
        private static final long serialVersionUID = 1L;

        {
            put(Material.ACACIA_LOG, Material.ACACIA_SAPLING);
            put(Material.ACACIA_WOOD, Material.ACACIA_SAPLING);
            put(Material.SPRUCE_LOG, Material.SPRUCE_SAPLING);
            put(Material.SPRUCE_WOOD, Material.SPRUCE_SAPLING);
            put(Material.BIRCH_LOG, Material.BIRCH_SAPLING);
            put(Material.BIRCH_WOOD, Material.BIRCH_SAPLING);
            put(Material.JUNGLE_LOG, Material.JUNGLE_SAPLING);
            put(Material.JUNGLE_WOOD, Material.JUNGLE_SAPLING);
            put(Material.DARK_OAK_LOG, Material.DARK_OAK_SAPLING);
            put(Material.DARK_OAK_WOOD, Material.DARK_OAK_SAPLING);
            put(Material.OAK_LOG, Material.OAK_SAPLING);
            put(Material.OAK_WOOD, Material.OAK_SAPLING);
            put(Material.STRIPPED_ACACIA_LOG, Material.ACACIA_SAPLING);
            put(Material.STRIPPED_ACACIA_WOOD, Material.ACACIA_SAPLING);
            put(Material.STRIPPED_SPRUCE_LOG, Material.SPRUCE_SAPLING);
            put(Material.STRIPPED_SPRUCE_WOOD, Material.SPRUCE_SAPLING);
            put(Material.STRIPPED_BIRCH_LOG, Material.BIRCH_SAPLING);
            put(Material.STRIPPED_BIRCH_WOOD, Material.BIRCH_SAPLING);
            put(Material.STRIPPED_JUNGLE_LOG, Material.JUNGLE_SAPLING);
            put(Material.STRIPPED_JUNGLE_WOOD, Material.JUNGLE_SAPLING);
            put(Material.STRIPPED_DARK_OAK_LOG, Material.DARK_OAK_SAPLING);
            put(Material.STRIPPED_DARK_OAK_WOOD, Material.DARK_OAK_SAPLING);
            put(Material.STRIPPED_OAK_LOG, Material.OAK_SAPLING);
            put(Material.STRIPPED_OAK_WOOD, Material.OAK_SAPLING);
        }
    };

    public Replant() {
        Bukkit.getPluginManager().registerEvents(this, INSTANCE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void replantSeed(BlockBreakEvent event) {

        if (event.isCancelled())
            return;

        Block brokenBlock = event.getBlock();

        if (!CONFIG.getReplantWorlds().contains(brokenBlock.getWorld())) {
            return;
        }

        Material brokenBlockType = brokenBlock.getType();
        Player player = event.getPlayer();

        if (!plants.containsKey(brokenBlockType)) return;

        Material seed = plants.get(brokenBlockType);

        if (!hasSeed(player, seed)) {
            event.setCancelled(true);
            return;
        }

        Ageable blockDataAgable = (Ageable) brokenBlock.getBlockData();
        if (blockDataAgable.getAge() != blockDataAgable.getMaximumAge()) {
            event.setCancelled(true);
            return;
        }

        Ageable newBlockDataAgeable = (Ageable) blockDataAgable.clone();
        newBlockDataAgeable.setAge(0);

        new BukkitRunnable(){
        
            @Override
            public void run() {
                Location blockLoc = brokenBlock.getLocation();
                if (!blockLoc.getBlock().getType().equals(Material.AIR))
                    return;
                blockLoc.getBlock().setType(brokenBlockType);
                blockLoc.getBlock().setBlockData(newBlockDataAgeable);
                
                takeSeed(player, seed);
            }
        }.runTaskLater(INSTANCE, 3L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void replantSapling(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block treeBlock = event.getBlock();

        if (!CONFIG.getReplantWorlds().contains(treeBlock.getWorld())) {
            return;
        }

        Material treeMaterial = treeBlock.getType();

        if (trees.containsValue(treeMaterial)) {
            event.setCancelled(true);
            return;
        }
        
        if (!trees.containsKey(treeMaterial)) {
            return;
        }

        Material sapling = trees.get(treeMaterial);

        Material blockBelow = treeBlock.getLocation().add(0D, -1D, 0D).getBlock().getBlockData().getMaterial();
        if (!blockBelow.equals(Material.DIRT) && !blockBelow.equals(Material.GRASS_BLOCK)
                && !blockBelow.equals(Material.PODZOL)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Location blockLoc = treeBlock.getLocation();
                blockLoc.getBlock().setType(sapling);
                blockLoc.getBlock().setBlockData(sapling.createBlockData());
            }
        }.runTaskLater(INSTANCE, 3L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void cancelBreakingDirt(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Material ground = event.getBlock().getType();
        if (!ground.equals(Material.DIRT) && !ground.equals(Material.GRASS_BLOCK)
                && !ground.equals(Material.PODZOL)) {
            return;
        }

        Material sapling = event.getBlock().getLocation().add(0, 1, 0).getBlock().getType();
        if (trees.containsValue(sapling)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void cancelBoneMeal(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        if (!trees.containsValue(clickedBlock.getType())) {
            return;
        }
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (!event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.BONE_MEAL)
                && !event.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.BONE_MEAL)) {
            return;
        }
        event.setCancelled(true);
    }

    private void takeSeed(Player player, Material seed) {
        if (!plants.containsValue(seed)) {
            return;
        }
        int stock;
        try {
            stock = Integer.parseInt(DATABASE.get(seed.name(), player.getName()));
        } catch (NumberFormatException exception) {
            exception.printStackTrace();
            return;
        }
        if (stock >= 1) {
            DATABASE.set(seed.name(), player.getName(), String.valueOf(stock - 1));
        } else {
            player.getInventory().removeItem(new ItemStack(seed));
        }
    }
    
    private boolean hasSeed(Player player, Material seed) {
        if (!plants.containsValue(seed)) {
            return false;
        }

        if (player.getInventory().contains(seed)) {
            return true;
        }

        if (!DATABASE.getColumnMap().containsKey(seed.name())) {
            return false;
        }

        int stock;
        try {
            stock = Integer.parseInt(DATABASE.get(seed.name(), player.getName()));
        } catch (NumberFormatException exception) {
            exception.printStackTrace();
            return false;
        }

        return stock > 0;
    }
}