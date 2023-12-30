package net.okocraft.box.core.model.manager.item;

import com.github.siroshun09.event4j.caller.AsyncEventCaller;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import net.okocraft.box.api.event.BoxEvent;
import net.okocraft.box.api.event.item.CustomItemRegisterEvent;
import net.okocraft.box.api.event.item.CustomItemRenameEvent;
import net.okocraft.box.api.model.item.BoxCustomItem;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.api.model.item.ItemVersion;
import net.okocraft.box.api.model.manager.ItemManager;
import net.okocraft.box.api.model.result.item.ItemRegistrationResult;
import net.okocraft.box.api.model.result.item.ItemRenameResult;
import net.okocraft.box.api.scheduler.BoxScheduler;
import net.okocraft.box.storage.api.factory.item.BoxItemFactory;
import net.okocraft.box.storage.api.model.item.ItemStorage;
import net.okocraft.box.storage.api.util.item.DefaultItemProvider;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class BoxItemManager implements ItemManager {

    private final ItemStorage itemStorage;
    private final AsyncEventCaller<BoxEvent> eventCaller;
    private final BoxScheduler scheduler;
    private final DefaultItemProvider defaultItemProvider;
    private final BukkitBoxItemMap boxItemMap;

    public BoxItemManager(@NotNull ItemStorage itemStorage, @NotNull AsyncEventCaller<BoxEvent> eventCaller,
                          @NotNull BoxScheduler scheduler, @NotNull DefaultItemProvider defaultItemProvider, @NotNull Iterator<BoxItem> initialBoxItemIterator) {
        this.itemStorage = itemStorage;
        this.eventCaller = eventCaller;
        this.scheduler = scheduler;
        this.defaultItemProvider = defaultItemProvider;
        this.boxItemMap = BukkitBoxItemMap.withItems(initialBoxItemIterator);
    }

    @Override
    public @NotNull Optional<BoxItem> getBoxItem(@NotNull ItemStack itemStack) {
        return Optional.ofNullable(this.boxItemMap.getByItemStack(itemStack));
    }

    @Override
    public @NotNull Optional<BoxItem> getBoxItem(@NotNull String name) {
        Objects.requireNonNull(name);
        BoxItem result = this.boxItemMap.getByItemName(name);
        return result != null ? Optional.of(result) : Optional.ofNullable(this.boxItemMap.getByItemName(name.toUpperCase(Locale.ENGLISH))); // backward compatibility
    }

    @Override
    public @NotNull Optional<BoxItem> getBoxItem(int id) {
        return Optional.ofNullable(this.boxItemMap.getById(id));
    }

    @Override
    public @Nullable BoxItem getBoxItemOrNull(int id) {
        return this.boxItemMap.getById(id);
    }

    @Override
    public @NotNull IntImmutableList getItemIdList() {
        return this.boxItemMap.getItemIdList();
    }

    @Override
    public @NotNull ObjectImmutableList<String> getItemNameList() {
        return this.boxItemMap.getItemNameList();
    }

    @Override
    public @NotNull ObjectImmutableList<BoxItem> getItemList() {
        return this.boxItemMap.getItemList();
    }

    @Override
    public boolean isRegistered(@NotNull ItemStack itemStack) {
        return this.boxItemMap.isRegistered(itemStack);
    }

    @Override
    public boolean isUsedName(@NotNull String name) {
        return this.boxItemMap.isRegistered(name);
    }

    @Override
    public boolean isCustomItem(@NotNull BoxItem item) {
        return BoxItemFactory.checkCustomItem(item);
    }

    @Override
    public void registerCustomItem(@NotNull ItemStack original, @Nullable String plainName, @NotNull Consumer<ItemRegistrationResult> resultConsumer) {
        var one = original.asOne();
        Objects.requireNonNull(resultConsumer);

        this.scheduler.runAsyncTask(() -> {
            this.boxItemMap.acquireWriteLock();
            ItemRegistrationResult result;

            try {
                result = this.registerNewCustomItem(one, plainName);
            } catch (Exception e) {
                result = new ItemRegistrationResult.ExceptionOccurred(e);
            } finally {
                this.boxItemMap.releaseWriteLock();
            }

            if (result instanceof ItemRegistrationResult.Success success) {
                this.eventCaller.callAsync(new CustomItemRegisterEvent(success.customItem()));
            }

            resultConsumer.accept(result);
        });
    }

    // Note: Update BoxItemMapTest#testRegisterItem when this method modified.
    private @NotNull ItemRegistrationResult registerNewCustomItem(@NotNull ItemStack original, @Nullable String plainName) throws Exception {
        if (plainName != null && this.boxItemMap.checkItemNameAtUnsynchronized(plainName)) {
            return new ItemRegistrationResult.DuplicateName(plainName);
        }

        if (this.boxItemMap.checkItemAtUnsynchronized(original)) {
            return new ItemRegistrationResult.DuplicateItem(original);
        }

        var customItem = this.itemStorage.saveNewCustomItem(original, plainName);

        this.boxItemMap.addItemAtUnsynchronized(customItem);
        this.boxItemMap.rebuildCache();

        return new ItemRegistrationResult.Success(customItem);
    }

    @Override
    public void renameCustomItem(@NotNull BoxCustomItem item, @NotNull String newName, @NotNull Consumer<ItemRenameResult> resultConsumer) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(newName);
        Objects.requireNonNull(resultConsumer);

        if (!BoxItemFactory.checkCustomItem(item)) {
            throw new IllegalStateException("Could not rename item because the item is not created by box.");
        }

        this.scheduler.runAsyncTask(() -> {
            this.boxItemMap.acquireWriteLock();
            ItemRenameResult result;

            try {
                result = this.renameItem(item, newName);
            } catch (Exception e) {
                result = new ItemRenameResult.ExceptionOccurred(e);
            } finally {
                this.boxItemMap.releaseWriteLock();
            }

            if (result instanceof ItemRenameResult.Success success) {
                this.eventCaller.callAsync(new CustomItemRenameEvent(success.customItem(), success.previousName()));
            }

            resultConsumer.accept(result);
        });
    }

    // Note: Update BoxItemMapTest#testRenameItem when this method modified.
    private @NotNull ItemRenameResult renameItem(@NotNull BoxCustomItem item, @NotNull String newName) throws Exception {
        if (this.boxItemMap.checkItemNameAtUnsynchronized(newName)) {
            return new ItemRenameResult.DuplicateName(newName);
        }

        this.boxItemMap.removeItemAtUnsynchronized(item);

        var previousName = item.getPlainName();
        var customItem = this.itemStorage.renameCustomItem(item, newName);

        this.boxItemMap.addItemAtUnsynchronized(customItem);
        this.boxItemMap.rebuildCache();

        return new ItemRenameResult.Success(customItem, previousName);
    }

    @Override
    public @NotNull ItemVersion getCurrentVersion() {
        return this.defaultItemProvider.version();
    }

    @Override
    public @NotNull UnaryOperator<String> getItemNameConverter(@NotNull ItemVersion sourceVersion) {
        return this.defaultItemProvider.itemNamePatcherFactory().create(sourceVersion)::renameIfNeeded;
    }
}
