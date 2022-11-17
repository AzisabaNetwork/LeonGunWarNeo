package net.azisaba.lgwneo.world;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.World;

/**
 * ワールドをロードしたりアンロードしたりするためのインターフェース
 *
 * @author siloneco
 */
public interface WorldLoader {

  /**
   * 現在ロードすることが出来るワールドのリストを取得します
   *
   * @return ロード可能なワールド名のリスト
   */
  List<String> getAvailableWorlds();

  /**
   * マップ自体は存在するが、何らかの理由で使用できない状態にあるマップのリストを取得します
   *
   * @return ロード不可能なワールド名のリスト
   */
  default List<String> getUnavailableWorlds() {
    return Collections.emptyList();
  }

  /**
   * 指定されたワールドをロードします
   *
   * @param mapName ロードするマップ名
   * @return ロードしたワールドを返すCompletableFuture
   */
  default CompletableFuture<World> loadWorld(String mapName) {
    return loadWorld(mapName, mapName);
  }

  /**
   * 指定されたワールドをロードします
   *
   * @param mapName   ロードするマップ名
   * @param worldName 実際に生成するワールド名
   * @return ロードしたワールドを返すCompletableFuture
   */
  CompletableFuture<World> loadWorld(String mapName, String worldName);

  /**
   * 指定されたワールドをアンロードします
   *
   * @param world アンロードするワールド
   * @return アンロードが成功したかどうかを返すCompletableFuture
   */
  CompletableFuture<Boolean> unloadWorld(World world);
}
