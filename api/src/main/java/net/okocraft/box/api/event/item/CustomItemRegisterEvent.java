package net.okocraft.box.api.event.item;

import net.okocraft.box.api.event.BoxEvent;
import net.okocraft.box.api.model.item.BoxCustomItem;
import org.jetbrains.annotations.NotNull;

/**
 * An event that is called when a new {@link BoxCustomItem} has been registered.
 */
public class CustomItemRegisterEvent extends BoxEvent {

    private final BoxCustomItem newItem;

    /**
     * The constructor of {@link CustomItemRegisterEvent}.
     *
     * @param newItem a registered {@link BoxCustomItem}
     */
    public CustomItemRegisterEvent(@NotNull BoxCustomItem newItem) {
        this.newItem = newItem;
    }

    /**
     * Gets the registered {@link BoxCustomItem}.
     *
     * @return the registered {@link BoxCustomItem}
     */
    public @NotNull BoxCustomItem getNewItem() {
        return newItem;
    }

    @Override
    public String toString() {
        return "CustomItemRegisterEvent{" +
                "newItem=" + newItem +
                '}';
    }
}
