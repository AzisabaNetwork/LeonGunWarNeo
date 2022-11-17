package net.azisaba.lgwneo.match.component;

import org.bukkit.ChatColor;
import org.bukkit.Color;

public enum MatchTeam {
  RED("赤チーム", Color.fromRGB(0x930000), ChatColor.DARK_RED),
  BLUE("青チーム", Color.fromRGB(0x0000A0), ChatColor.BLUE);

  private final String name;
  private final Color color;
  private final ChatColor chatColor;

  MatchTeam(String name, Color color, ChatColor chatColor) {
    this.name = name;
    this.color = color;
    this.chatColor = chatColor;
  }

  public String getTeamName() {
    return chatColor + name;
  }

  public String getTeamNameWithoutColor() {
    return name;
  }

  public ChatColor getChatColor() {
    return chatColor;
  }

  public Color getColor() {
    return color;
  }

  public MatchTeam getOppositeTeam() {
    // 赤の場合青を、青の場合赤を返す
    return this == RED ? BLUE : RED;
  }
}
