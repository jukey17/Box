package net.okocraft.box.command.box;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.okocraft.box.Box;
import net.okocraft.box.database.Database;
import net.okocraft.box.util.GeneralConfig;
import net.okocraft.box.util.MessageConfig;

abstract class BaseSubCommand {

    static final Box INSTANCE = Box.getInstance();
    static final GeneralConfig CONFIG = INSTANCE.getGeneralConfig();
    static final MessageConfig MESSAGE_CONFIG = INSTANCE.getMessageConfig();
    static final Database DATABASE = INSTANCE.getDatabase();

    /**
     * コンストラクタ
     */
    BaseSubCommand() {}

    /**
     * コマンドの処理内容
     * 
     * @param sender
     * @param args
     * @return コマンドが成功したらtrue
     */
    abstract boolean onCommand(CommandSender sender, String[] args);

    /**
     * コマンドのタブ補完の内容
     * 
     * @param sender
     * @param args
     * @return その時のタブ補完のリスト
     */
    abstract List<String> onTabComplete(CommandSender sender, String[] args);

    /**
     * コマンドの名前を取得する。
     * 
     * @return コマンドの名前
     */
    abstract String getCommandName();

    /**
     * このコマンドの権限を取得する。
     * 
     * @return 権限
     */
    String getPermissionNode() {
        return "box." + getCommandName();
    }

    /**
     * 最低限必要な引数の長さを取得する。
     * 
     * @return 最低限の引数の長さ
     */
    abstract int getLeastArgLength();

    /**
     * コマンドの引数の内容を取得する。例: "/box autostoreList [page]"
     * 
     * @return 引数の内容
     */
    abstract String getUsage();

    /**
     * コマンドの説明を取得する。例: "アイテムの自動収納の設定をリストにして表示する。"
     * 
     * @return コマンドの説明
     */
    abstract String getDescription();

    /**
     * 権限や引数の長さなどが基準を満たしているか確認する。
     * 
     * @return 満たしていればtrue
     */
    boolean validate(CommandSender sender, String[] args) {
        if ((sender instanceof Player) && !sender.hasPermission(getPermissionNode())) {
            sender.sendMessage(MESSAGE_CONFIG.getPermissionDenied());
            return false;
        }

        if (args.length < getLeastArgLength()) {
            sender.sendMessage(MESSAGE_CONFIG.getNotEnoughArguments());
            return false;
        }

        return true;
    }
}