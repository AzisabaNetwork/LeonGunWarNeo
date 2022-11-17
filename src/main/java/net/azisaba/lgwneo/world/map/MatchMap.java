package net.azisaba.lgwneo.world.map;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import net.azisaba.lgwneo.match.component.MatchTeam;
import org.bukkit.Location;

@Data
public class MatchMap implements Cloneable {

  private String mapName;
  private Location queueSpawn;

  private final HashMap<MatchTeam, Location> teamSpawns = new HashMap<>();
  private int maxTeamPlayer;

  public MatchMap(
      String mapName,
      Location queueSpawn,
      Location redSpawn,
      Location blueSpawn,
      int maxTeamPlayer) {
    this.mapName = mapName;
    this.queueSpawn = queueSpawn;
    this.teamSpawns.put(MatchTeam.RED, redSpawn);
    this.teamSpawns.put(MatchTeam.BLUE, blueSpawn);
    this.maxTeamPlayer = maxTeamPlayer;
  }

  public Location getSpawnFor(MatchTeam team) {
    return teamSpawns.get(team);
  }

  @Override
  public MatchMap clone() {
    try {
      MatchMap clone = (MatchMap) super.clone();
      clone.queueSpawn = queueSpawn.clone();

      for (Map.Entry<MatchTeam, Location> entry : teamSpawns.entrySet()) {
        clone.teamSpawns.put(entry.getKey(), entry.getValue().clone());
      }
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
