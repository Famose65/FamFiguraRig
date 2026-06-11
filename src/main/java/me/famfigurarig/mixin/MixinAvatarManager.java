package me.famfigurarig.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.text.Text;
import org.figuramc.figura.avatar.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Оператор-надзор для съёмок: когда у КОГО-ТО меняется Figura-аватар (по сети),
 * игроки с меткой «FiguraOperator» видят локальное сообщение в чате:
 *   «[Ник] — сменил аватар (имя_аватара)»
 *
 * Метка — любым из двух ванильных способов (что удобнее на сервере):
 *   /scoreboard objectives add FiguraOperator dummy
 *   /scoreboard players set <ник> FiguraOperator 1
 * или команда:
 *   /team add FiguraOperator
 *   /team join FiguraOperator <ник>
 *
 * Сообщение видят только клиенты с модом и меткой; без метки — тишина.
 */
@Mixin(value = AvatarManager.class, remap = false)
public class MixinAvatarManager {

    @Inject(method = "setAvatar", at = @At("HEAD"), remap = false)
    private static void famrig$onSetAvatar(UUID id, NbtCompound nbt, CallbackInfo ci) {
        try {
            // «смена» — только если аватар у игрока УЖЕ был (не первая загрузка при входе)
            if (AvatarManager.getLoadedAvatar(id) == null) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null || mc.world == null) return;
            if (!famrig$isOperator(mc)) return;

            String nick = famrig$resolveNick(mc, id);
            String avatarName = famrig$readAvatarName(nbt);
            String msg = "§l" + nick + "§r §7— сменил аватар"
                    + (avatarName.isEmpty() ? "" : " §8(" + avatarName + ")");
            mc.execute(() -> mc.inGameHud.getChatHud().addMessage(Text.literal(msg)));
        } catch (Throwable ignored) {
            // надзор не должен ломать загрузку аватаров ни при каких условиях
        }
    }

    private static boolean famrig$isOperator(MinecraftClient mc) {
        // вариант 1: скорборд FiguraOperator со значением > 0
        try {
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getNullableObjective("FiguraOperator");
            if (obj != null) {
                ReadableScoreboardScore score = sb.getScore(
                        ScoreHolder.fromName(mc.player.getNameForScoreboard()), obj);
                if (score != null && score.getScore() > 0) return true;
            }
        } catch (Throwable ignored) {}
        // вариант 2: команда (team) FiguraOperator
        try {
            AbstractTeam team = mc.player.getScoreboardTeam();
            if (team != null && "FiguraOperator".equals(team.getName())) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static String famrig$resolveNick(MinecraftClient mc, UUID id) {
        try {
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(id);
                if (entry != null) return entry.getProfile().getName();
            }
        } catch (Throwable ignored) {}
        return id.toString().substring(0, 8);
    }

    private static String famrig$readAvatarName(NbtCompound nbt) {
        try {
            if (nbt != null && nbt.contains("metadata")) {
                String n = nbt.getCompound("metadata").getString("name");
                if (n != null) return n;
            }
        } catch (Throwable ignored) {}
        return "";
    }
}
