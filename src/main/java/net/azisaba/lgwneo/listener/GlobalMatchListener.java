package net.azisaba.lgwneo.listener;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.component.MatchStatus;
import net.azisaba.lgwneo.match.mode.Match;
import net.azisaba.lgwneo.util.Chat;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 全モードに適用するリスナーを登録するクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class GlobalMatchListener implements Listener {

  private final LeonGunWarNeo plugin;

  /*
   * キルしたことを表示するListener
   */
  @EventHandler(priority = EventPriority.HIGH)
  public void onKill(PlayerDeathEvent e) {
    // 試合を取得し、試合が見つからない場合return
    Match match = plugin.getMatchOrganizer().getMatchFromPlayer(e.getEntity());
    if (match == null) {
      return;
    }
    // 試合中でない場合return
    if (match.getMatchStatus() != MatchStatus.PLAYING) {
      return;
    }

    // 殺したプレイヤーを取得
    Player killer = e.getEntity().getKiller();
    // 殺したプレイヤーがいない場合はreturn
    if (killer == null) {
      return;
    }

    // 殺したプレイヤーが試合に参加していない場合return
    if (!match.getParticipatePlayers().contains(killer)) {
      return;
    }

    // タイトルを表示して音を鳴らす
    killer.sendTitle("", Chat.f("&c+1 &7Kill"), 0, 10, 10);
    killer.playSound(killer.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);

    // キル数とデス数をカウントする
    if (match.getKillDeathAssistCounter() != null) {
      match.getKillDeathAssistCounter().addKills(killer.getUniqueId());
      match.getKillDeathAssistCounter().addDeaths(e.getEntity().getUniqueId());
    }
  }

  /*
   * 花火のダメージを無効化するListener
   */
  @EventHandler
  public void onFireworksDamage(EntityDamageByEntityEvent e) {
    // Entity による爆発ではない場合はreturn
    if (e.getCause() != DamageCause.ENTITY_EXPLOSION) {
      return;
    }
    // ダメージを受けたEntityがPlayerでなければreturn
    if (!(e.getEntity() instanceof Player)) {
      return;
    }
    // ダメージを与えたEntityが花火でなければreturn
    if (!(e.getDamager() instanceof Firework)) {
      return;
    }
    e.setCancelled(true);
  }

  /*
   * 試合開始前に受けたダメージを無効化するListener
   */
  @EventHandler
  public void onDamage(EntityDamageEvent e) {
    String worldName = e.getEntity().getWorld().getName();

    Match match = plugin.getMatchOrganizer().getMatch(worldName);
    if (match == null || match.getMatchStatus() == MatchStatus.PLAYING) {
      return;
    }

    e.setCancelled(true);
  }

  /*
   * 試合開始前の浮島から脱走することを防止するListener
   */
  @EventHandler
  public void onMove(PlayerMoveEvent e) {
    String worldName = e.getPlayer().getWorld().getName();

    Match match = plugin.getMatchOrganizer().getMatch(worldName);
    if (match == null || !Arrays.asList(MatchStatus.INITIALIZING, MatchStatus.WAITING,
        MatchStatus.STARTING).contains(match.getMatchStatus())) {
      return;
    }

    Location queueSpawn = match.getMapData().getQueueSpawn();
    if (queueSpawn == null || !e.getPlayer().getWorld().equals(queueSpawn.getWorld())) {
      return;
    }

    double xDistance = Math.abs(queueSpawn.getX() - e.getTo().getX());
    double yDistance = Math.abs(queueSpawn.getY() - e.getTo().getY());
    double zDistance = Math.abs(queueSpawn.getZ() - e.getTo().getZ());

    if (xDistance < 10 && yDistance < 10 && zDistance < 10) {
      return;
    }

    e.getPlayer().teleport(queueSpawn);
  }
}
