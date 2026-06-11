package me.famfigurarig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * Ядро FamFiguraRig — каталог скинов на папках.
 *
 * Структура (внутри папки экипированного аватара):
 *   skins/<имя скина>/skin.png      → при выборе копируется в <аватар>/skin.png
 *   skins/<имя скина>/elements.png  → при выборе копируется в <аватар>/skin_elements.png
 *   skins/<имя скина>/config.json   ↔ figura/config/model_settings.json
 *   skins/.active                   — имя активного скина (служебный файл)
 *
 * Файлы в папках скинов НЕ изменяются (кроме config.json — туда автосохраняются
 * настройки, пока скин активен). Figura читает текстуры модели с диска по
 * relative_path, поэтому подмена корневых файлов + перезагрузка аватара = смена скина.
 */
public final class SkinManager {

    private static final String SKINS_DIR = "skins";
    private static final String ACTIVE_FILE = ".active";
    private static final String SLOT_CONFIG = "config.json";
    private static final String FIGURA_CONFIG = "model_settings.json";

    private static long lastConfigMtime = 0;
    private static int tickCounter = 0;

    private SkinManager() {}

    /** Папка экипированного локального аватара (null, если аватар не загружен с диска). */
    public static Path avatarDir() {
        Path p = LocalAvatarLoader.getLastLoadedPath();
        if (p == null) return null;
        if (!Files.isDirectory(p)) p = p.getParent();   // на случай запакованного аватара
        return p;
    }

    public static Path skinsDir() {
        Path a = avatarDir();
        return a == null ? null : a.resolve(SKINS_DIR);
    }

    /** figura/config/model_settings.json — живой конфиг колеса (config:setName("model_settings")). */
    public static Path figuraConfigFile() {
        return FiguraMod.getFiguraDirectory().resolve("config").resolve(FIGURA_CONFIG);
    }

    /** Имена папок-скинов, по алфавиту. */
    public static List<String> listSkins() {
        List<String> out = new ArrayList<>();
        Path dir = skinsDir();
        if (dir == null || !Files.isDirectory(dir)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                String name = p.getFileName().toString();
                if (Files.isDirectory(p) && !name.startsWith("."))
                    out.add(name);
            }
        } catch (IOException ignored) {}
        out.sort(Comparator.naturalOrder());
        return out;
    }

    public static String getActive() {
        Path dir = skinsDir();
        if (dir == null) return "";
        try {
            Path f = dir.resolve(ACTIVE_FILE);
            if (Files.exists(f)) return Files.readString(f).trim();
        } catch (IOException ignored) {}
        return "";
    }

    private static void setActive(String name) {
        Path dir = skinsDir();
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(ACTIVE_FILE), name);
        } catch (IOException ignored) {}
    }

    /** Живой конфиг → в папку активного скина (настройки скина сохраняются «сразу»). */
    public static void saveConfigToActive() {
        String act = getActive();
        Path dir = skinsDir();
        Path cfg = figuraConfigFile();
        if (act.isEmpty() || dir == null || !Files.exists(cfg)) return;
        Path slot = dir.resolve(act);
        if (!Files.isDirectory(slot)) return;
        try {
            Files.copy(cfg, slot.resolve(SLOT_CONFIG), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            FiguraMod.LOGGER.error("[FamFiguraRig] не смог сохранить конфиг скина '{}'", act, e);
        }
    }

    /**
     * Выбрать скин: настройки текущего — в его папку; файлы слота — на рабочие места;
     * аватар перезагружается автоматически. Возвращает null при успехе, иначе текст ошибки.
     */
    public static String selectSkin(String name) {
        Path a = avatarDir();
        Path dir = skinsDir();
        if (a == null || dir == null) return "Figura-аватар не загружен с диска";
        if (name == null || name.isEmpty()) return "Не указано имя скина";
        Path slot = dir.resolve(name);
        if (!Files.isDirectory(slot)) return "Нет папки skins/" + name;
        try {
            // 1) настройки текущего скина — в его папку
            saveConfigToActive();

            // 2) текстуры слота — в корень аватара (файлы слота не изменяются)
            Path skin = slot.resolve("skin.png");
            Path elements = slot.resolve("elements.png");
            if (Files.exists(skin))
                Files.copy(skin, a.resolve("skin.png"), StandardCopyOption.REPLACE_EXISTING);
            if (Files.exists(elements))
                Files.copy(elements, a.resolve("skin_elements.png"), StandardCopyOption.REPLACE_EXISTING);

            // 3) конфиг слота — в живой конфиг Figura;
            //    у свежей папки конфига ещё нет — она наследует текущий и получает его копию
            Path slotCfg = slot.resolve(SLOT_CONFIG);
            Path cfg = figuraConfigFile();
            if (Files.exists(slotCfg)) {
                Files.createDirectories(cfg.getParent());
                Files.copy(slotCfg, cfg, StandardCopyOption.REPLACE_EXISTING);
            } else if (Files.exists(cfg)) {
                Files.copy(cfg, slotCfg, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4) пометить активным (до релоада — скрипт стартует уже с новым активным)
            setActive(name);
            lastConfigMtime = mtime(cfg);

            // 5) полная перезагрузка аватара — автоматически, на клиентском потоке
            MinecraftClient.getInstance().execute(() -> AvatarManager.loadLocalAvatar(a));
            return null;
        } catch (IOException e) {
            return "Ошибка копирования: " + e.getMessage();
        }
    }

    /** Открыть папку в проводнике ("" — корень аватара, "skins" — каталог скинов). */
    public static boolean openFolder(String rel) {
        Path a = avatarDir();
        if (a == null) return false;
        Path p = (rel == null || rel.isEmpty()) ? a : a.resolve(rel);
        if (!Files.exists(p)) return false;
        Util.getOperatingSystem().open(p.toFile());
        return true;
    }

    /**
     * Base64 PNG-текстуры скина (skins/<имя>/skin.png) — для превью-иконки слота в колесе.
     * Lua делает из неё текстуру через textures:read(name, base64). "" если файла нет.
     * (Figura не грузит png из подпапок сама, поэтому отдаём байты через мод.)
     */
    public static String skinBase64(String name) {
        Path dir = skinsDir();
        if (dir == null || name == null || name.isEmpty()) return "";
        Path png = dir.resolve(name).resolve("skin.png");
        if (!Files.exists(png)) return "";
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(png));
        } catch (IOException e) {
            return "";
        }
    }

    private static long mtime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    /** Каждые ~2 сек: конфиг изменился → копия в папку активного скина. */
    public static void tick() {
        if (++tickCounter < 40) return;
        tickCounter = 0;
        Path cfg = figuraConfigFile();
        if (!Files.exists(cfg)) return;
        long m = mtime(cfg);
        if (m != lastConfigMtime) {
            lastConfigMtime = m;
            saveConfigToActive();
        }
    }
}
