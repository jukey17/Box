package net.okocraft.box.feature.autostore.model.setting;

import net.okocraft.box.api.model.item.BoxItem;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A class to hold the user's auto-store settings.
 */
public class AutoStoreSetting {

    private final UUID uuid;
    private final PerItemSetting perItemSetting = new PerItemSetting();
    private boolean enabled = false;
    private boolean allMode = true;
    private boolean direct = false;

    /**
     * The constructor of the {@link AutoStoreSetting}.
     *
     * @param uuid the user's {@link UUID}
     */
    public AutoStoreSetting(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Gets the user's {@link UUID}.
     *
     * @return the user's {@link UUID}
     */
    public @NotNull UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the {@link PerItemSetting}.
     *
     * @return the {@link PerItemSetting}
     */
    public @NotNull PerItemSetting getPerItemModeSetting() {
        return perItemSetting;
    }

    /**
     * Whether the auto-store is enabled or not.
     *
     * @return if {@code true}, the auto-store is enabled, or if {@code false}, it is disabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the auto-store.
     *
     * @param enabled {@code true} to enable, or {@code false} to disable the auto-store
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Whether the current auto-store mode is all mode.
     *
     * @return if {@code true}, the mode is all mode, or if {@code false}, it is per-item mode
     */
    public boolean isAllMode() {
        return allMode;
    }

    /**
     * Sets the auto-store mode to all mode or per-item mode.
     *
     * @param allMode {@code true} to all mode, or {@code false} to per-item mode.
     */
    public void setAllMode(boolean allMode) {
        this.allMode = allMode;
    }

    /**
     * Whether the direct auto-store is enabled.
     *
     * @return if {@code true}, the direct auto-store is enabled, or if {@code false}, it is disabled
     */
    public boolean isDirect() {
        return this.direct;
    }

    /**
     * Enables or disables the direct auto-store.
     *
     * @param direct {@code true} to enable, or {@code false} to disable the direct auto-store
     */
    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    /**
     * Checks if the {@link BoxItem} should be auto-stored.
     * <p>
     * If the auto-store mode is all mode ({@link #isAllMode()} returns {@code true}),
     * or if the {@link BoxItem} is included in {@link PerItemSetting#getEnabledItems()},
     * this method returns {@code true}. Otherwise, this returns {@code false}.
     *
     * @param item the {@link BoxItem} to check
     * @return if {@code true}, the {@link BoxItem} should be auto-stored, or if {@code false}, it should not be autos-stored
     */
    public boolean shouldAutoStore(@NotNull BoxItem item) {
        return allMode || perItemSetting.isEnabled(item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoStoreSetting that = (AutoStoreSetting) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "AutoStoreSetting{" +
                "uuid=" + uuid +
                ", enabled=" + enabled +
                ", allMode=" + allMode +
                ", direct=" + direct +
                ", perItemModeSetting=" + perItemSetting +
                '}';
    }
}
