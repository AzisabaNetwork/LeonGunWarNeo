package net.azisaba.lgwneo.match.mode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.animation.MatchResultAnimation;
import net.azisaba.lgwneo.match.KillDeathAssistCounter;
import net.azisaba.lgwneo.match.component.MatchResult;
import net.azisaba.lgwneo.match.component.MatchStatus;
import net.azisaba.lgwneo.match.component.MatchTeam;
import net.azisaba.lgwneo.match.distributor.NormalTeamDistributor;
import net.azisaba.lgwneo.match.distributor.TeamDistributor;
import net.azisaba.lgwneo.party.Party;
import net.azisaba.lgwneo.scoreboard.LeaderDeathMatchScoreboard;
import net.azisaba.lgwneo.task.FlexibleCountdownTask;
import net.azisaba.lgwneo.util.Chat;
import net.azisaba.lgwneo.util.ItemStackUtils;
import net.azisaba.lgwneo.util.ServerTransferUtils;
import net.azisaba.lgwneo.util.TimeFormatUtils;
import net.azisaba.lgwneo.world.map.MatchMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

/**
 * リーダーデスマッチ <br> 2チームに分かれ、各チームにリーダーが一人存在する。リーダーが殺された場合、リーダーは別のプレイヤーとなり、相手チームにボーナスポイントが入る。
 * リーダーではないプレイヤーが殺された場合、相手チームに1ポイントが入る。
 *
 * @author siloneco
 */
@RequiredArgsConstructor
public class LeaderDeathMatch implements Match {

  private final LeonGunWarNeo plugin;
  @Getter
  private final String matchId;
  @Getter
  private final World world;
  @Getter
  private final MatchMap mapData;

  @Getter
  @Setter
  private TeamDistributor teamDistributor = new NormalTeamDistributor();

  private final HashMap<MatchTeam, Set<Player>> teamPlayerMap = new HashMap<>();
  private final HashMap<MatchTeam, Set<UUID>> offlineTeamPlayerMap = new HashMap<>();
  private final Set<Party> queueingParties = new HashSet<>();
  private final HashMap<MatchTeam, UUID> teamLeaderMap = new HashMap<>();

  @Getter
  private final FlexibleCountdownTask startCountdownTask = new FlexibleCountdownTask(15);
  @Getter
  private final FlexibleCountdownTask matchCountdownTask = new FlexibleCountdownTask(600);

  @Getter
  private final KillDeathAssistCounter killDeathAssistCounter = new KillDeathAssistCounter();

  @Getter
  private final HashMap<MatchTeam, AtomicInteger> scoreMap = new HashMap<>();
  private LeaderDeathMatchScoreboard leaderDeathMatchScoreboard;

  @Getter
  private Scoreboard bukkitScoreboard;
  private final HashMap<MatchTeam, Team> bukkitScoreboardTeamMap = new HashMap<>();

  @Getter
  private MatchStatus matchStatus = MatchStatus.INITIALIZING;

  @Getter
  @Setter
  private boolean privateMatch = true; // fail safe

  private final HashMap<MatchTeam, ItemStack> chestPlateMap = new HashMap<>();

  @Override
  public Map<String, Object> getMatchInformationAsMap() {
    HashMap<String, Object> data = new HashMap<>();

    // 基本的な情報
    data.put("matchId", matchId);
    data.put("serverId", plugin.getServerIdDefiner().getServerUniqueId());
    data.put("proxyRegisteredServerName", plugin.getProxyRegisteredServerNameFetcher().fetch());
    data.put("mapName", mapData.getMapName());
    data.put("status", matchStatus.name());
    data.put("mode", "Leader_Death_Match");
    data.put("remainingSeconds", matchCountdownTask.getRemainingTime());
    data.put("privateMatch", privateMatch);

    // プレイヤー情報
    data.put("maxPlayers", getMapData().getMaxTeamPlayer() * 2);
    data.put("playerNames",
        getParticipatePlayers().stream().map(Player::getName).collect(Collectors.toList()));
    data.put("playerUuids",
        getParticipatePlayers().stream().map(p -> p.getUniqueId().toString())
            .collect(Collectors.toList()));

    return data;
  }

  /**
   * リーダーデスマッチを開始する。プレイヤーが2人未満の場合は開始しない。
   *
   * @return 試合の開始に成功した場合はtrue、失敗した場合はfalse
   */
  @Override
  public boolean startMatch() {
    // TODO デバッグのためにコメントアウトしているので、終わったら外す
//     オンラインプレイヤーが2人未満か、パーティが1つの場合は開始しない
    if (!canStartMatch()) {
      return false;
    }

    // プレイヤーをチームに分ける
    Map<MatchTeam, List<UUID>> distributedTeamMap =
        teamDistributor.distribute(
            getQueueingParties(), Arrays.asList(MatchTeam.RED, MatchTeam.BLUE));
    // 振り分けたプレイヤーをteamPlayerMapとofflineTeamPlayerMapに適用する
    for (Entry<MatchTeam, List<UUID>> entry : distributedTeamMap.entrySet()) {
      Set<Player> onlinePlayers = new HashSet<>();
      for (UUID uuid : entry.getValue()) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
          onlinePlayers.add(p);
        } else {
          offlineTeamPlayerMap.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(uuid);
        }
      }

      teamPlayerMap.put(entry.getKey(), onlinePlayers);
    }

    // スコアボードを作成
    bukkitScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    // スコアボードチームを作成
    initializeBukkitScoreboardTeam(MatchTeam.RED);
    initializeBukkitScoreboardTeam(MatchTeam.BLUE);

    // チームに分けられたプレイヤーに対して初期化処理を行い、テレポートする
    for (Map.Entry<MatchTeam, List<UUID>> entry : distributedTeamMap.entrySet()) {
      // UUIDからオンラインのプレイヤーのみを取得
      List<Player> onlinePlayerList =
          entry.getValue().stream()
              .map(Bukkit::getPlayer)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      onlinePlayerList.forEach(p -> initializePlayerForTeam(p, entry.getKey()));

      // プレイヤーをテレポートし、音を鳴らす
      Location spawnLocation = mapData.getSpawnFor(entry.getKey());
      onlinePlayerList.forEach(
          p -> {
            p.teleport(spawnLocation);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_AMBIENT, 1, 1);
          });
    }

    // リーダーを決める
    for (MatchTeam team : Arrays.asList(MatchTeam.RED, MatchTeam.BLUE)) {
      Player leader = changeLeader(team);
      leader.sendTitle(Chat.f("&cあなたがリーダーです！"), Chat.f("&7倒されないように立ち回ろう！"), 0, 40, 10);
    }

    // スコアボードを初期化
    leaderDeathMatchScoreboard = new LeaderDeathMatchScoreboard(this, plugin);

    // 待機場所の浮島を消す
    deleteQueueIsland();

    // タイマーを実行する
    matchCountdownTask.setTimeElapsedAction(this::timeElapsedAction);
    matchCountdownTask.startCountdown(plugin);

    matchStatus = MatchStatus.PLAYING;
    return true;
  }

  @Override
  public CompletableFuture<Boolean> endMatch(boolean force) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (force) {
      // TODO: implement force terminate.
      future.complete(true);
      return future;
    }

    getParticipatePlayers().forEach(p -> {
      p.setGameMode(GameMode.SPECTATOR);
      p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    });

    MatchTeam winnerTeam = null;
    if (getScore(MatchTeam.RED) > getScore(MatchTeam.BLUE)) {
      winnerTeam = MatchTeam.RED;
    } else if (getScore(MatchTeam.RED) < getScore(MatchTeam.BLUE)) {
      winnerTeam = MatchTeam.BLUE;
    }

    Runnable sendToLobby = () -> {
      int waitSeconds = 7;
      for (Player p : getParticipatePlayers()) {
        p.sendMessage(
            Chat.f("{0} &a{1}秒後にサーバーにTPします...", LeonGunWarNeo.getChatPrefix(), waitSeconds));
      }

      Bukkit.getScheduler()
          .runTaskLater(plugin, () -> ServerTransferUtils.sendToLobby(getParticipatePlayers()),
              20L * waitSeconds);
    };

    if (winnerTeam != null) {
      MatchResultAnimation winnerAnimation = new MatchResultAnimation(plugin, MatchResult.VICTORY,
          getTeamPlayers(winnerTeam), sendToLobby);
      MatchResultAnimation defeatAnimation = new MatchResultAnimation(plugin, MatchResult.DEFEAT,
          getTeamPlayers(winnerTeam.getOppositeTeam()), null);

      winnerAnimation.start();
      defeatAnimation.start();
    } else {
      MatchResultAnimation drawAnimation = new MatchResultAnimation(plugin, MatchResult.DRAW,
          getParticipatePlayers(), sendToLobby);
      drawAnimation.start();
    }

    future.complete(true);
    return future;
  }

  @Override
  public Set<Player> getParticipatePlayers() {
    // teamPlayerMapから全てのプレイヤーを取得する
    return teamPlayerMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
  }

  /**
   * チームのプレイヤーを取得する
   *
   * @param team プレイヤーを取得したいチーム
   * @return チームに参加しているプレイヤーのSet。そのチームが存在しない場合は空のSetを返す
   */
  public Set<Player> getTeamPlayers(MatchTeam team) {
    return teamPlayerMap.getOrDefault(team, Collections.emptySet());
  }

  @Override
  public Set<Party> getQueueingParties() {
    return new HashSet<>(queueingParties);
  }

  @Override
  public boolean canJoin(Party party) {
    // 実際にリストにパーティを追加してみて、チームの人数がオーバーしないかどうか確認する
    List<Party> parties = new ArrayList<>(getQueueingParties());
    parties.add(party);

    parties.sort(Comparator.comparing(Party::size).reversed());
    int count = 0;
    for (int i = 0, size = parties.size(); i < size; i++) {
      if (i % 2 != 0) {
        continue;
      }
      count += parties.get(i).size();
    }

    return count <= mapData.getMaxTeamPlayer();
  }

  @Override
  public boolean addPartyToQueue(Party party) {
    boolean added = queueingParties.add(party);

    // プレイヤーがすでに鯖内に存在する場合はテレポートする
    for (UUID uuid : party.getMemberUuidSet()) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.isOnline()) {
        p.teleport(mapData.getQueueSpawn());
        p.setGameMode(GameMode.SURVIVAL);
      }
    }

    // 試合を始められる環境だった場合、カウントダウンを開始する
    if (canStartMatch()) {
      startCountdownTask.setTimeElapsedAction(
          (time) -> {
            if (time <= 0) {
              startMatch();
              return;
            }

            if (time <= 5 || time % 5 == 0) {
              for (Player p : world.getPlayers()) {
                p.sendMessage(
                    Chat.f("{0}&r試合開始まであと&a{1}秒&r！", LeonGunWarNeo.getChatPrefix(), time));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1, 1);
              }
            }
          });
      startCountdownTask.startCountdown(plugin);
    }

    return added;
  }

  @Override
  public boolean removePartyFromQueue(Party party) {
    return queueingParties.remove(party);
  }

  @Override
  public Location getRespawnLocationFor(Player player) {
    // チームが取得できる場合、そのチームのスポーン地点を返す
    MatchTeam team = getTeamFor(player);
    if (team != null) {
      return mapData.getSpawnFor(team);
    }

    // チームが取得できない場合、ワールドが同じであれば待機場所のスポーン地点を返す
    if (player.getWorld().equals(world)) {
      return mapData.getQueueSpawn();
    }
    // ワールドも違うのであれば、全く無関係な試合である可能性が高いためnullを返す
    return null;
  }

  /**
   * 試合でのプレイヤーのチームを取得する
   *
   * @param uuid チームを取得するプレイヤーのUUID
   * @return プレイヤーのチームを返す。プレイヤーが試合に参加していない場合はnull
   */
  @Nullable
  public MatchTeam getTeamFor(UUID uuid) {
    // 試合中のプレイヤーに居た場合
    for (Entry<MatchTeam, Set<Player>> entry : teamPlayerMap.entrySet()) {
      if (entry.getValue().stream().anyMatch(p -> p.getUniqueId().equals(uuid))) {
        return entry.getKey();
      }
    }

    // オフラインのチームプレイヤーに居た場合
    for (Entry<MatchTeam, Set<UUID>> entry : offlineTeamPlayerMap.entrySet()) {
      if (entry.getValue().contains(uuid)) {
        return entry.getKey();
      }
    }

    return null;
  }

  /**
   * 試合でのプレイヤーのチームを取得する
   *
   * @param player チームを取得するプレイヤー
   * @return プレイヤーのチームを返す。プレイヤーが試合に参加していない場合はnull
   */
  public MatchTeam getTeamFor(Player player) {
    return getTeamFor(player.getUniqueId());
  }

  /**
   * チームの現在のスコアを取得する
   *
   * @param team スコアを取得したいチーム
   * @return チームの現在のスコア
   */
  public int getScore(MatchTeam team) {
    if (scoreMap.containsKey(team)) {
      return scoreMap.get(team).get();
    }
    return 0;
  }

  /**
   * 指定したチームにスコアを追加する
   *
   * @param team  スコアを追加するチーム
   * @param score 追加するスコア
   */
  public void addScore(MatchTeam team, int score) {
    if (scoreMap.containsKey(team)) {
      scoreMap.get(team).addAndGet(score);
    } else {
      scoreMap.put(team, new AtomicInteger(score));
    }
  }

  /**
   * 指定したチームのリーダーを変更する
   *
   * @param team リーダーを変更したいチーム
   * @return 新しく選ばれたチームリーダー
   */
  public Player changeLeader(MatchTeam team) {
    // SetをListに変換する
    List<Player> playerList = new ArrayList<>(teamPlayerMap.get(team));
    // プレイヤーが一人もいない場合リーダーを決定できないためnullを返す
    if (playerList.isEmpty()) {
      return null;
    }

    // ランダムにリーダーを決定する
    Collections.shuffle(playerList);
    Player newLeader = playerList.get(0);
    teamLeaderMap.put(team, newLeader.getUniqueId());

    return newLeader;
  }

  /**
   * チームのリーダーを取得する
   *
   * @param team リーダーを取得したいチーム
   * @return チームのリーダー。そのチームにリーダーが存在しない場合は新しく設定されたリーダーを返す
   */
  public Player getLeader(MatchTeam team) {
    Player leader = Bukkit.getPlayer(teamLeaderMap.get(team));
    if (leader == null) {
      return changeLeader(team);
    }

    return leader;
  }

  /**
   * 現在のキュー状況で試合が開始できるかどうかを返す
   *
   * @return 試合が開始できる場合はtrue、できない場合はfalse
   */
  private boolean canStartMatch() {
    return getQueueingParties().stream().mapToLong(Party::getOnlineCount).sum() >= 2
        && getQueueingParties().size() >= 2;
  }

  /**
   * カウントダウンタスクで時間が変動した時に呼び出されるメソッド。 スコアボードを更新したり、残り時間をチャットやタイトルに表示したりする。
   *
   * @param remainingTime 　残り時間
   */
  private void timeElapsedAction(int remainingTime) {
    if (remainingTime <= 0) {
      endMatch(false);
    }

    // 非同期でスコアボードを更新する
    leaderDeathMatchScoreboard.updateLines(true);

    // 残り時間を音と共に表示する
    if (Arrays.asList(1, 2, 3, 4, 5, 10, 30, 60, 180, 300).contains(remainingTime)) {
      String msg =
          Chat.f(
              "{0} &7残り&6{1}&7！",
              LeonGunWarNeo.getChatPrefix(), TimeFormatUtils.format(remainingTime));
      for (Player p : world.getPlayers()) {
        p.sendMessage(msg);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1, 1);
      }
    }
  }

  /**
   * チームに追加されたプレイヤーに対して初期化処理を行う
   *
   * @param player 新規プレイヤー
   * @param team   プレイヤーが追加されたチーム
   */
  private void initializePlayerForTeam(Player player, MatchTeam team) {
    // スポーン地点を設定する
    Location spawnLoc = mapData.getSpawnFor(team);
    player.setBedSpawnLocation(spawnLoc);

    // チェストプレートが存在しない場合は生成し、プレイヤーのチェストプレートを変更する
    if (!chestPlateMap.containsKey(team)) {
      chestPlateMap.put(team, ItemStackUtils.getTeamChestPlate(team));
    }
    player.getInventory().setArmorContents(new ItemStack[4]);
    player.getInventory().setChestplate(chestPlateMap.get(team).clone());

    // チームスコアボードを設定
    player.setScoreboard(bukkitScoreboard);
    Team scoreboardTeam = bukkitScoreboardTeamMap.get(team);
    if (scoreboardTeam == null) {
      throw new IllegalStateException("Team scoreboard is not registered.");
    }
    scoreboardTeam.addEntry(player.getName());

    // 名前の色をチームの色に変更する
    player.setDisplayName(Chat.f("{0}{1}&r", team.getChatColor(), player.getName()));
    player.setPlayerListName(Chat.f("{0}{1}&r", team.getChatColor(), player.getName()));

    // ゲームモードをサバイバルにする
    player.setGameMode(GameMode.SURVIVAL);
  }

  /**
   * スコアボードのチームを作成し、初期設定を行う
   *
   * @param team 初期設定を行うMatchTeam
   */
  private void initializeBukkitScoreboardTeam(MatchTeam team) {
    Team scoreboardTeam = bukkitScoreboard.registerNewTeam(team.name());

    // 各パラメータを指定
    scoreboardTeam.setColor(team.getChatColor());
    scoreboardTeam.setAllowFriendlyFire(false);
    scoreboardTeam.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
    scoreboardTeam.setOption(Option.COLLISION_RULE, OptionStatus.NEVER);
    scoreboardTeam.setPrefix(team.getChatColor() + "");

    bukkitScoreboardTeamMap.putIfAbsent(team, scoreboardTeam);
  }

  /**
   * キューの待機中に使用される浮島を削除する
   */
  private void deleteQueueIsland() {
    Location start = mapData.getQueueSpawn().clone().subtract(8, 9, 8);
    Location end = mapData.getQueueSpawn().clone().add(8, 7, 8);

    for (int x = start.getBlockX(), endX = end.getBlockX(); x <= endX; x++) {
      for (int y = start.getBlockY(), endY = end.getBlockY(); y <= endY; y++) {
        for (int z = start.getBlockZ(), endZ = end.getBlockZ(); z <= endZ; z++) {
          world.getBlockAt(x, y, z).setType(Material.AIR);
        }
      }
    }
  }
}
