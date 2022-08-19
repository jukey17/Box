package net.okocraft.box.storage.api.model.user;

import net.okocraft.box.api.model.user.BoxUser;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface UserStorage {

    void init() throws Exception;

    @NotNull BoxUser getUser(@NotNull UUID uuid) throws Exception;

    void saveBoxUser(@NotNull BoxUser user) throws Exception;

    void saveBoxUserIfNotExists(@NotNull BoxUser user) throws Exception;

    @NotNull Optional<BoxUser> search(@NotNull String name) throws Exception;
}