package net.okocraft.box.bootstrap;

import com.github.siroshun09.event4j.bus.EventBus;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.okocraft.box.api.event.BoxEvent;
import net.okocraft.box.api.feature.BoxFeature;
import net.okocraft.box.storage.api.registry.StorageRegistry;
import net.okocraft.box.util.TranslationDirectoryUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class BoxBootstrapContext {

    @Contract("_ -> new")
    @SuppressWarnings("UnstableApiUsage")
    public static @NotNull BoxBootstrapContext create(@NotNull BootstrapContext context) {
        return new BoxBootstrapContext(
                context.getDataDirectory(),
                context.getPluginSource(),
                context.getConfiguration().getVersion()
        );
    }

    private final Path dataDirectory;
    private final Path jarFile;
    private final String version;
    private final StorageRegistry storageRegistry;
    private final EventBus<BoxEvent> eventBus;
    private final List<Supplier<? extends BoxFeature>> boxFeatureList = new ArrayList<>();
    private final TranslationDirectoryUtil.PathConsumerWrapper onLanguageDirectoryCreated;
    private final TranslationDirectoryUtil.TranslationLoaderCreatorHolder translationLoaderCreators;

    private BoxBootstrapContext(@NotNull Path pluginDirectory, @NotNull Path jarFile, @NotNull String version) {
        this.dataDirectory = pluginDirectory;
        this.jarFile = jarFile;
        this.version = version;
        this.storageRegistry = new StorageRegistry();
        this.eventBus = EventBus.create(BoxEvent.class);
        this.onLanguageDirectoryCreated = TranslationDirectoryUtil.createPathConsumer();
        this.translationLoaderCreators = TranslationDirectoryUtil.createCreatorHolder();
    }

    public @NotNull Path getPluginDirectory() {
        return dataDirectory;
    }

    public @NotNull Path getJarFile() {
        return jarFile;
    }

    public @NotNull String getVersion() {
        return version;
    }

    public @NotNull StorageRegistry getStorageRegistry() {
        return storageRegistry;
    }

    public @NotNull EventBus<BoxEvent> getEventBus() {
        return eventBus;
    }

    public @NotNull List<Supplier<? extends BoxFeature>> getBoxFeatureList() {
        return boxFeatureList;
    }

    @Contract("_ -> this")
    public @NotNull BoxBootstrapContext addFeature(@NotNull Supplier<? extends BoxFeature> featureSupplier) {
        boxFeatureList.add(featureSupplier);
        return this;
    }

    public @NotNull TranslationDirectoryUtil.PathConsumerWrapper onLanguageDirectoryCreated() {
        return onLanguageDirectoryCreated;
    }

    public @NotNull TranslationDirectoryUtil.TranslationLoaderCreatorHolder getTranslationLoaderCreators() {
        return translationLoaderCreators;
    }
}
