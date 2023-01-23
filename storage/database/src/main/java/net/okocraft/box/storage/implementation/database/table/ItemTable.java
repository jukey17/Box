package net.okocraft.box.storage.implementation.database.table;

import net.okocraft.box.api.model.item.BoxCustomItem;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.storage.api.factory.item.BoxItemFactory;
import net.okocraft.box.storage.api.model.item.ItemStorage;
import net.okocraft.box.storage.api.util.item.DefaultItem;
import net.okocraft.box.storage.api.util.item.ItemNameGenerator;
import net.okocraft.box.storage.implementation.database.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// | id | name | item_data | is_default_item
public class ItemTable extends AbstractTable implements ItemStorage {

    private final MetaTable metaTable;

    public ItemTable(@NotNull Database database, @NotNull MetaTable metaTable) {
        super(database, database.getSchemaSet().itemTable());
        this.metaTable = metaTable;
    }

    @Override
    public void init() throws Exception {
        createTableAndIndex();
    }

    @Override
    public int getDataVersion() throws Exception {
        return metaTable.getItemDataVersion();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void saveCurrentDataVersion() throws Exception {
        metaTable.saveItemDataVersion(Bukkit.getUnsafe().getDataVersion());
    }

    @Override
    public @NotNull List<BoxItem> loadAllDefaultItems() throws Exception {
        var result = new ArrayList<BoxItem>(1000); // The number of default items is currently around 1400, so this should only need to be expanded once.

        try (var connection = database.getConnection();
             var statement = prepareStatement(connection, "SELECT `id`, `name`, `item_data` FROM `%table%` WHERE `is_default_item`=TRUE")) {
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(readResultSet(rs, true));
                }
            }
        }

        return result;
    }

    @Override
    public @NotNull List<BoxItem> updateDefaultItems(@NotNull Map<BoxItem, DefaultItem> itemMap) throws Exception {
        var result = new ArrayList<BoxItem>(itemMap.size());

        try (var connection = database.getConnection();
             var statement = prepareStatement(connection, "UPDATE `%table%` SET `name`=?, `item_data`=? WHERE `id`=?")) {

            for (var entry : itemMap.entrySet()) {
                var boxItem = entry.getKey();
                var defaultItem = entry.getValue();

                statement.setString(1, defaultItem.plainName());
                writeBytesToStatement(statement, 2, defaultItem.itemStack().serializeAsBytes());
                statement.setInt(3, boxItem.getInternalId());

                statement.addBatch();
                result.add(BoxItemFactory.createDefaultItem(defaultItem, boxItem.getInternalId()));
            }

            statement.executeBatch();
        }

        return result;
    }

    @Override
    public @NotNull List<BoxItem> saveNewDefaultItems(@NotNull List<DefaultItem> newItems) throws Exception {
        var result = new ArrayList<BoxItem>(newItems.size());
        var map = new HashMap<String, DefaultItem>(newItems.size(), 2.0f);

        try (var connection = database.getConnection()) {
            try (var statement = prepareStatement(connection, "INSERT INTO `%table%` (`name`, `item_data`, `is_default_item`) VALUES(?,?,?)")) {
                for (var defaultItem : newItems) {
                    map.put(defaultItem.plainName(), defaultItem);

                    statement.setString(1, defaultItem.plainName());

                    writeBytesToStatement(statement, 2, defaultItem.itemStack().serializeAsBytes());
                    statement.setBoolean(3, true);

                    statement.addBatch();
                }

                statement.executeBatch();
            }

            try (var statement = prepareStatement(connection, "SELECT `id`, `name` FROM `%table%` WHERE `is_default_item`=?")) {
                statement.setBoolean(1, true);

                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        var name = resultSet.getString("name");

                        result.add(BoxItemFactory.createDefaultItem(map.get(name), id));
                    }
                }
            }
        }

        return result;
    }

    @Override
    public @NotNull List<BoxCustomItem> loadAllCustomItems() throws Exception {
        var result = new ArrayList<BoxCustomItem>();

        try (var connection = database.getConnection();
             var statement = prepareStatement(connection, "SELECT `id`, `name`, `item_data` FROM `%table%` WHERE `is_default_item`=FALSE")) {
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add((BoxCustomItem) readResultSet(rs, false));
                }
            }
        }

        return result;
    }

    @Override
    public void updateCustomItems(@NotNull Collection<BoxCustomItem> items) throws Exception {
        try (var connection = database.getConnection();
             var statement = prepareStatement(connection, "UPDATE `%table%` SET `name`=?, `item_data`=? WHERE `id`=?")) {

            for (var item : items) {
                statement.setString(1, item.getPlainName());
                writeBytesToStatement(statement, 2, item.getOriginal().serializeAsBytes());
                statement.setInt(3, item.getInternalId());

                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    @Override
    public @NotNull BoxCustomItem saveNewCustomItem(@NotNull ItemStack item) throws Exception {
        return saveNewCustomItem(item, null);
    }

    @Override
    public @NotNull BoxCustomItem saveNewCustomItem(@NotNull ItemStack item, @Nullable String itemName) throws Exception {
        try (var connection = database.getConnection()) {
            var itemBytes = item.serializeAsBytes();
            itemName = itemName != null ? itemName : ItemNameGenerator.generate(item.getType().name(), itemBytes);

            try (var statement = prepareStatement(connection, "INSERT INTO `%table%` (`name`, `item_data`, `is_default_item`) VALUES(?,?,?)")) {
                statement.setString(1, itemName);
                writeBytesToStatement(statement, 2, itemBytes);
                statement.setBoolean(3, false);

                statement.execute();
            }

            try (var statement = prepareStatement(connection, "SELECT `id` FROM `%table%` WHERE `name`=? LIMIT 1")) {
                statement.setString(1, itemName);

                try (var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        return BoxItemFactory.createCustomItem(item, itemName, id);
                    }
                }
            }
        }

        throw new IllegalStateException("Could not create BoxCustomItem");
    }

    @Override
    public @NotNull BoxCustomItem rename(@NotNull BoxCustomItem item, @NotNull String newName) throws Exception {
        try (var connection = database.getConnection();
             var statement = prepareStatement(connection, "UPDATE `%table%` SET `name`=? WHERE `id`=?")) {

            statement.setString(1, newName);
            statement.setInt(2, item.getInternalId());

            statement.execute();
        }

        BoxItemFactory.renameCustomItem(item, newName);
        return item;
    }

    private @NotNull BoxItem readResultSet(@NotNull ResultSet resultSet, boolean isDefaultItem) throws SQLException {
        int id = resultSet.getInt("id");
        var name = resultSet.getString("name");
        var itemData = readBytesFromResultSet(resultSet, "item_data");

        var item = ItemStack.deserializeBytes(itemData);

        return isDefaultItem ?
                BoxItemFactory.createDefaultItem(item, name, id) :
                BoxItemFactory.createCustomItem(item, name, id);
    }
}
