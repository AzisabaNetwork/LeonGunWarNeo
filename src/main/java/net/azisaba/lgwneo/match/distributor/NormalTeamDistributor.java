package net.azisaba.lgwneo.match.distributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.azisaba.lgwneo.match.component.MatchTeam;
import net.azisaba.lgwneo.party.Party;

public class NormalTeamDistributor implements TeamDistributor {

  @Override
  public Map<MatchTeam, List<UUID>> distribute(
      Collection<Party> parties, Collection<MatchTeam> teams) {
    HashMap<MatchTeam, List<UUID>> distributedMap = new HashMap<>();

    // 必要なチームのプレイヤーリストを空のリストで初期化する
    for (MatchTeam team : teams) {
      distributedMap.put(team, new ArrayList<>());
    }

    List<Party> sortedPartyList = new ArrayList<>(parties);
    sortedPartyList.sort((p1, p2) -> p2.getMemberUuidSet().size() - p1.getMemberUuidSet().size());

    // もっとも人数が少ないチームに対してパーティ内のプレイヤーを割り振る
    for (Party party : sortedPartyList) {
      MatchTeam team = getTeamWithLeastPlayers(distributedMap);
      party.getMemberUuidSet().forEach(p -> distributedMap.get(team).add(p));
    }

    return distributedMap;
  }

  @Override
  public <T extends Collection<UUID>> MatchTeam distribute(
      Party party, Map<MatchTeam, T> currentTeamPlayers) {
    // もっとも人数が少ないチームを取得し、返す
    return getTeamWithLeastPlayers(currentTeamPlayers);
  }

  /**
   * プレイヤーが割り振られているチームの中で、プレイヤーが最も少ないチームを返す
   *
   * @param map プレイヤーが割り振られているチームのマップ
   * @return プレイヤーが最も少ないチーム
   */
  private <T extends Collection<UUID>> MatchTeam getTeamWithLeastPlayers(Map<MatchTeam, T> map) {
    MatchTeam team = null;
    int least = Integer.MAX_VALUE;
    for (MatchTeam t : map.keySet()) {
      if (map.get(t).size() < least) {
        team = t;
        least = map.get(t).size();
      }
    }
    return team;
  }
}
