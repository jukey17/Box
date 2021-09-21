package net.okocraft.box.api.event.stock;

import net.okocraft.box.api.model.stock.StockHolder;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link StockEvent} called when the stockholder has been saved.
 */
public class StockSaveEvent extends StockEvent{

    /**
     * The constructor of {@link StockSaveEvent}.
     *
     * @param stockHolder the saved stock
     */
    public StockSaveEvent(@NotNull StockHolder stockHolder) {
        super(stockHolder);
    }

    @Override
    public String toString() {
        return "StockSaveEvent{" +
                "stockholder=" + getStockHolder() +
                "}";
    }
}
