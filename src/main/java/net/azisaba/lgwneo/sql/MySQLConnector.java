package net.azisaba.lgwneo.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

/**
 * SQLに接続するためのクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class MySQLConnector {

  private final MySQLConnectionData connectionData;

  private HikariDataSource hikariDataSource;

  public boolean isConnected() {
    return hikariDataSource != null && !hikariDataSource.isClosed();
  }

  public void connect() {
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException ignore) {
      // pass
    }

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(
        "jdbc:mysql://"
            + connectionData.getHostname()
            + ":"
            + connectionData.getPort()
            + "/"
            + connectionData.getDatabase());
    config.setUsername(connectionData.getUsername());
    config.setPassword(connectionData.getPassword());

    hikariDataSource = new HikariDataSource(config);
  }

  public void close() {
    if (isConnected()) {
      hikariDataSource.close();
    }
  }

  @Nonnull
  public HikariDataSource getHikariDataSource() {
    if (hikariDataSource == null) {
      throw new IllegalStateException("SQL connection not established.");
    }
    return hikariDataSource;
  }
}
