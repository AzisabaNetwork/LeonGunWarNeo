package net.azisaba.lgwneo.listener.ldm;

import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.match.MatchOrganizer;
import net.azisaba.lgwneo.match.component.MatchTeam;
import net.azisaba.lgwneo.match.mode.LeaderDeathMatch;
import net.azisaba.lgwneo.match.mode.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * {@link LeaderDeathMatch} に適用するリスナーを登録するクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class LeaderDeathMatchListener implements Listener {

  private final MatchOrganizer matchOrganizer;

  /*
   * リーダーが倒された時の処理をするListener
   */
  @EventHandler
  public void onLeaderKilled(PlayerDeathEvent e) {
    Player p = e.getEntity();
    Match tmpMatch = matchOrganizer.getMatchFromPlayer(p);

    // LDMではなければreturn
    if (!(tmpMatch instanceof LeaderDeathMatch)) {
      return;
    }
    // キャストする
    LeaderDeathMatch match = (LeaderDeathMatch) tmpMatch;

    // 死んだプレイヤーと殺したプレイヤーが同じ (またはnull) ならreturn
    if (p.getKiller() == null || p == p.getKiller()) {
      return;
    }

    // キルしたプレイヤーを取得
    Player killer = p.getKiller();
    // キルしたプレイヤーのチームを取得
    MatchTeam killerTeam = match.getTeamFor(killer);

    // 殺されたプレイヤーがリーダーではない場合return
    if (match.getLeader(killerTeam.getOppositeTeam()) != p) {
      return;
    }

    // ポイントを与え、リーダーを変更する
    match.addScore(killerTeam, 10);
    match.changeLeader(killerTeam.getOppositeTeam());
  }
}
