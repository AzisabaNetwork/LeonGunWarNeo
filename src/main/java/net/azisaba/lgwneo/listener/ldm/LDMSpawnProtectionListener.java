package net.azisaba.lgwneo.listener.ldm;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

@SuppressWarnings("UnstableApiUsage")
@RequiredArgsConstructor
public class LDMSpawnProtectionListener implements Listener {

  private final LeonGunWarNeo plugin;

  private final RateLimiter protectedThrottle = RateLimiter.create(1);
  private final RateLimiter victimProtectedThrottle = RateLimiter.create(1);

  private static final long duration = 5;

  private final Map<Player, Instant> remainTimes = new HashMap<>();

  private final List<Player> invincibleQueue = new ArrayList<>();

  private boolean isProtected(Player victim) {
    return invincibleQueue.contains(victim)
        || Instant.now().isBefore(remainTimes.getOrDefault(victim, Instant.now()));
  }

  private void sendProtected(Player victim) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              if (protectedThrottle.tryAcquire()) {
                victim.sendMessage(
                    Chat.f("{0}&rあなた &7は保護されています！", LeonGunWarNeo.getChatPrefix()));
              }
            });
  }

  private void sendVictimProtected(Player attacker, Player victim) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              if (victimProtectedThrottle.tryAcquire()) {
                attacker.sendMessage(
                    Chat.f("{0}{1} &7は保護されています！", LeonGunWarNeo.getChatPrefix(),
                        victim.getDisplayName()));
              }
            });
  }

  private void startCountdown(Player player) {
    // リスポーン時間指定
    remainTimes.put(player, Instant.now().plusSeconds(duration));
  }

  @EventHandler
  public void onDamage(EntityDamageEvent e) {
    // ダメージを受けたEntityがプレイヤーでなければreturn
    if (!(e.getEntity() instanceof Player)) {
      return;
    }

    Player victim = (Player) e.getEntity();

    // リスポーンから5秒以内ならキャンセル
    if (isProtected(victim)) {
      e.setCancelled(true);
      // sendProtected(victim);
    }
  }

  @EventHandler
  public void onDamageByEntity(EntityDamageByEntityEvent e) {
    // ダメージを受けたEntityがプレイヤーでなければreturn
    if (!(e.getEntity() instanceof Player)) {
      return;
    }

    Player victim = (Player) e.getEntity();

    // リスポーンから5秒以内ならキャンセル
    if (isProtected(victim)) {
      e.setCancelled(true);
      sendProtected(victim);

      Player attacker = null;
      // 攻撃したEntityがプレイヤーならメッセージ送信対象に指定
      if (e.getDamager() instanceof Player) {
        attacker = (Player) e.getDamager();
      } else if (e.getDamager() instanceof Projectile) {
        // 攻撃したEntityが投げ物なら、投げたEntityを取得
        ProjectileSource shooter = ((Projectile) e.getDamager()).getShooter();

        // shooterがプレイヤーならメッセージ送信対象に指定
        if (shooter instanceof Player) {
          attacker = (Player) shooter;
        }
      }

      // attackerがnullではない場合、メッセージを送信
      if (attacker != null) {
        sendVictimProtected(attacker, victim);
      }
    }
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent e) {
    Player p = e.getPlayer();

    // 無敵時間カウントダウンのキューに追加
    if (!invincibleQueue.contains(p)) {
      invincibleQueue.add(p);
    }
  }

  @EventHandler
  public void onMove(PlayerMoveEvent e) {
    Player p = e.getPlayer();
    if (invincibleQueue.contains(p)) {
      startCountdown(p);
      invincibleQueue.remove(p);
    }
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();
    if (!e.isCancelled() && invincibleQueue.contains(p)) {
      startCountdown(p);
      invincibleQueue.remove(p);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    Player p = e.getPlayer();
    invincibleQueue.remove(p);
  }
}
