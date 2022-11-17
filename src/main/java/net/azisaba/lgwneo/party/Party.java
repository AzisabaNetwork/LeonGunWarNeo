package net.azisaba.lgwneo.party;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 複数プレイヤーが組むことが出来るパーティクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class Party {

  @Getter
  private UUID leader;
  @Getter
  private final Set<UUID> memberUuidSet;

  /**
   * 誰も存在しないパーティを作成する
   */
  public Party() {
    leader = null;
    memberUuidSet = new HashSet<>();
  }

  /**
   * プレイヤー一人だけのパーティを作成する
   *
   * @param uuid パーティに追加するプレイヤーのUUID
   */
  public Party(UUID uuid) {
    leader = uuid;
    memberUuidSet = new HashSet<>();
    memberUuidSet.add(uuid);
  }

  /**
   * プレイヤーをパーティに追加する
   *
   * @param player 追加するプレイヤー
   */
  @Deprecated
  public void addPlayer(Player player) {
    memberUuidSet.add(player.getUniqueId());
  }

  /**
   * プレイヤーをパーティから脱退させる
   *
   * @param player 脱退させるプレイヤー
   */
  @Deprecated
  public void removePlayer(Player player) {
    memberUuidSet.remove(player.getUniqueId());
  }

  /**
   * プレイヤーをパーティに追加する
   *
   * @param uuid 追加するプレイヤーのUUID
   */
  public void addPlayer(UUID uuid) {
    memberUuidSet.add(uuid);
  }

  /**
   * プレイヤーをパーティから脱退させる
   *
   * @param uuid 脱退させるプレイヤーのUUID
   */
  public void removePlayer(UUID uuid) {
    memberUuidSet.remove(uuid);
  }

  /**
   * パーティリーダーを変更する
   *
   * @param uuid 新しくリーダーになるプレイヤーのUUID
   */
  public void changeLeader(UUID uuid) {
    leader = uuid;
  }

  /**
   * パーティに参加しているプレイヤーの数を取得する
   *
   * @return パーティメンバー数
   */
  public int size() {
    return memberUuidSet.size();
  }

  /**
   * パーティ内のメンバーでオンラインのプレイヤー数を取得する
   *
   * @return オンラインのパーティメンバー数
   */
  public int getOnlineCount() {
    return (int) memberUuidSet.stream().filter(uuid -> Bukkit.getPlayer(uuid) != null).count();
  }
}
