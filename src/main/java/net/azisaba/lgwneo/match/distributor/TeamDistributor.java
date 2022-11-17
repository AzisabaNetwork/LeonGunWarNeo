package net.azisaba.lgwneo.match.distributor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.azisaba.lgwneo.match.component.MatchTeam;
import net.azisaba.lgwneo.party.Party;

/**
 * プレイヤーをチームに振り分けるインターフェース
 *
 * @author siloneco
 */
public interface TeamDistributor {

  /**
   * 与えられたプレイヤーを与えられたチームに振り分ける
   *
   * @param parties 振り分けるパーティのリスト
   * @param teams   振り分けるチームのリスト
   * @return 振り分けられたチームのMap
   */
  Map<MatchTeam, List<UUID>> distribute(Collection<Party> parties, Collection<MatchTeam> teams);

  /**
   * 与えられたプレイヤーを適切なチームに振り分ける
   *
   * @param party              振り分けるパーティ
   * @param currentTeamPlayers 現在のチームのプレイヤーのMap
   * @return 振り分けられたチーム
   */
  <T extends Collection<UUID>> MatchTeam distribute(
      Party party, Map<MatchTeam, T> currentTeamPlayers);
}
