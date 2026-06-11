package me.famfigurarig;

import me.famfigurarig.lua.plugins.FamRigPlugin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class FamFiguraRig implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        new FamRigPlugin();   // регистрирует Lua-глобал FamRig для аватаров
        // авто-сохранение конфига активного скина (см. SkinManager.tick)
        ClientTickEvents.END_CLIENT_TICK.register(client -> SkinManager.tick());
    }
}
