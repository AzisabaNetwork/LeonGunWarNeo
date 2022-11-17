package net.azisaba.lgwneo.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.component.MatchStatus;
import net.azisaba.lgwneo.match.mode.Match;
import net.azisaba.lgwneo.match.mode.MatchFactory;
import org.bukkit.entity.Player;

/**
 * 複数試合の進行を司るクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class MatchOrganizer {

  private final LeonGunWarNeo plugin;

  // 試合を管理するマップ。キーは試合ID
  private final HashMap<String, Match> matches = new HashMap<>();

  private final HashMap<UUID, String> playerMatchCache = new HashMap<>();

  /**
   * 試合情報を取得する
   *
   * @param matchId 試合ID
   * @return 試合IDと一致する試合情報
   */
  public Match getMatch(String matchId) {
    return matches.get(matchId);
  }

  /**
   * 登録されている全試合を取得する
   *
   * @return 登録されている全試合
   */
  public List<Match> getAllMatches() {
    return new ArrayList<>(matches.values());
  }

  /**
   * 作成した試合を登録する。{@link MatchFactory} によって作成された試合は、このメソッドを用いて自動的に登録される
   *
   * @param match 登録する{@link Match}
   */
  public void registerMatch(Match match) {
    if (matches.containsKey(match.getMatchId())) {
      return;
    }
    matches.put(match.getMatchId(), match);
  }

  // TODO Implement unregister

  /**
   * プレイヤーが参加しているマッチを取得する
   *
   * @param uuid プレイヤーのUUID
   * @return プレイヤーが参加しているマッチ
   */
  public Match getMatchFromPlayer(UUID uuid) {
    // キャッシュに保存されている場合、そのマッチを確かめる
    if (playerMatchCache.containsKey(uuid)) {
      Match match = matches.get(playerMatchCache.get(uuid));
      if (match.getMatchStatus() != MatchStatus.FINISHED
          && match.getParticipatePlayers().stream().anyMatch(p -> p.getUniqueId().equals(uuid))) {
        return match;
      }

      // キャッシュが古いので削除する
      playerMatchCache.remove(uuid);
    }

    // キャッシュが利用できなかった場合、全てのマッチを確かめる
    Match match =
        matches.values().stream()
            .filter(
                m -> m.getParticipatePlayers().stream().anyMatch(p -> p.getUniqueId().equals(uuid)))
            .findFirst()
            .orElse(null);
    if (match != null) {
      playerMatchCache.put(uuid, match.getMatchId());
    }
    return match;
  }

  /**
   * プレイヤーが参加しているマッチを取得する
   *
   * @param p プレイヤー
   * @return プレイヤーが参加しているマッチ
   */
  public Match getMatchFromPlayer(Player p) {
    return getMatchFromPlayer(p.getUniqueId());
  }
}
