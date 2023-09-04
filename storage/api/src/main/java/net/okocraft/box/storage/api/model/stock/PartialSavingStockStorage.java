package net.okocraft.box.storage.api.model.stock;

import net.okocraft.box.api.model.stock.StockData;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public interface PartialSavingStockStorage extends StockStorage {

    void savePartialStockData(@NotNull UUID uuid, @NotNull Collection<StockData> stockData) throws Exception;

}
