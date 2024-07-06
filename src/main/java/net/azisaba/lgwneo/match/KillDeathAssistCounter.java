package net.azisaba.lgwneo.match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.match.mode.Match;

/**
 * プレイヤーの1試合でのキル数デス数アシスト数を記録するクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class KillDeathAssistCounter {

  private final Match match;

  private final HashMap<UUID, AtomicInteger> kills = new HashMap<>();
  private final HashMap<UUID, AtomicInteger> deaths = new HashMap<>();
  private final HashMap<UUID, AtomicInteger> assists = new HashMap<>();

  private final Map<UUID, Map<UUID, Long>> lastDamagedMap = new HashMap<>();

  /**
   * プレイヤーのキル数を1追加する
   *
   * @param uuid キル数を追加するプレイヤーのUUID
   */
  public void addKills(UUID uuid) {
    kills.computeIfAbsent(uuid, k -> new AtomicInteger()).incrementAndGet();
    match.getKillStreaks().add(uuid);
  }

  /**
   * プレイヤーのキル数を1追加する
   *
   * @param uuid   キル数を追加するプレイヤーのUUID
   * @param victim プレイヤーがキルした相手のUUID
   */
  public void addKills(UUID uuid, UUID victim) {
    addKills(uuid);

    // キルする前に攻撃したプレイヤーが存在する場合
    if (lastDamagedMap.containsKey(victim)) {
      Map<UUID, Long> lastDamaged = lastDamagedMap.get(victim);
      for (UUID lastDamagedUUID : lastDamaged.keySet()) {
        // キルした本人の場合はcontinue
        if (lastDamagedUUID.equals(uuid)) {
          continue;
        }

        // ダメージを与えてから10秒以内に倒された場合はアシストとしてカウントする
        long milliSecDiedAfter = System.currentTimeMillis() - lastDamaged.get(lastDamagedUUID);
        if (milliSecDiedAfter < 10000) {
          addAssists(lastDamagedUUID);
        }
      }
      lastDamaged.clear();
    }
  }

  /**
   * プレイヤーのデス数を1追加する
   *
   * @param uuid デス数を追加するプレイヤーのUUID
   */
  public void addDeaths(UUID uuid, UUID killer) {
    deaths.computeIfAbsent(uuid, k -> new AtomicInteger()).incrementAndGet();
    match.getKillStreaks().removedBy(uuid, killer);
    match.getAssistStreaks().removedBy(uuid);
  }

  /**
   * プレイヤーのアシスト数を1追加する
   *
   * @param uuid アシスト数を追加するプレイヤーのUUID
   */
  public void addAssists(UUID uuid) {
    assists.computeIfAbsent(uuid, k -> new AtomicInteger()).incrementAndGet();
    match.getAssistStreaks().add(uuid);
  }

  /**
   * プレイヤーのキル数を取得する
   *
   * @param uuid キル数を取得するプレイヤーのUUID
   * @return 記録されているキル数
   */
  public int getKills(UUID uuid) {
    if (kills.containsKey(uuid)) {
      return kills.get(uuid).get();
    }
    return 0;
  }

  /**
   * プレイヤーのデス数を取得する
   *
   * @param uuid デス数を取得するプレイヤーのUUID
   * @return 記録されているデス数
   */
  public int getDeaths(UUID uuid) {
    if (deaths.containsKey(uuid)) {
      return deaths.get(uuid).get();
    }
    return 0;
  }

  /**
   * プレイヤーのアシスト数を取得する
   *
   * @param uuid アシスト数を取得するプレイヤーのUUID
   * @return 記録されているアシスト数
   */
  public int getAssists(UUID uuid) {
    if (assists.containsKey(uuid)) {
      return assists.get(uuid).get();
    }
    return 0;
  }

  /**
   * プレイヤーのキルデス比を割り出す
   *
   * @param uuid キルデス比を取得するプレイヤーのUUID
   * @return プレイヤーのキルデス比
   */
  public double getKillDeathRatio(UUID uuid) {
    int kills = getKills(uuid);
    int deaths = getDeaths(uuid);

    if (deaths == 0) {
      return kills;
    }
    return (double) kills / deaths;
  }

  /**
   * プレイヤーがダメージを与えたことを記録する
   *
   * @param attacker ダメージを与えたプレイヤーのUUID
   * @param victim   ダメージを受けたプレイヤーのUUID
   */
  public void setLastDamaged(UUID attacker, UUID victim) {
    Map<UUID, Long> milliSecMap = lastDamagedMap.computeIfAbsent(victim, k -> new HashMap<>());
    milliSecMap.put(attacker, System.currentTimeMillis());
  }
}
