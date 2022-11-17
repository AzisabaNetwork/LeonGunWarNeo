package net.azisaba.lgwneo.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.redis.data.RedisConnectionData;
import net.azisaba.lgwneo.sql.MySQLConnectionData;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
@RequiredArgsConstructor
public class LeonGunWarNeoConfig {

  private final LeonGunWarNeo plugin;

  private RedisConnectionData redisConnectionData;
  private MySQLConnectionData mySQLConnectionData;

  private String serverName;

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
    return this;
  }
}
