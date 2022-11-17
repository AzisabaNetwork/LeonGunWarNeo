package net.azisaba.lgwneo.sql;

import lombok.Data;

/**
 * MySQLの接続情報を保存するクラス
 *
 * @author siloneco
 */
@Data
public class MySQLConnectionData {

  private final String hostname;
  private final int port;
  private final String username;
  private final String password;
  private final String database;
}
