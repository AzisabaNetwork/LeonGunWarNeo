package net.azisaba.lgwneo.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.redis.data.RedisConnectionData;
import net.azisaba.lgwneo.sql.MySQLConnectionData;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class LeonGunWarNeoConfig {

  private final LeonGunWarNeo plugin;

  private RedisConnectionData redisConnectionData;
  private MySQLConnectionData mySQLConnectionData;

  private String serverName;

  private Map<Integer, Map.Entry<List<String>, List<String>>> streaks;
  private Map<Integer, Map.Entry<List<String>, List<String>>> killLevels;
  private String removed;

  private Map<Integer, Map.Entry<List<String>, List<String>>> assistLevels;

  /**
   * Configを読み込みます
   *
   * @return 呼び出されたインスタンス
   */
  public LeonGunWarNeoConfig load() {
    FileConfiguration conf = plugin.getConfig();

    // Redisの接続情報を読み込む
    String redisHostname = conf.getString("redis.hostname");
    int redisPort = conf.getInt("redis.port");
    String redisUsername = conf.getString("redis.username");
    String redisPassword = conf.getString("redis.password");

    if (redisUsername != null && redisUsername.equals("")) {
      redisUsername = null;
    }
    if (redisPassword != null && redisPassword.equals("")) {
      redisPassword = null;
    }

    redisConnectionData =
        new RedisConnectionData(redisHostname, redisPort, redisUsername, redisPassword);

    // MySQLの接続情報を読み込む
    String mySQLHostname = conf.getString("mysql.hostname");
    int mySQLPort = conf.getInt("mysql.port");
    String mySQLUsername = conf.getString("mysql.username");
    String mySQLPassword = conf.getString("mysql.password");
    String mySQLDatabase = conf.getString("mysql.database");

    mySQLConnectionData =
        new MySQLConnectionData(
            mySQLHostname, mySQLPort, mySQLUsername, mySQLPassword, mySQLDatabase);

    serverName = conf.getString("server-name");

    streaks = new HashMap<>();
    conf.getConfigurationSection("streaks").getValues(false).keySet().stream()
            .map(Integer::valueOf)
            .collect(
                    Collectors.toMap(
                            Function.identity(),
                            count -> {
                              List<String> messages = conf.getStringList("streaks." + count + ".messages");
                              List<String> commands = conf.getStringList("streaks." + count + ".commands");
                              return new AbstractMap.SimpleEntry<>(messages, commands);
                            }))
            .forEach(streaks::put);
    streaks = Collections.unmodifiableMap(streaks);

    killLevels = new HashMap<>();
    conf.getConfigurationSection("levels").getValues(false).keySet().stream()
            .map(Integer::valueOf)
            .collect(
                    Collectors.toMap(
                            Function.identity(),
                            count -> {
                              List<String> messages = conf.getStringList("killLevels." + count + ".messages");
                              List<String> commands = conf.getStringList("killLevels." + count + ".commands");
                              return new AbstractMap.SimpleEntry<>(messages, commands);
                            }))
            .forEach(killLevels::put);
    killLevels = Collections.unmodifiableMap(killLevels);

    removed = conf.getString("removed");

    assistLevels = new HashMap<>();
    conf.getConfigurationSection("levels").getValues(false).keySet().stream()
            .map(Integer::valueOf)
            .collect(
                    Collectors.toMap(
                            Function.identity(),
                            count -> {
                              List<String> messages = conf.getStringList("assistLevels." + count + ".messages");
                              List<String> commands = conf.getStringList("assistLevels." + count + ".commands");
                              return new AbstractMap.SimpleEntry<>(messages, commands);
                            }))
            .forEach(assistLevels::put);
    assistLevels = Collections.unmodifiableMap(assistLevels);
    return this;
  }
}
