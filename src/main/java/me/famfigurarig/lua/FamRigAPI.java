package me.famfigurarig.lua;

import me.famfigurarig.SkinManager;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaTypeDoc;

import java.util.List;

/**
 * Lua-глобал FamRig для аватара.
 *   FamRig.listSkins()      — имена папок-скинов одной строкой через \n (split в Lua)
 *   FamRig.getActiveSkin()  — имя активного скина ("" если не выбран)
 *   FamRig.selectSkin(имя)  — применить скин (копирование файлов + авторелоад);
 *                             возвращает nil при успехе, иначе текст ошибки
 *   FamRig.openFolder(путь) — открыть проводник ("" = корень аватара, "skins" = каталог)
 *   FamRig.version()        — версия мода
 */
@LuaWhitelist
@LuaTypeDoc(
        name = "FamRig",
        value = "fam_rig"
)
public class FamRigAPI {

    @LuaWhitelist
    public static String listSkins() {
        List<String> skins = SkinManager.listSkins();
        return String.join("\n", skins);
    }

    @LuaWhitelist
    public static String getActiveSkin() {
        return SkinManager.getActive();
    }

    @LuaWhitelist
    public static String selectSkin(String name) {
        return SkinManager.selectSkin(name);
    }

    @LuaWhitelist
    public static boolean openFolder(String rel) {
        return SkinManager.openFolder(rel);
    }

    @LuaWhitelist
    public static String version() {
        return "1.0.0";
    }

    @Override
    public String toString() {
        return "FamRigAPI";
    }
}
