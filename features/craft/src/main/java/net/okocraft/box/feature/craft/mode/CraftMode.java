package net.okocraft.box.feature.craft.mode;

import net.kyori.adventure.text.Component;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.feature.craft.lang.Displays;
import net.okocraft.box.feature.craft.menu.CraftMenu;
import net.okocraft.box.feature.craft.menu.RecipeSelector;
import net.okocraft.box.feature.craft.model.RecipeHolder;
import net.okocraft.box.feature.gui.api.menu.Menu;
import net.okocraft.box.feature.gui.api.mode.BoxItemClickMode;
import net.okocraft.box.feature.gui.api.mode.SettingMenuButton;
import net.okocraft.box.feature.gui.api.util.MenuOpener;
import net.okocraft.box.feature.gui.api.util.TranslationUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("ClassCanBeRecord")
public class CraftMode implements BoxItemClickMode {

    private final Map<BoxItem, RecipeHolder> recipeMap;

    public CraftMode(@NotNull Map<BoxItem, RecipeHolder> recipeMap) {
        this.recipeMap = recipeMap;
    }

    @Override
    public @NotNull String getName() {
        return "craft";
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Displays.CRAFT_MODE;
    }

    @Override
    public void onClick(@NotNull Context context) {
        var clicker = context.clicker();
        var recipes = recipeMap.get(context.item());

        if (recipes == null) {
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 100f, 1.5f);
            return;
        }

        Menu menu;

        if (recipes.getRecipeList().size() == 1) {
            menu = new CraftMenu(recipes.getRecipeList().get(0), context.currentMenu());
        } else {
            menu = new RecipeSelector(context.item(), recipes, context.currentMenu());
        }

        MenuOpener.open(menu, clicker);
    }

    @Override
    public void applyIconMeta(@NotNull Player viewer, @NotNull BoxItem item, @NotNull ItemMeta target) {
        var newLore = Optional.ofNullable(target.lore()).map(ArrayList::new).orElseGet(ArrayList::new);

        newLore.add(Component.empty());

        if (recipeMap.containsKey(item)) {
            newLore.add(TranslationUtil.render(Displays.CLICK_TO_SHOW_RECIPES, viewer));
        } else {
            newLore.add(TranslationUtil.render(Displays.RECIPE_NOT_FOUND, viewer));
        }

        newLore.add(Component.empty());

        target.lore(newLore);
    }

    @Override
    public boolean hasSettingMenu() {
        return false;
    }

    @Override
    public @NotNull SettingMenuButton createSettingMenuButton(@NotNull Player viewer, @NotNull Menu currentMenu) {
        throw new UnsupportedOperationException();
    }
}
