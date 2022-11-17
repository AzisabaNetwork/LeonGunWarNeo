package net.azisaba.lgwneo.match.mode;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.redis.data.RedisKeys;
import net.azisaba.lgwneo.world.WorldLoader;
import net.azisaba.lgwneo.world.map.MatchMap;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.World;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * 試合の作成と初期化を行うクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class MatchFactory {

  private final LeonGunWarNeo plugin;
  private final JedisPool jedisPool;

  @Setter
  private WorldLoader worldLoader;

  public LeaderDeathMatch createLeaderDeathMatch(String mapName, boolean privateMatch) {
    // WorldLoaderが存在しない場合はエラーを出す
    if (worldLoader == null) {
      throw new IllegalStateException("WorldLoader is not set. Unable to create world.");
    }

    // 試合IDを生成する
    String matchId = reserveMatchId();

    // MapLoaderを使用してマップを読み込む
    World world =
        worldLoader
            .loadWorld(mapName, matchId)
            .handle(
                (loadedWorld, error) -> {
                  if (error != null) {
                    error.printStackTrace();
                    return null;
                  }
                  return loadedWorld;
                })
            .join();

    // マップの読み込みに失敗している場合はnullを返す
    if (world == null) {
      return null;
    }

    // マップデータを取得
    MatchMap mapData = plugin.getMatchMapLoader().getMatchMapFor(mapName, world);
    if (mapData == null) {
      plugin
          .getLogger()
          .warning(
              "Failed to get map data for "
                  + mapName
                  + ". Maybe admins forgot to add it or data broken?");
      return null;
    }

    // 試合を生成してOrganizer仁登録する
    LeaderDeathMatch match = new LeaderDeathMatch(plugin, matchId, world, mapData);
    match.setPrivateMatch(privateMatch);
    plugin.getMatchOrganizer().registerMatch(match);

    return match;
  }

  /**
   * Redisを用いて固有の試合IDを生成する
   *
   * @return 生成された試合ID
   */
  private String reserveMatchId() {
    String matchId = null;

    try (Jedis jedis = jedisPool.getResource()) {
      while (matchId == null) {
        matchId = RandomStringUtils.randomAlphanumeric(8);
        String response =
            jedis.set(
                RedisKeys.MATCH_PREFIX.getKey() + matchId,
                "initializing",
                SetParams.setParams().nx().ex(60));

        if (response == null) {
          matchId = null;
        }
      }
    }

    return matchId;
  }
}
