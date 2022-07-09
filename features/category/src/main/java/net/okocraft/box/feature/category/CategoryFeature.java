package net.okocraft.box.feature.category;

import com.github.siroshun09.configapi.yaml.YamlConfiguration;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.api.feature.AbstractBoxFeature;
import net.okocraft.box.api.feature.Disableable;
import net.okocraft.box.api.feature.Reloadable;
import net.okocraft.box.api.message.Components;
import net.okocraft.box.feature.category.internal.CategoryLoader;
import net.okocraft.box.feature.category.internal.CustomItemListener;
import net.okocraft.box.feature.category.internal.DefaultCategoryFile;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.util.logging.Level;

public class CategoryFeature extends AbstractBoxFeature implements Disableable, Reloadable {

    private final CustomItemListener customItemListener = new CustomItemListener();

    public CategoryFeature() {
        super("category");
    }

    @Override
    public void enable() {
        try (var yaml = YamlConfiguration.create(BoxProvider.get().getPluginDirectory().resolve("categories.yml"))) {
            if (!Files.exists(yaml.getPath())) {
                DefaultCategoryFile.copy(yaml.getPath());
            }

            yaml.load();

            CategoryHolder.addAll(CategoryLoader.load(yaml));

            yaml.save();
        } catch (Exception e) {
            BoxProvider.get().getLogger().log(Level.SEVERE, "Could not load categories.yml", e);
        }

        customItemListener.register(getListenerKey());
    }

    @Override
    public void disable() {
        customItemListener.unregister(getListenerKey());
        CategoryHolder.get().clear();
    }

    @Override
    public void reload(@NotNull CommandSender sender) {
        disable();
        enable();
        sender.sendMessage(Components.grayTranslatable("box.category.reloaded"));
    }
}
