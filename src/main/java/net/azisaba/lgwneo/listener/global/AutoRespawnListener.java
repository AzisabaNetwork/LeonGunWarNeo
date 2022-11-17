package net.azisaba.lgwneo.listener.global;

import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.mode.Match;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
public class AutoRespawnListener implements Listener {

  private final LeonGunWarNeo plugin;

  @EventHandler(priority = EventPriority.HIGH)
  public void onDeath(PlayerDeathEvent e) {
    Player deader = e.getEntity();
    deader.spigot().respawn();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onRespawn(PlayerRespawnEvent e) {
    Player p = e.getPlayer();
    Match match = plugin.getMatchOrganizer().getMatchFromPlayer(p);
    // 試合が取得できなかった場合return
    if (match == null) {
      return;
    }

    // リスポーン地点を取得し、存在する場合は設定する
    Location location = match.getRespawnLocationFor(p);
    if (location != null && location.getWorld() != null) {
      e.setRespawnLocation(location);
    }

    // プレイヤーの火を消す
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> p.setFireTicks(0), 0);
    // ノックバックを無効化する
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> p.setVelocity(new Vector()), 0);
  }
}