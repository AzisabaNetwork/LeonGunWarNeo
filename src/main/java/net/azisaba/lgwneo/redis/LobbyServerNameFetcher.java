package net.azisaba.lgwneo.redis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.redis.data.RedisKeys;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * プロキシ鯖でのLGWロビーサーバー名を取得するクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class LobbyServerNameFetcher {

  private final JedisPool jedisPool;

  private final Set<String> lobbies = new HashSet<>();
  private final ReentrantLock lock = new ReentrantLock();
  private final AtomicReference<BukkitTask> atomicReferenceTask = new AtomicReference<>();

  /**
   * ロビーサーバー名のリストを取得する
   *
   * @return ロビーサーバー名のリスト
   */
  public Set<String> getLobbyServerNames() {
    lock.lock();
    try {
      return lobbies;
    } finally {
      lock.unlock();
    }
  }

  /**
   * ランダムなロビーサーバー名を取得する
   *
   * @return ランダムなロビーサーバー名
   */
  public String getRandomLobbyServerName() {
    lock.lock();
    try {
      if (lobbies.isEmpty()) {
        return null;
      }
      return lobbies.stream().skip((int) (Math.random() * lobbies.size())).findFirst().orElse(null);
    } finally {
      lock.unlock();
    }
  }

  /**
   * ロビーサーバー名を更新するタスクを実行する
   *
   * @param plugin LeonGunWarNeoプラグインのインスタンス
   */
  public void runRefreshTask(LeonGunWarNeo plugin) {
    atomicReferenceTask.getAndUpdate(
        (task) -> {
          if (task != null) {
            task.cancel();
          }

          return Bukkit.getScheduler()
              .runTaskTimerAsynchronously(plugin, this::refresh, 20L * 30L, 20L * 30L);
        });
  }

  /**
   * ロビーサーバー名の情報を更新する。無効な古いデータがあった場合は削除する
   */
  private void refresh() {
    lock.lock();
    try {
      lobbies.clear();

      try (Jedis jedis = jedisPool.getResource()) {
        // ロビーのサーバー名を取得
        Map<String, String> lobbyMap = jedis.hgetAll(RedisKeys.LOBBY_MAP.getKey());
        // 起動しているロビーのIDリストを取得
        Set<String> serverKeys = jedis.keys(RedisKeys.UNIQUE_SERVER_ID_PREFIX.getKey() + "*");

        // 起動しているロビーのみBungeeCordのロビー名を記録
        for (String serverUniqueId : lobbyMap.keySet()) {
          if (serverKeys.contains(RedisKeys.UNIQUE_SERVER_ID_PREFIX.getKey() + serverUniqueId)) {
            lobbies.add(lobbyMap.get(serverUniqueId));
          } else {
            // 起動していないロビーのデータは削除する
            jedis.hdel(RedisKeys.LOBBY_MAP.getKey(), serverUniqueId);
          }
        }
      }
    } finally {
      lock.unlock();
    }
  }
}
