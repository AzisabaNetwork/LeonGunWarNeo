package net.azisaba.lgwneo.redis.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.mode.Match;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public class MatchJoinRequestSubscriber extends JedisPubSub {

  private final LeonGunWarNeo plugin;

  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onMessage(String channel, String message) {
    Map<String, Object> map;
    try {
      map = mapper.readValue(message, new TypeReference<Map<String, Object>>() {
      });
    } catch (JsonProcessingException e) {
      plugin.getLogger().warning("Unable to parse JSON in channel \"" + channel + "\": " + message);
      return;
    }

    if (!validateMap(map)) {
      plugin.getLogger()
          .warning("Invalid JSON received in channel \"" + channel + "\": " + message);
      return;
    }

    String server = (String) map.get("server");
    if (!server.equalsIgnoreCase(plugin.getServerIdDefiner().getServerUniqueId())) {
      return;
    }

    UUID uuid = UUID.fromString((String) map.get("uuid"));
    String matchId = (String) map.get("matchId");

    Match match = plugin.getMatchOrganizer().getMatch(matchId);
    if (match == null) {
      return;
    }

    match.addPartyToQueue(plugin.getPartyController().getPartyOf(uuid));
    plugin.getMatchOrganizer().setJoinRequest(uuid, matchId);

    if (map.containsKey("partyMembers")) {
      Object o = map.get("partyMembers");
      if (o instanceof List) {
        List<UUID> uuidList = getPartyMembers(map);
        if (uuidList == null) {
          return;
        }

        for (UUID partyMemberUUID : uuidList) {
          plugin.getMatchOrganizer().setJoinRequest(partyMemberUUID, matchId);
        }
      }
    }
  }

  private boolean validateMap(Map<String, Object> map) {
    return map.containsKey("server") && map.containsKey("uuid") && map.containsKey("matchId");
  }

  private List<UUID> getPartyMembers(Map<String, Object> map) {
    Object o = map.get("partyMembers");

    if (!(o instanceof List)) {
      return null;
    }

    List<String> strUuidList = new ArrayList<>();

    for (Object obj : (List<?>) o) {
      if (obj instanceof String) {
        strUuidList.add((String) obj);
      }
    }

    return strUuidList.stream().map(UUID::fromString).collect(Collectors.toList());
  }
}
