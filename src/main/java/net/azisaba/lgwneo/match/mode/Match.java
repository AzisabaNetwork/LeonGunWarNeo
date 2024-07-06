package net.azisaba.lgwneo.match.mode;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.azisaba.lgwneo.match.AssistStreaks;
import net.azisaba.lgwneo.match.KillDeathAssistCounter;
import net.azisaba.lgwneo.match.KillStreaks;
import net.azisaba.lgwneo.match.component.MatchStatus;
import net.azisaba.lgwneo.party.Party;
import net.azisaba.lgwneo.world.map.MatchMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * 1試合の進行を管理するインターフェース
 *
 * @author siloneco
 */
public interface Match {

  /**
   * 試合を識別するためのIDを取得する。全サーバーで固有のIDである必要がある
   *
   * @return 試合のID
   */
  String getMatchId();

  /**
   * Map形式で試合の情報を取得する
   *
   * @return 試合の情報
   */
  Map<String, Object> getMatchInformationAsMap();

  /**
   * 試合で使用するマップのデータを取得する
   *
   * @return 試合で使用するマップのデータ
   */
  MatchMap getMapData();

  /**
   * 試合を行うワールドを取得する
   *
   * @return 試合を行うワールド
   */
  World getWorld();

  /**
   * 試合を開始する
   *
   * @return 試合の開始に成功した場合はtrue、失敗した場合はfalse
   */
  boolean startMatch();

  /**
   * 試合を終了する
   *
   * @param force 強制終了する場合はtrue、正常終了する場合はfalse
   * @return 試合の終了に成功した場合はtrue、失敗した場合はfalseを返すCompletableFuture
   */
  CompletableFuture<Boolean> endMatch(boolean force);

  /**
   * 現在の試合の状態を取得する
   *
   * @return 現在の試合のステータス
   */
  MatchStatus getMatchStatus();

  /**
   * 試合に参加している全プレイヤーを取得する。試合の開始前にこのメソッドを呼び出した場合、キューに参加しているプレイヤーを返す。
   *
   * @return 試合に参加しているプレイヤーのSet
   */
  Set<Player> getParticipatePlayers();

  /**
   * 試合が開始する前のキューに参加しているパーティを取得する。試合が始まった後にこのメソッドを呼び出しても空のSetが返される必要がある。
   *
   * @return キューに参加しているパーティのSet
   */
  Set<Party> getQueueingParties();

  /**
   * パーティがこの試合に参加できるかどうかを判定する
   *
   * @param party 参加するパーティ
   * @return 参加できる場合はtrue、できない場合はfalse
   */
  boolean canJoin(Party party);

  /**
   * パーティを試合のキューに追加する
   *
   * @param party 追加するパーティ
   * @return 追加に成功した場合はtrue、すでに追加されているなどの理由で失敗した場合はfalse
   */
  boolean addPartyToQueue(Party party);

  /**
   * パーティを試合のキューから除外する
   *
   * @param party 除外するパーティ
   * @return 除外に成功した場合はtrue、キューに追加されていないなどの理由で失敗した場合はfalse
   */
  boolean removePartyFromQueue(Party party);

  /**
   * すでにキューか試合に参加しているパーティのプレイヤーが後から参加してきた場合に処理を行うメソッド
   *
   * @param p 後から参加してきたプレイヤー
   * @return プレイヤーのセットアップが完了した場合はtrue、失敗した場合はfalse
   */
  boolean setupPartyPlayer(Player p);

  /**
   * プレイヤーのリスポーン地点を返す
   *
   * @param player リスポーン地点を取得するプレイヤー
   * @return プレイヤーのリスポーン地点
   */
  Location getRespawnLocationFor(Player player);

  /**
   * キル数とデス数をカウントするためのKillDeathAssistCounterを取得する
   *
   * @return 試合で使用しているKillDeathAssistCounterインスタンス
   */
  KillDeathAssistCounter getKillDeathAssistCounter();

  void broadcastMessage(String message);

  KillStreaks getKillStreaks();
  AssistStreaks getAssistStreaks();
}
