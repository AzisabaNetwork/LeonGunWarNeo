package net.azisaba.lgwneo.redis;

import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;

@RequiredArgsConstructor
public class ProxyRegisteredServerNameFetcher {

  private final LeonGunWarNeo plugin;

  public String fetch() {
    // TODO ファイル読み込みに依存しないサーバー名取得方法を考える
    return plugin.getLeonGunWarNeoConfig().getServerName();
  }
}
