package net.okocraft.box.api.model.stock;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.okocraft.box.api.event.stockholder.stock.StockEvent;
import net.okocraft.box.api.model.item.BoxItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.UUID;

/**
 * An interface that wraps {@link StockHolder}.
 */
@FunctionalInterface
public interface StockHolderWrapper extends StockHolder {

    /**
     * Gets the delegating {@link StockHolder}.
     *
     * @return the delegating {@link StockHolder}
     */
    @NotNull StockHolder delegate();

    @Override
    default @NotNull String getName() {
        return this.delegate().getName();
    }

    @Override
    default @NotNull UUID getUUID() {
        return this.delegate().getUUID();
    }

    @Override
    default int getAmount(int itemId) {
        return this.delegate().getAmount(itemId);
    }

    @Override
    default void setAmount(@NotNull BoxItem item, @Range(from = 0, to = Integer.MAX_VALUE) int amount, StockEvent.@NotNull Cause cause) {
        this.delegate().setAmount(item, amount, cause);
    }

    @Override
    default int increase(@NotNull BoxItem item, @Range(from = 0, to = Integer.MAX_VALUE) int increment, StockEvent.@NotNull Cause cause) {
        return this.delegate().increase(item, increment, cause);
    }

    @Override
    default int decrease(@NotNull BoxItem item, @Range(from = 0, to = Integer.MAX_VALUE) int decrement, StockEvent.@NotNull Cause cause) {
        return this.delegate().decrease(item, decrement, cause);
    }

    @Override
    default int decreaseToZero(@NotNull BoxItem item, @Range(from = 0, to = Integer.MAX_VALUE) int limit, StockEvent.@NotNull Cause cause) {
        return this.delegate().decreaseToZero(item, limit, cause);
    }

    @Override
    default int decreaseIfPossible(@NotNull BoxItem item, @Range(from = 0, to = Integer.MAX_VALUE) int decrement, StockEvent.@NotNull Cause cause) {
        return this.delegate().decreaseIfPossible(item, decrement, cause);
    }

    @Override
    default boolean decreaseIfPossible(@NotNull Object2IntMap<BoxItem> decrementMap, StockEvent.@NotNull Cause cause) {
        return this.delegate().decreaseIfPossible(decrementMap, cause);
    }

    @Override
    default @NotNull @Unmodifiable Collection<BoxItem> getStockedItems() {
        return this.delegate().getStockedItems();
    }

    @Override
    default @NotNull @Unmodifiable Collection<StockData> toStockDataCollection() {
        return this.delegate().toStockDataCollection();
    }

    @Override
    default @NotNull @Unmodifiable Collection<StockData> reset() {
        return this.delegate().reset();
    }
}
