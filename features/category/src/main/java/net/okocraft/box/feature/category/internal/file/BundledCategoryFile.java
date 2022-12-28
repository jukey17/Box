package net.okocraft.box.feature.category.internal.file;

import com.github.siroshun09.configapi.api.util.ResourceUtils;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.api.util.MCDataVersion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public final class BundledCategoryFile {

    public static void copy(@NotNull Path dist) throws IOException {
        ResourceUtils.copyFromJarIfNotExists(BoxProvider.get().getJar(), getFilename(), dist);
    }

    private static @NotNull String getFilename() {
        var currentVer = MCDataVersion.CURRENT;
        String version;

        if (currentVer.isBeforeOrSame(MCDataVersion.MC_1_17_1)) { // ~ 1.17.1
            version = "1_17";
        } else if (currentVer.isBeforeOrSame(MCDataVersion.MC_1_18_2)) { // 1.18, 1.18.1, 1.18.2
            version = "1_18";
        } else if (currentVer.isBeforeOrSame(MCDataVersion.MC_1_19_2)) { // 1.19, 1.19.1, 1.19.2
            version = "1_19";
        } else if (currentVer.isSame(MCDataVersion.MC_1_19_3)) { // 1.19.3
            version = "1_19_3";
        } else {
            // Future version? Use latest categories.yml
            version = "1_19_3";
        }

        return "categories_" + version + ".yml";
    }
}