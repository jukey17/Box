package net.okocraft.box.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.okocraft.box.ConfigManager;
import net.okocraft.box.Box;
import net.okocraft.box.command.Commands;
import net.okocraft.box.database.Database;
import com.google.common.primitives.Ints;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.plugin.Plugin;

public class GuiManager implements Listener {

    private Plugin instance;
    private Database database;

    private ConfigManager configManager;
    private Map<String, MemorySection> category;
    private String categorySelectionGuiName;
    private Map<String, String> categoryGuiNameMap;
    private NamespacedKey quantityKey;
    private NamespacedKey categoryNameKey;
    private List<Integer> categorySelectionGuiFrame;

    Sound openSound;
    Sound changePageSound;
    Sound returnToSelectionGuiSound;
    Sound decreaseSound;
    Sound increaseSound;
    Sound notEnoughSound;
    Sound takeOutSound;
    Sound takeInSound;

    public GuiManager(Database database, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.database = database;
        this.instance = plugin;
        configManager = Box.getInstance().getConfigManager();
        category = configManager.getCategories();
        categorySelectionGuiName = configManager.getCategorySelectionGuiName();
        categoryGuiNameMap = configManager.getCategoryGuiNameMap();

        quantityKey = new NamespacedKey(instance, "quantity");
        categoryNameKey = new NamespacedKey(instance, "categoryname");

        categorySelectionGuiFrame = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47,
                48, 49, 50, 51, 52, 53);

        openSound = configManager.getOpenSound();
        changePageSound = configManager.getChangePageSound();
        returnToSelectionGuiSound = configManager.getReturnToSelectionGuiSound();
        decreaseSound = configManager.getDecreaseSound();
        increaseSound = configManager.getIncreaseSound();
        notEnoughSound = configManager.getNotEnoughSound();
        takeOutSound = configManager.getTakeOutSound();
        takeInSound = configManager.getTakeInSound();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String actionName = event.getAction().name();
        Inventory inv = event.getClickedInventory();
        if (inv == null)
            return;

        int clickedSlot = event.getSlot();
        ItemStack clickedItem = inv.getItem(clickedSlot);
        if (clickedItem == null)
            return;

        String invName = event.getView().getTitle();

        if (invName.equals(categorySelectionGuiName)) {
            event.setCancelled(true);
            if (actionName.equals("NOTHING") || inv.getType() == InventoryType.PLAYER)
                return;

            if (categorySelectionGuiFrame.contains(clickedSlot))
                return;

            String categoryName = clickedItem.getItemMeta().getCustomTagContainer().getCustomTag(categoryNameKey,
                    ItemTagType.STRING);

            openCategoryGui(player, categoryName, 1, 1, true);
            return;
        }

        if (categoryGuiNameMap.containsValue(invName)) {
            event.setCancelled(true);
            if (actionName.equals("NOTHING") || inv.getType() == InventoryType.PLAYER)
                return;

            String categoryName = clickedItem.getItemMeta().getCustomTagContainer().getCustomTag(categoryNameKey,
                    ItemTagType.STRING);

            if (clickedSlot == 45 || clickedSlot == 53) {
                if (changePageSound != null)
                    player.playSound(player.getLocation(), changePageSound, SoundCategory.MASTER,
                            configManager.getSoundPitch(), configManager.getSoundVolume());

                openCategoryGui(player, categoryName, inv.getItem(clickedSlot).getAmount(), 1, inv, false);
                return;
            }

            if (clickedSlot == 49) {
                if (returnToSelectionGuiSound != null)
                    player.playSound(player.getLocation(), returnToSelectionGuiSound, SoundCategory.MASTER,
                            configManager.getSoundPitch(), configManager.getSoundVolume());

                openCategorySelectionGui(player, 1, false);
                return;
            }

            if (Arrays.asList(46, 47, 48, 50, 51, 52).contains(clickedSlot)) {
                ItemStack firstItem = inv.getItem(0);
                if (firstItem == null)
                    return;

                int quantity = firstItem.getItemMeta().getCustomTagContainer().getCustomTag(quantityKey,
                        ItemTagType.INTEGER);

                int difference = 0;

                switch (clickedSlot) {
                case 46:
                    difference = -64;
                    break;
                case 47:
                    difference = -8;
                    break;
                case 48:
                    difference = -1;
                    break;
                case 50:
                    difference = 1;
                    break;
                case 51:
                    difference = 8;
                    break;
                case 52:
                    difference = 64;
                    break;
                }

                if ((quantity == 640 && difference > 0) || (quantity == 1 && difference < 0))
                    return;

                quantity = (quantity + difference >= 1) ? quantity + difference : 1;
                if (quantity > 640)
                    quantity = 640;

                if (difference < 0) {
                    if (decreaseSound != null)
                        player.playSound(player.getLocation(), decreaseSound, SoundCategory.MASTER,
                                configManager.getSoundPitch(), configManager.getSoundVolume());
                } else if (difference > 0) {
                    if (increaseSound != null)
                        player.playSound(player.getLocation(), increaseSound, SoundCategory.MASTER,
                                configManager.getSoundPitch(), configManager.getSoundVolume());
                }

                changeQuantity(inv, quantity);
                return;
            }

            Material clickedItemMaterial = clickedItem.getType();

            MemorySection categorySetting = category.get(categoryName);
            MemorySection clickedItemMaterialSection = ConfigManager
                    .memorySectionOrNull(categorySetting.get("item." + clickedItemMaterial.name()));
            if (clickedItemMaterialSection == null) {
                player.sendMessage(configManager.getMessageConfig()
                        .getString("ErrorOccured", "&cエラーが発生して処理を実行できませんでした。").replaceAll("&([a-f0-9])", "§$1"));
                player.closeInventory();
                return;
            }

            int quantity = clickedItem.getItemMeta().getCustomTagContainer().getCustomTag(quantityKey,
                    ItemTagType.INTEGER);

            Integer storedItemAmount = Ints
                    .tryParse(database.get(clickedItemMaterial.name(), player.getUniqueId().toString()));
            if (storedItemAmount == null) {
                player.sendMessage(configManager.getMessageConfig()
                        .getString("InvalidValueIsStored", "&cデータベースに不正な値が格納されています。管理者に報告して下さい。")
                        .replaceAll("&([a-f0-9])", "§$1"));
                return;
            }

            int resultStoredAmount;

            if (event.isRightClick()) {

                if (storedItemAmount <= 0) {
                    if (notEnoughSound != null)
                        player.playSound(player.getLocation(), notEnoughSound, SoundCategory.MASTER,
                                configManager.getSoundPitch(), configManager.getSoundVolume());
                    return;
                }

                if (takeOutSound != null)
                    player.playSound(player.getLocation(), takeOutSound, SoundCategory.MASTER,
                            configManager.getSoundPitch(), configManager.getSoundVolume());

                int quantityClone = (storedItemAmount < quantity) ? storedItemAmount : quantity;

                Map<Integer, ItemStack> nonStoredItemStacks = player.getInventory()
                        .addItem(new ItemStack(clickedItemMaterial, quantityClone));
                int nonAddedAmount = nonStoredItemStacks.values().stream().mapToInt(item -> item.getAmount()).sum();
                resultStoredAmount = storedItemAmount - quantityClone + nonAddedAmount;

            } else {
                Inventory playerInv = event.getView().getBottomInventory();
                int playerItemAmount = playerInv.all(clickedItemMaterial).values().stream()
                        .filter(item -> !item.hasItemMeta()).mapToInt(item -> item.getAmount()).sum();
                if (playerItemAmount == 0) {
                    if (notEnoughSound != null)
                        player.playSound(player.getLocation(), notEnoughSound, SoundCategory.MASTER,
                                configManager.getSoundPitch(), configManager.getSoundVolume());
                    return;
                }

                if (takeInSound != null)
                    player.playSound(player.getLocation(), takeInSound, SoundCategory.MASTER,
                            configManager.getSoundPitch(), configManager.getSoundVolume());

                Map<Integer, ItemStack> nonRemovedItemStacks = playerInv
                        .removeItem(new ItemStack(clickedItemMaterial, quantity));
                int nonRemovedAmount = nonRemovedItemStacks.values().stream().mapToInt(item -> item.getAmount()).sum();
                resultStoredAmount = storedItemAmount + quantity - nonRemovedAmount;
            }

            String newValue = String.valueOf(resultStoredAmount);

            database.set(clickedItemMaterial.name(), player.getUniqueId().toString(), newValue);

            inv.setItem(clickedSlot,
                    createItem(newValue, clickedItemMaterial.name(), clickedItemMaterialSection, categoryName, quantity));

            return;
        }
    }

    public void openCategorySelectionGui(Player player, int page, boolean playSound) {
        player.closeInventory();
        if (playSound && openSound != null)
            player.playSound(player.getLocation(), openSound, SoundCategory.MASTER, configManager.getSoundPitch(),
                    configManager.getSoundVolume());
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName("§6");
        glass.setItemMeta(meta);
        Inventory categorySelectionGui = Bukkit.createInventory(null, 54, categorySelectionGuiName);
        categorySelectionGuiFrame.forEach(slot -> categorySelectionGui.setItem(slot, glass.clone()));
        category.forEach((categoryName, categorySetting) -> categorySelectionGui
                .addItem(createCategorySelectionItem(categoryName, categorySetting)));
        player.openInventory(categorySelectionGui);
    }

    private ItemStack createCategorySelectionItem(String categoryName, MemorySection section) {
        Material categoryIconMaterial = Material.valueOf(section.getString("icon", "BARRIER").toUpperCase());
        ItemStack categorySelectionItem = new ItemStack(categoryIconMaterial);
        ItemMeta categorySelectionItemMeta = categorySelectionItem.getItemMeta();
        String categoryItemName = section.getString("display_name", "No display_name defined.")
                .replaceAll("&([a-f0-9])", "§$1");
        categorySelectionItemMeta.setDisplayName(categoryItemName);
        categorySelectionItemMeta.getCustomTagContainer().setCustomTag(categoryNameKey, ItemTagType.STRING,
                categoryName);
        categorySelectionItem.setItemMeta(categorySelectionItemMeta);
        return categorySelectionItem;
    }

    public void openCategoryGui(Player player, String categoryName, int page, int quantity, boolean playSound) {
        openCategoryGui(player, categoryName, page, quantity, null, playSound);
    }

    public void openCategoryGui(Player player, String categoryName, int page, int quantity, Inventory modifiedInventory,
            boolean playSound) {

        MemorySection categorySetting = category.get(categoryName);
        MemorySection categoryItems = ConfigManager.memorySectionOrNull(categorySetting.get("item"));
        if (categoryItems == null) {
            Commands.errorOccured(player,
                    configManager.getMessageConfig().getString("ErrorOnOpenGui", "§cエラーが発生してGuiを開けませんでした。"));
            player.closeInventory();
            return;
        }

        Inventory categoryGui = Bukkit.createInventory(player, 54, categoryGuiNameMap.get(categoryName));

        configManager.getFooterItemStacks().forEach((slot, itemStack) -> {
            ItemStack usingItemStack = itemStack.clone();
            usingItemStack.getItemMeta().getCustomTagContainer().setCustomTag(categoryNameKey, ItemTagType.STRING,
                    categoryName);
            if (slot == 45) {
                if (page > 1)
                    usingItemStack.setAmount(page - 1);
                else
                    usingItemStack.setAmount(0);
            }
            if (slot == 53) {
                if (page < 64)
                    usingItemStack.setAmount(page + 1);
                else
                    usingItemStack.setAmount(0);
            }
            categoryGui.setItem(slot, usingItemStack);
        });

        Map<String, String> columnValueMap = database.getMultiValue(new ArrayList<String>(categoryItems.getKeys(false)), player.getUniqueId().toString());

        categoryItems.getValues(false).entrySet().stream().skip(45 * (page - 1)).limit(45).forEach(itemEntry -> {
            MemorySection categoryItemSection = ConfigManager.memorySectionOrNull(itemEntry.getValue());
            if (categoryItemSection == null)
                return;

            ItemStack categoryItem = createItem(columnValueMap.get(itemEntry.getKey()), itemEntry.getKey(), categoryItemSection, categoryName,
                    quantity);
            if (categoryItem.getType() == Material.AIR)
                return;

            categoryGui.addItem(categoryItem);
        });

        if (playSound && openSound != null)
            player.playSound(player.getLocation(), openSound, SoundCategory.MASTER, configManager.getSoundPitch(),
                    configManager.getSoundVolume());

        if (modifiedInventory != null) {
            modifiedInventory.setContents(categoryGui.getStorageContents());
        } else {
            player.closeInventory();
            player.openInventory(categoryGui);
        }
    }

    private ItemStack createItem(String stock, String materialName, MemorySection section, String categoryName,
            int quantity) {
        Material categoryItemMaterial;
        try {
            categoryItemMaterial = Material.valueOf(materialName);
        } catch (IllegalArgumentException exception) {
            return new ItemStack(Material.AIR);
        }

        String jp = section.getString("jp", "");
        String en = section.getString("en", "");
        String categoryItemName = replacePlaceholders(configManager.getItemTemplateName(), jp, en, stock, quantity);
        List<String> categoryItemLore = new ArrayList<>(configManager.getItemTemplateLore());
        categoryItemLore.replaceAll(loreLine -> replacePlaceholders(loreLine, jp, en, stock, quantity));

        ItemStack categoryItem = new ItemStack(categoryItemMaterial);
        ItemMeta categoryItemMeta = categoryItem.getItemMeta();
        categoryItemMeta.setDisplayName(categoryItemName);
        categoryItemMeta.setLore(categoryItemLore);
        CustomItemTagContainer tagContainer = categoryItemMeta.getCustomTagContainer();
        tagContainer.setCustomTag(quantityKey, ItemTagType.INTEGER, quantity);
        tagContainer.setCustomTag(categoryNameKey, ItemTagType.STRING, categoryName);
        categoryItem.setItemMeta(categoryItemMeta);

        return categoryItem;
    }

    private void changeQuantity(Inventory inv, int quantity) {
        Player player = (Player) inv.getViewers().get(0);
        String categoryName = inv.getItem(0).getItemMeta().getCustomTagContainer().getCustomTag(categoryNameKey,
                ItemTagType.STRING);
        if (categoryName == null) {
            System.out.println("カテゴリ名の取得に失敗しました。");
            return;
        }
        MemorySection section = ConfigManager
                .memorySectionOrNull(configManager.getStoringItemConfig().get("categories." + categoryName + ".item"));
        if (section == null) {
            System.out.println("アイテム設定の取得に失敗しました。");
            return;
        }

        List<ItemStack> items = IntStream.rangeClosed(0, 44).boxed().map(slot -> inv.getItem(slot))
                .filter(item -> item != null).collect(Collectors.toList());
        List<String> itemMaterialNameList = (new ArrayList<ItemStack>(items)).stream().map(item -> item.getType().name())
                .collect(Collectors.toList());

        Map<String, String> itemAmountMap = database.getMultiValue(itemMaterialNameList,
                player.getUniqueId().toString());

        items.forEach(item -> {
            String itemMaterialName = item.getType().name();
            ItemMeta meta = item.getItemMeta();
            meta.getCustomTagContainer().setCustomTag(quantityKey, ItemTagType.INTEGER, quantity);
            String jp = section.getString(itemMaterialName + ".jp", "");
            String en = section.getString(itemMaterialName + ".en", "");
            String stock = itemAmountMap.get(itemMaterialName);

            String categoryItemName = replacePlaceholders(configManager.getItemTemplateName(), jp, en, stock, quantity);
            List<String> categoryItemLore = new ArrayList<>(configManager.getItemTemplateLore());
            categoryItemLore.replaceAll(loreLine -> replacePlaceholders(loreLine, jp, en, stock, quantity));
            meta.setDisplayName(categoryItemName);
            meta.setLore(categoryItemLore);
            item.setItemMeta(meta);

        });
    }

    private String replacePlaceholders(String original, String jp, String en, String stock, int quantity) {
        return original
                .replaceAll("%item_jp%", jp)
                .replaceAll("%item_en%", en)
                .replaceAll("%item_quantity%", String.valueOf(quantity))
                .replaceAll("%stock%", stock)
                .replaceAll("&([a-f0-9])", "§$1");
    }
}