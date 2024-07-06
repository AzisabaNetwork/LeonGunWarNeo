package net.azisaba.lgwneo.match;

import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.mode.LeaderDeathMatch;
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
public class KillStreaks {

    private final LeonGunWarNeo plugin;

    private final Map<UUID, AtomicInteger> streaksMap = new HashMap<>();

    public void removedBy(UUID uuid, UUID killerU) {
        Player player = Bukkit.getPlayer(uuid);
        Player killer = Bukkit.getPlayer(killerU);
        int streaks = get(uuid).get();
        int minStreaks =
                plugin.getLeonGunWarNeoConfig().getStreaks().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(-1);

        if (killer != null && streaks >= minStreaks) {
            Match match = plugin.getMatchOrganizer().getMatchFromPlayer(uuid);
            match.broadcastMessage(
                    Chat.f(
                            plugin.getLeonGunWarNeoConfig().getRemoved(),
                            LeonGunWarNeo.getChatPrefix(),
                            killer.getDisplayName(),
                            player.getDisplayName()
                    )
            );
        }

        streaksMap.remove(player.getUniqueId());
    }

    public AtomicInteger get(UUID uuid) {
        streaksMap.putIfAbsent(uuid, new AtomicInteger(0));
        return streaksMap.get(uuid);
    }

    private void giveRewards(int streaks, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        plugin.getLeonGunWarNeoConfig().getStreaks().entrySet().stream()
                .filter(entry -> streaks == entry.getKey())
                .map(Map.Entry::getValue)
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .map(command -> Chat.f(command, player.getName()))
                .forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        plugin.getLeonGunWarNeoConfig().getKillLevels().entrySet().stream()
                .filter(entry -> streaks % entry.getKey() == 0)
                .map(Map.Entry::getValue)
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .map(command -> Chat.f(command, player.getName()))
                .forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    public void add(UUID uuid) {
        // カウントを追加
        int streaks = get(uuid).incrementAndGet();
        Match match = plugin.getMatchOrganizer().getMatchFromPlayer(uuid);
        Player player = Bukkit.getPlayer(uuid);
        // 報酬を付与
        giveRewards(streaks, uuid);
        if (match instanceof LeaderDeathMatch) {
            if (((LeaderDeathMatch)match).getTeamLeaderMap().containsValue(uuid)) {
                giveRewards(streaks, uuid);
                player.sendMessage(Chat.f("{0}&7あなたはリーダーなので &e2倍 &7の報酬を受け取りました！", LeonGunWarNeo.getChatPrefix()));
            }
        }

        // キルストリークをお知らせ
        plugin.getLeonGunWarNeoConfig().getStreaks().entrySet().stream()
                .filter(entry -> streaks == entry.getKey())
                .map(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .flatMap(List::stream)
                .map(message -> Chat.f(message, LeonGunWarNeo.getChatPrefix(), player.getDisplayName()))
                .forEach(match::broadcastMessage);
        plugin.getLeonGunWarNeoConfig().getKillLevels().entrySet().stream()
                .filter(entry -> streaks % entry.getKey() == 0)
                .map(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .flatMap(List::stream)
                .map(message -> Chat.f(message, LeonGunWarNeo.getChatPrefix(), player.getDisplayName()))
                .forEach(match::broadcastMessage);
    }

}
