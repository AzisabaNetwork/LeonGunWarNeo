package net.azisaba.lgwneo.listener.global;

import com.shampaggon.crackshot.CSDirector;
import com.shampaggon.crackshot.CSUtility;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.mode.Match;
import net.azisaba.lgwneo.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class KillMessageChanger implements Listener {

  private final LeonGunWarNeo plugin;
  private final CSUtility crackShot = new CSUtility();

  /**
   * キルログを変更するListener
   */
  @EventHandler
  public void deathMessageChanger(PlayerDeathEvent e) {
    Player p = e.getEntity();
    Match match = plugin.getMatchOrganizer().getMatchFromPlayer(p);

    // 試合中ではない場合はreturn
    if (match == null) {
      return;
    }
    // 試合中のワールドではない場合はreturn
    if (p.getWorld() != match.getWorld()) {
      return;
    }

    // 殺したEntityが居ない場合か、同じプレイヤーの場合自滅とする
    if (p.getKiller() == null || p.getKiller() == p) {

      // メッセージ削除
      e.setDeathMessage(null);

      // メッセージを作成
      String msg = Chat.f("{0}{1} &7は自滅した！", LeonGunWarNeo.getChatPrefix(), p.getDisplayName());
      // メッセージ送信
      match.getWorld().getPlayers().forEach(player -> player.sendMessage(msg));

      // コンソールに出力
      Bukkit.getConsoleSender().sendMessage("(" + match.getMatchId() + ") " + msg);
      return;
    }

    Player killer = e.getEntity().getKiller();

    // 殺したアイテム
    ItemStack item = killer.getInventory().getItemInMainHand();

    // アイテム名を取得
    String itemName;
    if (item == null || item.getType() == Material.AIR) { // null または Air なら素手
      itemName = Chat.f("&6素手");
    } else if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) { // DisplayNameが指定されている場合
      // CrackShot Pluginを取得
      CSDirector crackShotPlugin = (CSDirector) Bukkit.getPluginManager().getPlugin("CrackShot");

      // 銃ID取得
      String nodes = crackShot.getWeaponTitle(item);
      // DisplayNameを取得
      itemName = crackShotPlugin.getString(nodes + ".Item_Information.Item_Name");

      // DisplayNameがnullの場合は普通にアイテム名を取得
      if (itemName == null) {
        itemName = item.getItemMeta().getDisplayName();
      }
    } else { // それ以外
      itemName = Chat.f("&6{0}", item.getType().name());
    }

    // メッセージ削除
    e.setDeathMessage(null);
    // メッセージ作成
    String msg =
        Chat.f(
            "{0}&r{1} &7━━━ [ &r{2} &7] ━━━> &r{3}",
            LeonGunWarNeo.getChatPrefix(), killer.getDisplayName(), itemName, p.getDisplayName());

    // メッセージ送信
    match.getWorld().getPlayers().forEach(player -> player.sendMessage(msg));

    // コンソールに出力
    Bukkit.getConsoleSender().sendMessage("(" + match.getMatchId() + ") " + msg);
  }
}
