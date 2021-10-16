package net.okocraft.box.feature.gui.api.util;

import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.feature.gui.api.menu.Menu;
import net.okocraft.box.feature.gui.internal.holder.BoxInventoryHolder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MenuOpener {

    public static void open(@NotNull Menu menu, @NotNull Player viewer) {
        var holder = new BoxInventoryHolder(menu);

        holder.initializeMenu(viewer);
        holder.applyContents();

        BoxProvider.get().getTaskFactory()
                .run(() -> viewer.openInventory(holder.getInventory()))
                .join();

        viewer.playSound(viewer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100f, 2.0f);
    }

    private MenuOpener() {
        throw new UnsupportedOperationException();
    }
}
