package net.azisaba.lgwneo.match.component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 試合のステータスを表すEnum
 *
 * @author siloneco
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum MatchStatus {
  INITIALIZING("準備中"),
  WAITING("待機中"),
  STARTING("カウントダウン中"),
  PLAYING("試合中"),
  ENDING("終了処理中"),
  FINISHED("終了");

  @Getter
  private final String statusName;
}
