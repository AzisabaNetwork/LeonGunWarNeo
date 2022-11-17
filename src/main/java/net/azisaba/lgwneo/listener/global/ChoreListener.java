package net.azisaba.lgwneo.listener.global;

import java.util.HashMap;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.mode.Match;
import net.azisaba.lgwneo.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 細々としたListenerを定義するクラス
 */
public class ChoreListener implements Listener {

  private final LeonGunWarNeo plugin;

  public ChoreListener(LeonGunWarNeo plugin) {
    this.plugin = plugin;

    // すでに読み込まれている全ワールドのKeepInventoryを有効にする
    Bukkit.getWorlds().forEach(this::setEnableKeepInventory);
  }


  /*
   * ホッパーが外部のアイテムを吸引するのを無効化するListener
   */
  @EventHandler
  public void onPickUp(InventoryPickupItemEvent e) {
    if (e.getInventory().getType() != InventoryType.HOPPER) {
      return;
    }

    e.setCancelled(true);
  }

  /*
   * アイテムの耐久値が減るのを無効化するListener
   */
  @EventHandler
  public void onPlayerItemDamage(PlayerItemDamageEvent e) {
    ItemStack item = e.getItem();

    // アイテムがnullの場合return
    if (item == null) {
      return;
    }

    // 釣り竿だった場合return
    if (item.getType() == Material.FISHING_ROD) {
      return;
    }

    // 耐久値の減少をキャンセル
    e.setCancelled(true);
    // インベントリを更新
    e.getPlayer().updateInventory();
  }

  /*
   * オフハンドにアイテムを持ち帰ることを禁止するListener
   */
  @EventHandler
  public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    e.setCancelled(true);
  }

  /*
   * インベントリーを閉じた際にオフハンドにアイテムがある場合は移動するかドロップするListener
   */
  @EventHandler
  public void onInventoryClose(InventoryCloseEvent e) {
    Player p = (Player) e.getPlayer();
    PlayerInventory inventory = p.getInventory();
    ItemStack offhand = inventory.getItemInOffHand();

    // オフハンドにアイテムがない場合はreturn
    if (offhand == null || offhand.getType() == Material.AIR) {
      return;
    }

    if (inventory.firstEmpty() != -1) {
      // インベントリーに空きがある場合はアイテムを追加
      inventory.addItem(offhand);
    } else {
      // インベントリーに空きがない場合はドロップ
      p.getWorld().dropItem(p.getLocation(), offhand);
    }

    // オフハンドからアイテムを消す
    inventory.setItemInOffHand(null);
  }

  /*
   * 金床とかまどのインベントリを開けれないようにするListener
   */
  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent e) {
    Player p = (Player) e.getPlayer();

    // 金床かかまどを開けようとしている場合、キャンセルして音を鳴らす
    if (e.getInventory().getType() == InventoryType.ANVIL
        || e.getInventory().getType() == InventoryType.FURNACE) {
      e.setCancelled(true);
      p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
    }
  }

  // priority MONITORでresultを元のアイテムに変更するListener
  private final HashMap<Player, ItemStack> cancelPlayerMap = new HashMap<>();

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCraftItem(CraftItemEvent e) {
    Player p = (Player) e.getWhoClicked();

    // クラフト結果
    ItemStack result = e.getInventory().getResult();

    // クラフト結果がない場合は無視
    if (result == null) {
      return;
    }

    // クラフト後のアイテムにカスタム名がある場合は無視
    if (result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
      return;
    }

    // クラフト後のアイテムがダイヤかエメラルドの場合は無視
    if (result.getType() == Material.DIAMOND || result.getType() == Material.EMERALD) {
      return;
    }

    // クラフト後のアイテムがダイヤブロックかエメラルドブロックの場合は無視
    if (result.getType() == Material.DIAMOND_BLOCK || result.getType() == Material.EMERALD_BLOCK) {
      return;
    }

    // 普通のアイテムはクラフト禁止！！
    e.setCancelled(true);

    // 音を鳴らす
    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);

    // Priority MONITORで元のアイテムに戻すためにHashMapに追加
    ItemStack item = result.clone();
    cancelPlayerMap.put(p, item);
  }

  /*
   * CrackShotがignoreCancelled = trueにしていないため、こちらでキャンセルしても向こう側でResultアイテムの名前が変わってしまい、
   * 2回目のクリックでクラフトできてはいけないアイテムまでクラフトできてしまうため、MONITORでデフォルトアイテムを再セットするメソッド
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void restoreDefaultItem(CraftItemEvent e) {
    Player p = (Player) e.getWhoClicked();

    // mapにプレイヤーが含まれている場合アイテムをセット
    if (cancelPlayerMap.containsKey(p)) {
      e.getInventory().setResult(cancelPlayerMap.get(p));

      // 削除
      cancelPlayerMap.remove(p);
    }
  }

  /*
   * TNTによるブロック破壊を禁止するListener
   */
  @EventHandler
  public void onGrenadeExplode(EntityExplodeEvent event) {
    event.blockList().clear();
  }

  /*
   * ワールドがロードされたときにKeep InventoryをtrueにするListener
   */
  @EventHandler
  public void onWorldInit(WorldInitEvent e) {
    // PlayerDeathEventで処理しても、CrackShotが勝手に処理してアイテムが消えるため、
    // ワールド初期化時にゲームルールからKeepInventoryを有効化する
    setEnableKeepInventory(e.getWorld());
  }

  /**
   * ゲームルールのKeepInventoryをtrueに設定する
   *
   * @param world KeepInventoryをtrueにしたいワールド
   */
  private void setEnableKeepInventory(World world) {
    // 既にKeepInventoryがtrueになってる場合はreturn
    if (world.getGameRuleValue("keepInventory").equals("true")) {
      return;
    }

    // KeepInventoryを有効化
    world.setGameRuleValue("keepInventory", "true");
  }

  /*
   * 誤った村人との取引が起こらないように、チャンクロード時に村人を消すListener
   */
  @EventHandler
  public void onChunkLoad(ChunkLoadEvent e) {
    for (Entity entity : e.getChunk().getEntities()) {
      if (entity.getType().equals(org.bukkit.entity.EntityType.VILLAGER)) {
        ((Villager) entity).damage(9999);
      }
    }
  }

  /*
   * 地面に刺さった矢を削除するListener
   */
  @EventHandler
  public void onProjectileHit(ProjectileHitEvent e) {
    // 当たった飛び道具が矢でなければreturn
    if (!(e.getEntity() instanceof Arrow)) {
      return;
    }

    Arrow arrow = (Arrow) e.getEntity();

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (arrow != null && arrow.isOnGround()) {
        // 矢を削除
        arrow.remove();
      }
    }, 0);
  }

  /*
   * 試合中に釣りをすることを禁止するListener
   */
  @EventHandler
  public void onInteract(PlayerFishEvent e) {
    Player p = e.getPlayer();

    // 試合プレイヤーに含まれていない場合return
    Match match = plugin.getMatchOrganizer().getMatchFromPlayer(p);
    if (match == null) {
      return;
    }

    // イベントキャンセル
    e.setCancelled(true);
    p.sendMessage(Chat.f("&c試合中に釣りをすることはできません！"));
  }
}