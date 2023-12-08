package net.okocraft.box.core.model.manager.stock;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.api.event.BoxEvent;
import net.okocraft.box.api.event.stockholder.StockHolderLoadEvent;
import net.okocraft.box.api.event.stockholder.StockHolderResetEvent;
import net.okocraft.box.api.event.stockholder.StockHolderSaveEvent;
import net.okocraft.box.api.event.stockholder.stock.StockDecreaseEvent;
import net.okocraft.box.api.event.stockholder.stock.StockEvent;
import net.okocraft.box.api.event.stockholder.stock.StockIncreaseEvent;
import net.okocraft.box.api.event.stockholder.stock.StockOverflowEvent;
import net.okocraft.box.api.event.stockholder.stock.StockSetEvent;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.api.model.manager.StockManager;
import net.okocraft.box.api.model.stock.StockData;
import net.okocraft.box.api.model.stock.StockEventCaller;
import net.okocraft.box.api.model.stock.StockHolder;
import net.okocraft.box.api.model.user.BoxUser;
import net.okocraft.box.api.scheduler.BoxScheduler;
import net.okocraft.box.api.util.BoxLogger;
import net.okocraft.box.core.model.loader.LoadingPersonalStockHolder;
import net.okocraft.box.core.model.manager.stock.autosave.ChangeQueue;
import net.okocraft.box.core.model.stock.StockHolderFactory;
import net.okocraft.box.storage.api.model.stock.PartialSavingStockStorage;
import net.okocraft.box.storage.api.model.stock.StockStorage;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class BoxStockManager implements StockManager {

    private static final long MILLISECONDS_TO_UNLOAD = Duration.ofMinutes(10).toMillis();

    private final StockStorage stockStorage;
    private final IntFunction<BoxItem> toBoxItem;
    private final Predicate<UUID> onlineChecker;
    private final ChangeQueue.Factory queueFactory;

    private final Object2ReferenceMap<UUID, LoadingPersonalStockHolder> loaderMap = Object2ReferenceMaps.synchronize(new Object2ReferenceOpenHashMap<>());
    private final ConcurrentLinkedQueue<ChangeQueue> availableQueues = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean autoSaveTaskScheduled = new AtomicBoolean(false);

    public BoxStockManager(@NotNull StockStorage stockStorage, @NotNull IntFunction<BoxItem> toBoxItem, @NotNull Predicate<UUID> onlineChecker) {
        this.stockStorage = stockStorage;
        this.toBoxItem = toBoxItem;
        this.onlineChecker = onlineChecker;
        this.queueFactory = ChangeQueue.createFactory(stockStorage, this::logStockStorageError);
    }

    @Override
    public @NotNull LoadingPersonalStockHolder getPersonalStockHolder(@NotNull BoxUser user) {
        return this.loaderMap.computeIfAbsent(user.getUUID(), ignored -> this.createLoader(user));
    }

    private @NotNull LoadingPersonalStockHolder createLoader(@NotNull BoxUser user) {
        return new LoadingPersonalStockHolder(user, this::loadStockHolder);
    }

    private @NotNull StockHolder loadStockHolder(@NotNull LoadingPersonalStockHolder loader) {
        var user = loader.getUser();

        Collection<StockData> stockData;

        try {
            stockData = this.stockStorage.loadStockData(user.getUUID());
        } catch (Exception e) {
            throw new RuntimeException("Could not load user's stock holder (" + user.getUUID() + ")", e);
        }

        var eventCaller = new QueuingStockEventCaller(loader);
        var stockHolder = StockHolderFactory.create(user, eventCaller, stockData, this.toBoxItem);
        var queue = this.queueFactory.createQueue(stockHolder);

        eventCaller.queue = queue;

        this.availableQueues.offer(queue);

        BoxProvider.get().getEventBus().callEventAsync(new StockHolderLoadEvent(loader));

        return stockHolder;
    }

    @Override
    public @NotNull StockHolder createStockHolder(@NotNull UUID uuid, @NotNull String name, @NotNull StockEventCaller eventCaller) {
        return this.createStockHolder(uuid, name, eventCaller, Collections.emptyList());
    }

    @Override
    public @NotNull StockHolder createStockHolder(@NotNull UUID uuid, @NotNull String name, @NotNull StockEventCaller eventCaller, @NotNull Collection<StockData> stockData) {
        return StockHolderFactory.create(uuid, name, eventCaller, stockData, this.toBoxItem);
    }

    public void schedulerAutoSaveTask(@NotNull BoxScheduler scheduler) {
        this.autoSaveTaskScheduled.set(true);
        scheduler.scheduleRepeatingAsyncTask(this::saveChangesAndCleanupOffline, Duration.ofMinutes(5), this.autoSaveTaskScheduled::get);
    }

    public void close() {
        this.autoSaveTaskScheduled.set(false);

        ChangeQueue queue;

        while ((queue = this.availableQueues.poll()) != null) {
            queue.saveChanges();

            var removedLoader = this.loaderMap.remove(queue.getStockHolder().getUUID());

            if (removedLoader != null) {
                removedLoader.unload();
            }
        }

        if (this.stockStorage instanceof PartialSavingStockStorage partialSavingStockStorage) {
            try {
                partialSavingStockStorage.cleanupZeroStockData();
            } catch (Exception e) {
                BoxLogger.logger().error("Could not cleanup stock data", e);
            }
        }

        this.loaderMap.clear();
    }

    private void saveChangesAndCleanupOffline() {
        ChangeQueue queue;
        ChangeQueue firstRestoredQueue = null;

        while ((queue = this.availableQueues.poll()) != null) {
            if (queue == firstRestoredQueue) {
                this.availableQueues.offer(queue);
                break;
            }

            queue.saveChanges();

            var uuid = queue.getStockHolder().getUUID();
            var loader = this.loaderMap.get(uuid);

            if (loader == null) {
                continue;
            }

            BoxProvider.get().getEventBus().callEventAsync(new StockHolderSaveEvent(loader));

            if (this.onlineChecker.test(uuid) || !loader.unloadIfNeeded(MILLISECONDS_TO_UNLOAD)) {
                this.availableQueues.offer(queue);

                if (firstRestoredQueue == null) {
                    firstRestoredQueue = queue;
                }
            }
        }
    }

    private void logStockStorageError(@NotNull StockHolder stockHolder, @NotNull Exception e) {
        BoxLogger.logger().error("Could not save user's stock holder (name: {} uuid: {})", stockHolder.getName(), stockHolder.getUUID(), e);
    }

    private static class QueuingStockEventCaller implements StockEventCaller {

        private final LoadingPersonalStockHolder loader;
        private ChangeQueue queue; // initialize later

        private QueuingStockEventCaller(@NotNull LoadingPersonalStockHolder loader) {
            this.loader = loader;
        }

        @Override
        public void callSetEvent(@NotNull StockHolder stockHolder, @NotNull BoxItem item, int amount, int previousAmount, StockEvent.@NotNull Cause cause) {
            this.queue.rememberChange(item.getInternalId());
            this.callEvent(new StockSetEvent(this.loader, item, amount, previousAmount, cause));
        }

        @Override
        public void callIncreaseEvent(@NotNull StockHolder stockHolder, @NotNull BoxItem item, int increments, int currentAmount, StockEvent.@NotNull Cause cause) {
            this.queue.rememberChange(item.getInternalId());
            this.callEvent(new StockIncreaseEvent(this.loader, item, increments, currentAmount, cause));
        }

        @Override
        public void callOverflowEvent(@NotNull StockHolder stockHolder, @NotNull BoxItem item, int increments, int excess, StockEvent.@NotNull Cause cause) {
            this.queue.rememberChange(item.getInternalId());
            this.callEvent(new StockOverflowEvent(this.loader, item, increments, excess, cause));
        }

        @Override
        public void callDecreaseEvent(@NotNull StockHolder stockHolder, @NotNull BoxItem item, int decrements, int currentAmount, StockEvent.@NotNull Cause cause) {
            this.queue.rememberChange(item.getInternalId());
            this.callEvent(new StockDecreaseEvent(this.loader, item, decrements, currentAmount, cause));
        }

        @Override
        public void callResetEvent(@NotNull StockHolder stockHolder, @NotNull Collection<StockData> stockDataBeforeReset) {
            this.queue.rememberReset(stockDataBeforeReset);
            this.callEvent(new StockHolderResetEvent(this.loader, stockDataBeforeReset));
        }

        private void callEvent(@NotNull BoxEvent event) {
            BoxProvider.get().getEventBus().callEventAsync(event);
        }
    }
}
