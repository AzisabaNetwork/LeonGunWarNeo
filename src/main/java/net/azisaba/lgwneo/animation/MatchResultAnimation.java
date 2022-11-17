package net.azisaba.lgwneo.animation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.azisaba.lgwneo.LeonGunWarNeo;
import net.azisaba.lgwneo.match.component.MatchResult;
import net.azisaba.lgwneo.util.Chat;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
@AllArgsConstructor
public class MatchResultAnimation implements Animation {

  private final LeonGunWarNeo plugin;
  private final MatchResult matchResult;
  private final Collection<Player> players;
  private Runnable callback;

  private final List<String> victoryTitle = Stream.of(
      AnimationTextGenerator.typing(Chat.f("&aVictory!")),
      AnimationTextGenerator.insertLeftToRight(Chat.f("&a&nVictory!"), Chat.f("&a")),
      AnimationTextGenerator.insertLeftToRight(Chat.f("&aVictory!"), Chat.f("&n"))
  ).flatMap(Collection::stream).collect(Collectors.toList());
  private final List<String> defeatTitle = Stream.of(
      AnimationTextGenerator.typing(Chat.f("&cDefeat..."))
  ).flatMap(Collection::stream).collect(Collectors.toList());
  private final List<String> drawTitle = Stream.of(
      AnimationTextGenerator.typing(Chat.f("&eDraw"))
  ).flatMap(Collection::stream).collect(Collectors.toList());

  @Override
  public void start() {
    Sound sound;
    List<String> animationTexts;
    if (matchResult == MatchResult.VICTORY) {
      sound = Sound.ENTITY_PLAYER_LEVELUP;
      animationTexts = victoryTitle;
    } else if (matchResult == MatchResult.DEFEAT) {
      sound = Sound.ENTITY_WITHER_DEATH;
      animationTexts = defeatTitle;
    } else {
      sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
      animationTexts = drawTitle;
    }

    new BukkitRunnable() {
      int i = 0;
      final String pauseMessage = Chat.f("&a&nVictory!");
      int pauseCount = 30;

      @Override
      public void run() {
        String title = animationTexts.get(i);
        if (title.equals(pauseMessage) && pauseCount > 0) {
          pauseCount--;
        } else {
          i++;
        }

        for (Player p : players) {
          p.sendTitle(title, "", 0, 100, 20);

          if (i == 1) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
          }
        }

        if (i == animationTexts.size()) {
          this.cancel();
          callback.run();
        }
      }
    }.runTaskTimer(plugin, 0L, 1L);

    for (Player p : players) {
      p.playSound(p.getLocation(), sound, 1, 1);
    }
  }

  @Override
  public void cancel() {
    // do nothing
  }

  public void setCallback(Runnable callback) {
    this.callback = callback;
  }
}
