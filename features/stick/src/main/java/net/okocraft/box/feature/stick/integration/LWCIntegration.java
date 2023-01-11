package net.okocraft.box.feature.stick.integration;

import com.griefcraft.integration.IPermissions;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Permission;
import com.griefcraft.model.Protection;
import net.okocraft.box.feature.stick.function.container.ContainerOperation;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LWCIntegration {

    public static boolean canModifyInventory(@NotNull Player player, @NotNull BlockState state, @NotNull ContainerOperation.OperationType operationType) {
        if (Bukkit.getPluginManager().getPlugin("LWC") == null) {
            return true;
        }

        var protection = LWC.getInstance().findProtection(state);

        if (protection == null) {
            return true;
        }

        return switch (protection.getType()) {
            case DONATION -> operationType == ContainerOperation.OperationType.WITHDRAW  || canAccess(player, protection);
            case DISPLAY -> canAccess(player, protection);
            default -> {
                if (protection.getType().name().equals("SUPPLY")) { // SUPPLY is the protection type that is only available in OKOCRAFT currently.
                    yield operationType == ContainerOperation.OperationType.DEPOSIT || canAccess(player, protection);
                } else {
                    yield true; // Otherwise, the click to the chest has already been rejected.
                }
            }
        };
    }

    private static boolean canAccess(@NotNull Player player, @NotNull Protection protection) {
        // copied from https://github.com/pop4959/LWCX/blob/master/src/main/java/com/griefcraft/listeners/LWCPlayerListener.java#L798-L815
        if (LWC.getInstance().canAdminProtection(player, protection)) {
            return true;
        }

        boolean canAccess = false;
        if (protection.getAccess(player.getUniqueId().toString(), Permission.Type.PLAYER) == Permission.Access.PLAYER) {
            canAccess = true;
        } else if (protection.getAccess(player.getName(), Permission.Type.PLAYER) == Permission.Access.PLAYER) {
            canAccess = true;
        } else {
            IPermissions permissions = LWC.getInstance().getPermissions();
            if (permissions != null) {
                for (String groupName : permissions.getGroups(player)) {
                    if (protection.getAccess(groupName, Permission.Type.GROUP) == Permission.Access.PLAYER) {
                        canAccess = true;
                    }
                }
            }
        }

        return canAccess;
    }
}
