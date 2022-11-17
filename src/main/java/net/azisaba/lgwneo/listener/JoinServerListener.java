package net.azisaba.lgwneo.listener;

import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.mode.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class JoinServerListener implements Listener {

  private final LeonGunWarNeo plugin;

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    Player p = e.getPlayer();

    Match match = plugin.getMatchOrganizer().getAllMatches().get(0);
    if (match != null) {
      match.addPartyToQueue(plugin.getPartyController().getPartyOf(p));
    }
  }
}
