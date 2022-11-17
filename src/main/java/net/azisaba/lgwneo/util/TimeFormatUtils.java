package net.azisaba.lgwneo.util;

import lombok.experimental.UtilityClass;

/**
 * 時間のフォーマットに関するUtilityクラス
 *
 * @author siloneco
 */
@UtilityClass
public class TimeFormatUtils {

  /**
   * 秒数から日本語でmm分ss秒の形式に変換する
   *
   * @param seconds 変換したい秒数
   * @return mm分ss秒の形式に変換された文字列
   */
  public static String format(int seconds) {
    int min = seconds / 60;
    int sec = seconds % 60;

    if (min == 0) {
      return sec + "秒";
    } else if (sec == 0) {
      return min + "分";
    } else {
      return min + "分" + sec + "秒";
    }
  }
}
