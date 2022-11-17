package net.azisaba.lgwneo.util;

import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;

/**
 * メッセージをフォーマットしてプレイヤーに送信するUtilityクラス
 *
 * @author siloneco
 */
@UtilityClass
public class MessageFormatUtils {

  private final String prefix = Chat.f("&8[&7System&8] &f");

  /**
   * メッセージにPrefixを付けてプレイヤーに送信する
   *
   * @param player  メッセージを送信するプレイヤー
   * @param message 送信するメッセージ
   */
  public static void sendMessageWithPrefix(Player player, String message) {
    player.sendMessage(prefix + message);
  }

  /**
   * メッセージにPrefixを付けてプレイヤーに送信する
   *
   * @param players メッセージを送信するプレイヤー群
   * @param message 送信するメッセージ
   */
  public static void sendMessageWithPrefix(Collection<Player> players, String message) {
    players.forEach(p -> sendMessageWithPrefix(p, message));
  }
}
