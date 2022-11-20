package net.azisaba.lgwneo.listener;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.mode.Match;
import net.azisaba.lgwneo.party.Party;
import net.azisaba.lgwneo.util.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

@RequiredArgsConstructor
public class JoinServerListener implements Listener {

  private final LeonGunWarNeo plugin;

  @EventHandler
  public void onJoin(PlayerLoginEvent e) {
    Player p = e.getPlayer();

    Match match = plugin.getMatchOrganizer().getJoinRequestFor(p.getUniqueId());

    if (match == null) {
      e.setResult(Result.KICK_OTHER);
      e.setKickMessage(Chat.f("&cロビーから試合への参加処理を行ってください！"));
      return;
    }

    if (match.getQueueingParties().stream().map(Party::getMemberUuidSet).flatMap(Set::stream)
        .noneMatch(uuid -> uuid.equals(p.getUniqueId()))) {
      e.setResult(Result.KICK_OTHER);
      e.setKickMessage(Chat.f("&c試合に参加できませんでした。"));
      return;
    }

    LeonGunWarNeo.newChain()
        .delay(1)
        .sync(() -> {
          if (!p.isOnline()) {
            return;
          }

          match.setupPartyPlayer(p);
        }).execute();
  }
}
