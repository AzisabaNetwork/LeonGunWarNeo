package net.azisaba.lgwneo.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.component.MatchTeam;
import net.azisaba.lgwneo.match.mode.LeaderDeathMatch;
import net.azisaba.lgwneo.util.Chat;
import net.azisaba.lgwneo.util.TimeFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * リーダーデスマッチ用のスコアボードを表示するクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class LeaderDeathMatchScoreboard {

  private final LeaderDeathMatch match;
  private final LeonGunWarNeo plugin;

  private final HashMap<UUID, FastBoard> fastBoardMap = new HashMap<>();

  /**
   * スコアボードを最新の内容に更新し、試合中のプレイヤーと観戦中のプレイヤー両方に表示する
   *
   * @param async 非同期で実行する場合true、同期で実行する場合false
   */
  public void updateLines(boolean async) {
    // 非同期で実行する場合はスレッドを変える
    if (async) {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> updateLines(false));
      return;
    }

    // 各チームの試合中プレイヤーを取得
    HashMap<MatchTeam, Set<Player>> teamMap = new HashMap<>();
    teamMap.put(MatchTeam.RED, match.getTeamPlayers(MatchTeam.RED));
    teamMap.put(MatchTeam.BLUE, match.getTeamPlayers(MatchTeam.BLUE));

    // チームごとにスコアボードを表示する
    for (Entry<MatchTeam, Set<Player>> entry : teamMap.entrySet()) {
      int allyScore = match.getScore(entry.getKey());
      int enemyScore = match.getScore(entry.getKey().getOppositeTeam());

      // そのチームのスコアボードのテンプレートを作成
      List<String> template = getTeamScoreboardTemplate(entry.getKey(), allyScore, enemyScore);

      // チームのプレイヤーに対して個人の表示内容を適用し、表示する
      for (Player p : entry.getValue()) {
        FastBoard board = getFastBoard(p);

        // テンプレートをコピーし、個人の表示内容を適用する
        List<String> cloned = new ArrayList<>(template);
        applyTemplateForPlayer(cloned, p);

        // スコアボードを更新する
        board.updateLines(cloned);
      }
    }

    // 観戦プレイヤー用のスコアボード内容を取得
    List<String> lines = getGhostScoreboardLines();

    // 試合に参加していないが、ワールドには居るプレイヤーを観戦プレイヤーとみなし、スコアボードを表示する
    Set<Player> fightingPlayers = match.getParticipatePlayers();
    match.getWorld().getPlayers().stream()
        .filter(p -> !fightingPlayers.contains(p))
        .forEach(
            p -> {
              FastBoard board = getFastBoard(p);
              board.updateLines(lines);
            });
  }

  /**
   * チーム用のスコアボードのテンプレートを取得する
   *
   * @param team       テンプレートを生成するチーム
   * @param allyScore  チームのスコア
   * @param enemyScore 敵チームのスコア
   * @return 作成されたテンプレート
   */
  private List<String> getTeamScoreboardTemplate(MatchTeam team, int allyScore, int enemyScore) {
    String allyTeamInitial = team.name().substring(0, 1);
    String enemyTeamInitial = team.getOppositeTeam().name().substring(0, 1);

    ChatColor allyTeamColor = team.getChatColor();
    ChatColor enemyTeamColor = team.getOppositeTeam().getChatColor();

    String date = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
    String remainingTime = TimeFormatUtils.format(match.getMatchCountdownTask().getRemainingTime());

    return Arrays.asList(
        Chat.f("&7{0} &8{1}", date, match.getMatchId()),
        "",
        Chat.f("残り時間: &a{0}", remainingTime),
        "",
        Chat.f("{0}{1} &r自チーム: &a{2}pt", allyTeamColor, allyTeamInitial, allyScore),
        Chat.f("{0}{1} &r敵チーム: &a{2}pt", enemyTeamColor, enemyTeamInitial, enemyScore),
        "",
        Chat.f("リーダー: &a{0}", match.getLeader(team).getName()),
        "",
        Chat.f("K/D: &7<kill> / <death> (<ratio>)"),
        Chat.f("Assist: &7<assist>"),
        "",
        Chat.f("今すぐ &6{0} &rで遊べ！", "azisaba.net"));
  }

  /**
   * テンプレートにキル数などの個人の情報を適用する
   *
   * @param template テンプレート
   * @param p        適用するプレイヤー
   */
  private void applyTemplateForPlayer(List<String> template, Player p) {
    int kills = match.getKillDeathAssistCounter().getKills(p.getUniqueId());
    int deaths = match.getKillDeathAssistCounter().getDeaths(p.getUniqueId());
    int assists = match.getKillDeathAssistCounter().getAssists(p.getUniqueId());
    double ratio = match.getKillDeathAssistCounter().getKillDeathRatio(p.getUniqueId());

    for (int i = 0, size = template.size(); i < size; i++) {
      String rawStr = template.get(i);
      String replaced = rawStr
          .replace("<kill>", String.valueOf(kills))
          .replace("<death>", String.valueOf(deaths))
          .replace("<assist>", String.valueOf(assists))
          .replace("<ratio>", String.format("%.2f", ratio));
      template.set(i, replaced);
    }
  }

  /**
   * 観戦中のプレイヤーに対して表示するスコアボードの内容を取得する
   *
   * @return 観戦プレイヤー用のスコアボードの内容
   */
  private List<String> getGhostScoreboardLines() {
    String redTeamInitial = MatchTeam.RED.name().substring(0, 1);
    String blueTeamInitial = MatchTeam.BLUE.name().substring(0, 1);

    ChatColor redTeamColor = MatchTeam.RED.getChatColor();
    ChatColor blueTeamColor = MatchTeam.BLUE.getChatColor();

    String redTeamName = MatchTeam.RED.getTeamNameWithoutColor();
    String blueTeamName = MatchTeam.BLUE.getTeamNameWithoutColor();

    int redScore = match.getScore(MatchTeam.RED);
    int blueScore = match.getScore(MatchTeam.BLUE);

    String date = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
    String remainingTime = TimeFormatUtils.format(match.getMatchCountdownTask().getRemainingTime());

    return Arrays.asList(
        Chat.f("&7{0} &8{1}", date, match.getMatchId()),
        "",
        Chat.f("残り時間: &a{0}", remainingTime),
        "",
        Chat.f("{0}{1} &r{2}: &a{1}pt", redTeamColor, redTeamInitial, redTeamName, redScore),
        Chat.f("{0}{1} &r{2}: &a{1}pt", blueTeamColor, blueTeamInitial, blueTeamName, blueScore),
        "",
        Chat.f("今すぐ &6{0} &rで遊べ！", "azisaba.net"));
  }

  /**
   * 指定したプレイヤーのFastBoardを取得する。存在しないか無効な物の場合、新しく作成して登録する
   *
   * @param p FastBoardを取得したいプレイヤー
   * @return 取得か作成されたFastBoard
   */
  private FastBoard getFastBoard(Player p) {
    FastBoard board = fastBoardMap.get(p.getUniqueId());
    if (board == null || board.isDeleted()) {
      board = new FastBoard(p);
      board.updateTitle(
          Chat.f("&a{0} &dv{1}", plugin.getName(), plugin.getDescription().getVersion()));
      fastBoardMap.put(p.getUniqueId(), board);
    }
    return board;
  }
}
