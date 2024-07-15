package net.azisaba.lgwneo.match;

import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.mode.Match;
import net.azisaba.lgwneo.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class AssistStreaks {

    private final Match match;
    private final Map<UUID, AtomicInteger> streaksMap = new HashMap<>();

    public void removedBy(UUID uuid) {
        streaksMap.remove(uuid);
    }

    public AtomicInteger get(UUID uuid) {
        streaksMap.putIfAbsent(uuid, new AtomicInteger(0));
        return streaksMap.get(uuid);
    }

    public void add(UUID uuid) {
        // カウントを追加
        int streaks = get(uuid).incrementAndGet();
        Player player = Bukkit.getPlayer(uuid);

        // 報酬を付与
        match.getPlugin().getLeonGunWarNeoConfig().getAssistLevels().entrySet().stream()
                .filter(entry -> streaks % entry.getKey() == 0)
                .map(Map.Entry::getValue)
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .map(command -> Chat.f(command, player.getName()))
                .forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        // アシストストリークをお知らせ
        match.getPlugin().getLeonGunWarNeoConfig().getAssistLevels().entrySet().stream()
                .filter(entry -> streaks % entry.getKey() == 0)
                .map(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .flatMap(List::stream)
                .map(message -> Chat.f(message, LeonGunWarNeo.getChatPrefix(), player.getDisplayName()))
                .forEach(match::broadcastMessage);
    }

}
