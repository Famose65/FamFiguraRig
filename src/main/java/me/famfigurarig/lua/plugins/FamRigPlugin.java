package me.famfigurarig.lua.plugins;

import me.famfigurarig.lua.FamRigAPI;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.entries.FiguraAPI;
import org.figuramc.figura.entries.annotations.FiguraAPIPlugin;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaTypeDoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.figuramc.figura.lua.FiguraAPIManager.API_GETTERS;
import static org.figuramc.figura.lua.FiguraAPIManager.WHITELISTED_CLASSES;

@FiguraAPIPlugin
@LuaWhitelist
public class FamRigPlugin implements FiguraAPI {

    public static final Class<?>[] PLUGIN_CLASSES = new Class[] {
            FamRigPlugin.class,
    };

    public static final Class<?>[] DOC_CLASSES = new Class[] {};

    public static final String PLUGIN_ID = "famfigurarig";
    private Avatar avatar;

    public FamRigPlugin(Avatar avatar) {
        this.avatar = avatar;
    }

    public FamRigPlugin() {
        WHITELISTED_CLASSES.add(FamRigAPI.class);
        API_GETTERS.put("FamRig", r -> new FamRigAPI());
    }

    @Override
    public FiguraAPI build(Avatar avatar) {
        return new FamRigPlugin(avatar);
    }

    @Override
    public String getName() {
        return PLUGIN_ID;
    }

    @Override
    public Collection<Class<?>> getWhitelistedClasses() {
        List<Class<?>> classesToRegister = new ArrayList<>();
        for (Class<?> aClass : PLUGIN_CLASSES) {
            if (aClass.isAnnotationPresent(LuaWhitelist.class)) {
                classesToRegister.add(aClass);
            }
        }
        return classesToRegister;
    }

    @Override
    public Collection<Class<?>> getDocsClasses() {
        List<Class<?>> classesToRegister = new ArrayList<>();
        for (Class<?> aClass : DOC_CLASSES) {
            if (aClass.isAnnotationPresent(LuaTypeDoc.class)) {
                classesToRegister.add(aClass);
            }
        }
        return classesToRegister;
    }
}
