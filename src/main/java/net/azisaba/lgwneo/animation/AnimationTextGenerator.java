package net.azisaba.lgwneo.animation;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

@UtilityClass
public class AnimationTextGenerator {

  public static List<String> typing(String baseText) {
    List<String> list = new ArrayList<>();
    for (int i = 1; i < baseText.length(); i++) {
      if (baseText.charAt(i - 1) == ChatColor.COLOR_CHAR) {
        while (baseText.charAt(i + 1) == ChatColor.COLOR_CHAR) {
          i += 2;
        }
        continue;
      }
      list.add(baseText.substring(0, i + 1));
    }
    return list;
  }

  public static List<String> insertLeftToRight(String baseText, String insertText) {
    int i = 1;
    List<String> list = new ArrayList<>();
    while (i <= baseText.length()) {
      if (baseText.charAt(i - 1) == ChatColor.COLOR_CHAR) {
        while (i + 1 < baseText.length() && baseText.charAt(i + 1) == ChatColor.COLOR_CHAR) {
          i += 2;
        }
        i++;
        continue;
      }
      list.add(baseText.substring(0, i) + insertText + baseText.substring(i));
      i++;
    }

    return list;
  }

  public static List<String> insertRightToLeft(String baseText, String insertText) {
    int i = baseText.length();
    List<String> list = new ArrayList<>();
    while (i >= 1) {
      if (baseText.charAt(i - 1) == ChatColor.COLOR_CHAR) {
        while (i - 3 >= 0 && baseText.charAt(i - 3) == ChatColor.COLOR_CHAR) {
          i -= 2;
        }
        i--;
        continue;
      }
      list.add(baseText.substring(0, i) + insertText + baseText.substring(i));
      i--;
    }

    return list;
  }
}
