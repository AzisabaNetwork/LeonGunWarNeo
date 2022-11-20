package net.azisaba.lgwneo.redis.data;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Redisのキーをまとめるクラス
 *
 * @author siloneco
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum RedisKeys {
  UNIQUE_SERVER_ID_PREFIX("server:"), // 試合サーバーIDの使用を示すキー
  LOBBY_MAP("lobbies"), // ロビーのサーバー名を格納するHashのキー
  MATCH_PREFIX("matches:"), // ロビーのサーバー名を格納するHashのキー

  MATCH_JOIN_REQUEST_PREFIX("join-request:"), // 試合参加リクエストのPubSubキー
  ;

  private final String key;

  public String getKey() {
    return "leongunwarneo:" + key;
  }

  @Override
  public String toString() {
    return getKey();
  }
}
