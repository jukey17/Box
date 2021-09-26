package net.okocraft.box.feature.autostore.event;

import com.github.siroshun09.event4j.event.Event;
import net.okocraft.box.feature.autostore.model.setting.AutoStoreSetting;
import org.jetbrains.annotations.NotNull;

public class AutoStoreSettingChangeEvent extends Event {

    private final AutoStoreSetting setting;

    public AutoStoreSettingChangeEvent(@NotNull AutoStoreSetting setting) {
        this.setting = setting;
    }

    public @NotNull AutoStoreSetting getSetting() {
        return setting;
    }

    @Override
    public String toString() {
        return "AutoStoreSettingChangeEvent{" +
                "setting=" + setting +
                '}';
    }
}