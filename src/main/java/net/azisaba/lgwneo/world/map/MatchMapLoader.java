package net.azisaba.lgwneo.world.map;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.component.MatchTeam;
import net.azisaba.lgwneo.sql.MySQLConnector;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * 試合マップ情報をロードするクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class MatchMapLoader {

  private final LeonGunWarNeo plugin;
  private final MySQLConnector connector;
  private final HashMap<String, MatchMap> matchMapDataMap = new HashMap<>();

  /**
   * マップ情報をMySQLからロードする
   */
  public void load() {
    try (Connection connection = connector.getHikariDataSource().getConnection();
        Statement stm = connection.createStatement()) {

      ResultSet resultSet = stm.executeQuery("SELECT * FROM `maps`;");

      while (resultSet.next()) {
        try {
          String mapName = resultSet.getString("map_name");
          String queueSpawnStr = resultSet.getString("queue_spawn");
          String redSpawnStr = resultSet.getString("red_spawn");
          String blueSpawnStr = resultSet.getString("blue_spawn");
          int maxTeamPlayerCount = resultSet.getInt("max_team_player");

          Location queueSpawnLoc = convertStringToLocation(queueSpawnStr);
          Location redSpawnLoc = convertStringToLocation(redSpawnStr);
          Location blueSpawnLoc = convertStringToLocation(blueSpawnStr);

          // 変換に失敗している場合はエラーを出す
          boolean failed = false;
          if (queueSpawnLoc == null) {
            warnConvertError(mapName, "queue", queueSpawnStr);
            failed = true;
          }
          if (redSpawnLoc == null) {
            warnConvertError(mapName, "red", redSpawnStr);
            failed = true;
          }
          if (blueSpawnLoc == null) {
            warnConvertError(mapName, "blue", blueSpawnStr);
            failed = true;
          }

          // 座標の変換に失敗している場合はマップを登録しない
          if (failed) {
            continue;
          }

          matchMapDataMap.put(
              mapName.toLowerCase(Locale.ROOT),
              new MatchMap(mapName, queueSpawnLoc, redSpawnLoc, blueSpawnLoc, maxTeamPlayerCount));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * ランダムなマップを1つ取得する
   *
   * @return ロードされているマップの中から選ばれたランダムなマップ
   */
  public MatchMap getRandomMatchMap() {
    List<MatchMap> maps = getAllMatchMaps();
    return maps.get((int) (Math.random() * maps.size()));
  }

  /**
   * 全てのマップをListで返す
   *
   * @return ロードされている全てのマップ
   */
  public List<MatchMap> getAllMatchMaps() {
    return new ArrayList<>(matchMapDataMap.values());
  }

  /**
   * マップ名からマップ情報を取得する。スポーン座標は与えられたワールドの座標に変換される。
   *
   * @param world マップのワールド
   * @return 与えられたワールドの名前のマップ情報をMatchMapで返す。存在しない場合はnullを返す
   */
  public MatchMap getMatchMapFor(World world) {
    return getMatchMapFor(world.getName(), world);
  }

  /**
   * マップ名からマップ情報を取得する。スポーン座標は与えられたワールドの座標に変換される。
   *
   * @param mapName マップ名
   * @param world   マップのワールド
   * @return 与えられたワールド名のマップ情報をMatchMapで返す。存在しない場合はnullを返す
   */
  public MatchMap getMatchMapFor(String mapName, World world) {
    MatchMap map = matchMapDataMap.get(mapName.toLowerCase(Locale.ROOT));
    if (map == null) {
      return null;
    }

    map = map.clone();
    map.getQueueSpawn().setWorld(world);
    map.getSpawnFor(MatchTeam.RED).setWorld(world);
    map.getSpawnFor(MatchTeam.BLUE).setWorld(world);

    return map;
  }

  /**
   * 文字列に変換された座標をLocationに変換する
   *
   * @param str 座標を表す文字列
   * @return 変換されたLocation。変換に失敗した場合はnullを返す
   */
  private Location convertStringToLocation(String str) {
    String[] splitStr = str.split(",");
    Location loc;
    try {
      loc =
          new Location(
              null,
              Double.parseDouble(splitStr[0]),
              Double.parseDouble(splitStr[1]),
              Double.parseDouble(splitStr[2]));

      if (splitStr.length >= 4) {
        loc.setYaw(Float.parseFloat(splitStr[3]));
      }
      if (splitStr.length >= 5) {
        loc.setPitch(Float.parseFloat(splitStr[4]));
      }
    } catch (NumberFormatException ex) {
      return null;
    }

    return loc;
  }

  /**
   * 座標の変換に失敗した場合にエラーを出すためのメソッド
   *
   * @param mapName   マップ名
   * @param placeName 変換に失敗した座標のチーム名
   * @param spawnStr  座標を表す文字列
   */
  private void warnConvertError(String mapName, String placeName, String spawnStr) {
    plugin
        .getLogger()
        .warning(
            "Failed to load map data '"
                + mapName
                + "'. Unable to deserialize "
                + placeName
                + " spawn. ( "
                + spawnStr
                + " )");
  }
}
