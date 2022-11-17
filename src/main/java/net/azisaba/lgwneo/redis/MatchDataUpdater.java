package net.azisaba.lgwneo.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.MatchOrganizer;
import net.azisaba.lgwneo.match.mode.Match;
import net.azisaba.lgwneo.redis.data.RedisKeys;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

@RequiredArgsConstructor
public class MatchDataUpdater {

  private final LeonGunWarNeo plugin;

  private final JedisPool jedisPool;
  private final MatchOrganizer matchOrganizer;

  private final ObjectMapper mapper = new ObjectMapper();

  private final AtomicReference<BukkitTask> taskReference = new AtomicReference<>();

  public void update() {
    try (Jedis jedis = jedisPool.getResource()) {
      for (Match match : matchOrganizer.getAllMatches()) {
        Map<String, Object> data = match.getMatchInformationAsMap();

        if (!validate(data)) {
          // 必要な情報が含まれていない場合はデバッグで気づけるようにエラーを出す
          // 他の試合のために処理は中断したくないので throw ではなく printStackTrace
          new IllegalStateException("Invalid data returned: " + data).printStackTrace();
          continue;
        }

        String jsonData = convertToString(data);
        if (jsonData == null) {
          continue;
        }

        jedis.set(RedisKeys.MATCH_PREFIX.getKey() + match.getMatchId(), jsonData,
            SetParams.setParams().ex(30));
      }
    }
  }

  public void runUpdateTask() {
    taskReference.getAndUpdate(
        task -> {
          if (task != null) {
            task.cancel();
          }
          return plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::update,
              0, 20 * 5);
        });
  }

  public String convertToString(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public boolean validate(Map<String, Object> data) {
    return data.containsKey("matchId")
        && data.containsKey("serverId")
        && data.containsKey("proxyRegisteredServerName")
        && data.containsKey("mapName")
        && data.containsKey("status")
        && data.containsKey("mode")
        && data.containsKey("remainingSeconds");
  }
}
