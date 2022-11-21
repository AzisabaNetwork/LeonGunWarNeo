package net.azisaba.lgwneo;

import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import net.azisaba.lgwneo.command.TestCommand;
import net.azisaba.lgwneo.config.LeonGunWarNeoConfig;
import net.azisaba.lgwneo.listener.GlobalMatchListener;
import net.azisaba.lgwneo.listener.JoinServerListener;
import net.azisaba.lgwneo.listener.global.AutoRespawnListener;
import net.azisaba.lgwneo.listener.global.ChoreListener;
import net.azisaba.lgwneo.listener.global.ExplosionDamageCalculateListener;
import net.azisaba.lgwneo.listener.global.KillMessageChanger;
import net.azisaba.lgwneo.listener.ldm.LDMSpawnProtectionListener;
import net.azisaba.lgwneo.listener.ldm.LeaderDeathMatchListener;
import net.azisaba.lgwneo.match.MatchOrganizer;
import net.azisaba.lgwneo.match.mode.MatchFactory;
import net.azisaba.lgwneo.party.PartyController;
import net.azisaba.lgwneo.redis.LobbyServerNameFetcher;
import net.azisaba.lgwneo.redis.MatchDataUpdater;
import net.azisaba.lgwneo.redis.ProxyRegisteredServerNameFetcher;
import net.azisaba.lgwneo.redis.ServerIdDefiner;
import net.azisaba.lgwneo.redis.data.RedisConnectionData;
import net.azisaba.lgwneo.redis.data.RedisKeys;
import net.azisaba.lgwneo.redis.pubsub.MatchJoinRequestSubscriber;
import net.azisaba.lgwneo.redis.pubsub.PubSubHandler;
import net.azisaba.lgwneo.sql.MySQLConnector;
import net.azisaba.lgwneo.taskchain.BukkitTaskChainFactory;
import net.azisaba.lgwneo.util.Chat;
import net.azisaba.lgwneo.util.ServerTransferUtils;
import net.azisaba.lgwneo.world.SlimeWorldManagerWorldLoader;
import net.azisaba.lgwneo.world.map.MatchMapLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Getter
public class LeonGunWarNeo extends JavaPlugin {

  private static TaskChainFactory taskChainFactory;
  private static final String CHAT_PREFIX = Chat.f("&8[&7System&8] ");

  private LeonGunWarNeoConfig leonGunWarNeoConfig;
  private ServerIdDefiner serverIdDefiner;
  private LobbyServerNameFetcher lobbyServerNameFetcher;
  private ProxyRegisteredServerNameFetcher proxyRegisteredServerNameFetcher;
  private PubSubHandler pubSubHandler;
  private MatchMapLoader matchMapLoader;
  private MatchOrganizer matchOrganizer;
  private MatchFactory matchFactory;
  private MatchDataUpdater matchDataUpdater;
  private PartyController partyController;

  private JedisPool jedisPool;
  private MySQLConnector mySQLConnector;

  @Override
  public void onEnable() {
    taskChainFactory = BukkitTaskChainFactory.create(this);

    Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    ServerTransferUtils.init(this);

    // Configを読み込む
    saveDefaultConfig();
    leonGunWarNeoConfig = new LeonGunWarNeoConfig(this).load();

    // Redisに接続する
    jedisPool = createJedisPool(leonGunWarNeoConfig.getRedisConnectionData());

    // MySQLに接続する
    mySQLConnector = new MySQLConnector(leonGunWarNeoConfig.getMySQLConnectionData());
    mySQLConnector.connect();

    // サーバーIDを決定する
    serverIdDefiner = new ServerIdDefiner(jedisPool);
    String uniqueServerId = serverIdDefiner.define();
    serverIdDefiner.runKeepServerUniqueIdTask(this);

    partyController = new PartyController();

    // ロビーサーバー名を取得するためのクラスを初期化する
    lobbyServerNameFetcher = new LobbyServerNameFetcher(jedisPool);
    lobbyServerNameFetcher.runRefreshTask(this);

    // プロキシに登録されているサーバー名を取得するためのクラスを初期化する
    proxyRegisteredServerNameFetcher = new ProxyRegisteredServerNameFetcher(this);

    // マップ情報を取得する
    matchMapLoader = new MatchMapLoader(this, mySQLConnector);
    matchMapLoader.load();

    matchOrganizer = new MatchOrganizer(this);
    matchFactory = new MatchFactory(this, jedisPool);
    matchDataUpdater = new MatchDataUpdater(this, jedisPool, matchOrganizer);
    matchDataUpdater.runUpdateTask();

    pubSubHandler = new PubSubHandler(jedisPool);
    pubSubHandler.startSubscribe(new MatchJoinRequestSubscriber(this),
        RedisKeys.MATCH_JOIN_REQUEST_PREFIX + "request");

    SlimePlugin slimePlugin =
        (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
    SlimeLoader slimeSQLLoader = slimePlugin.getLoader("mysql");
    matchFactory.setWorldLoader(
        new SlimeWorldManagerWorldLoader(this, slimePlugin, slimeSQLLoader));

    Bukkit.getPluginCommand("test").setExecutor(new TestCommand());

    Bukkit.getPluginManager().registerEvents(new GlobalMatchListener(this), this);
    Bukkit.getPluginManager().registerEvents(new JoinServerListener(this), this);

    Bukkit.getPluginManager().registerEvents(new AutoRespawnListener(this), this);
    Bukkit.getPluginManager().registerEvents(new ChoreListener(this), this);
    Bukkit.getPluginManager().registerEvents(new ExplosionDamageCalculateListener(), this);
    Bukkit.getPluginManager().registerEvents(new KillMessageChanger(this), this);

    Bukkit.getPluginManager().registerEvents(new LDMSpawnProtectionListener(this), this);
    Bukkit.getPluginManager().registerEvents(new LeaderDeathMatchListener(matchOrganizer), this);

    Bukkit.getLogger().info(getName() + " enabled.");

    // TODO: must be deleted. its debug code
    LeonGunWarNeo.newChain()
        .delay(5, TimeUnit.SECONDS)
        .async(() -> getMatchFactory()
            .createLeaderDeathMatch(
                getMatchMapLoader().getRandomMatchMap().getMapName(), false))
        .execute();
  }

  @Override
  public void onDisable() {
    Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);

    if (serverIdDefiner != null) {
      serverIdDefiner.executeShutdownProcess();
    }
    if (jedisPool != null && !jedisPool.isClosed()) {
      jedisPool.close();
    }
    if (mySQLConnector != null && mySQLConnector.isConnected()) {
      mySQLConnector.close();
    }

    Bukkit.getLogger().info(getName() + " disabled.");
  }

  private JedisPool createJedisPool(RedisConnectionData data) {
    if (data.getUsername() != null && data.getPassword() != null) {
      return new JedisPool(
          data.getHostname(), data.getPort(), data.getUsername(), data.getPassword());
    } else if (data.getPassword() != null) {
      return new JedisPool(
          new JedisPoolConfig(), data.getHostname(), data.getPort(), 3000, data.getPassword());
    } else if (data.getUsername() != null && data.getPassword() == null) {
      throw new IllegalArgumentException(
          "Redis password cannot be null if redis username is not null");
    } else {
      return new JedisPool(new JedisPoolConfig(), data.getHostname(), data.getPort());
    }
  }

  public static <T> TaskChain<T> newChain() {
    return taskChainFactory.newChain();
  }

  public static <T> TaskChain<T> newSharedChain(String name) {
    return taskChainFactory.newSharedChain(name);
  }

  public static String getChatPrefix() {
    return CHAT_PREFIX;
  }
}
