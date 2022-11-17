package net.azisaba.lgwneo.redis;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.redis.data.RedisKeys;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * Redisを仕様してサーバーのユニークIDを決定するためのクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class ServerIdDefiner {

  private final JedisPool jedisPool;

  @Getter
  private String serverUniqueId = null;

  private BukkitTask task;
  private final AtomicReference<BukkitTask> atomicReferenceTask = new AtomicReference<>();

  /**
   * サーバーIDを決定します。すでに決定されている場合はその値を返します。
   *
   * @return 決定されたサーバーID
   */
  @NonNull
  public String define() {
    // すでにIDが決まっている場合はそれを返す
    if (serverUniqueId != null) {
      return serverUniqueId;
    }

    String definedId = null;
    try (Jedis jedis = jedisPool.getResource()) {
      // 使用されていないIDを登録できるまで繰り返す
      while (definedId == null) {
        definedId = RandomStringUtils.randomAlphabetic(8);

        String reply = jedis.set(RedisKeys.UNIQUE_SERVER_ID_PREFIX.getKey() + definedId,
            "MATCH",
            SetParams.setParams().nx().ex(600));

        if (reply == null) {
          definedId = null;
        }
      }

      serverUniqueId = definedId;
    }

    return serverUniqueId;
  }

  /**
   * サーバーIDを期限切れにならないように保つタスクを実行する
   *
   * @param plugin タスクを実行するプラグイン
   */
  public void runKeepServerUniqueIdTask(JavaPlugin plugin) {
    // IDが決まっていない場合はエラーを出す
    if (serverUniqueId == null) {
      throw new IllegalStateException("Unique server ID is not defined yet");
    }

    atomicReferenceTask.getAndUpdate(
        task -> {
          if (task != null) {
            task.cancel();
          }

          return Bukkit.getScheduler()
              .runTaskTimerAsynchronously(
                  plugin,
                  () -> {
                    try (Jedis jedis = jedisPool.getResource()) {
                      jedis.expire(RedisKeys.UNIQUE_SERVER_ID_PREFIX.getKey() + getServerUniqueId(),
                          600);
                    }
                  },
                  0L,
                  20L * 60L * 3L);
        });
  }

  /**
   * サーバーIDを期限切れにならないように保つタスクを停止する
   */
  public void executeShutdownProcess() {
    Optional.ofNullable(atomicReferenceTask.getAndSet(null)).ifPresent(BukkitTask::cancel);

    if (serverUniqueId == null) {
      return;
    }

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(RedisKeys.UNIQUE_SERVER_ID_PREFIX.getKey() + serverUniqueId);
    }
  }
}
