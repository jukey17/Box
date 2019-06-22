package net.okocraft.box.util;

import javax.annotation.Nonnull;

public class OtherUtil {

    /**
     * numberを解析してint型にして返す。numberのフォーマットがintではないときはdefを返す。
     * 
     * @author LazyGon
     * @since v1.1.0
     * 
     * @param number
     * @param def
     * 
     * @return int型の数字。
     */
    public static int parseIntOrDefault(@Nonnull String number, int def) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    /**
     * numberを解析してlong型にして返す。numberのフォーマットがlongではないときはdefを返す。
     * 
     * @author LazyGon
     * @since v1.1.0
     * 
     * @param number
     * @param def
     * 
     * @return long型の数字。
     */
    public static long parseLongOrDefault(@Nonnull String number, long def) {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException exception) {
            return def;
        }
    }
}