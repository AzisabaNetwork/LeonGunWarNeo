package net.azisaba.lgwneo.util;

import lombok.experimental.UtilityClass;
import net.azisaba.lgwneo.match.component.MatchTeam;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

@UtilityClass
public class ItemStackUtils {

  /**
   * チームに応じたチェストプレートを生成する
   *
   * @param team チェストプレートを生成するチーム
   * @return 生成されたチェストプレート
   */
  public static ItemStack getTeamChestPlate(MatchTeam team) {
    ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
    LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
    meta.setDisplayName(team.getTeamName());
    meta.setColor(team.getColor());
    meta.setUnbreakable(true);
    item.setItemMeta(meta);
    item.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
    item.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 1);
    return item;
  }
}
