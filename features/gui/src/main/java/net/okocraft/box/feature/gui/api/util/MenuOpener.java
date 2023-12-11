package net.okocraft.box.feature.gui.api.util;

import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.feature.gui.api.menu.Menu;
import net.okocraft.box.feature.gui.api.session.PlayerSession;
import net.okocraft.box.feature.gui.internal.holder.BoxInventoryHolder;
import net.okocraft.box.feature.gui.internal.util.XmasChecker;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public final class MenuOpener {

    private static final SoundBase XMAS_SOUND = SoundBase.builder().sound(Sound.BLOCK_NOTE_BLOCK_CHIME).pitch(1.8f).build();
    private static final SoundBase NORMAL_SOUND = SoundBase.builder().sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP).pitch(2.0f).build();

    public static void open(@NotNull Menu menu, @NotNull PlayerSession session) {
        var holder = new BoxInventoryHolder(menu, session);
        var viewer = session.getViewer();

        BoxProvider.get().getScheduler().runEntityTask(viewer, () -> {
            viewer.openInventory(holder.getInventory());

            if (XmasChecker.isXmas()) {
                XMAS_SOUND.play(viewer);
            } else {
                NORMAL_SOUND.play(viewer);
            }
        });
    }

    public static void open(@NotNull Menu menu, @NotNull PlayerSession session, @NotNull Consumer<UUID> onOpened) {
        var holder = new BoxInventoryHolder(menu, session);
        var viewer = session.getViewer();

        BoxProvider.get().getScheduler().runEntityTask(viewer, () -> {
            viewer.openInventory(holder.getInventory());

            if (XmasChecker.isXmas()) {
                XMAS_SOUND.play(viewer);
            } else {
                NORMAL_SOUND.play(viewer);
            }

            onOpened.accept(viewer.getUniqueId());
        });
    }

    private MenuOpener() {
        throw new UnsupportedOperationException();
    }
}
