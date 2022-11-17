package net.azisaba.lgwneo.util;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.experimental.UtilityClass;
import net.azisaba.lgwneo.LeonGunWarNeo;
import org.bukkit.entity.Player;

@UtilityClass
public class ServerTransferUtils {

  private static LeonGunWarNeo plugin;

  public static void init(LeonGunWarNeo plugin) {
    ServerTransferUtils.plugin = plugin;
  }

  public static void sendToServer(Player p, String server) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("Connect");
    out.writeUTF(server);

    p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
  }

  public static boolean sendToLobby(Player p) {
    String lobbyServerName = plugin.getLobbyServerNameFetcher().getRandomLobbyServerName();
    if (lobbyServerName == null) {
      return false;
    }

    sendToServer(p, lobbyServerName);
    return true;
  }

  public static boolean sendToLobby(Collection<Player> players) {
    List<String> lobbies =
        new ArrayList<>(plugin.getLobbyServerNameFetcher().getLobbyServerNames());
    if (lobbies.isEmpty()) {
      return false;
    }

    if (lobbies.size() > players.size()) {
      for (Player p : players) {
        sendToLobby(p);
      }
    } else {
      int playersPerServer = players.size() / lobbies.size();
      int serverIndex = 0;
      int sentCount = 0;

      for (Player p : players) {
        if (serverIndex >= lobbies.size()) {
          sendToLobby(p);
          continue;
        }

        sendToServer(p, lobbies.get(serverIndex));
        sentCount++;

        if (sentCount >= playersPerServer) {
          serverIndex++;
          sentCount = 0;
        }
      }
    }

    return true;
  }
}
