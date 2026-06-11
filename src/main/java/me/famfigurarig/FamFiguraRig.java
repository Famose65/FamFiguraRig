package me.famfigurarig;

import me.famfigurarig.lua.plugins.FamRigPlugin;
import net.fabricmc.api.ClientModInitializer;

public class FamFiguraRig implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        new FamRigPlugin();   // регистрирует Lua-глобал FamRig для аватаров
        // ВАЖНО: периодическое авто-сохранение (SkinManager.tick) УБРАНО.
        // Оно каждые 2с писало config.json в папку аватара → вотчер Figura видел
        // изменение и ПОСТОЯННО перезагружал аватар (колесо мигало, пресеты затирались).
        // Теперь конфиг сохраняется ТОЛЬКО при смене скина — в SkinManager.selectSkin:
        // сначала saveConfigToActive (сохранить текущий) → подмена файлов → загрузка
        // конфига нового скина → одна перезагрузка.
    }
}
