package net.azisaba.lgwneo.redis.data;

import lombok.Data;

/**
 * Redisの接続情報を保存するクラス
 *
 * @author siloneco
 */
@Data
public class RedisConnectionData {

  private final String hostname;
  private final int port;
  private final String username;
  private final String password;
}
